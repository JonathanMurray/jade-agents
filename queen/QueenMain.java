package queen;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class QueenMain extends Agent{
	@Override
	protected void setup() {
		super.setup();
		try{
			setupQueens();
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	private void setupQueens() throws StaleProxyException{
		int n =7;
		ContainerController cc = getContainerController();
		List<AgentController> agents = new ArrayList<AgentController>();
		for(int i = 0; i < n - 1; i++){
			Object[] args = new Object[]{n,	i, 0, "queen_" + (i+1)};
			AgentController ac = cc.createNewAgent("queen_" + i, "queen.QueenAgent", args);
			agents.add(ac);
		}
		agents.add(cc.createNewAgent("queen_" + (n-1), "queen.QueenAgent", new Object[]{n, n-1, 0}));
		
		for(AgentController ac : agents){
			ac.start();
		}
	}
}
