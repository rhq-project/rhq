package org.rhq.plugins.iptables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.rhqtransform.RhqTransform;

public class IptablesConfigTransform {

	private AugeasTree tree;
	private RhqTransform transf;
	
	public IptablesConfigTransform(AugeasTree  tree){
	  this.tree = tree;	
	  this.transf = new RhqTransform(tree);	
	}
	/*
	public Configuration transform(List<AugeasNode> startNodes,ConfigurationDefinition resourceConfigDef) throws Exception
	{

	       Configuration resourceConfig = new Configuration();
	       Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

	        if (propDefs.size()!=1)
	        	throw new Exception("Error in mapping.");
	        
	        PropertyList propList = new PropertyList("chains");
	        
	        	for (PropertyDefinition propDef : propDefs) {
	        		if (propDef.getName().equals("chain")){
	        			PropertyDefinition definition = ((PropertyDefinitionList)propDef).getMemberDefinition();
	        			for (AugeasNode node : startNodes){
	        				//for (AugeasNode nd : node.getChildNodes())
	        			       propList.add(transf.loadProperty(definition,node));
	        		    }
	        	     }
	        	}
	        
	        resourceConfig.put(propList);
	        return resourceConfig;
	}*/
	
	public Configuration transform(List<AugeasNode> startNodes,ConfigurationDefinition resourceConfigDef) throws Exception
	{
	       Configuration resourceConfig = new Configuration();

			if (startNodes.isEmpty())
			   { 
			   PropertyList propList = new PropertyList("chains");
			   PropertyMap mp = new PropertyMap("rule");   	     
			   propList.add(mp);
			   resourceConfig.put(propList);
			   return resourceConfig;
			   }
			
	       Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

	        if (propDefs.size()!=1)
	        	throw new Exception("Error in mapping.");
	        
	        PropertyList propList = new PropertyList("chains");
	        
	        	 for (AugeasNode node : startNodes){
	        				//for (AugeasNode nd : node.getChildNodes())
	        		 PropertyMap mp = new PropertyMap("rule");   	        		 
	        		 for (AugeasNode nd : node.getChildByLabel("rule"))
	        		    {//rules
	        		     	
	        		     String rule = "";
	        		     
	        		     for (AugeasNode tempNode : nd.getChildByLabel("parameters"))
	        		        {
	        		    	rule += buildRule(tempNode);
	        		        }
	        		     
	        		     PropertySimple ruleProp = new PropertySimple("param",rule);
	        		     mp.put(ruleProp);
	        		    }
	        		 propList.add(mp);	        		 
	        }
	        	   
	        resourceConfig.put(propList);
	        return resourceConfig;
	}
	
	private String buildRule(AugeasNode nd) throws Exception{
		 String  paramName =  getValue(nd,"/param/paramName");
   	     String negation = getValue(nd, "/param/negation");
		 String value = getValue(nd, "/param/value");
		 
		 return (((paramName.length()> 1) ?"--" : "-") + paramName + " " + negation + " " +value);
	}
	
	private String getValue(AugeasNode nd,String nm) throws Exception{
	  List<AugeasNode> nds = tree.matchRelative(nd, nm);
	  if (!nds.isEmpty())
		  return nds.get(0).getValue();
	  else
		  return "";
	}
	
	
	public void updateAugeas(Configuration config,List<AugeasNode> nodes)
	{
		List<String> values = new ArrayList<String>();
		
	   Collection<Property> props = config.getAllProperties().values();
	   
	   for (Property prop : props)
	   {
		if (prop.getName().equals("chains")){
			 PropertyList propList = (PropertyList) prop;
			 for (Property property : propList.getList())
			 {
				 if (property.getName().equals("rule"))
				 {
			       PropertyMap propMap = (PropertyMap) property;
			       for (Property propVal : propMap.getMap().values()){
			    	  if (propVal.getName().equals("param")){
			    		values.add(((PropertySimple)propVal).getStringValue());
			    	  }
			       }
				 }
			 }			 
		  }
	   }
	   String name;
	}
	
	
	public void buildTree(List<AugeasNode> nodes) throws Exception
	{
		List<String> values = new ArrayList<String>();

   	    for (AugeasNode node : nodes){
   		 PropertyMap mp = new PropertyMap("rule");   	        		 
   		 for (AugeasNode nd : node.getChildByLabel("rule"))
   		    {	
   		     String rule = "";
   		     for (AugeasNode tempNode : nd.getChildByLabel("parameters"))
   		        {
   		    	rule += buildRule(tempNode);
   		        }
   		     
   		     values.add(rule);
   		    }   		 
         }
	}
	
	public void compare(List<String> values,AugeasTree tree,String chainName,String tableName) throws Exception
	{
		String expr = File.separatorChar+tableName+File.separatorChar+chainName;
		
		List<AugeasNode> nodes = tree.matchRelative(tree.getRootNode(), expr);
		
		for (AugeasNode node : nodes)
		{
			node.remove(false);
		}
		int i =1;
		for (String val : values){
			
			i = i + 1;
			String temp = File.separatorChar+tableName+File.separatorChar+chainName+"["+String.valueOf(i) +"]"+File.separatorChar+"rule";
			
			
		}
	}
	
	public void addToAugeas(String parent,String param){
		
	}
	
}
