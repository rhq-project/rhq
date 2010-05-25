package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;

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

    /**
     * Returns all params of node as one string.
     * @param nd
     * @return
     */
    public static String getNodeParamString(AugeasNode nd){
        List<AugeasNode> childNodes = nd.getChildByLabel("param");
        StringBuilder str = new StringBuilder();
        for (AugeasNode paramNode : childNodes){
            str.append(paramNode.getValue());
        }
        return str.toString();   
    }
    
    /**
     * If there is a more nodes which have the same node name and the same value of parameters than we need index
     * for identification of Node.
     * 
     * @param node
     * @return
     */
    public static int getNodeIndex(AugeasNode node){
        List<AugeasNode> nodes = node.getParentNode().getChildByLabel(node.getLabel());
        String param = getNodeParamString(node);
        int index = 1;
        
        for(AugeasNode nd:nodes){
            if (getNodeParamString(nd).equals(param)){
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
    public static String getNodeKeyFromParent(AugeasNode node){
       
        String param = getNodeParamString(node);
        int index = getNodeIndex(node);
       return  (node.getLabel()+"|"+param+"|"+String.valueOf(index)+";");
        
    }
    
    /**
     * Returns the unique identifier of node. By this string we can identify the node in parent node's subtree.
     * @param node
     * @param parentNode
     * @return
     */
    public static String getNodeKey(AugeasNode node,AugeasNode parentNode){
        AugeasNode pNode = node;
        StringBuilder str = new StringBuilder();
        while (pNode!=null & !pNode.equals(parentNode))
         {                       
            str.append(getNodeKeyFromParent(pNode));
            pNode = pNode.getParentNode();
         }      
        return str.toString();
    }
    
    public static AugeasNode findNodeById(AugeasNode parentNode,String params){
        
        AugeasNode tempNode=parentNode;
        String [] ids = params.split(";");
        for (int i=ids.length-1;i>=0;i--){         
            tempNode = getNodeFromParentById(tempNode,ids[i]);
        }        
        return tempNode;
    }
    
    public static AugeasNode getNodeFromParentById(AugeasNode parentNode,String params){
       String nodeName;
       String parameters;
       int index=1;
       
       String [] paramsArray = params.split("\\|");
       if (paramsArray.length != 3)
           throw new RuntimeException("Node with id "+params +" was not found.");
       
       nodeName = paramsArray[0];
       parameters = paramsArray[1];
            
       index = Integer.valueOf(paramsArray[2]);
                   
          for (AugeasNode nd : parentNode.getChildByLabel(nodeName)){  
              String ndParam = getNodeKeyFromParent(nd);
              if (ndParam.equals(params+";")){                
                    return nd;                
           }
       }
       
      throw new RuntimeException("Searched node with id " + params+ " was not found.");  
    }
}
 
