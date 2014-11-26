package agents;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREResponder;
import jade.proto.SubscriptionInitiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


@SuppressWarnings("serial")
public class Curator extends AbstractAgent{
	private static final int UPDATE_ARTIFACTS_PERIOD_MS = 15000;
	private HashMap<Integer, Artifact> artifacts = new HashMap<Integer, Artifact>();
	private List<AID> tourGuides = new ArrayList<AID>();
	
	@Override
	public void setupWithoutArgs(){
		publishService(Services.CURATOR);
		updateArtifacts();
		addBehaviour(new TickerBehaviour(this, UPDATE_ARTIFACTS_PERIOD_MS) {
			protected void onTick() {
				updateArtifacts();
			}
		});
		MessageTemplate allArtifacts = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchConversationId(Conversations.REQUEST_ALL_ARTIFACTS)
				);
		MessageTemplate artifactInfo = MessageTemplate.and(
				MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
				MessageTemplate.MatchConversationId(Conversations.REQUEST_ARTIFACT_INFO)
				);
		addBehaviour(new ReplyAllArtifacts(allArtifacts));
		addBehaviour(new ReplyArtifactInfo(artifactInfo));
		subscribeTourGuide();
		
	}
	
	private void subscribeTourGuide(){
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(Services.TOUR_ORGANIZER);
		dfd.addServices(sd);
		
		addBehaviour( new SubscriptionInitiator( this, 
				DFService.createSubscriptionMessage( this, getDefaultDF(), dfd, null)) 
		{
			protected void handleInform(ACLMessage inform) {
				try {
					DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
					for(DFAgentDescription tourGuide : dfds){
						handleNewTourGuide(tourGuide.getName());
					}
				}
				catch (FIPAException fe) {fe.printStackTrace(); }
			}
		});
	}
	
	
	
	private void handleNewTourGuide(AID tourGuide){
		tourGuides.add(tourGuide);
		sendArtifactUpdateToTourGuide(tourGuide);
	}
	
	private void sendArtifactUpdateToTourGuide(AID tourGuide){
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				try{
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					ArrayList<Integer> artifactIds = getNewListWithAllIds();
					msg.setContentObject(artifactIds);
					msg.addReceiver(tourGuide);
					sendVerbose(msg);	
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
	}

	private class ReplyArtifactInfo extends SimpleAchieveREResponder{
		public ReplyArtifactInfo(MessageTemplate template) {
			super(Curator.this, template);
		}

		@Override
		protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response){
			ACLMessage result = request.createReply();
			result.setReplyWith(request.getReplyWith());
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
				result.setContentObject(getNewListWithAllIds());
				result.setPerformative(ACLMessage.INFORM);
			} catch (IOException e) {
				e.printStackTrace();
				result.setPerformative(ACLMessage.FAILURE);
			}
			return result;
		}
	}
	
	private ArrayList<Integer> getNewListWithAllIds(){
		ArrayList<Integer> artifactIds = new ArrayList<Integer>();
		artifactIds.addAll(artifacts.keySet());
		return artifactIds;
	}
	
	private void updateArtifacts(){
		artifacts.clear();

		Artifact[] array = new Artifact[]{
				new Artifact(1, "Bronze sword", "Olaf Stenhammar", 1247, "Gamla Uppsala"),
				new Artifact(2, "Mona Lisa", "Leonardo da Vinci", 1503, "Venice"),
				new Artifact(3, "ABBA album", "ABBA", 1976	, "Stockholm")
		};
		for(Artifact artifact : array){
			if(new Random().nextFloat() < 0.8){
				artifacts.put(artifact.id, artifact);
			}
		}
		System.err.println("[Curator upd. artifacts: " + artifacts.toString());
		for(AID tourGuide : tourGuides){
			sendArtifactUpdateToTourGuide(tourGuide);
		}
	}
}
