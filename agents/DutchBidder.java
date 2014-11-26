package agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import java.util.function.Consumer;

@SuppressWarnings("serial")
public class DutchBidder extends FSMBehaviour{
	
	private int proposal;
	
	public DutchBidder(Profiler agent, AID auctioneer, Consumer<Integer> successfulBidCallback){
		super(agent);
		String WAIT_FOR_START = "WAIT_FOR_START";
		String WAIT_FOR_CFP = "WAIT_FOR_OFFER";
		String WAIT_FOR_REPLY = "WAIT_FOR_OFFER_REPLY";
		String DONE = "DONE";
		
		CyclicBehaviour waitForStartMessage = new CyclicBehaviour() {
			public void action() {
				Behaviours.receive(this, agent, informStartOfAuction(), msg -> {agent.removeBehaviour(this);});
			}
		}; 
		
		AchieveREResponder waitForCFP = new AchieveREResponder(agent, auctionOffer()){
			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
				int offer = Integer.parseInt(request.getContent());
				if(offer <= 10){
					proposal = offer;
				}else{
					proposal = 0;
				}
				ACLMessage proposalMsg = new ACLMessage(ACLMessage.PROPOSE);
				proposalMsg.setContent("" + proposal);
				proposalMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION);
				proposalMsg.addReceiver(auctioneer);
				agent.sendVerbose(proposalMsg);
				return proposalMsg;
			}
		};
		
		SimpleBehaviour waitForReply = new SimpleBehaviour() {

			private boolean done = false;
			private boolean accepted = false;
			
			public boolean done() {
				return done;
			}
			
			@Override
			public int onEnd() {
				return accepted? 1 : 0;
			}
			
			public void action() {
				Behaviours.receive(this, agent, offerReply(), msg -> {
					if(msg.getPerformative() == ACLMessage.REFUSE){
						accepted = false;
					}else if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
						accepted = true;
					}
					done = true;
				});
			}
		};
		
		OneShotBehaviour done = new OneShotBehaviour() {
			public void action() {
				successfulBidCallback.accept(proposal);
			}
		};
		
		registerFirstState(waitForStartMessage, WAIT_FOR_START);
		registerState(waitForCFP, WAIT_FOR_CFP);
		registerState(waitForReply, WAIT_FOR_REPLY);
		registerLastState(done, DONE);
		
		registerDefaultTransition(WAIT_FOR_START, WAIT_FOR_CFP);
		registerDefaultTransition(WAIT_FOR_CFP, WAIT_FOR_REPLY);
		registerTransition(WAIT_FOR_REPLY, WAIT_FOR_CFP, 0);
		registerTransition(WAIT_FOR_REPLY, DONE, 1);
	}
	
	private MessageTemplate informStartOfAuction(){
		return MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
				MessageTemplate.MatchContent(Messages.AUCTION_START)
		);
	}
	
	private MessageTemplate auctionOffer(){
		return MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
				MessageTemplate.MatchPerformative(ACLMessage.CFP)
		);
	}
	
	private MessageTemplate offerReply(){
		return MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_DUTCH_AUCTION),
				MessageTemplate.or(
						MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
						MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
				)
		); 
	}
}
