package play;

import java.util.Iterator;

import gametree.GameNode;
import gametree.GameNodeDoesNotExistException;
import play.exception.InvalidStrategyException;

public class MyStrategy  extends Strategy {
	
	public GameNode oppP1;
	public GameNode meP1;
	public GameNode meP2;
	public GameNode oppP2;
	
	public int defectCountP1 = 0; //number of defects made by the opponent
	public int defectCountP2 = 0; //number of defects made by the opponent
	public int defectAndCooperateLeftP1 = 0; //Defects left + 2 cooperates
	public int defectAndCooperateLeftP2 = 0; //Defects left + 2 cooperates
	public boolean dirtyP1 = false;
	public boolean dirtyP2 = false;

	@Override
	public void execute() throws InterruptedException {
		while(!this.isTreeKnown()) {
			System.err.println("Waiting for game tree to become available.");
			Thread.sleep(1000);
		}
		while(true) {
			PlayStrategy myStrategy = this.getStrategyRequest();
			if(myStrategy == null) //Game was terminated by an outside event
				break;	
			boolean playComplete = false;
						
			while(! playComplete ) {
				System.out.println("*******************************************************");
				if(myStrategy.getFinalP1Node() != -1) {
					oppP1 = this.tree.getNodeByIndex(myStrategy.getFinalP1Node());
					meP1 = null;
					if(oppP1 != null) {
						try {
							meP1 = oppP1.getAncestor();
						} catch (GameNodeDoesNotExistException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.print("Last round as P1: " + showLabel(meP1.getLabel()) + "|" + showLabel(oppP1.getLabel()));
						System.out.println(" -> (Me) " + oppP1.getPayoffP1() + " : (Opp) "+ oppP1.getPayoffP2());
					}
				}			
				if(myStrategy.getFinalP2Node() != -1) {
					meP2 = this.tree.getNodeByIndex(myStrategy.getFinalP2Node());
					oppP2 = null;
					if(meP2 != null) {
						try {
							oppP2 = meP2.getAncestor();
						} catch (GameNodeDoesNotExistException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.print("Last round as P2: " + showLabel(oppP2.getLabel()) + "|" + showLabel(meP2.getLabel()));
						System.out.println(" -> (Opp) " + meP2.getPayoffP1() + " : (Me) "+ meP2.getPayoffP2());
					}
				}
				GameNode rootNode = tree.getRootNode();
				int n1 = rootNode.numberOfChildren();
				int n2 = rootNode.getChildren().next().numberOfChildren();
				String[] labelsP1 = new String[n1];
				String[] labelsP2 = new String[n2];
				int[][] U1 = new int[n1][n2];
				int[][] U2 = new int[n1][n2];
				Iterator<GameNode> childrenNodes1 = rootNode.getChildren();
				GameNode childNode1;
				GameNode childNode2;
				int i = 0;
				int j = 0;
				while(childrenNodes1.hasNext()) {
					childNode1 = childrenNodes1.next();		
					labelsP1[i] = childNode1.getLabel();	
					j = 0;
					Iterator<GameNode> childrenNodes2 = childNode1.getChildren();
					while(childrenNodes2.hasNext()) {
						childNode2 = childrenNodes2.next();
						if (i==0) labelsP2[j] = childNode2.getLabel();
						U1[i][j] = childNode2.getPayoffP1();
						U2[i][j] = childNode2.getPayoffP2();
						j++;
					}	
					i++;
				}
				showMoves(1,labelsP1);
				showMoves(2,labelsP2);
				showUtility(1,n1,n2,U1);
				showUtility(2,n1,n2,U2);
				double[] strategyP1 = setStrategy(1,labelsP1,myStrategy);
				double[] strategyP2 = setStrategy(2,labelsP2,myStrategy);
				showStrategy(1,strategyP1,labelsP1);
				showStrategy(2,strategyP2,labelsP2);			
				try{
					this.provideStrategy(myStrategy);
					playComplete = true;
				} catch (InvalidStrategyException e) {
					System.err.println("Invalid strategy: " + e.getMessage());;
					e.printStackTrace(System.err);
				} 
			}
		}
		
	}
	
	public String showLabel(String label) {
		return label.substring(label.lastIndexOf(':')+1);
	}
	
	public void showMoves(int P, String[] labels) {
		System.out.println("Moves Player " + P + ":");
		for (int i = 0; i<labels.length; i++) System.out.println("   " + showLabel(labels[i]));
	}
	
	public void showUtility(int P, int n1, int n2, int[][] M) {
		System.out.println("Utility Player " + P + ":");
		for (int i = 0; i<n1; i++) {
			for (int j = 0; j<n2; j++) System.out.print("| " + M[i][j] + " ");
			System.out.println("|");
		}
	}
	
	public double[] setStrategy(int P, String[] labels, PlayStrategy myStrategy) {
		int n = labels.length;
		double[] strategy = new double[n];
		for (int i = 0; i<n; i++)  strategy[i] = 0;
		if(oppP1 != null || meP2 != null) {
			if (P==1) {
				computeStrategyP1(labels, strategy);
			}
			else {
				computeStrategyP2(labels, strategy);
			}
		}
		else {
			//if this is the first round we are going to play cooperate
			strategy[0] = 1.0;
		}
		for (int i = 0; i<n; i++) myStrategy.put(labels[i], strategy[i]);
		return strategy;
	}
	
	public void computeStrategyP1(String[] labels, double[] strategy) {
		//When we played as Player1 we are going to check what were the moves
		//of our opponent as player2.
		if(showLabel(oppP1.getLabel()).equals(showLabel(labels[1]))) {
			defectCountP1++;
			dirtyP1 = true;
		}
		
		if(defectAndCooperateLeftP1 == 0 && dirtyP1) {
			defectAndCooperateLeftP1 = defectCountP1 + 2;
			dirtyP1 = false;
		}
		
		if(defectAndCooperateLeftP1 > 2) {
			strategy[1] = 1.0;
		}
		else {
			strategy[0] = 1.0;
		}
		
		if(defectAndCooperateLeftP1 > 0)
			defectAndCooperateLeftP1--;
	}
	
	public void computeStrategyP2(String[] labels, double[] strategy) {
		//When we played as Player2 we are going to check what were the moves
		//of our opponent as player1.
		if(showLabel(oppP2.getLabel()).equals(showLabel(labels[1]))) {
			defectCountP2++;
			dirtyP2 = true;
		}
		
		if(defectAndCooperateLeftP2 == 0 && dirtyP2) {
			defectAndCooperateLeftP2 = defectCountP2 + 2;
			dirtyP2 = false;
		}
		
		if(defectAndCooperateLeftP2 > 2) {
			strategy[1] = 1.0;
		}
		else {
			strategy[0] = 1.0;
		}
		
		if(defectAndCooperateLeftP2 > 0)
			defectAndCooperateLeftP2--;
	}
	
	public void showStrategy(int P, double[] strategy, String[] labels) {
		System.out.println("Strategy Player " + P + ":");
		for (int i = 0; i<labels.length; i++) System.out.println("   " + strategy[i] + ":" + showLabel(labels[i]));
	}

}
