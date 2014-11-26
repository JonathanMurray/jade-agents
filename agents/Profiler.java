package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.proto.ProposeInitiator;
import jade.proto.SimpleAchieveREInitiator;

@SuppressWarnings({ "serial" })
public class Profiler extends AbstractAgent{

	private String name = "Jonathan";
	private int age = 24;
	private String occupation = "Student";
	private DFAgentDescription[] tourOrganizers;
	private AID curator;

	@Override
	public void setupWithoutArgs(){
		addBehaviour(new Welcome(1000));
	}
	
	private class Welcome extends WakerBehaviour{
		public Welcome(long timeout){
			super(Profiler.this, timeout);
		}
		
		public void onWake(){
			System.out.println("Welcome " + name);
			System.out.println("SETUP GUIDED TOUR: ");
			System.out.println("--------------------------------------");
			addBehaviour(new PrepareTourRequest(1, 5));
		}
	}
	
	private class PrepareTourRequest extends OneShotBehaviour{
		int minNumItems = 1;
		int maxNumItems = 5;
		
		public PrepareTourRequest(int minNumItems, int maxNumItems) {
			super(Profiler.this);
			this.minNumItems = minNumItems;
			this.maxNumItems = maxNumItems;
		}
		
		public void action(){
			System.out.println("How many artifacts to visit?");
			
			int preferredNumItems = UserInput.promptIntUntilCorrect("Enter a number between " + minNumItems + " and " + maxNumItems + ":", minNumItems, maxNumItems);
			
			DFAgentDescription[] tourOrganizers = findService(Services.TOUR_ORGANIZER);
			if(tourOrganizers.length > 0){
				DFAgentDescription organizer = tourOrganizers[0];
				System.err.println("Found tour organizer " + organizer.getName().getLocalName());
				addBehaviour(new RequestTour(Profiler.this, preferredNumItems, organizer));
			}else{
				System.out.println("No tour organizer found. Trying again later...");
				addBehaviour(new WakerBehaviour(Profiler.this, 2000) {
					public void onWake(){
						addBehaviour(new PrepareTourRequest(minNumItems, maxNumItems));
					}
				});
			}
		}
	}
	
	private class RequestTour extends ProposeInitiator{

		public RequestTour(Profiler profiler, int preferredNumItems, DFAgentDescription organizer) {
			super(profiler, proposeTourMessage(preferredNumItems, organizer));
		}
		
		@Override
		protected void handleRejectProposal(ACLMessage reject_proposal) {
			String content = reject_proposal.getContent();
			int maxNumItems = Integer.parseInt(content);
			System.out.println("Organizer says no!");
			addBehaviour(new PrepareTourRequest(1, maxNumItems));
		}
		
		@Override
		protected void handleAcceptProposal(ACLMessage accept_proposal) {
			try {
				Tour tour = (Tour) accept_proposal.getContentObject();
				handleReceivedTour(tour);
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void handleReceivedTour(Tour tour){
		System.out.println("------------------------------");
		System.out.println("TOUR:  " + tour);
		for(Integer artifactId : tour){
			addBehaviour(new GetInfoAboutArtifact(this, artifactId));
		}
	}
	
	private class GetInfoAboutArtifact extends SimpleAchieveREInitiator{

		public GetInfoAboutArtifact(Agent a, int artifactId) {
			super(a, getInfoAboutArtifactMessage(artifactId));
		}
		
		@Override
		protected void handleInform(ACLMessage msg) {
			try {
				System.out.println("Artifact-" + msg.getReplyWith() + ": " + msg.getContentObject());
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}
	
	private ACLMessage getInfoAboutArtifactMessage(int artifactId){
		ensureHasCurator();
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(curator);
		msg.setContent("" + artifactId);
		msg.setConversationId(Conversations.REQUEST_ARTIFACT_INFO);
		msg.setReplyWith("" + artifactId);
		return msg;
	}
	
	private void ensureHasCurator(){
		if(curator == null){
			DFAgentDescription[] curators = findService(Services.CURATOR);
			if(curators.length > 0){
				curator = curators[0].getName();
			}
		}
	}
	
	private static ACLMessage proposeTourMessage(int preferredNumItems, DFAgentDescription organizer){
		ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
		msg.setContent(Integer.toString(preferredNumItems));
		msg.addReceiver(organizer.getName());
		return msg;
	}

}
