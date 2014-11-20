package agents;

import java.io.Serializable;


@SuppressWarnings("serial")
public class Artifact implements Serializable{
	int id;
	String name;
	String creator;
	int creationYear;
	String creationPlace;
	
	public Artifact(int id, String name, String creator, int creationYear, String creationPlace){
		this.id = id;
		this.name = name;
		this.creator = creator;
		this.creationYear = creationYear;
		this.creationPlace = creationPlace;
	}
}
