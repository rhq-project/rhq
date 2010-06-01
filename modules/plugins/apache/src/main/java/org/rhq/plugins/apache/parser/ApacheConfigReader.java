package org.rhq.plugins.apache.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApacheConfigReader {
    private static final String EMPTY_LINE="^[\t ]*$";
    private static final Pattern emptyLinePattern = Pattern.compile(EMPTY_LINE);
    
    public static void buildTree(String path,ApacheParser parser) throws Exception{       
        searchFile(path, parser);          
    }
    
    public static void searchFile(String path,ApacheParser parser) throws Exception{
        File configFile = new File(path);
        if (!configFile.exists())
            throw new RuntimeException("Configuration file does not exist.");
        
        BufferedReader br=null;
        
        try {    
        FileInputStream fstream = new FileInputStream(configFile);
        br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        
        while ((strLine = br.readLine()) != null) {    
            Matcher matcher = emptyLinePattern.matcher(strLine);
            if (!matcher.matches())            
               {
                ApacheDirective dir = new ApacheDirective(strLine);
                dir.setFile(path);
                String name = dir.getName();
                if (!name.equals("#")){
                    if (name.startsWith("</"))
                        parser.endNestedDirective(dir);
                    else
                        if (name.startsWith("<"))
                            parser.startNestedDirective(dir);
                        else
                            parser.addDirective(dir);
                }
               }
        }
            br.close();
            
        }catch(Exception e){
            if (br!=null)
                br.close();
            throw new Exception(e);
        }
    }
}
