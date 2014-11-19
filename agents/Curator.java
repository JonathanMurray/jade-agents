package agents;

import jade.core.behaviours.TickerBehaviour;

import java.util.HashMap;
import java.util.Map;


public class Curator extends AbstractAgent{
	private static final long serialVersionUID = 1L;
	private static final int UPDATE_ARTIFACTS_PERIOD_MS = 15000;
	
	private Map<Integer, Artifact> artifacts = new HashMap<Integer, Artifact>();
	
	@Override
	public void setupWithoutArgs(){
		addBehaviour(new UpdateArtifacts(this, UPDATE_ARTIFACTS_PERIOD_MS));
	}
	
	
	private static class UpdateArtifacts extends TickerBehaviour{

		private static final long serialVersionUID = 1L;
		Curator curator;
		public UpdateArtifacts(Curator curator, long period) {
			super(curator, period);
			this.curator = curator;
		}

		@Override
		protected void onTick() {
			curator.artifacts.clear();
			
			Artifact[] array = new Artifact[]{
				new Artifact(1, "Bronze sword", "Olaf Stenhammar", 1247, "Gamla Uppsala"),
				new Artifact(2, "Mona Lisa", "Leonardo da Vinci", 1503, "Venice"),
				new Artifact(3, "ABBA album", "ABBA", 1976	, "Stockholm")
			};
			for(Artifact artifact : array){
				curator.artifacts.put(artifact.id, artifact);
			}
		}
	}
}
