package agents;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("serial")
public class DutchAuctioneer extends FSMBehaviour{
	
	final static String STATE_INFORM_START = "INFORM_START";
	final static String STATE_SEND_REQUEST = "SEND_CFP";
	final static String STATE_AWAIT_BIDS = "RECEIVE_PROP";
	final static String STATE_REFUSE_BIDS = "REFUSE_BIDS";
	final static String STATE_INFORM_ABOUT_WINNER = "INFORM_ABOUT_WINNER";
	final static String STATE_INFORM_FAILED_AUCTION = "INFORM_FAIL";
	
	final static int FAILED_AUCTION = 0;
	final static int SUCCESSFUL_AUCTION = 1;
	final static int NO_BID_YET = 2;
	
	private AbstractAgent agent;
	private List<AID> bidders;
	private int highestBid;
	private AID highestBidder;
	private int currentPrice;
	private AuctioneerStrategy strategy;
	private int artifactId;
	
	public DutchAuctioneer(AbstractAgent agent, List<AID> bidders, int artifactId, AuctioneerStrategy strategy){
		this.bidders = bidders;
		this.agent = agent;
		this.strategy = strategy;
		this.artifactId = artifactId;
		
		currentPrice = strategy.startPrice;
		highestBid = 0;
		highestBidder = null;
		
		
		OneShotBehaviour informAuctionStart = new InformAuctionStart();
		OneShotBehaviour sendRequest = new SendRequest();
		SimpleBehaviour awaitBids = new AwaitBids();
		OneShotBehaviour refuseBids = new RefuseBids();
		OneShotBehaviour informAboutWinner = new InformAboutWinner();
		OneShotBehaviour informFailedAuction = new InformFailedAuction();
		
		registerFirstState(informAuctionStart, STATE_INFORM_START);
		registerState(sendRequest, STATE_SEND_REQUEST);
		registerState(awaitBids, STATE_AWAIT_BIDS);
		registerState(refuseBids, STATE_REFUSE_BIDS);
		registerLastState(informAboutWinner, STATE_INFORM_ABOUT_WINNER);
		registerLastState(informFailedAuction, STATE_INFORM_FAILED_AUCTION);
		
		registerDefaultTransition(STATE_INFORM_START, STATE_SEND_REQUEST);
		registerDefaultTransition(STATE_SEND_REQUEST, STATE_AWAIT_BIDS);
		registerTransition(STATE_AWAIT_BIDS, STATE_REFUSE_BIDS, NO_BID_YET);
		registerTransition(STATE_AWAIT_BIDS, STATE_INFORM_ABOUT_WINNER, SUCCESSFUL_AUCTION);
		registerTransition(STATE_AWAIT_BIDS, STATE_INFORM_FAILED_AUCTION, FAILED_AUCTION);
		registerDefaultTransition(STATE_REFUSE_BIDS, STATE_SEND_REQUEST);
		
		//TODO not handling failed auction yet.
	}
	
	private class InformAuctionStart extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setConversationId(Conversations.AUCTION_START);
			msg.setContent("" + artifactId);
			sendDutchMsg(msg, bidders);
		}
	}
	
	private class SendRequest extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("" + currentPrice);
			msg.setReplyWith("" + currentPrice);
			sendDutchMsg(msg, bidders);
		}
	}
	
	private class AwaitBids extends SimpleBehaviour{
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
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.and(
							MessageTemplate.MatchPerformative(ACLMessage.INFORM),
							MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION)
						),
						MessageTemplate.MatchInReplyTo("" + currentPrice)
					);
			Behaviours.receive(this, agent, template, this::handleBid);
		}
		
		private void handleBid(ACLMessage msg){
			final int bid = Integer.parseInt(msg.getContent());
			if(bid >= currentPrice && bid > highestBid){
				highestBid = bid;
				highestBidder = msg.getSender();
				System.err.println("It's winning");
			}
			receivedProposals ++;
		}
	}
	
	private class RefuseBids extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.REFUSE);
			sendDutchMsg(msg, bidders);
		}
	}
	
	private class InformAboutWinner extends OneShotBehaviour{
		public void action() {
			ACLMessage winnerMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
			sendDutchMsg(winnerMsg, highestBidder);
			
			ACLMessage loserMsg = new ACLMessage(ACLMessage.REFUSE);
			loserMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
			bidders.stream().filter(b -> !b.equals(highestBidder)).forEach(loser -> {
				loserMsg.addReceiver(loser);
			});
			agent.sendVerbose(loserMsg);
			System.err.println("Informing bidders about winner.");
		}
	}
	
	private class InformFailedAuction extends OneShotBehaviour{
		public void action() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent(Messages.AUCTION_FAILED);
			sendDutchMsg(msg, bidders);
		}
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
	
	private void sendDutchMsg(ACLMessage msg, Collection<AID> receivers){
		msg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
		for(AID receiver : receivers){
			msg.addReceiver(receiver);
		}
		agent.sendVerbose(msg);
	}
	
	private void sendDutchMsg(ACLMessage msg, AID receiver){
		sendDutchMsg(msg, Arrays.asList(new AID[]{receiver}));
	}
}
