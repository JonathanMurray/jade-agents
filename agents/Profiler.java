package agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.Iterator;
import java.util.Scanner;

@SuppressWarnings({ "serial" })
public class Profiler extends AbstractAgent{

	private static final int ASK_FOR_TOUR_PERIOD_MS = 5000;
	
	private String name = "Jonathan";
	private int age = 24;
	private String occupation = "Student";
	private DFAgentDescription[] tourOrganizers;

	@Override
	public void setupWithoutArgs(){
		prepareOrganizerSearch();
		addBehaviour(new PresentServices(1000));
//		addBehaviour(new ReceiveTours(this));
	}
	
	private class PresentServices extends WakerBehaviour{

		public PresentServices(long timeout) {
			super(Profiler.this, timeout);
		}
		
		public void onWake(){
			DFAgentDescription[] agents = findServices();
			if(agents.length > 0){
				
				System.out.println("The following services were found:");
				for(int i = 0; i < agents.length; i++){
					DFAgentDescription agent = agents[i];
					
					System.out.print(i + ": " + agent.getName().getLocalName() + "(");
					Iterator<ServiceDescription> it = agent.getAllServices();
					while(it.hasNext()){
						System.out.print(" " + it.next().getType());
					}
					System.out.println(")");
				}
				
				int choice = promptIntUntilCorrect("Write a number to choose one of them", 0, agents.length - 1);
				DFAgentDescription agent = agents[choice];
				System.out.println("How many artifacts would you like to visit?");
				
			}else{
				System.out.println("no organizer found.");
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
	
	private DFAgentDescription[] findServices(){
		try {
			DFAgentDescription[] r =  DFService.search(this, new DFAgentDescription());
			return r;
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void prepareOrganizerSearch(){
		DFAgentDescription tourOrganizer = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tourOrganizer");
		tourOrganizer.addServices(sd);
	}
}
