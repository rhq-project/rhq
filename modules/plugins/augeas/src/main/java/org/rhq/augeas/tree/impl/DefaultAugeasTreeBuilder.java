package org.rhq.augeas.tree.impl;

import java.io.File;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasRootNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;

public class DefaultAugeasTreeBuilder implements AugeasTreeBuilder{
	 private static String AUGEAS_DATA_PATH=File.separatorChar+"files";
	 
	public DefaultAugeasTreeBuilder(){}
	
	public AugeasTree buildTree(AugeasProxy component,
			AugeasConfiguration moduleConfig, String name, boolean lazy)
			throws Exception {
		
             AugeasTree tree ;
             AugeasModuleConfig module = moduleConfig.getModuleByName(name);
             if (lazy = true)
            	 tree = new AugeasTreeLazy(component.getAugeas(),module);
             else
            	 tree = new AugeasTreeReal(component.getAugeas(),module);
             
             AugeasNode rootNode = new AugeasRootNode();
             
	              for (String fileName : module.getConfigFiles())
	                     {
	                     rootNode.addChildNode(tree.createNode(AUGEAS_DATA_PATH+File.separatorChar+fileName));
	                     }
            
	          tree.setRootNode(rootNode);
	    
		return tree;
	}

}
