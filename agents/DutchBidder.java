package agents;

import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

@SuppressWarnings("serial")
public class DutchBidder extends FSMBehaviour{
	
	private final static String STATE_BEFORE_START = "WAIT_FOR_START";
	private final static String STATE_BIDDING = "WAIT_FOR_OFFER";
	private final static String STATE_DONE = "DONE";
	
	private int bid;
	private AID auctioneer;
	private AbstractAgent agent;
	private Consumer<Integer> successfulBidCallback;
	
	public DutchBidder(AbstractAgent agent, AID auctioneer, Consumer<Integer> successfulBidCallback){
		super(agent);
		
		this.auctioneer = auctioneer;
		this.agent = agent;
		this.successfulBidCallback = successfulBidCallback;
		
		SimpleBehaviour beforeStart = new BeforeStart();
		SimpleBehaviour doBidding = new DoBidding(); 
		OneShotBehaviour done = new Done();
		
		registerFirstState(beforeStart, STATE_BEFORE_START);
		registerState(doBidding, STATE_BIDDING);
		registerLastState(done, STATE_DONE);
		
		registerDefaultTransition(STATE_BEFORE_START, STATE_BIDDING);
		registerDefaultTransition(STATE_BIDDING, STATE_DONE);
	}
	
	private class BeforeStart extends SimpleBehaviour{
		private boolean done;
		public boolean done(){
			return done;
		}
		public void action() {
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.MatchContent(Messages.AUCTION_START)
			);
			Behaviours.receive(this, DutchBidder.this.getAgent(), template, msg -> {done = true;});
		}
	}

	private class DoBidding extends SimpleBehaviour{
		private int state = 0;
		private boolean bidAccepted = false;
		
		@Override
		public void action() {
			switch(state){
			case 0:
				receiveRequest();
				state = 1;
				break;
			case 1:
				receiveBidResponse();
				state = 0;
				break;
			}
		}
		
		@Override
		public boolean done() {
			return bidAccepted;
		}
		
		private void receiveRequest(){
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
			);
			Behaviours.receive(this, getAgent(), template, this::handleRequest);
		}
		
		private void handleRequest(ACLMessage msg){
			int offer = Integer.parseInt(msg.getContent());
			if(offer <= 60){
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
		
		private void receiveBidResponse(){
			MessageTemplate template = MessageTemplate.and(
					MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
					MessageTemplate.or(
							MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
							MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
					)
			); 
			Behaviours.receive(this, agent, template, this::handleBidResponse);
		}
		
		private void handleBidResponse(ACLMessage msg){
			if(msg.getPerformative() == ACLMessage.REFUSE){
				
			}else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
				bidAccepted = true;
			}
		}
	}
	
	private class Done extends OneShotBehaviour{
		public void action() {
			successfulBidCallback.accept(bid);
		}
	}
	
}
