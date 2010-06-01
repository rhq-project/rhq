package org.rhq.plugins.apache.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.util.Glob;

public class ApacheParserImpl implements ApacheParser{

    private ApacheDirectiveStack stack;
    private static String INCLUDE_DIRECTIVE = "Include";
    private String serverRootPath;
    private ApacheDirectiveTree tree;
    
    public ApacheParserImpl(ApacheDirectiveTree tree,String serverRootPath){          
      stack = new ApacheDirectiveStack();
      this.serverRootPath = serverRootPath;
      this.tree = tree;
      stack.addDirective(this.tree.getRootNode());
    }
    
    public void addDirective(ApacheDirective directive) throws Exception{
        if (directive.getName().equals(INCLUDE_DIRECTIVE)){
            List<File> files = getIncludeFiles(directive.getValuesAsString());
            for (File fl : files){
              ApacheConfigReader.searchFile(fl.getAbsolutePath(), this);   
            }
        }
        directive.setParentNode(stack.getLastDirective());
        stack.getLastDirective().addChildDirective(directive);
    }

    public void endNestedDirective(ApacheDirective directive) {       
        stack.removeLastDirective();        
    }

    public void startNestedDirective(ApacheDirective directive) {
      directive.setParentNode(stack.getLastDirective());
      stack.getLastDirective().addChildDirective(directive);
      stack.addDirective(directive);      
    }

    private  List<File> getIncludeFiles(String foundInclude) {
        List<File> ret = new ArrayList<File>();
        File serverRootFile = new File(serverRootPath);
            File check = new File(foundInclude);
            if (check.isAbsolute()) {
                ret.add(check);
            } else {
                for (File f : Glob.match(serverRootFile, foundInclude)) {
                    ret.add(f);
                }
            }
        
        return ret;
    }
}
