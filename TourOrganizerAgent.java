import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;


public class TourOrganizerAgent extends AbstractAgent{

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("serial")
	@Override
	public void setupWithoutArgs(){

		MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST) 
				);

		addBehaviour(new AchieveREResponder(this, template) {
			protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
				System.out.println("Agent "+getLocalName()+": REQUEST received from "+request.getSender().getName()+". Action is "+request.getContent());
				if (checkAction()) {
					System.out.println("Agent "+getLocalName()+": Agree");
					ACLMessage agree = request.createReply();
					agree.setPerformative(ACLMessage.AGREE);
					return agree;
				}
				else {
					System.out.println("Agent "+getLocalName()+": Refuse");
					throw new RefuseException("check-failed");
				}
			}

			protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
				if (performAction()) {
					System.out.println("Agent "+getLocalName()+": Action successfully performed");
					ACLMessage inform = request.createReply();
					inform.setPerformative(ACLMessage.INFORM);
					return inform;
				}
				else {
					System.out.println("Agent "+getLocalName()+": Action failed");
					throw new FailureException("unexpected-error");
				}	
			}
		} );
	}

	private boolean checkAction() {
		// Simulate a check by generating a random number
		return (Math.random() > 0.2);
	}

	private boolean performAction() {
		// Simulate action execution by generating a random number
		return (Math.random() > 0.2);
	}
}
