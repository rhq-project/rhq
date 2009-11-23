package org.rhq.augeas.tree;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;

public interface AugeasTree {
	public void load() throws AugeasTreeException;
	public void update();
	public void save();
	public AugeasNode getNode(String path) throws AugeasTreeException;
	public List<AugeasNode> match(String expression) throws AugeasTreeException;
	public List<AugeasNode> matchRelative(AugeasNode node,String expression) throws AugeasTreeException;
	public AugeasNode createNode(String fullPath) throws AugeasTreeException;
	public AugeasNode createNode(AugeasNode parentNode,String value,int seq);
	public String get(String expr);
	public AugeasNode getRootNode();
	public void removeNode(AugeasNode node,boolean updateSeq) throws Exception;
	public void setValue(AugeasNode node,String value);
}
