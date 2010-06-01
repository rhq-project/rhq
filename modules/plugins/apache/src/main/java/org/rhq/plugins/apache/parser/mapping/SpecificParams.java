package org.rhq.plugins.apache.parser.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class is used for avoid using of specific parameters which can not be used in augeas resp. in Rhq configuration.
 * During the transformation from augeas tree to configuration and back we need to replace directive specific parameters
 * by parameters which are understandable for RHQ configuration. 
 * @author fdrabek
 *
 */
public class SpecificParams {

    /**
     * Before mapping back to cofiguration file we need to replace all configuration specific parameters
     * by directive specific parameters.
     * 
     * @param name Name of the directive.
     * @param param Parameters of directive. 
     * @return directive prepared for validation without configuration specific parameters.
     */
    public static String prepareForAugeas(String name,String value){
        if (name.equals("Options"))
        {
            StringBuilder ret = new StringBuilder();
            Pattern pattern =  Pattern.compile("[ \t]*(Add|Remove|Set)[ \t]*([a-zA-Z]+)"); 
            int startIndex = 0;
            boolean updated = true;
            while (updated & startIndex < value.length()){
                updated = false;
                    Matcher m = pattern.matcher(value);
                    while (m.find(startIndex)) {
                        if (m.groupCount() >0)
                        for (int i = 1; i <= m.groupCount(); i++) {
                            String val = m.group(i);
                            if (val.equals("Add"))
                                val = "+";
                            if (val.equals("Remove"))
                                val = "-";
                            if (val.equals("Set"))
                                val = "";
                            ret.append(val+ " ");
                        }
                        updated = true;
                        startIndex = m.end();
                    }       
            }
            if (ret.length()>0)
              ret.deleteCharAt(ret.length()-1);            
            
            return ret.toString();
        }
        
        if (name.equals("Listen")){
            int i;
            StringBuilder val = new StringBuilder(value);
            while (val.charAt(0) == ' ' | val.charAt(0) == '\t')
                val.deleteCharAt(0);
            
            String [] str = val.toString().split("[ \t]+");
            
            Pattern pat = Pattern.compile("[0-9]+");
            StringBuilder bld = new StringBuilder();
            for (i =0;i<str.length;i++){
                Matcher m = pat.matcher(str[i]);
                if (m.matches())
                {if (i==1)
                  str[i] = ":"+str[i];
                else
                    str[i]=" "+str[i];
                }else
                    str[i]=" "+str[i];
                bld.append(str[i]);
            }
        return bld.toString();
        }
        
        return value;

    }

    /**
     * Before mapping back to cofiguration file we need to replace all configuration specific parameters
     * by directive specific parameters.
     * 
     * @param name Name of the directive.
     * @param param Parameters of directive. 
     * @return directive prepared for validation without configuration specific parameters.
     */

    public static StringBuilder prepareForConfiguration(String name,StringBuilder value){
        if (name.equals("Options"))
        {
            StringBuilder ret = new StringBuilder();
            Pattern pattern =  Pattern.compile("[ \t]*([+-]?)([a-zA-Z]+)"); 
            int startIndex = 0;
            boolean updated = true;
            while (updated & startIndex < value.length()){
                updated = false;
                    Matcher m = pattern.matcher(value);
                    while (m.find(startIndex)) {
                        if (m.groupCount() >0)
                        for (int i = 1; i <= m.groupCount(); i++) {
                            String val = m.group(i);
                            if (val.equals("+"))
                                val = "Add";
                            if (val.equals("-"))
                                val = "Remove";
                            if (val.equals(""))
                                val = "Set";
                            ret.append(val+ " ");
                        }
                        updated = true;
                        startIndex = m.end();
                    }       
            }
            if (ret.length()>0)
              ret.deleteCharAt(ret.length()-1);            
            
            return ret;
        }
        
        return value;
    }
}
