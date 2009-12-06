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

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.RhqConfig;

public class AugeasConfigurationApache extends RhqConfig implements AugeasConfiguration {

    public static String INCLUDE_DIRECTIVE = "Include";
    private AugeasModuleConfig module;
    private String INCLUDE_FILES_PATTERN = "^[\t ]*Include[\t ]+(.*)$";
    private Pattern includePattern = Pattern.compile(INCLUDE_FILES_PATTERN);
    

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

  
    private void loadIncludes(String expression){
     
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
                       }
                    in.close();
                    }
                    
           }catch(Exception e){
                    e.printStackTrace();
            }
               
    }
        
}
