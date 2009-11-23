package org.rhq.rhqtransform;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.augeas.Augeas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;



public class RhqConfig implements AugeasConfiguration{

	    public static final String INCLUDE_GLOBS_PROP = "configurationFilesInclusionPatterns";
	    public static final String EXCLUDE_GLOBS_PROP = "configurationFilesExclusionPatterns";
	    public static final String AUGEAS_MODULE_NAME_PROP = "augeasModuleName";
	    private static final String AUGEAS_LOAD_PATH = "augeasLoadPath";  
	    
	    public static final String DEFAULT_AUGEAS_ROOT_PATH = File.listRoots()[0].getPath();
	    
	    private final Log log = LogFactory.getLog(this.getClass());
	    private List<AugeasModuleConfig> modules;
	    private String loadPath;
	    
	public RhqConfig(Configuration configuration) throws AugeasRhqException{
		List<String> includes = determineGlobs(configuration,INCLUDE_GLOBS_PROP);
		List<String> excludes = determineGlobs(configuration,EXCLUDE_GLOBS_PROP);
		modules = new ArrayList<AugeasModuleConfig>();
	    if (includes.isEmpty())
	    	throw new AugeasRhqException("At least one Include glob must be defined.");

	      AugeasModuleConfig config = new AugeasModuleConfig();
	      config.setIncludedGlobs(includes);
	      config.setExcludedGlobs(excludes);
	      config.setLensPath(getAugeasModuleName(configuration)+".lns");
	      config.setModuletName(getAugeasModuleName(configuration));
	      modules.add(config);	
	      
	      loadPath = configuration.getSimpleValue(AUGEAS_LOAD_PATH, null);
	}
	
	
  protected List<String> determineGlobs(Configuration configuration,String name) {
      PropertySimple includeGlobsProp = configuration.getSimple(name);
      if (includeGlobsProp== null)
    	  return null;
      
      List<String> ret = new ArrayList<String>();
      ret.addAll(getGlobList(includeGlobsProp));
      
      return ret;
  }
  
  protected String getAugeasModuleName(Configuration configuration){
	  return (configuration.getSimpleValue(AUGEAS_MODULE_NAME_PROP, null));
  }
  
  public static PropertySimple getGlobList(String name, List<String> simples) {
      StringBuilder bld = new StringBuilder();
      if (simples != null) {
          for (String s : simples) {
              bld.append(s).append("|");
          }
      }
      if (bld.length() > 0) {
          bld.deleteCharAt(bld.length() - 1);
      }
      return new PropertySimple(name, bld);
  }

  public static List<String> getGlobList(PropertySimple list) {
      if (list != null) {
          return Arrays.asList(list.getStringValue().split("\\s*\\|\\s*"));
      } else {
          return Collections.emptyList();
      }
  }

  public Configuration updateConfiguration(Configuration configuration) throws AugeasRhqException{
	  if (modules.isEmpty())
		  throw new AugeasRhqException("Error in augeas Configuration.");
	  AugeasModuleConfig tempModule = modules.get(0);
	  
      PropertySimple includeProps = getGlobList(INCLUDE_GLOBS_PROP, tempModule.getIncludedGlobs());
      PropertySimple excludeProps = getGlobList(EXCLUDE_GLOBS_PROP, tempModule.getExcludedGlobs());
      configuration.put(includeProps);
      configuration.put(excludeProps);
      
      return configuration;
  }
  
public String getLoadPath() {
	return loadPath;
}

public int getMode() {
	return Augeas.NO_MODL_AUTOLOAD;
}

public List<AugeasModuleConfig> getModules() {
	return modules;
}

public String getRootPath() {
	return DEFAULT_AUGEAS_ROOT_PATH;
}
}
