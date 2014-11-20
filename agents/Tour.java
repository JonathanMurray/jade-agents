package agents;

import jade.util.leap.Serializable;

import java.util.List;

@SuppressWarnings("serial")
public class Tour implements Serializable {
	List<Integer> artifactIds;
	public Tour(List<Integer> artifactIds){
		this.artifactIds = artifactIds;
	}
	
	@Override
	public String toString(){
		return "Tour-" + artifactIds;
	}
}
