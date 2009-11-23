package org.rhq.augeas.node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

public class AugeasNodeLazy extends AugeasNodeParent implements AugeasNode {

	public AugeasNodeLazy(String FullPath,AugeasTree ag)
	{
		super();
		
		this.ag = ag;
	    this.path = FullPath.substring(0,FullPath.lastIndexOf(File.separatorChar)+1);
	    String val = FullPath.substring(FullPath.lastIndexOf(File.separatorChar)+1,FullPath.length());
	    
	    int firstB = val.indexOf("[");
	    if (firstB != -1){
	      seq = Integer.valueOf(val.substring(firstB+1,val.indexOf(']')));
	      label = val.substring(0,firstB);
	    }
	    else{
	    	seq = 0;
	    	label = FullPath.substring(FullPath.lastIndexOf(File.separatorChar)+1);
	    }    
	}
	
	public List<AugeasNode> getChildNodes() {
		List <AugeasNode> nodes = null;
		try {
		
			nodes = ag.match(getFullPath()+File.separatorChar+"*");
		    
		}catch(AugeasTreeException e)
		{
			//TODO loggin
		e.printStackTrace();	
		}
		return nodes;
	}


	public AugeasNode getParentNode(){
		String parentNodePath = path.substring(0,path.length()-1);
		if (parentNodePath.equals(ag.getRootNode().getFullPath()))
			return ag.getRootNode();
		try {
		parentNode = ag.getNode(parentNodePath);
		}catch(Exception e)
		{
			return null;
		}
		
		return parentNode;
	}

	public String getValue() {
		if (value == null)
			value =  ag.get(getFullPath());
		
		return value;
	}

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        AugeasNode that = (AugeasNode) obj;

        if (!this.getFullPath().equals(that.getFullPath()))
    		return false;

        return true;
    }

    public void setValue(String value){
    	ag.setValue(this, value);
    }
	
	public String getFullPath() {
		return path + label + (seq !=0 ? "["+String.valueOf(seq)+"]" : "" );
	}

	
	public void addChildNode(AugeasNode node) {
		//TODO kontrola jestli sem patri
		childNodes.add(node);
	}

	
	public List<AugeasNode> getChildByLabel(String labelName){
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
		ag.removeNode(this,updateSeq);
	}

	public void setPath(String path) throws Exception{
		this.path = path;
		
	}

	public void updateFromParent() {
		AugeasNode node = this.getParentNode();
		if (!this.path.equals(node.getFullPath())){
			this.path = node.getFullPath();
		    }
	}
}
