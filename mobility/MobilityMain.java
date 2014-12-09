package mobility;

import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.Location;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import agents.DutchAuctioneer.AuctioneerStrategy;

@SuppressWarnings("serial")
public class MobilityMain extends Agent{
	
	public static HashMap<String, Location> locations = new HashMap<String, Location>();
	public static jade.wrapper.AgentContainer HMContainer;
	public static jade.wrapper.AgentContainer GContainer;
	public static String G = "G";
	public static String HM = "HM";
	
	public static String getOther(String containerName){
		if(containerName.equals(HM)){
			return G;
		}else if(containerName.equals(G)){
			return HM;
		}
		throw new IllegalArgumentException("" + containerName);
	}
	
	@Override
	public void setup(){
		try {
			setupContainers();
			setupLocations();
			setupAgents();
		} catch (CodecException | OntologyException | ControllerException e) {
			e.printStackTrace();
		}
	}
	
	private void setupContainers(){
		jade.core.Runtime runtime = jade.core.Runtime.instance();
		Profile HMProfile = new ProfileImpl(false);
		HMProfile.setParameter(Profile.CONTAINER_NAME, HM);
		Profile GProfile = new ProfileImpl(false);
		GProfile.setParameter(Profile.CONTAINER_NAME, G);
		HMContainer = runtime.createAgentContainer(HMProfile);
		GContainer = runtime.createAgentContainer(GProfile);
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
	}
	
	private void setupLocations() throws UngroundedException, CodecException, OntologyException{
		sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));
        MessageTemplate mt = MessageTemplate.and(
			                  MessageTemplate.MatchSender(getAMS()),
			                  MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage resp = blockingReceive(mt);
        ContentElement ce = getContentManager().extractContent(resp);
        Result result = (Result) ce;
        jade.util.leap.Iterator it = result.getItems().iterator();
        while (it.hasNext()) {
           Location loc = (Location)it.next();
           locations.put(loc.getName(), loc);
        }
        System.out.println(locations);
	}
	
	
	
	private void setupAgents() throws ControllerException{
		List<AgentController> bidders = new ArrayList<AgentController>();
		bidders.add(HMContainer.createNewAgent("HM1", MobileBidder.class.getName(), new Object[]{HM}));
		bidders.add(HMContainer.createNewAgent("HM2", MobileBidder.class.getName(), new Object[]{HM}));
		bidders.add(GContainer.createNewAgent("G1", MobileBidder.class.getName(), new Object[]{G}));
		bidders.add(GContainer.createNewAgent("G2", MobileBidder.class.getName(), new Object[]{G}));
		for(AgentController bidder : bidders){
			bidder.start();
		}
		
		ContainerController cc = getContainerController();	
		AgentController auctioneer = cc.createNewAgent("A", MobileAuctioneer.class.getName(), new Object[]{0, new AuctioneerStrategy(100,30, 10), cc.getContainerName()});
		auctioneer.start();
	}
	
	private void sendRequest(Action action) {
      ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
      request.setLanguage(new SLCodec().getName());
      request.setOntology(MobilityOntology.getInstance().getName());
      try {
	     getContentManager().fillContent(request, action);
	     request.addReceiver(action.getActor());
	     send(request);
	  }
	  catch (Exception ex) { ex.printStackTrace(); }
   }

}
