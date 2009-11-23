package org.rhq.augeas.tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeLazy;
import org.rhq.augeas.node.AugeasRootNode;

public class AugeasTreeLazy implements AugeasTree{
	private AugeasModuleConfig moduleConfig;
	private Augeas ag;
	private AugeasNode rootNode;
	private AugeasNode rootConfigNode;
	private AugeasNodeBuffer nodeBuffer;
	
	private static String AUGEAS_DATA_PATH=File.separatorChar+"files";
	
	public AugeasTreeLazy(Augeas ag ,AugeasModuleConfig moduleConfig)
	{
		rootNode = new AugeasRootNode();
		nodeBuffer = new AugeasNodeBuffer();
		this.moduleConfig = moduleConfig;
		this.ag = ag;
	}
	
	public void load() throws AugeasTreeException
	{
		buildTree();
	}
	
	public void update()
	{
	
	}
	
	public void save()
	{
		ag.save();
	}
	
	
	private AugeasNode getLoadedNode(String path) throws AugeasTreeException
	{		
		if (nodeBuffer.isNodeLoaded(path))
			return nodeBuffer.getNode(path);
		
		throw new AugeasTreeException("Node not found.");
	}
	
	public AugeasNode getNode(String path) throws AugeasTreeException
	{
		AugeasNode node;
		try {
		node = getLoadedNode(path);
		}catch(AugeasTreeException e)
		{
		 node = createNode(path);	
		}
		
		return node;
	}
	
	public List<AugeasNode> match(String expression) throws AugeasTreeException
	{
			List<String> res = ag.match(expression);

			List<AugeasNode> nodes = new ArrayList<AugeasNode>();
			
		for (String name:res){
			nodes.add(getNode(name));
		}
		
		return nodes;
	}
	
	public List<AugeasNode> matchRelative(AugeasNode node,String expression) throws AugeasTreeException
	{
		   if (rootNode.getChildNodes().isEmpty())
			   throw new AugeasTreeException("Root node has not childs.");
		  
		   if (node.equals(rootNode)){
			   List<AugeasNode> nodes = rootNode.getChildNodes();
			   List<AugeasNode> returnNodes = new ArrayList<AugeasNode>();
			   for (AugeasNode nd :nodes)
			   	{
				   String tempName = nd.getFullPath()+expression;
				   List<AugeasNode> temp = match(tempName);
				   returnNodes.addAll(temp);
			   	}
			   return returnNodes; 
		   }
		   
		   return match(node.getFullPath()+File.separatorChar+expression);
	}
	
	public AugeasNode createNode(String fullPath) throws AugeasTreeException
	{
	AugeasNode node=null;
		
			ag.set(fullPath, null);
			node = new AugeasNodeLazy(fullPath,this);
			nodeBuffer.addNode(node);
			return node;		
	}
	
	public AugeasNode createNode(AugeasNode parentNode,String value,int seq)
	{
		return null;
	}
	
	public String get(String expr)
	{
		return ag.get(expr);
	}
	
	public AugeasNode getRootNode(){
		return rootNode;
	}
	
	private void buildTree() throws AugeasTreeException
	{
		rootConfigNode = createNode("/augeas");
		
		for (String name : moduleConfig.getIncludedGlobs())
			{
			rootNode.addChildNode(createNode(AUGEAS_DATA_PATH+File.separatorChar+name));
			}
	}
	
	public void removeNode(AugeasNode node,boolean updateSeq) throws Exception
	{
		int res = ag.remove(node.getFullPath());
		nodeBuffer.removeNode(node,updateSeq,true);
		
   }
	
	public void setValue(AugeasNode node,String value)
	{
		ag.set(node.getFullPath(), value);
	}
	/*
	   protected String summarizeAugeasError(Augeas augeas) {
	        StringBuilder summary = new StringBuilder();
	        String metadataNodePrefix = "/augeas/files";
	        for (String glob : moduleConfig.getIncludedGlobs()) {
	            if (glob.startsWith(""+File.separatorChar)) {
	                glob = glob.substring(1);
	            }
	            AugeasNode metadataNode = new AugeasNodeLazy();
	            AugeasNode errorNode = new AugeasNode(metadataNode, "error");
	            List<String> nodePaths = augeas.match(errorNode.getPath() + "/*");
	            for (String path : nodePaths) {
	                String error = augeas.get(path);
	                summary.append("File \"").append(path.substring(metadataNodePrefix.length(), path.length())).append(
	                    "\":\n").append(error).append("\n");
	            }
	        }

	        return summary.toString();
	    }
*/
}
