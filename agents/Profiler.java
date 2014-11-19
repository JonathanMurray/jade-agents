package agents;

import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings({ "serial" })
public class Profiler extends AbstractAgent{

	private static final int ASK_FOR_TOUR_PERIOD_MS = 5000;
	
	private String name;
	private int age;
	private String occupation;
	private DFAgentDescription tourOrganizer;

	@Override
	public void setupWithoutArgs(){
		setupTourOrganizer();
		TickerBehaviour askForTours = new TickerBehaviour(this, ASK_FOR_TOUR_PERIOD_MS) {
			
			@Override
			protected void onTick() {
				DFAgentDescription[] organizers = findTourOrganizers();
				if(organizers.length > 0){
					AID organizer = organizers[0].getName();
					ACLMessage askForTour = new ACLMessage(ACLMessage.REQUEST);
					askForTour.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					
					askForTour.addReceiver(organizer);
					sendVerbose(askForTour);
				}else{
					System.out.println("no organizer found.");
				}
			}
		};
		addBehaviour(askForTours);
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
	
	private void setupTourOrganizer(){
		tourOrganizer = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tourOrganizer");
		tourOrganizer.addServices(sd);
	}
}
