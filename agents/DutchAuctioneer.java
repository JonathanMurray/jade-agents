package agents;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("serial")
public class DutchAuctioneer extends FSMBehaviour{
	
	final String INFORM = "INFORM";
	final String SEND_CFP = "SEND_CFP";
	final String RECEIVE_PROP = "RECEIVE_PROP";
	final String INFORM_ABOUT_WINNER = "INFORM_ABOUT_WINNER";
	
	final int FAILED_AUCTION = 0;
	final int SUCCESSFUL_AUCTION = 1;
	final int NO_BID_YET = 2;
	
	private AbstractAgent agent;
	private List<AID> bidders;
	int highestBid;
	AID highestBidder;
	int currentPrice;
	AuctioneerStrategy strategy;
	
	public DutchAuctioneer(AbstractAgent agent, List<AID> bidders, AuctioneerStrategy strategy){
		this.bidders = bidders;
		this.agent = agent;
		this.strategy = strategy;
		
		currentPrice = strategy.startPrice;
		highestBid = 0;
		highestBidder = null;
		
		OneShotBehaviour informAuctionStart = sendToBidders(ACLMessage.INFORM, () -> Messages.AUCTION_START);
		OneShotBehaviour sendCFP = sendToBidders(ACLMessage.CFP, () -> {return "" + currentPrice;});
		ReceiveProposals receiveProposals = new ReceiveProposals();
		OneShotBehaviour informAboutWinner = informBiddersAboutWinner();
		
		registerFirstState(informAuctionStart, INFORM);
		registerState(sendCFP, SEND_CFP);
		registerState(receiveProposals, RECEIVE_PROP);
		registerLastState(informAboutWinner, INFORM_ABOUT_WINNER);
		
		registerDefaultTransition(INFORM, SEND_CFP);
		registerDefaultTransition(SEND_CFP, RECEIVE_PROP);
		registerTransition(RECEIVE_PROP, SEND_CFP, NO_BID_YET);
		registerTransition(RECEIVE_PROP, INFORM_ABOUT_WINNER, SUCCESSFUL_AUCTION);
		//TODO not handling failed auction yet.
	}
	
	private class ReceiveProposals extends SimpleBehaviour{
		private int receivedProposals = 0;
		
		public boolean done() {
			boolean done = receivedProposals >= bidders.size();
			if(done){
				reset(); //Don't change order. reset() breaks condition
				return true;
			}
			return false;
		}
		
		@Override
		public void reset() {
			receivedProposals = 0;
			super.reset();
		}
		
		@Override
		public int onEnd() {
			if(currentPrice <= strategy.minPrice){
				System.out.println("curPrice = " + currentPrice);
				System.out.println("");
				return FAILED_AUCTION;
			}else if(highestBidder != null){
				return SUCCESSFUL_AUCTION;
			}else{
				System.err.println("NO BID YET. LOWER PRICE TO " + (currentPrice - strategy.change));
				currentPrice -= strategy.change;
				return NO_BID_YET;
			}
		}
		
		@Override
		public void action() {
			Behaviours.receive(this, agent, proposalTemplate(), msg -> {
				final int bid = Integer.parseInt(msg.getContent());
				System.err.println("received bid " + bid + " from " + msg.getSender().getLocalName());
				if(bid >= currentPrice && bid > highestBid){
					highestBid = bid;
					highestBidder = msg.getSender();
					System.err.println("It's winning");
				}

				receivedProposals ++;
				System.out.println("now have received " + receivedProposals);
			});
		}
	}
	
	private MessageTemplate proposalTemplate(){
		return MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
	}
	
	private OneShotBehaviour informBiddersAboutWinner(){
		return new OneShotBehaviour() {
			public void action() {
				ACLMessage winnerMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				winnerMsg.addReceiver(highestBidder);
				winnerMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				agent.sendVerbose(winnerMsg);
				
				ACLMessage loserMsg = new ACLMessage(ACLMessage.REFUSE);
				loserMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				bidders.stream().filter(b -> b != highestBidder).forEach(loser -> {
					loserMsg.addReceiver(loser);
				});
				agent.sendVerbose(loserMsg);
				System.err.println("Informing bidders about winner");
			}
		};
	}
	
	private OneShotBehaviour sendToBidders(int performative, Supplier<String> content){
		return new OneShotBehaviour() {
			public void action() {
				ACLMessage msg = new ACLMessage(performative);
				msg.setContent(content.get());
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				for(AID bidder : bidders){
					msg.addReceiver(bidder);
				}
				agent.sendVerbose(msg);
			}
		};
	}
	
	public static class AuctioneerStrategy{
		int startPrice;
		int minPrice;
		int change;
		public AuctioneerStrategy(int startPrice, int minPrice, int change){
			this.startPrice = startPrice;
			this.minPrice = minPrice;
			this.change = change;
		}
	}
}
