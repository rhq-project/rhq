package org.rhq.plugins.apache.augeas;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.util.Glob;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.RhqConfig;


public class AugeasConfigurationApache extends RhqConfig implements AugeasConfiguration {

    public static String INCLUDE_DIRECTIVE = "Include";
    private AugeasModuleConfig module;
    private String INCLUDE_FILES_PATTERN = "^[\t ]*Include[\t ]+(.*)$";
    private String SERVER_ROOT_PATTERN = "^[\t ]*ServerRoot[\t ]+[\"]?([^\"\n]*)[\"]?$";
     
    private Pattern includePattern = Pattern.compile(INCLUDE_FILES_PATTERN);
    private Pattern serverRootPattern = Pattern.compile(SERVER_ROOT_PATTERN);
    private String serverRootPath;
    
    public String getServerRootPath() {
		return serverRootPath;
	}
    
	public AugeasConfigurationApache(Configuration configuration) throws AugeasRhqException {
        super(configuration);
       
        if (modules.isEmpty())
            throw new AugeasRhqException("There is not configuration for this resource.");
        try {
            module = modules.get(0);
           
            loadIncludes(module.getIncludedGlobs().get(0));
        } catch (Exception e) {
            throw new AugeasRhqException(e.getMessage());
        }
    }

    public List<String> getIncludes(File file) {
        List<String> includeFiles = new ArrayList<String>();

        return includeFiles;
    }

<<<<<<< HEAD:modules/plugins/apache/src/main/java/org/rhq/plugins/apache/augeas/AugeasConfigurationApache.java
  
    public void loadIncludes(String expression){
		    
	        try {
	    	   File file = new File(expression);
	            
	    	   if (file.exists())
	             {
	        	  FileInputStream fstream = new FileInputStream(file);
	        	    DataInputStream in = new DataInputStream(fstream);
	        	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        	    String strLine;
	        	    while ((strLine = br.readLine()) != null)   {
	        	    	Matcher m = includePattern.matcher(strLine);
	        	    	if (m.matches())
                          {
	        	    		String glob = m.group(1);
                    	    module.addIncludedGlob(glob);
                    	    loadIncludes(glob);
                          }
	        	    	Matcher serverRootMatcher = serverRootPattern.matcher(strLine);
	        	    	if (serverRootMatcher.matches())
                         {
	        	    		serverRootPath = serverRootMatcher.group(1);
                         }
	        	    }
	        	    in.close();
	              }
	    	   
	        }catch(Exception e){
	        	throw new IllegalStateException(e);
	        }
=======
    public void updateIncludes() throws Exception {

    	
		boolean updated = false;
		AugeasProxy augeas = new AugeasProxy(this);
		augeas.load();
		
		AugeasTree tree = augeas.getAugeasTree(module.getModuletName(), true);
		
		AugeasNode nd = tree.getRootNode();
		List<AugeasNode> nds = nd.getChildNodes();
	
		for (AugeasNode ns : nds)
		{
		List<AugeasNode> nodes = tree.matchRelative(ns,"/Include");
		for (AugeasNode node : nodes)
		{
			String value = node.getValue();
			if (!module.getIncludedGlobs().contains(value))
			{
				module.addIncludedGlob(value);
				updated = true;
			}
		}
		}
		if (updated)
			updateIncludes();		
>>>>>>> e6d33692e86119a1c8a9c8278e5240131b84b5f3:modules/plugins/apache/src/main/java/org/rhq/plugins/apache/augeas/AugeasConfigurationApache.java
    }
    
	public void loadFiles() {
		  File root = new File(serverRootPath);

		  for (AugeasModuleConfig module : modules){
	        List<String> includeGlobs = module.getIncludedGlobs();

	        if (includeGlobs.size() <= 0) {
	            throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
	        }
	        
	        ArrayList<File> files = new ArrayList<File>();
	        
	        for (String incl : includeGlobs){
	               if (incl.indexOf(File.separatorChar)==0){
	            	   files.add(new File(incl));
	               }else
	            	   files.addAll(Glob.match(root, incl));
		        }
	        

	        if (module.getExcludedGlobs() != null) {
	            List<String> excludeGlobs = module.getExcludedGlobs();
	            Glob.excludeAll(files, excludeGlobs);
	        }

	        for (File configFile : files) {
	            if (!configFile.isAbsolute()) {
	                throw new IllegalStateException("Configuration files inclusion patterns contain a non-absolute file.");
	            }
	            if (!configFile.exists()) {
	                throw new IllegalStateException("Configuration files inclusion patterns refer to a non-existent file.");
	            }
	            if (configFile.isDirectory()) {
	                throw new IllegalStateException("Configuration files inclusion patterns refer to a directory.");
	            }
	            if (!module.getConfigFiles().contains(configFile.getAbsolutePath()))
	                module.addConfigFile(configFile.getAbsolutePath());
	        }
		 }
	}
        
}
