package edu.uw.feedlack;

public class CFGPathEdge implements AbstractCFGEdge {

	private final CFGPath path;
	private final int firstIndex, lastIndex;
	
	public CFGPathEdge(CFGPath path, int firstIndex, int lastIndex) {
		
		this.path = path;
		this.firstIndex = firstIndex;
		this.lastIndex = lastIndex;
		
	}

	public int length() { return path.length(); }
	public CFGPath getPath() { return path; }
	public int getFirstIndex() { return firstIndex; }
	public int getLastIndex() { return lastIndex; }
	
	@Override
	public CFGNode getLastNode() {
		
		return path.getLastNode();
		
	}
	
	public String toString() { 
		
		StringBuilder sb = new StringBuilder();
		for(int i = firstIndex; i <= lastIndex; i++)
			sb.append(path.getEdgeAtIndex(i).toString());
		return sb.toString();
		
	}
	
}
