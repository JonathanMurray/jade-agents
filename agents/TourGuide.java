package agents;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ProposeResponder;


@SuppressWarnings("serial")
public class TourGuide extends AbstractAgent{

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
	
	private static class ReceiveTourRequests extends ProposeResponder{
		
		private static MessageTemplate template = MessageTemplate.and(
			MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST) 
		);
		
		public ReceiveTourRequests(Agent a) {
			super(a, template);
		}
		
		
		
		@Override
		protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {
			String content = propose.getContent();
			int preferredNumItems = Integer.parseInt(content);
			System.out.println("got a request for a tour with " + preferredNumItems + " items");
			ACLMessage refuse = new ACLMessage(ACLMessage.REFUSE);
			refuse.setContent(Integer.toString(3));//allow max 3
			throw new RefuseException(refuse);
		}

//		@Override
//		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
//			try {
//				ACLMessage informTour = request.createReply();
//				informTour.setPerformative(ACLMessage.INFORM);
//				informTour.setContentObject(new Tour(new ArrayList<Integer>()));
//				return informTour;
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			return null;
//		}
	}

}
