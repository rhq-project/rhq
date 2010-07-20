package org.rhq.plugins.apache;

import java.io.File;
import java.util.List;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pc.PluginContainer;
import org.rhq.plugins.apache.augeas.AugeasConfigurationApache;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.util.ApacheConfigurationUtil;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheAugeasTest {
	  
  	  /**
	   * Tests if all included configuration files were loaded. 
	   * @return
	   */
	  public void testFiles(AugeasProxy augeas){
	      System.out.println("Test if all included cofiguration files was discovered and loaded.");
	      AugeasConfigurationApache config = (AugeasConfigurationApache)augeas.getConfiguration();
	      List<File> configFiles = config.getAllConfigurationFiles();
	      
	      /*
	       * There are three files one main file one which is included from main file and one which is included from 
	       * included file and which is declared in IfModule. All of them must be discovered.
	       */
	      boolean found=false;
	      for (File confFile : configFiles){
	          found = false;
	          for (String fileName : ApacheTestConstants.CONFIG_FILE_NAMES){
	            if (!confFile.getName().equals(fileName))
	                found= true;
	          }
	          assert found;
	      }
	  }
      /**
       * Tests mapping of augeas tree to configuration and back.
       * @param cont
       * @throws Exception
       */
	  public void testMapping(PluginContainer cont){
	      ApacheAugeasUtil apacheUtil = new ApacheAugeasUtil();
	      try{
	      //copy all configuration files to temporary folder
	      String path = apacheUtil.prepareConfigFiles();
	      //loading of augeas from temporary folder
  	      AugeasProxy proxy = apacheUtil.initAugeas(path+File.separator+ApacheTestConstants.ROOT_CONFIG_FILE_NAME, path, path);	
	      AugeasTree tree = proxy.getAugeasTree(ApacheTestConstants.MODULE_NAME, true);
	      
	      testLoadConfig(tree, cont);
	      testSaveConfig(cont);
	      apacheUtil.cleanConfigFiles();
	      }catch(Exception e){
	          e.printStackTrace();
	          apacheUtil.cleanConfigFiles();
	      }
	  }
	 /**
	  * Tests mapping of augeas tree to rhq configuration.
	  * @param tree
	  * @param container
	  * @throws Exception
	  */
	 public void testLoadConfig(AugeasTree tree,PluginContainer container)throws Exception{
	      System.out.println("Test mapping of augeas tree to rhq configuration.");
	      ApacheConfigurationUtil util = new ApacheConfigurationUtil();
	        for (Components component : Components.values()){
	            //get nodes for each component
	              List<AugeasNode> nodes = component.getAllNodes(tree);
	              for (int i=0;i<nodes.size();i++){
	                  String key = AugeasNodeSearch.getNodeKey(nodes.get(i), tree.getRootNode());
	                  Configuration config = ApacheConfigurationUtil.componentToConfiguration(container, component, key, tree);
	                  //load configuration for related component and augeas node from tempfolder
	                  Configuration conf = util.loadConfiguration(ApacheTestConstants.getConfigFilesPathForLoad()+
	                                                     File.separator+component.getComponentName()+String.valueOf(i));
	                  assert config.equals(conf);	                      
	              }
	          }	      
	  }
	 
	  /**
	   * Tests mapping of rhq configuration to augeas tree.
	   * 
	   * @param container
	   * @throws Exception
	   */
	  public void testSaveConfig(PluginContainer container)throws Exception{
	         System.out.println("Test mapping of rhq configuration to augeas tree.");
	         ApacheAugeasUtil apacheUtil = new ApacheAugeasUtil();	         
	         //load augeas tree from temporary folder
	         String path = ApacheTestConstants.getApacheConfigFilesPath();
	         AugeasProxy proxy = apacheUtil.initAugeas(path+ApacheTestConstants.ROOT_CONFIG_FILE_NAME, path, path); 
	         AugeasTree tree = proxy.getAugeasTree(ApacheTestConstants.MODULE_NAME, true);
	         //load augeas tree from temporary folder "updateconfig"
	         String pathUpdate = ApacheTestConstants.getApacheConfigFilesPathForUpdate();
	         AugeasProxy proxyUpdate = apacheUtil.initAugeas(pathUpdate+ApacheTestConstants.ROOT_CONFIG_FILE_NAME, pathUpdate, pathUpdate); 
	         AugeasTree treeUpdate = proxyUpdate.getAugeasTree(ApacheTestConstants.MODULE_NAME, true);
	         
	         for (Components component : Components.values()){
                 List<AugeasNode> nodes = component.getAllNodes(tree);
                 ConfigurationDefinition configDef = ApacheConfigurationUtil.getConfigurationDefinition(container, component);

                 for (int i=0;i<nodes.size();i++){
                     //load component from first tree - transfare that tree to configuration - and update second tree with that configuration
                     String key = AugeasNodeSearch.getNodeKey(nodes.get(i), tree.getRootNode());
                     Configuration config = ApacheConfigurationUtil.componentToConfiguration(container, component, key, tree);
                     AugeasNode updateNode = AugeasNodeSearch.findNodeById(treeUpdate.getRootNode(), key);
                                         
                     ApacheAugeasMapping mapping = new ApacheAugeasMapping(treeUpdate);                     
                     mapping.updateAugeas(updateNode, config, configDef);
                 }
             }
	         treeUpdate.save();
             //test if the updated augeas tree can be mapped to configuration stored at files in "loadconfig" temporary directory 
	         testLoadConfig(treeUpdate, container);
	  }
}
