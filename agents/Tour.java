package agents;

import jade.util.leap.Serializable;

import java.util.List;

public class Tour implements Serializable {
	private static final long serialVersionUID = 1L;
	List<Integer> artifactIds;
	public Tour(List<Integer> artifactIds){
		this.artifactIds = artifactIds;
	}
	
	@Override
	public String toString(){
		return "Tour-" + artifactIds;
	}
}
