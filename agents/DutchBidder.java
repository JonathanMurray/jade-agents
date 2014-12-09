package agents;

import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.BiConsumer;

@SuppressWarnings("serial")
public class DutchBidder extends FSMBehaviour{
	
	private final static int BID_ACCEPTED = 0;
	private final static int SOMEONE_ELSE_WON = 1;
	
	private final static String STATE_BEFORE_START = "WAIT_FOR_START";
	private final static String STATE_BIDDING = "WAIT_FOR_OFFER";
	private final static String STATE_BID_ACCEPTED = "BID_ACCEPTED";
	private final static String STATE_SOMEONE_ELSE_WON = "SOMEONE_ELSE_WON";
	
	private int bid;
	private AbstractAgent agent;
	private BiConsumer<Integer, Integer> successfulBidCallback;
	private BiConsumer<Integer, Integer> someoneElseWonCallback;
	private int willingToPay;
	private int artifactId;
	
	/**
	 * 
	 * @param agent
	 * @param willingToPay
	 * @param successfulBidCallback (artifactId, bid)
	 */
	public DutchBidder(AbstractAgent agent, int willingToPay, BiConsumer<Integer, Integer> successfulBidCallback, BiConsumer<Integer,Integer> someoneElseWonCallback){
		super(agent);
		
		this.agent = agent;
		this.successfulBidCallback = successfulBidCallback;
		this.someoneElseWonCallback = someoneElseWonCallback;
		this.willingToPay = willingToPay;
		
		SimpleBehaviour beforeStart = new BeforeStart();
		SimpleBehaviour doBidding = new DoBidding(); 
		OneShotBehaviour bidAccepted = new BidAccepted();
		OneShotBehaviour someoneElseWon = new SomeoneElseWon();
		
		registerFirstState(beforeStart, STATE_BEFORE_START);
		registerState(doBidding, STATE_BIDDING);
		registerLastState(bidAccepted, STATE_BID_ACCEPTED);
		registerLastState(someoneElseWon, STATE_SOMEONE_ELSE_WON);
		
		registerDefaultTransition(STATE_BEFORE_START, STATE_BIDDING);
		registerTransition(STATE_BIDDING, STATE_BID_ACCEPTED, BID_ACCEPTED);
		registerTransition(STATE_BIDDING, STATE_SOMEONE_ELSE_WON, SOMEONE_ELSE_WON);
	}
	
	private class BeforeStart extends SimpleBehaviour{
		private boolean done;
		public boolean done(){
			return done;
		}
		public void action() {
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.MatchConversationId(Conversations.AUCTION_START)
			);
			ACLMessage msg = Behaviours.receive(DutchBidder.this.getAgent(), template);
			if(msg == null){
				block();
				return;
			}
			done = true;
			artifactId = Integer.parseInt(msg.getContent());
		}
	}

	private class DoBidding extends SimpleBehaviour{
		private int state = 0;
		private boolean bidAccepted = false;
		private boolean someoneElseWon = false;
		
		@Override
		public void action() {
			switch(state){
			case 0:
				if(receiveRequest()){
					state = 1;
				}
				break;
			case 1:
				if(receiveBidResponse()){
					state = 0;
				}
				break;
			}
		}
		
		@Override
		public void onStart() {
			System.out.println(agent.getLocalName() + " start do bidding");
			super.onStart();
		}
		
		@Override
		public boolean done() {
			return bidAccepted || someoneElseWon;
		}
		
		@Override
		public int onEnd() {
			if(bidAccepted){
				return BID_ACCEPTED;
			}else if(someoneElseWon){
				return SOMEONE_ELSE_WON;
			}else{
				throw new RuntimeException();
			}
		}
		
		private boolean receiveRequest(){
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
			);
			ACLMessage msg = Behaviours.receive( getAgent(), template);
			if(msg == null){
				block();
				return false;
			}
			handleRequest(msg);
			return true;
		}
		
		private void handleRequest(ACLMessage msg){
			int offer = Integer.parseInt(msg.getContent());
			if(offer <= willingToPay){
				bid = offer;
			}else{
				bid = 0;
			}
			ACLMessage bidMsg = msg.createReply();
			bidMsg.setPerformative(ACLMessage.INFORM);
			bidMsg.setContent("" + bid);
			bidMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
			agent.sendVerbose(bidMsg);
		}
		
		private boolean receiveBidResponse(){
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
							MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
					)
			); 
			ACLMessage msg = Behaviours.receive(agent, template);
			if(msg == null){
				block();
				return false;
			}
			handleBidResponse(msg);
			return true;
		}
		
		private void handleBidResponse(ACLMessage msg){
			if(msg.getPerformative() == ACLMessage.REFUSE){
				if(Messages.SOMEONE_ELSE_WON.equals(msg.getContent())){
					someoneElseWon = true;
				}
			}else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
				bidAccepted = true;
			}
		}
	}
	
	private class BidAccepted extends OneShotBehaviour{
		public void action() {
			successfulBidCallback.accept(artifactId, bid);
		}
	}
	
	private class SomeoneElseWon extends OneShotBehaviour{
		public void action(){
			someoneElseWonCallback.accept(artifactId, bid);
		}
	}
	
}
