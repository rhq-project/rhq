package org.rhq.plugins.apache.parser;

import java.util.ArrayList;
import java.util.List;

public class ApacheDirectiveTree {

    private ApacheDirective rootNode;
    private List<String> globs;
    private List<String> includedFiles;
    
    public ApacheDirectiveTree(){
       rootNode = new ApacheDirective();
       rootNode.setRootNode(true);
    }

    public ApacheDirective getRootNode() {
        return rootNode;
    }

    public void setRootNode(ApacheDirective rootNode) {
        this.rootNode = rootNode;
    }
    
    public List<ApacheDirective> search(ApacheDirective nd,String name){
        return parseExpr(nd,name);
    }
    
    public List<ApacheDirective> search(String name){     
        if (name.startsWith("/"))
          return parseExpr(rootNode,name.substring(1));
        else
          return parseExpr(rootNode,name);
    }
    
    private List<ApacheDirective> parseExpr(ApacheDirective nd, String expr)  {
        int index = expr.indexOf("/");
        String name;
        
        if (index ==-1)
            name = expr;
        else
           name = expr.substring(0,index);
        
        List<ApacheDirective> nds = new ArrayList<ApacheDirective>();
        
        for (ApacheDirective dir : nd.getChildByName(name)){
            if (index ==-1)
                nds.add(dir);
            else{
              List<ApacheDirective> tempNodes = parseExpr(dir, expr.substring(index+1));
              if (tempNodes != null)
                  nds.addAll(tempNodes);
            }
        }
        
        return nds;
    }
    
    public ApacheDirective createNode(ApacheDirective parentNode,String name){
       ApacheDirective dir = new ApacheDirective(name);
       dir.setParentNode(parentNode);
       parentNode.addChildDirective(dir);
       return dir;
    }

    public List<String> getGlobs() {
        return globs;
    }

    public void setGlobs(List<String> globs) {
        this.globs = globs;
    }

    public List<String> getIncludedFiles() {
        return includedFiles;
    }

    public void setIncludedFiles(List<String> includedFiles) {
        this.includedFiles = includedFiles;
    }
    
    public void addGlob(String glob){
        if (globs==null)
            globs = new ArrayList<String>();
        globs.add(glob);
    }
    
    public void addIncludedFile(String file){
        if (includedFiles==null)
            includedFiles = new ArrayList<String>();

        includedFiles.add(file);
    }
    }
