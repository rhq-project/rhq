package org.rhq.plugins.apache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pc.PluginContainer;
import org.rhq.plugins.apache.augeas.AugeasConfigurationApache;
import org.rhq.plugins.apache.augeas.AugeasTreeBuilderApache;
import org.rhq.plugins.apache.helper.LensHelper;
import org.rhq.plugins.apache.util.ApacheConfigurationUtil;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

public class ApacheAugeasUtil {

    /**
     * Loads augeas.
     * @param configFilePath
     * @param serverRootPath
     * @param lensPath
     * @return
     */
    public AugeasProxy initAugeas(String configFilePath,String serverRootPath,String lensPath){         
        Configuration configuration = new Configuration();
        configuration.put(new PropertySimple("configurationFilesInclusionPatterns",configFilePath));
        configuration.put(new PropertySimple("augeasModuleName", ApacheTestConstants.MODULE_NAME));
        configuration.put(new PropertySimple(ApacheTestConstants.PLUGIN_CONFIG_PROP_SERVER_ROOT,serverRootPath));
        
        AugeasConfigurationApache conf = new AugeasConfigurationApache(lensPath,configuration);      
        AugeasTreeBuilderApache builder = new AugeasTreeBuilderApache();
        AugeasProxy augeas = new AugeasProxy(conf,builder);
      
        augeas.load();
        
        return augeas;
    }
  
    /**
     * This method will create a new temporary directory and copy there all configuration files.
     * @return
     * @throws UnitTestException
     */
  public String prepareConfigFiles() {
      String tempDirPath=null;
      try {

        File tempDirectory = LensHelper.createTempDir(ApacheTestConstants.TEMP_CONFIG_FILE_DIRECTORY);
        if (!tempDirectory.exists())
            tempDirectory.createNewFile();
        //copy the apache configuration files to temp folder        
        tempDirPath = tempDirectory.getAbsolutePath()+File.separatorChar;
        copyFiles(ApacheTestConstants.FILES_TO_LOAD, "", tempDirectory);
        
        File updateDir = new File(tempDirectory,ApacheTestConstants.TEST_FILE_APACHE_CONFIG_FOLDER);
        if (!updateDir.exists()){
            updateDir.mkdir();
            updateDir.createNewFile();
        }
        //copy the apache configuration files to temp folder "updateconfig"
        copyFiles(ApacheTestConstants.FILES_TO_LOAD, ApacheTestConstants.TEST_FILE_APACHE_CONFIG_FOLDER, tempDirectory);
        
        File loadDir = new File(tempDirectory,ApacheTestConstants.TEST_FILE_CONFIG_FOLDER);
        if (!loadDir.exists()){
            loadDir.mkdir();
            loadDir.createNewFile();
        }
        //copy xml files with rhq configuration to loadconfig 
        for(Components component : Components.values()){
            copyFiles(component.getConfigurationFiles().toArray(new String[0]), ApacheTestConstants.TEST_FILE_CONFIG_FOLDER, tempDirectory);
        }
       
        ApacheTestConstants.TEMP_FILES_PATH=tempDirectory.getAbsolutePath();
        
      }catch (Exception e){
          e.printStackTrace();
      }
      
      return tempDirPath;
  }
  
  public void cleanConfigFiles(){
      try {

        File tempDirectory = new File(ApacheTestConstants.getApacheConfigFilesPath());
        if (!tempDirectory.exists())
            return;
                
        deleteFiles(ApacheTestConstants.FILES_TO_LOAD, "", tempDirectory);
        
        File updateDir = new File(tempDirectory,ApacheTestConstants.TEST_FILE_APACHE_CONFIG_FOLDER);
        if (updateDir.exists()){
        deleteFiles(ApacheTestConstants.FILES_TO_LOAD, ApacheTestConstants.TEST_FILE_APACHE_CONFIG_FOLDER, tempDirectory);
        updateDir.delete();
        }
        File loadDir = new File(tempDirectory,ApacheTestConstants.TEST_FILE_CONFIG_FOLDER);
        if (loadDir.exists()) 
           {
            for(Components component : Components.values()){
              deleteFiles(component.getConfigurationFiles().toArray(new String[0]), ApacheTestConstants.TEST_FILE_CONFIG_FOLDER, tempDirectory);
             }
            loadDir.delete();
           }
       
       tempDirectory.delete();        
      }catch (Exception e){
          e.printStackTrace();
      }      
  }
  public void copyFiles(String[] files,String folder,File destination) throws Exception{
  for (String fileName : files){
      String path=null;
      
      if (folder.equals(""))
          path = fileName;
      else
          path = folder+File.separator+fileName;
      
      File configFile = LensHelper.cpFileFromPluginToTemp(this.getClass().getClassLoader(),destination, path);
      if (!configFile.exists())
          throw new UnitTestException("Creation of temporary configuration file failed.");
     }
  }
  
  public void deleteFiles(String[] files,String folder,File destination) throws Exception{
      for (String fileName : files){
          String path=null;
          
          if (folder.equals(""))
              path = fileName;
          else
              path = folder+File.separator+fileName;
                    
          File configFile =  new File(destination,path);;
          if (configFile.exists())
              configFile.delete();
      }
  }
 /**
  * Method for searching all components in ageasTree which returns List of keys which identify the component.
  * @param parentNodeNames
  * @param componentName
  * @param tree
  * @return
  */
  public List<String> loadComponent(String[] parentNodeNames,String componentName,AugeasTree tree){
        List<String> paramsString = new ArrayList<String>();
        List<AugeasNode> nodes = AugeasNodeSearch.searchNode(parentNodeNames, componentName, tree.getRootNode());
        for (AugeasNode node : nodes){
            paramsString.add(AugeasNodeSearch.getNodeKey(node, tree.getRootNode()));
        }      
       return paramsString;
    }
  
  public void saveFiles(AugeasTree tree,PluginContainer container) throws Exception{
      for (Components component : Components.values()){
          String name = component.getComponentName();
          List<AugeasNode> nodes = component.getAllNodes(tree);
          for (int i=0;i<nodes.size();i++){
              String key = AugeasNodeSearch.getNodeKey(nodes.get(i), tree.getRootNode());
              //PATH TO THE CONFIG FILES
              String fileName = name+String.valueOf(i);
              Configuration config = ApacheConfigurationUtil.componentToConfiguration(container, component, key, tree);
              ApacheConfigurationUtil.saveConfiguration(config, fileName);
          }
      }
  }
}
