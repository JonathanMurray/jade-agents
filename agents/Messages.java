package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import java.lang.reflect.Field;
import java.util.Iterator;

public class Messages {
	public static String AUCTION_START = "AUCTION_START";
	
	public static String performativeStr(int performative){
		try {
			for(Field f : Class.forName("jade.lang.acl.ACLMessage").getFields()){
				if(f.getType().equals(int.class) && f.getInt(null) == performative){
					return f.getName();
				}
			}
		} catch (SecurityException | ClassNotFoundException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Should't reach this");
	}
	
	private static String debugMessageContent(ACLMessage msg){
		String content;
		int len = 25;
		if(msg.getContent().length() > len){
			content = msg.getContent().substring(0, len) + "...";
		}else{
			content = msg.getContent();
		}
		return content;
	}
	
	public static String debugSendMessage(Agent sender, ACLMessage msg){
		String content = debugMessageContent(msg);
		String log = sender.getLocalName() + " ---[" + Messages.performativeStr(msg.getPerformative()) + ": " + content + "]--> ";
		@SuppressWarnings("rawtypes")
		Iterator receivers = msg.getAllReceiver();
		while(receivers.hasNext()){
			AID receiver = (AID) receivers.next();
			log += receiver.getLocalName() + " ";
		}
		return log;
	}
	
	public static String debugReceiveMessage(Agent receiver, ACLMessage msg){
		String content = debugMessageContent(msg);
		String log = receiver.getLocalName() + " <---[" + Messages.performativeStr(msg.getPerformative()) + ": " + content + "]-- ";
		log += msg.getSender().getLocalName();
		return log;
	}
}
