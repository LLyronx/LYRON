import java.util.*;

public class NFANode {	

	private String serialCode = null;
	
	private Map<String, Set<NFANode> > stateTransformTable;
	
	private boolean isFinal = false;
	
	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public NFANode() {
		serialCode = StringGenerator.getNextCode();
		stateTransformTable = new HashMap<>();
	}
	
	public String getSerial() {
		return serialCode;
	}
	
	public void setTransformTableByText(Set<String> result){
		for (String trans: stateTransformTable.keySet()) {
			for (NFANode end: stateTransformTable.get(trans)) {
				String willAdd = getSerial() + " -> " + end.getSerial() + "[label=\"" + trans + "\"]";
				if (!result.contains(willAdd)) {
					result.add(willAdd);
					end.setTransformTableByText(result);
				}
			}
		}
	}
	
	public void setLinkedNodes(Set<NFANode> result){
		result.add(this);
		for (String trans: stateTransformTable.keySet()) {
			for (NFANode end: stateTransformTable.get(trans)) {
				if (!result.contains(end)) {
					result.add(end);
					end.setLinkedNodes(result);
				}
			}
		}
	}
	
	public void addToTransformTable(String s, NFANode next) {
		if (!stateTransformTable.containsKey(s)) {
			stateTransformTable.put(s, new HashSet<NFANode>());			
		}
		stateTransformTable.get(s).add(next);
	}
	
	@Override
	public String toString() {
		return getSerial();
	}
	
}
