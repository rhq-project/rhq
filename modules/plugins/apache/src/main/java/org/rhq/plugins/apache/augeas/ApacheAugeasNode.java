package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeLazy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

/**
 * A specialization of an AugeasNode for purposes of Apache mapping.
 * We need to specialize, because the Augeas tree we are building needs 
 * transparently places nodes from different files in appropriate
 * places in the tree where those files are included using the Apache
 * &lt;Include&gt; directive.
 * 
 * This node holds a list of all nodes that have been included to it
 * and modifies the get* methods to handle these as well.
 * 
 * @author Filip Drabek
 */
public class ApacheAugeasNode extends AugeasNodeLazy implements AugeasNode {

    /**
     * List of included nodes.
     */
    private List<AugeasNode> includedNodes;

    public ApacheAugeasNode(String fullPath, AugeasTree tree) {
        super(fullPath, tree);
    }

    public ApacheAugeasNode(AugeasNode parentNode, AugeasTree tree, String fullPath) {
        super(fullPath, tree);
        this.parentNode = parentNode;
    }

    /**
     * Returns the list of child nodes.
     * A child node is either a direct child node or a child of
     * an direct child include node.
     * I.e. we are transparently leaving out the Include directives
     * and replacing them with their children.
     * 
     * @return the child nodes
     * @see AugeasNodeLazy#getChildNodes()
     */
    public List<AugeasNode> getChildNodes() {
        List<AugeasNode> nodes = null;
        nodes = ag.match(getFullPath() + File.separatorChar + "*");
        if (includedNodes != null)
            nodes.addAll(includedNodes);

        return nodes;
    }

    /**
     * Adds the provided nodes to the list of the included child nodes.
     * 
     * @param nodes
     */
    public void addIncludeNodes(List<AugeasNode> nodes) {
        if (nodes.isEmpty())
            return;

        if (includedNodes == null)
            includedNodes = new ArrayList<AugeasNode>();

        includedNodes.addAll(nodes);
    }

    /**
     * Adds the node to the list of the included child nodes.
     * 
     * @param node
     */
    public void addIncludeNode(AugeasNode node) {
        if (includedNodes == null)
            includedNodes = new ArrayList<AugeasNode>();

        includedNodes.add(node);
    }

    public AugeasNode getParentNode() {
        //apache nodes get their parent nodes set by the tree builder
        //(AugeasTreeBuilderApache)
        //in case they are included from another files.
        //so bail out immediately if we have our parent set.
        if (parentNode != null)
            return parentNode;

        return super.getParentNode();
    }

    public void addChildNode(AugeasNode node) {
        //TODO check if the node belongs here
        childNodes.add(node);
    }

    public List<AugeasNode> getChildByLabel(String labelName) {
        List<AugeasNode> nodes = super.getChildByLabel(labelName);

        if (includedNodes != null) {
            for (AugeasNode node : includedNodes) {
                if (node.getLabel().equals(labelName))
                    nodes.add(node);
            }
        }
        return nodes;
    }

    public void setPath(String path) throws AugeasTreeException {
        this.path = path;

    }

    public void updateFromParent() {
        //don't update from parent
        //tree builder can set the parent node to something
        //else if this node is included from another file
        //and we would destroy that association here.
    }
    public void setParentNode(AugeasNode node){
    	this.parentNode = node;
    }
}
