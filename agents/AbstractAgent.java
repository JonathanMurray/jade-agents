package agents;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

import java.util.Arrays;

/**
 * Override the setup method with the desired number of arguments.
 * @author Jonathan
 *
 */
@SuppressWarnings("serial")
public class AbstractAgent extends Agent{

	@Override
	public void takeDown(){
		System.err.println("[takeDown " + getLocalName() + "]");
		//TODO should deregister any services (?)
		super.takeDown();
	}
	
	@Override
	public final void setup(){
		Object[] args = getArguments();
		System.err.println("[setup " + getLocalName() + arrayToString(args) + "]");
		if(args == null){
			setupWithoutArgs();
		}else{
			switch(args.length){
			case 0:
				setupWithoutArgs();
				break;
			case 1:
				setup(args[0]);
				break;
			case 2:
				setup(args[0], args[1]);
				break;
			case 3:
				setup(args[0], args[1], args[2]);
				break;
			default:
				wrongNumArgs(args.length);
			}
		}
	}
	
	public void setupWithoutArgs(){
		wrongNumArgs(0);
	}
	
	public void setup(Object arg1){
		wrongNumArgs(1);
	}
	
	public void setup(Object arg1, Object arg2){
		wrongNumArgs(2);
	}
	
	public void setup(Object arg1, Object arg2, Object arg3){
		wrongNumArgs(3);
	}
	
	private void wrongNumArgs(int numArgs){
		throw new IllegalArgumentException("There is no setup method that accepts " + numArgs + " arguments.");
	}
	
	public void sendVerbose(ACLMessage msg){
		System.err.println(Messages.debugSendMessage(this, msg));
		send(msg);
	}
	
	public void publishService(String serviceType){
		try {
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName( getAID() );
			ServiceDescription sd = new ServiceDescription();
			sd.setType(serviceType);
			sd.setName( getLocalName() );
			dfd.addServices(sd);
			DFService.register(this, dfd );
		} catch (FIPAException e) {
			e.printStackTrace();
		} 
	}
	
	public DFAgentDescription[] findService(String serviceType){
		DFAgentDescription agent = new DFAgentDescription();
		ServiceDescription service = new ServiceDescription();
		service.setType(serviceType);
		agent.addServices(service);
		DFAgentDescription[] found = new DFAgentDescription[]{};
		try {
			found = DFService.search(this, agent);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		return found;
	}
	
	private String arrayToString(Object[] array){
		if(array == null){
			return "";
		}
		return Arrays.toString(array);
	}
}
