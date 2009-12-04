package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.util.Glob;


public class AugeasTreeBuilderApache implements AugeasTreeBuilder {

    private Map<String, List<File>> includes;
    private Map<AugeasNode, List<String>> incl;

    private Augeas ag;

    public AugeasTreeBuilderApache() {
        includes = new HashMap<String, List<File>>();
        incl = new HashMap<AugeasNode, List<String>>();
    }

    public AugeasTree buildTree(AugeasProxy component, AugeasConfiguration config, String name, boolean lazy)
        throws Exception {

        this.ag = component.getAugeas();

        AugeasModuleConfig module = config.getModuleByName(name);
        ApacheAugeasTree tree = new ApacheAugeasTree(component.getAugeas(), module);

        List<String> incl = module.getConfigFiles();

        if (incl.isEmpty())
            throw new Exception("No configuration provided.");

        String rootPath = incl.get(0);

        AugeasNode rootNode = new ApacheAugeasNode(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath, tree);

        tree.setRootNode(rootNode);
        File rootFile = new File(config.getRootPath());
        // we need to know which files are related to each glob
        for (String inclName : module.getIncludedGlobs()) {
            List<File> files = Glob.match(rootFile, inclName);
            if (module.getExcludedGlobs() != null)
                Glob.excludeAll(files, module.getExcludedGlobs());
            if (!includes.containsKey(inclName))
                includes.put(inclName, files);
        }

        updateIncludes((ApacheAugeasNode) rootNode, tree, rootPath);
        List<String> rootconf = new ArrayList<String>();
        rootconf.add(ApacheAugeasTree.AUGEAS_DATA_PATH + rootPath);
        this.incl.put(rootNode, rootconf);
        tree.setIncludes(this.incl);
        return tree;
    }

    public void updateIncludes(ApacheAugeasNode parentNode, AugeasTree tree, String fileName) throws Exception {

   	 
	  	  List<String> nestedNodes = ag.match(ApacheAugeasTree.AUGEAS_DATA_PATH+fileName+File.separator+"*");
	  	  
	  	  List<AugeasNode> createdNodes = new ArrayList<AugeasNode>();
	  	  
	  	  for (String nodeName : nestedNodes){
	  			  ApacheAugeasNode newNode = new ApacheAugeasNode(parentNode,tree,nodeName);
	  			  createdNodes.add(newNode);
	  		  }
	  		  
	  	   parentNode.addIncludeNodes(createdNodes);
		
      for (AugeasNode node : createdNodes){		  
            if (node.getLabel().equals("Include")){
	        	if (includes.containsKey(node.getValue()))
	        	{
	        		//include directive contains globNames
	        	  	List<File> files = includes.get(node.getValue());
	        	  	List<String> names = new ArrayList<String>();
	        	  	for (File file : files)
	        	  	{
	        	      names.add(ApacheAugeasTree.AUGEAS_DATA_PATH+file.getAbsolutePath());
	        	  	  updateIncludes((ApacheAugeasNode)node.getParentNode(),tree,file.getAbsolutePath());
	        	  	 }
	        	  	incl.put(node, names);
                 }
	        	}   
         }
    }
}
