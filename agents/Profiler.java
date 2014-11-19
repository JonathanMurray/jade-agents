package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

@SuppressWarnings({ "serial" })
public class Profiler extends AbstractAgent{

	private static final int ASK_FOR_TOUR_PERIOD_MS = 5000;
	
	private String name = "Jonathan";
	private int age = 24;
	private String occupation = "Student";
	private DFAgentDescription tourOrganizer;

	@Override
	public void setupWithoutArgs(){
		prepareOrganizerSearch();
		TickerBehaviour askForTours = new TickerBehaviour(this, ASK_FOR_TOUR_PERIOD_MS) {
			@Override
			protected void onTick() {
				addBehaviour(new AskForTour(Profiler.this));
			}
		};
		addBehaviour(askForTours);
		addBehaviour(new ReceiveTours(this));
	}
	
	private static class AskForTour extends OneShotBehaviour{
		private Profiler profiler;
		public AskForTour(Profiler profiler) {
			super(profiler);
			this.profiler = profiler;
		}

		@Override
		public void action() {
			DFAgentDescription[] organizers = profiler.findTourOrganizers();
			if(organizers.length > 0){
				AID organizer = organizers[0].getName();
				ACLMessage askForTour = new ACLMessage(ACLMessage.REQUEST);
				askForTour.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				
				askForTour.addReceiver(organizer);
				profiler.sendVerbose(askForTour);
			}else{
				System.out.println("no organizer found.");
			}
		}
	}
	
	private static class ReceiveTours extends CyclicBehaviour{

		public ReceiveTours(Profiler profiler) {
			super(profiler);
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
	
	private DFAgentDescription[] findTourOrganizers(){
		try {
			DFAgentDescription[] r =  DFService.search(this, tourOrganizer);
			return r;
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void prepareOrganizerSearch(){
		tourOrganizer = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tourOrganizer");
		tourOrganizer.addServices(sd);
	}
}
