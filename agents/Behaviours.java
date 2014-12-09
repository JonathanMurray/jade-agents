package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

public class Behaviours {
	
	//TODO
//	public static void receive(Behaviour behaviour, Agent agent, MessageTemplate template, Consumer<ACLMessage> msgHandler){
//		while(true){
//			if(agent.getName().contains("Clone2")){
//				ACLMessage tmp = agent.receive(MessageTemplate.MatchConversationId(Conversations.AUCTION_START));
//				System.out.println(agent.getName() + " RECEIVED: \n" +  tmp);
//				agent.putBack(tmp);
//			}
//			
//			ACLMessage msg = agent.receive(template);
//			if(msg != null){
////				System.err.println(Messages.debugReceiveMessage(agent, msg));
//				msgHandler.accept(msg);
//				return;
//			}else{
//				behaviour.block();
//			}
//		}
//	}
	
	public static ACLMessage receive(Agent agent, MessageTemplate template){
		ACLMessage msg = agent.receive(template);
		if(msg != null){
			System.err.println(Messages.debugReceiveMessage(agent, msg));
		}
		return msg;
	}
}
