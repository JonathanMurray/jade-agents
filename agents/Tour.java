package agents;

import jade.util.leap.Serializable;

import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
public class Tour implements Serializable, Iterable<Integer>{
	private List<Integer> artifactIds;
	public Tour(List<Integer> artifactIds){
		this.artifactIds = artifactIds;
	}
	
	@Override
	public String toString(){
		return "Tour-" + artifactIds;
	}

	@Override
	public Iterator<Integer> iterator() {
		return artifactIds.iterator();
	}
}
