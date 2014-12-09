package agents;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ProposeResponder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


@SuppressWarnings("serial")
public class TourGuide extends AbstractAgent{
	
	List<Integer> artifacts = new ArrayList<Integer>();

	@Override
	public void setupWithoutArgs(){
		publishService(Services.TOUR_ORGANIZER);
		addBehaviour(new ReceiveTourRequests(this));
		addBehaviour(new ReceiveArtifactUpdates(this));
	}

	private class ReceiveArtifactUpdates extends CyclicBehaviour{

		public ReceiveArtifactUpdates(TourGuide tourGuide){
			super(tourGuide);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = Behaviours.receive(myAgent, template);
			if(msg == null){
				block();
				return;
			}
			try {
				artifacts = (List<Integer>) msg.getContentObject();
				System.err.println("[tourguide upd. artifacts.");
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ReceiveTourRequests extends ProposeResponder{
		
		
		public ReceiveTourRequests(Agent a) {
			super(a, MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
		}
		
		@Override
		protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
			try{
				String content = propose.getContent();
				int preferredNumItems = Integer.parseInt(content);
				System.err.println("[got a request for a tour with " + preferredNumItems + " items]");
				if(artifacts.size() >= preferredNumItems){
					ACLMessage accept = propose.createReply();
					accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
					accept.setContentObject(createTour(preferredNumItems));
					return accept;
				}else{
					ACLMessage refuse = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					refuse.setContent(Integer.toString(artifacts.size()));
					throw new RefuseException(refuse);
				}
			}catch(IOException e){
				e.printStackTrace();	
			}
			
			throw new RuntimeException("Should not be reached");
		}
	}
	
	private Tour createTour(int numItems){
		return new Tour(new ArrayList<Integer>(artifacts.subList(0, numItems)));
	}

}
