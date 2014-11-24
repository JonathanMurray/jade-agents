package agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ProposeInitiator;

import java.util.Scanner;

@SuppressWarnings({ "serial" })
public class Profiler extends AbstractAgent{

	private String name = "Jonathan";
	private int age = 24;
	private String occupation = "Student";
	private DFAgentDescription[] tourOrganizers;

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
			System.out.println("We will set up a guided tour for you.");
			addBehaviour(new PrepareForTour(1, 5));
		}
	}
	
	private class PrepareForTour extends OneShotBehaviour{
		int minNumItems = 1;
		int maxNumItems = 5;
		
		public PrepareForTour(int minNumItems, int maxNumItems) {
			super(Profiler.this);
			this.minNumItems = minNumItems;
			this.maxNumItems = maxNumItems;
		}
		
		public void action(){
			System.out.println("How many items would you like to visit on the guided tour?");
			
			int preferredNumItems = promptIntUntilCorrect("Please give a number between " + minNumItems + " and " + maxNumItems, minNumItems, maxNumItems);
			
			DFAgentDescription[] tourOrganizers = searchTourOrganizer();
			if(tourOrganizers.length > 0){
				DFAgentDescription organizer = tourOrganizers[0];
				System.out.println("Found tour organizer " + organizer.getName().getLocalName());
				addBehaviour(new AskForTour(Profiler.this, preferredNumItems));
			}else{
				System.out.println("no tour organizer found. Trying again later...");
				addBehaviour(new WakerBehaviour(Profiler.this, 1000) {
					public void onWake(){
						addBehaviour(new PrepareForTour(minNumItems, maxNumItems));
					}
				});
			}
		}
		
		private int promptIntUntilCorrect(String prompt, int min, int max){
			Scanner reader = new Scanner(System.in);
			while(true){
				System.out.println(prompt);
				int input = reader.nextInt();
				if(input >= min && input <= max){
					reader.close();
					return input;
				}
				System.out.println("Invalid input!");
			}
		}
		
	}
	
	
	
	private class AskForTour extends ProposeInitiator{

		public AskForTour(Profiler profiler, int preferredNumItems) {
			super(profiler, proposeTourMessage(preferredNumItems));
		}
		
		@Override
		protected void handleRejectProposal(ACLMessage reject_proposal) {
			String content = reject_proposal.getContent();
			int maxNumItems = Integer.parseInt(content);
			System.out.println("Unfortunately the tour organizer doesn't agree with the number of items.");
			addBehaviour(new PrepareForTour(1, maxNumItems));
		}
		
		@Override
		protected void handleAcceptProposal(ACLMessage accept_proposal) {
			Tour tour = null;
			try {
				tour = (Tour) accept_proposal.getContentObject();
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
			System.out.println("Got tour:  " + tour);
		}
	}
	
	private static ACLMessage proposeTourMessage(int preferredNumItems){
		ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
		msg.setContent(Integer.toString(preferredNumItems));
		return msg;
	}

	private class ReceiveTours extends CyclicBehaviour{

		public ReceiveTours() {
			super(Profiler.this);
		}
		
		@Override
		public void action() {
			MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			Behaviours.receive(this, myAgent, template, this::handleTourMessage);
		}
		
		private void handleTourMessage(ACLMessage msg) {
			try {
				System.out.println("Received message: " + msg.getContentObject());
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
		
	}

	private DFAgentDescription[] searchTourOrganizer(){
		DFAgentDescription tourOrganizer = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tourOrganizer");
		tourOrganizer.addServices(sd);
		DFAgentDescription[] found = new DFAgentDescription[]{};
		try {
			found = DFService.search(this, tourOrganizer);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return found;
	}
}
