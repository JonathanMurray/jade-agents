package agents;

import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREResponder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;


@SuppressWarnings("serial")
public class Curator extends AbstractAgent{
	private static final int UPDATE_ARTIFACTS_PERIOD_MS = 15000;

	public static final String CONVERSATION_ALL_ARIFACTS = "ALL_ARTIFACTS";
	public static final String CONVERSATION_ARTIFACT_INFO = "ARTIFACT_INFO";

	private HashMap<Integer, Artifact> artifacts = new HashMap<Integer, Artifact>();

	@Override
	public void setupWithoutArgs(){
		addBehaviour(new PeriodicallyUpdateArtifacts(UPDATE_ARTIFACTS_PERIOD_MS));
		MessageTemplate allArtifacts = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchConversationId(CONVERSATION_ALL_ARIFACTS)
				);
		MessageTemplate artifactInfo = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchConversationId(CONVERSATION_ARTIFACT_INFO)
				);
		addBehaviour(new ReplyAllArtifacts(allArtifacts));
		addBehaviour(new ReplyArtifactInfo(artifactInfo));
		subscribeTourGuide();
	}
	
	private void subscribeTourGuide(){
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tourOrganizer");
		dfd.addServices(sd);
		SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults(new Long(1));
		send(DFService.createSubscriptionMessage(this, getDefaultDF(), dfd, sc));
	}

	private class ReplyArtifactInfo extends SimpleAchieveREResponder{
		public ReplyArtifactInfo(MessageTemplate template) {
			super(Curator.this, template);
		}

		@Override
		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
			ACLMessage result = request.createReply();
			try {
				int requestedArtifactId = Integer.parseInt(request.getContent());
				result.setContentObject(artifacts.get(requestedArtifactId));
				result.setPerformative(ACLMessage.INFORM);
			} catch (IOException|NumberFormatException e) {
				e.printStackTrace();
				result.setPerformative(ACLMessage.FAILURE);
			}
			return result;
		}
	}

	private class ReplyAllArtifacts extends SimpleAchieveREResponder{

		public ReplyAllArtifacts(MessageTemplate template) {
			super(Curator.this, template);
		}

		@Override
		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
			ACLMessage result = request.createReply();
			try {
				result.setContentObject(artifacts);
				result.setPerformative(ACLMessage.INFORM);
			} catch (IOException e) {
				e.printStackTrace();
				result.setPerformative(ACLMessage.FAILURE);
			}
			return result;
		}
	}

	private class PeriodicallyUpdateArtifacts extends TickerBehaviour{

		public PeriodicallyUpdateArtifacts(long period) {
			super(Curator.this, period);
		}

		@Override
		protected void onTick() {
			Curator.this.artifacts.clear();

			Artifact[] array = new Artifact[]{
					new Artifact(1, "Bronze sword", "Olaf Stenhammar", 1247, "Gamla Uppsala"),
					new Artifact(2, "Mona Lisa", "Leonardo da Vinci", 1503, "Venice"),
					new Artifact(3, "ABBA album", "ABBA", 1976	, "Stockholm")
			};
			for(Artifact artifact : array){
				if(new Random().nextFloat() < 0.8){
					Curator.this.artifacts.put(artifact.id, artifact);
				}
			}
		}
	}
}
