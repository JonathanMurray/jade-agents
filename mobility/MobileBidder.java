package mobility;

import jade.content.lang.sl.SLCodec;
import jade.core.Location;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.mobility.MobilityOntology;
import jade.wrapper.ControllerException;

import java.io.Serializable;
import java.util.Random;
import java.util.function.BiConsumer;

import agents.AbstractAgent;
import agents.DutchBidder;

@SuppressWarnings("serial")
public class MobileBidder extends AbstractAgent{
	
	private boolean isClone;
	private boolean bidAccepted;
	private int artifactId;
	private int winningBid;
	private int myBid;
	private String homeLocation;
	
	
	@Override
	public void setup(Object homeLocation){
		isClone = false;
		this.homeLocation = (String) homeLocation;
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		setupBehaviours();
	}
	
	//Must have this as a separate class because of serialization issues with anonymous classes and lambda functions
	private class SuccessfulBidFunction implements BiConsumer<Integer, Integer>, Serializable{
		public void accept(Integer artifactId, Integer winningBid) {
			bidAccepted = true;
			MobileBidder.this.artifactId = artifactId;
			MobileBidder.this.winningBid = winningBid;
			afterAuction();
		}
	}
	
	private class SomeoneElseWonFunction implements BiConsumer<Integer, Integer>, Serializable{
		public void accept(Integer artifactId, Integer myBid) {
			bidAccepted = false;
			MobileBidder.this.artifactId = artifactId;
			MobileBidder.this.myBid = myBid;
			afterAuction();
		}
	}
	
	private void afterAuction(){
		try {
			if(!getContainerController().getContainerName().equals(homeLocation)){
				doMove(MobilityMain.locations.get(homeLocation));
			}else{
				announceResults();
			}
		} catch (ControllerException e) {
			e.printStackTrace();
		}
	}
	
	private void announceResults(){
		if(bidAccepted){
			System.out.println(getLocalName() + ": I won artifact-" + artifactId + " for " + winningBid);
		}else{
			System.out.println(getLocalName() + ": Someone else won artifact-" + artifactId + ". My bid was " + myBid);
		}
	}

	@Override
	protected void afterMove() {
		announceResults();
	}
	
	private void setupBehaviours(){
		addBehaviour(new SimpleBehaviour() {
			int numCloned = 0;
			@Override
			public boolean done() {
				return isClone || numCloned == 2;
			}
			
			@Override
			public int onEnd() {
				if(!isClone){
					addBehaviour(new WakerBehaviour(getAgent(), 2000) {
						public void onWake(){
							if(!isClone){
								System.out.println("mobileBidder wake, add bidder behaviour");
								int willingToPay = 40 + new Random().nextInt(40);
								addBehaviour(new DutchBidder(MobileBidder.this, willingToPay, new SuccessfulBidFunction(), new SomeoneElseWonFunction()));
							}
						}
					});
					
				}
				removeBehaviour(this);
				return super.onEnd();
			}
			
			@Override
			public void action() {
				try {
					if(done()){
						return;
					}
					String name = getLocalName();
					String containerName = getContainerController().getContainerName();
					if(numCloned == 0){
						Location here = MobilityMain.locations.get(containerName);
						doClone(here, name + "-Clone1");
					}else if(numCloned == 1){
						Location other = MobilityMain.locations.get(MobilityMain.getOther(containerName));
						doClone(other, name + "-Clone2");
					}
					numCloned ++;
				} catch (ControllerException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected void beforeClone() {
		System.out.println(getName() + " about to clone");
		super.beforeClone();
	}
	
	@Override
	protected void afterClone() {
		isClone = true;
		try {
			System.out.println(getName() + " cloned to " + getContainerController().getContainerName());
			int willingToPay = 40 + new Random().nextInt(40);
			addBehaviour(new DutchBidder(this, willingToPay, new SuccessfulBidFunction(), new SomeoneElseWonFunction()));
		} catch (ControllerException e) {
			e.printStackTrace();
		}
		super.afterClone();
	}


}
