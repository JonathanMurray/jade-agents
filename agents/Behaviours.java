package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.function.Consumer;

public class Behaviours {
	
	public static void receive(Behaviour behaviour, Agent agent, MessageTemplate template, Consumer<ACLMessage> msgHandler){
		ACLMessage msg = agent.receive(template);
		if(msg != null){
			System.err.println(agent.getLocalName() + " <--[" + msg.getPerformative() + "]-- " +  msg.getSender().getLocalName());
			msgHandler.accept(msg);
		}else{
			behaviour.block();
		}
	}
}
