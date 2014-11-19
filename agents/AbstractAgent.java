package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Override the setup method with the desired number of arguments.
 * @author Jonathan
 *
 */
public class AbstractAgent extends Agent{

	private static final long serialVersionUID = 1L;
	
	@Override
	public final void setup(){
		Object[] args = getArguments();
		System.out.println("setup " + getLocalName() + arrayToString(args));
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
		String log = getName() + " ---> ";
		@SuppressWarnings("rawtypes")
		Iterator receivers = msg.getAllReceiver();
		while(receivers.hasNext()){
			AID receiver = (AID) receivers.next();
			log += receiver.getName() + " ";
		}
		System.out.println(log);
		send(msg);
	}
	
	private String arrayToString(Object[] array){
		if(array == null){
			return "";
		}
		return Arrays.toString(array);
	}
}
