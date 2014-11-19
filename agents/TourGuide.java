package agents;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import java.io.IOException;
import java.util.ArrayList;


public class TourGuide extends AbstractAgent{

	private static final long serialVersionUID = 1L;

	@Override
	public void setupWithoutArgs(){
		try {
			publishService();
			addBehaviour(new ReceiveTourRequests(this));
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private void publishService() throws FIPAException{
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName( getAID() );
		ServiceDescription sd = new ServiceDescription();
		sd.setType( "tourOrganizer" );
		sd.setName( getLocalName() );
		dfd.addServices(sd);
		DFService.register(this, dfd ); 
	}
	
	@SuppressWarnings("serial")
	private static class ReceiveTourRequests extends AchieveREResponder{
		
		private static MessageTemplate template = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST) 
		);
		
		public ReceiveTourRequests(Agent a) {
			super(a, template);
		}

		@Override
		protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
			ACLMessage agree = request.createReply();
			agree.setPerformative(ACLMessage.AGREE);
			return agree;
		}

		@Override
		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
			try {
				ACLMessage informTour = request.createReply();
				informTour.setPerformative(ACLMessage.INFORM);
				informTour.setContentObject(new Tour(new ArrayList<Integer>()));
				return informTour;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

}
