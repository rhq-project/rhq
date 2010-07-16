package org.rhq.plugins.apache;

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.plugins.apache.util.AugeasNodeSearch;

/**
 * Enum of all components in Apache plugin.
 * @author fdrabek
 *
 */
public enum Components {

    IFMODULE{
       public String getComponentName(){
            return "IfModule";
       }
       public String[] getPossParentNodeName(){
         String [] parentNames = new String[2];
         parentNames[0]="<IfModule";
         parentNames[1]="<VirtualHost";
         return parentNames;
       }
       public String getNodeName(){
           return "<IfModule";
       }
       public List<AugeasNode> getAllNodes(AugeasTree tree){
           return AugeasNodeSearch.searchNode(IFMODULE.getPossParentNodeName(), IFMODULE.getNodeName(), tree.getRootNode());
       }
       public List<String> getConfigurationFiles(){
           List<String> list = new ArrayList<String>();
            list.add("IfModule0");
            list.add("IfModule1");
           return list;
       }
    },

    IFMODULE_DIRECTORY{
        public String getComponentName(){
            return "IfModule Parameters";
        }
        public String[] getPossParentNodeName(){
            String [] parentNames = new String[1];
            parentNames[0]="<IfModule";
            return parentNames;
        }
        public String getNodeName(){
            return "<IfModule";
        }
        public List<AugeasNode> getAllNodes(AugeasTree tree){
            List<AugeasNode> nodes = DIRECTORY.getAllNodes(tree);
            List<AugeasNode> ifModNodes = new ArrayList<AugeasNode>();
            for (AugeasNode node : nodes){
                  List<AugeasNode> tempNodes = AugeasNodeSearch.searchNode(IFMODULE_DIRECTORY.getPossParentNodeName(), IFMODULE_DIRECTORY.getNodeName(), node);
                  if (tempNodes != null)
                      ifModNodes.addAll(tempNodes);
            }
            return ifModNodes;

        }

        public List<String> getConfigurationFiles(){
            List<String> list = new ArrayList<String>();
            list.add("IfModule Parameters0");
            return list;
        }
     },

    DIRECTORY{
        public String getComponentName(){
            return "Directory";
        }
        public String[] getPossParentNodeName(){
            String [] parentNames = new String[2];
            parentNames[0]="<IfModule";
            parentNames[1]="<VirtualHost";
            return parentNames;
        }
        public String getNodeName(){
            return "<Directory";
        }
        public List<AugeasNode> getAllNodes(AugeasTree tree){
            return AugeasNodeSearch.searchNode(DIRECTORY.getPossParentNodeName(), DIRECTORY.getNodeName(), tree.getRootNode());
        }

        public List<String> getConfigurationFiles(){
            List<String> list = new ArrayList<String>();
            list.add("Directory0");
            list.add("Directory1");
            list.add("Directory2");
            return list;
        }
     },

    VIRTUALHOST{
        public String getComponentName(){
            return "Apache Virtual Host";
        }
        public String[] getPossParentNodeName(){
            String [] parentNames = new String[0];
            return parentNames;
        }
        public String getNodeName(){
            return "<VirtualHost";
        }
        public List<AugeasNode> getAllNodes(AugeasTree tree){
            return tree.getRootNode().getChildByLabel("<VirtualHost");
        }

        public List<String> getConfigurationFiles(){
            List<String> list = new ArrayList<String>();
            list.add("Apache Virtual Host0");
            return list;
        }
     };

    public abstract String getComponentName();
    public abstract String[] getPossParentNodeName();
    public abstract String getNodeName();
    public abstract List<AugeasNode> getAllNodes(AugeasTree tree);
    public abstract List<String> getConfigurationFiles();
}
