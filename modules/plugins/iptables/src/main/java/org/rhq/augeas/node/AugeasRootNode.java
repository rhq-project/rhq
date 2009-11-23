package org.rhq.augeas.node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AugeasRootNode extends AugeasNodeParent implements AugeasNode{
	
	public AugeasRootNode(List<AugeasNode> nodes){
		super();
		childNodes=nodes;
	}
	
	public AugeasRootNode(){
		super();
	}
	
	public void addChildNode(AugeasNode node) {
		childNodes.add(node);
	}

	
	public List<AugeasNode> getChildNodes() {
		return childNodes;
	}


	public String getFullPath() {
		return ""+File.pathSeparatorChar;
	}


	public String getLabel() {
		return null;
	}


	public AugeasNode getParentNode() {
		return null;
	}


	public String getPath() {
		return File.separator;
	}
	
	public List<AugeasNode> getChildByLabel(String labelName) {
		List<AugeasNode> nodes = getChildNodes();
		List<AugeasNode> tempNode = new ArrayList<AugeasNode>();

		for (AugeasNode node : nodes){
			if (node.getLabel().equals(labelName))
				tempNode.add(node);
		}
		return tempNode;
}
	
	public void remove(boolean updateSeq) throws Exception
	{
		throw new Exception("Root node is virtual and can not be removed. If you want to remove data remove all child nodes.");
	}

	public void setPath(String path) throws Exception{
		// TODO Auto-generated method stub
		
	}

	public void updateFromParent() {
		// TODO Auto-generated method stub
		
	}
}
