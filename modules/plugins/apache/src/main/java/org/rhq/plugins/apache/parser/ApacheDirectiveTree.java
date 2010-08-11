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

    public static String [] NESTED_DIRECTIVES = {"<IfModule","<Directive","<VirtualHost","<Files"};
    
    /**
     * Searches for all nodes in parent nodes's subtree which matches specified criteria. 
     * 
     * @param parentNodeNames 
     * @param nodeName
     * @param startNode
     * @return
     */
    public List<ApacheDirective> searchNode(String [] parentNodeNames,String nodeName,ApacheDirective startNode){
     List<ApacheDirective> nodes = new ArrayList<ApacheDirective>();   
     search(nodes,parentNodeNames,startNode,nodeName);
     return nodes;
    }
    
    private void search(List<ApacheDirective>nodes,String [] parentNodeNames,ApacheDirective node,String name){
        
        nodes.addAll(node.getChildByName(name));
        
         for (String parentNodeName : parentNodeNames){
           List<ApacheDirective> nds = node.getChildByName(parentNodeName);
           for (ApacheDirective tempNode : nds){
               search(nodes,parentNodeNames,tempNode, name);
              }
          }    
    }
    

        
    /**
     * If there is a more nodes which have the same node name and the same value of parameters than we need index
     * for identification of Node.
     * 
     * @param node
     * @return
     */
    public int getNodeIndex(ApacheDirective node){
        List<ApacheDirective> nodes = node.getParentNode().getChildByName(node.getName());
        String param = node.getValuesAsString();
        int index = 1;
        
        for(ApacheDirective nd : nodes){
            if (nd.getValuesAsString().equals(param)){
                if (node.equals(nd))
                    return index;
                else
                    index++;
            }
        }
        return index;
    }
    /**
     * Returns the unique identifier of node. By this string we can identify node from parentNode's child nodes.
     * @param node
     * @return
     */
    public String getNodeKeyFromParent(ApacheDirective node){
       
        String param = node.getValuesAsString();
        int index = getNodeIndex(node);
       return  (node.getName()+"|"+param+"|"+String.valueOf(index)+";");
        
    }
    
    /**
     * Returns the unique identifier of node. By this string we can identify the node in parent node's subtree.
     * @param node
     * @param parentNode
     * @return
     */
    public String getNodeKey(ApacheDirective node,ApacheDirective parentNode){
        ApacheDirective pNode = node;
        StringBuilder str = new StringBuilder();
        while (pNode!=null & !pNode.equals(parentNode))
         {                       
            str.append(getNodeKeyFromParent(pNode));
            pNode = pNode.getParentNode();
         }      
        return str.toString();
    }
    /**
     * Finds the ApacheDirective node indentified by params.
     * @param parentNode
     * @param params
     * @return
     */
    public ApacheDirective findNodeById(ApacheDirective parentNode,String params){
        
        ApacheDirective tempNode=parentNode;
        String [] ids = params.split(";");
        for (int i=ids.length-1;i>=0;i--){         
            tempNode = getNodeFromParentById(tempNode,ids[i]);
        }        
        return tempNode;
    }
    
    public ApacheDirective getNodeFromParentById(ApacheDirective parentNode,String params){
       String nodeName;       
       String [] paramsArray = params.split("\\|");
       if (paramsArray.length != 3)
           throw new RuntimeException("Node with id "+params +" was not found.");
       
       nodeName = paramsArray[0];
                   
       for (ApacheDirective nd : parentNode.getChildByName(nodeName)){  
              String ndParam = getNodeKeyFromParent(nd);
              if (ndParam.equals(params+";")){                
                    return nd;                
           }
       }
       
      throw new RuntimeException("Searched node with id " + params+ " was not found.");  
    }
}
