package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;

public class AugeasNodeSearch {
    public static String [] NESTED_DIRECTIVES = {"<IfModule","<Directive","<VirtualHost","<Files"};
    
    public static List<AugeasNode> searchNode(String [] parentNodeNames,String nodeName,AugeasNode startNode){
     List<AugeasNode> nodes = new ArrayList<AugeasNode>();   
     search(nodes,parentNodeNames,startNode,nodeName);
     return nodes;
    }
    
    private static void search(List<AugeasNode>nodes,String [] parentNodeNames,AugeasNode node,String name){
        
        nodes.addAll(node.getChildByLabel(name));
        
         for (String parentNodeName : parentNodeNames){
           List<AugeasNode> nds = node.getChildByLabel(parentNodeName);
           for (AugeasNode tempNode : nds){
               search(nodes,parentNodeNames,tempNode, name);
              }
          }    
    }
    
    public static List<AugeasNode> getNodeByParentParams(AugeasNode parentNode,String nodeName,List<String> nodeParams){
        
        List<AugeasNode> searchedNodes = AugeasNodeSearch.searchNode(NESTED_DIRECTIVES, nodeName, parentNode);
        List<AugeasNode> results = new ArrayList<AugeasNode>();
        
        for (AugeasNode node : searchedNodes){
            AugeasNode tempNode = node;
            boolean match = true;
            int i =0;           
             
            while (match & i<nodeParams.size() & tempNode!=parentNode){          
              List<AugeasNode> childNodes = tempNode.getChildByLabel("param");
              if (childNodes.size()>0){                  
                 if (!nodeParams.get(i).equals(childNodes.get(0).getValue()))
                     match = false;
                 }else
                     match = false;
              i=i+1;
              tempNode = tempNode.getParentNode();
            }            
                        
             if (match==true)
                results.add(node);
          }
        
        return results;
    }
    
    public static List<String> getParams(AugeasNode node,AugeasNode parentNode){
        List<String> res = new ArrayList<String>();   
    
        AugeasNode pNode = node;
        while (!pNode.equals(parentNode))
         {             
            List<AugeasNode> paramNodes = pNode.getChildByLabel("param");
            for (AugeasNode param : paramNodes){
                res.add(param.getValue());
              }
            pNode = pNode.getParentNode();
         }
        return res;
       }
    
    public static String getParamsString(AugeasNode node,AugeasNode parentNode){
        StringBuffer str = new StringBuffer();   
    
        AugeasNode pNode = node;
        while (!pNode.equals(parentNode))
         {             
            List<AugeasNode> paramNodes = pNode.getChildByLabel("param");
            for (AugeasNode param : paramNodes){
                str.append(param.getValue()+";");
              }
            pNode = pNode.getParentNode();
         }
       
        return str.toString();
       }
    
    
}
