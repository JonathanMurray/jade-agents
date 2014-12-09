package mobility;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.mobility.MobilityOntology;
import jade.wrapper.ControllerException;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import agents.AbstractAgent;
import agents.DutchAuctioneer;
import agents.DutchAuctioneer.AuctioneerStrategy;

@SuppressWarnings("serial")
public class MobileAuctioneer extends AbstractAgent{
	
	private boolean isClone;
	int artifactId;
	int finalBid;
	AuctioneerStrategy strategy;
	String homeContainer;
	
	@Override
	public void setup(Object artifactId, Object strategy, Object homeContainer){
		isClone = false;
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());
		this.artifactId = (int) artifactId;
		this.strategy = (AuctioneerStrategy) strategy;
		this.homeContainer = (String) homeContainer;
		setupBehaviours();
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
				removeBehaviour(this);
				return super.onEnd();
			}

			@Override
			public void action() {
				if(done()){
					return;
				}
				if(numCloned == 0){
					doClone(MobilityMain.locations.get(MobilityMain.G), "A-G");
				}else if(numCloned == 1){
					doClone(MobilityMain.locations.get(MobilityMain.HM), "A-HM");
				}
				numCloned ++;
			}
		});
		
	}
	
	private void addAuctioneerBehaviour(int artifactId, AuctioneerStrategy strategy) {
		addBehaviour(new WakerBehaviour(this, 4000) {
			public void onWake(){
				try {
					String containerName = getContainerController().getContainerName();
					String otherContainerName = MobilityMain.getOther(containerName);
					List<AID> bidders = Arrays.asList(new AID[]{
							new AID(containerName + "1", AID.ISLOCALNAME),
							new AID(containerName + "1-Clone1", AID.ISLOCALNAME),
							new AID(containerName + "2", AID.ISLOCALNAME),
							new AID(containerName + "2-Clone1", AID.ISLOCALNAME),
							new AID(otherContainerName + "1-Clone2", AID.ISLOCALNAME),
							new AID(otherContainerName + "2-Clone2", AID.ISLOCALNAME),
					});		
					addBehaviour(new DutchAuctioneer(MobileAuctioneer.this, bidders, artifactId, strategy, new AfterAuction()));
				}catch(ControllerException e){
					e.printStackTrace();
				}
			}
		});
	}
	
	private class AfterAuction implements BiConsumer<Integer, Integer>{
		public void accept(Integer artifactId, Integer bid) {
			MobileAuctioneer.this.artifactId = artifactId;
			finalBid = bid;
			doMove(MobilityMain.locations.get(homeContainer));
		}
	}
	
	@Override
	protected void afterMove() {
		announceResults();
	}
	
	private void announceResults(){
		if(finalBid <= 0){
			System.out.println(getLocalName() + ": Didn't manage to sell artifact-" + artifactId + ".");
		}else{
			System.out.println(getLocalName() + ": Sold artifact-" + artifactId + " for " + finalBid + "!");
		}
	}
	
	
	@Override
	protected void beforeClone() {
		System.out.println(getName() + " about to clone");
		super.beforeClone();
	}
	
	@Override
	protected void afterClone() {
		try {
			isClone = true;
			System.out.println("Cloned " + getName() + " to " + getContainerController().getContainerName());
			addAuctioneerBehaviour(artifactId, strategy);
		} catch (ControllerException e) {
			e.printStackTrace();
		}
		super.afterClone();
	}

}
