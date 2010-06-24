package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.List;

import org.rhq.plugins.apache.parser.ApacheDirective;

public class AugeasNodeSearch {
    public static String [] NESTED_DIRECTIVES = {"<IfModule","<Directive","<VirtualHost","<Files"};
    
    /**
     * Searches for all nodes in parent nodes's subtree which matches specified criteria. 
     * 
     * @param parentNodeNames 
     * @param nodeName
     * @param startNode
     * @return
     */
    public static List<ApacheDirective> searchNode(String [] parentNodeNames,String nodeName,ApacheDirective startNode){
     List<ApacheDirective> nodes = new ArrayList<ApacheDirective>();   
     search(nodes,parentNodeNames,startNode,nodeName);
     return nodes;
    }
    
    private static void search(List<ApacheDirective>nodes,String [] parentNodeNames,ApacheDirective node,String name){
        
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
    public static int getNodeIndex(ApacheDirective node){
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
    public static String getNodeKeyFromParent(ApacheDirective node){
       
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
    public static String getNodeKey(ApacheDirective node,ApacheDirective parentNode){
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
    public static ApacheDirective findNodeById(ApacheDirective parentNode,String params){
        
        ApacheDirective tempNode=parentNode;
        String [] ids = params.split(";");
        for (int i=ids.length-1;i>=0;i--){         
            tempNode = getNodeFromParentById(tempNode,ids[i]);
        }        
        return tempNode;
    }
    
    public static ApacheDirective getNodeFromParentById(ApacheDirective parentNode,String params){
       String nodeName;
       String parameters;
       int index=1;
       
       String [] paramsArray = params.split("\\|");
       if (paramsArray.length != 3)
           throw new RuntimeException("Node with id "+params +" was not found.");
       
       nodeName = paramsArray[0];
       parameters = paramsArray[1];
            
       index = Integer.valueOf(paramsArray[2]);
                   
          for (ApacheDirective nd : parentNode.getChildByName(nodeName)){  
              String ndParam = getNodeKeyFromParent(nd);
              if (ndParam.equals(params+";")){                
                    return nd;                
           }
       }
       
      throw new RuntimeException("Searched node with id " + params+ " was not found.");  
    }
}
 
