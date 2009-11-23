package org.rhq.augeas.tree;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.augeas.node.AugeasNode;

public class AugeasNodeBuffer {

	private Map<String,AugeasNode> buffer;
	
	public AugeasNodeBuffer()
	{
		buffer = new HashMap<String,AugeasNode>();
	}
	
	public boolean isNodeLoaded(String name){
		return buffer.containsKey(name);
	}
	
	public AugeasNode getNode(String name){
	   return buffer.get(name);	
	}
	
	public void addNode(AugeasNode node){
		if (!isNodeLoaded(node.getFullPath()))
			buffer.put(node.getFullPath(), node);
	}
	
	public void removeNode(AugeasNode node,boolean updateSeq,boolean lazy)
	{
		if (buffer.containsKey(node.getFullPath()))
				buffer.remove(node.getFullPath());	
		
		if (updateSeq)
			if (lazy)
				reloadLazy(node);
			else
				reload(node);
	}
	
	public void reload(AugeasNode nodes)
	{
		for (String key : buffer.keySet()){
			AugeasNode nd  = buffer.get(key);
			if (!key.equals(nd.getFullPath()))
			{
		       buffer.remove(key);
		       buffer.put(nd.getFullPath(), nd);
			}
		}
    }
	
	public void reloadLazy(AugeasNode node){
		Map<String,String> nodesToChange = new HashMap<String,String>();
		List<String> nodesToChangeSeq = new ArrayList<String>();
		
		AugeasNode parentNode = node.getParentNode();
		for (String key : buffer.keySet()){
			
			int index = key.indexOf(parentNode.getFullPath());
			if ((index==0) & (key.length()>parentNode.getFullPath().length())){
				String localPath = key.substring(parentNode.getFullPath().length());
				int endOfLabel = localPath.indexOf(File.separatorChar);
				String label;
                String restOfPath;
                
				if (endOfLabel!=-1){
					label = localPath.substring(0,endOfLabel);
					restOfPath = localPath.substring(endOfLabel+1,localPath.length()-1);
					}
				else{
					label = localPath.substring(0,localPath.length()-1);
					restOfPath = "";
					}
				int startOfSeq = label.indexOf('[');
				if (startOfSeq !=-1){
					String labelName = label.substring(0,startOfSeq);
					if (labelName.equals(node.getLabel())){
						int endOfSeq = label.indexOf(']');
						String seqNr = label.substring(startOfSeq,endOfSeq-1);
						int seq = Integer.valueOf(seqNr).intValue();
						if (seq>node.getSeq())
						{
							if (restOfPath.equals(""))
							   nodesToChangeSeq.add(key);
							else{
					      	   String newPath = parentNode.getFullPath()+File.separator + labelName+
					      					"[" + String.valueOf(seq-1)+"]"+(restOfPath.equals("") ? File.separator+ restOfPath : "");
					      	
					      	nodesToChange.put(key, newPath);}
						}
					}
				}
			}
				
		}
			
		try {
			
			for (String key : nodesToChange.keySet()){
				AugeasNode nd = buffer.get(key);
				nd.setPath((String)nodesToChange.get(key));
			}
			
			for (String key : nodesToChangeSeq){
				AugeasNode nd  = buffer.get(key);
				nd.setSeq(nd.getSeq()-1);
			}
				
		}catch(Exception e){
			//Exception is not thrown here because this method is called only for AugeasNodeLazy
		}
	}
	  
}
