package org.rhq.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.tree.AugeasTreeLazy;
import org.rhq.augeas.tree.AugeasTreeReal;
import org.rhq.augeas.util.Glob;

public class AugeasComponent {

	private AugeasConfiguration config;
	private Augeas augeas;
	private List<String> modules;

	public AugeasComponent(AugeasConfiguration config){
		this.config = config;
		modules = new ArrayList<String>();
	}
	
	public void load() throws Exception
	{
            augeas = new Augeas(config.getRootPath(), config.getLoadPath(), config.getMode());
            
            for (AugeasModuleConfig module : config.getModules()){
                   
            		 checkModule(module);
            		 modules.add(module.getModuletName());
            		 augeas.set("/augeas/load/" + module.getModuletName() + "/lens", module.getLensPath());

                     int idx = 1;
                     for (String incl : module.getIncludedGlobs()) {
                          augeas.set("/augeas/load/" + module.getModuletName() + "/incl[" + (idx++) + "]", incl);
                         }
                     idx = 1;
                    
                     if (module.getExcludedGlobs()!=null)
                       for (String excl : module.getExcludedGlobs()) {
                          augeas.set("/augeas/load/" + module.getModuletName() + "/excl[" + (idx++) + "]", excl);
                         }
            	            	
            }
            augeas.load();	
	}
	
	
	 private void checkModule(AugeasModuleConfig module) {
	        File root = new File(config.getRootPath());

	        List<String> includeGlobs = module.getIncludedGlobs();

	        if (includeGlobs.size()<=0) {
	            throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
	        }

	        List<File> files = Glob.matchAll(root, includeGlobs);

	        if (module.getExcludedGlobs()!=null)
	    	{
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
	        }
	    }
	 
	 public AugeasTree getAugeasTree(String name,boolean lazy) throws AugeasTreeException
	 {
		if (!modules.contains(name))
			throw new AugeasTreeException("Augeas Module "+ name +" not found.");
		
		try {
			if (augeas == null)
				load();
		}catch(Exception e){
			throw new AugeasTreeException("Loading of augeas failed");
		}
		
		AugeasModuleConfig module = null;
		
		for (AugeasModuleConfig conf : config.getModules())
		{
			if (conf.getModuletName().equals(name))
				{
				module = conf; 
				break;
				}
		}
		AugeasTree tree;
		
		if (lazy==true)
			tree = new AugeasTreeLazy(augeas,module);
		else
			tree = new AugeasTreeReal(augeas,module);

		return tree;
	 }
	 
		public String printTree(String path){
			StringBuilder builder = new StringBuilder();
			builder.append(path +  "    "+ augeas.get(path)+'\n');
			List<String> list = augeas.match(path+File.separatorChar+"*");
			for (String tempStr : list)
			{
				builder.append(printTree(tempStr));	
			}
			
			return builder.toString();
		}
	
		public Augeas getAugeas()
		{
			return augeas;
		}
}
