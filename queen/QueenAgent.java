package queen;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

@SuppressWarnings("serial")
public class QueenAgent extends Agent{
	
	private int n;
	private int row;
	private int col;
	private AID prevQueen;
	private AID nextQueen;
	private int[] prevCols;
	private HashSet<int[]> failedConfigurations = new HashSet<int[]>();
	
	private final static boolean FIND_ALL = true;
	
	private ArrayList<int[]> foundSolutions = new ArrayList<int[]>();
	
	@Override
	protected void setup() {
		super.setup();
		
		Object[] args = getArguments();
		n = (int) args[0];
		row = (int) args[1];
		col = (int) args[2];
		if(args.length == 4){
			nextQueen = new AID((String) args[3], AID.ISLOCALNAME);
		}
		
		ParallelBehaviour b = new ParallelBehaviour();
		b.addSubBehaviour(new RespondToInfoFromPrev());
		b.addSubBehaviour(new RespondToComplaintFromNext());
		addBehaviour(b);
		
		boolean isFirstQueen = row == 0;
		if(isFirstQueen){
			prevCols = new int[0];
			addBehaviour(new OneShotBehaviour() {
				public void action() {
					tryToAdapt(); //Will send info to next queen
				}
			});
		}
		
	}
	
	private class RespondToInfoFromPrev extends CyclicBehaviour{
		public void action() {
			try {
				ACLMessage infoMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				if(infoMsg != null){
					prevQueen = infoMsg.getSender();
					prevCols = (int[]) infoMsg.getContentObject();
					tryToAdapt();
				}else{
					block();
				}
			} catch (UnreadableException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class RespondToComplaintFromNext extends CyclicBehaviour{
		public void action() {
			ACLMessage complaintMsg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			if(complaintMsg != null){
				int[] currentConfiguration = extend(prevCols, col);
				failedConfigurations.add(currentConfiguration);
				tryToAdapt();
			}else{
				block();
			}
		}
	}
	
	private void tryToAdapt(){
		try{
			if(!adaptToPrevQueens()){
				System.err.println(getLocalName() + " can't adapt to " + Arrays.toString(prevCols));
				System.err.println("( forbidden: " + toString(failedConfigurations) + ")");
				boolean isFirstQueen = row == 0;
				if(isFirstQueen){
					System.out.println("DONE!");
				}else{
					complainAtPrevious();	
				}
			}else{
				int[] config = extend(prevCols, col);
				System.err.println(getLocalName() + " adapting: " + Arrays.toString(config));
				boolean isLastQueeN = row == n - 1; 
				if(isLastQueeN){
					System.out.println("Found solution: " + Arrays.toString(config));
					if(FIND_ALL){
						foundSolutions.add(config);
						System.out.println("---------------------------------------------");
						System.out.println("FOUND " + foundSolutions.size() + " SO FAR: " + toString(foundSolutions));
						System.out.println("---------------------------------------------");
						complainAtPrevious();
					}
				}else{
					informNext();
				}
			}	
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void complainAtPrevious(){
		ACLMessage complaintMsg = new ACLMessage(ACLMessage.REQUEST);
		complaintMsg.addReceiver(prevQueen);
		send(complaintMsg);
	}
	
	private void informNext() throws IOException{
		ACLMessage infoMsg = new ACLMessage(ACLMessage.INFORM);
		infoMsg.addReceiver(nextQueen);
		infoMsg.setContentObject(extend(prevCols, col));
		send(infoMsg);
	}
	
	private String toString(Collection<int[]> set){
		String s = "";
		Iterator<int[]> it = set.iterator();
		while(it.hasNext()){
			s += Arrays.toString(it.next()) + " ";
		}
		return s;
	}
	
	private boolean adaptToPrevQueens(){
		for(int testCol = 0; testCol < n; testCol++){
			if(!alreadyTried(extend(prevCols, testCol)) && positionsAreCompatible(testCol)){
				col = testCol;
				return true;
			}
		}
		return false;
	}
	
	private boolean alreadyTried(int[] config){
		return failedConfigurations.stream().anyMatch(failed -> Arrays.equals(failed, config));
	}
	
	private boolean positionsAreCompatible(int col){
		for(int otherRow = 0; otherRow < prevCols.length; otherRow++){
			int otherCol = prevCols[otherRow];
			if(col == otherCol || Math.abs(col-otherCol) == Math.abs(row-otherRow)){
				return false;
			}
		}
		return true;
	}
	
	private int[] extend(int[] array, int appendToEnd){
		int[] newArray = new int[array.length + 1];
		for(int i = 0; i < array.length; i ++){
			newArray[i] = array[i];
		}
		newArray[newArray.length - 1] = appendToEnd;
		return newArray;
	}
}
