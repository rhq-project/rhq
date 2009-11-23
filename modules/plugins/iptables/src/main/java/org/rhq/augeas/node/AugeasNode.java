package org.rhq.augeas.node;

import java.util.List;

import org.rhq.augeas.tree.AugeasTreeException;

public interface AugeasNode {

	public String getPath();

	public void setPath(String path) throws Exception;
	
	public String getLabel() ;

	public void setLabel(String label);

	public String getValue();

	public void setValue(String value) ;

	public int getSeq();

	public void setSeq(int seq);

	public AugeasNode getParentNode();

	public List<AugeasNode> getChildNodes() ;
	
	public boolean equals(Object obj);
	
	public String getFullPath();
	
	public void addChildNode(AugeasNode node);
	
	public List<AugeasNode> getChildByLabel(String labelName);
	
	public void remove(boolean updateSeq) throws Exception;
	
	public void updateFromParent();
}
