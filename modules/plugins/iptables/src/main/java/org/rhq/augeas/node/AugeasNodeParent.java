package org.rhq.augeas.node;

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.tree.AugeasTree;

public class AugeasNodeParent {
	
	protected String path;
	protected String  label;
	protected String value;
	protected AugeasTree ag;
	protected int seq;
	protected AugeasNode parentNode;
	protected List<AugeasNode> childNodes;

	public AugeasNodeParent(){
		childNodes = new ArrayList<AugeasNode>();
	}
	public String getPath() {
		return path;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public AugeasNode getParentNode() {
		return parentNode;
	}

	public List<AugeasNode> getChildNodes() {
		return childNodes;
	}
}
