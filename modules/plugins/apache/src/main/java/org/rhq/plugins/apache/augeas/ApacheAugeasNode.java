package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.node.AugeasNodeLazy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

public class ApacheAugeasNode extends AugeasNodeLazy implements AugeasNode {

    private List<AugeasNode> includedNodes;

    public ApacheAugeasNode(String fullPath, AugeasTree tree) {
        super(fullPath, tree);
    }

    public ApacheAugeasNode(AugeasNode parentNode, AugeasTree tree, String fullPath) {
        super(fullPath, tree);
        this.parentNode = parentNode;
    }

    public List<AugeasNode> getChildNodes() {
        List<AugeasNode> nodes = null;
        try {
            nodes = ag.match(getFullPath() + File.separatorChar + "*");
            if (includedNodes != null)
                nodes.addAll(includedNodes);

        } catch (AugeasTreeException e) {
            //TODO loggin
            e.printStackTrace();
        }
        return nodes;
    }

    public void addIncludeNodes(List<AugeasNode> nodes) {
        if (nodes.isEmpty())
            return;

        if (includedNodes == null)
            includedNodes = new ArrayList<AugeasNode>();

        includedNodes.addAll(nodes);
    }

    public void addIncludeNode(AugeasNode node) {
        if (includedNodes == null)
            includedNodes = new ArrayList<AugeasNode>();

        includedNodes.add(node);
    }

    public AugeasNode getParentNode() {
        if (parentNode != null)
            return parentNode;

        String parentNodePath = path.substring(0, path.length() - 1);
        if (parentNodePath.equals(ag.getRootNode().getFullPath()))
            return ag.getRootNode();
        try {
            return ag.getNode(parentNodePath);
        } catch (Exception e) {
            return null;
        }
    }

    public void addChildNode(AugeasNode node) {
        //TODO kontrola jestli sem patri
        childNodes.add(node);
    }

    public List<AugeasNode> getChildByLabel(String labelName) {
        List<AugeasNode> nodes = getChildNodes();
        List<AugeasNode> tempNode = new ArrayList<AugeasNode>();

        for (AugeasNode node : nodes) {
            if (node.getLabel().equals(labelName))
                tempNode.add(node);
        }

        if (includedNodes != null) {
            for (AugeasNode node : includedNodes) {
                if (node.getLabel().equals(labelName))
                    tempNode.add(node);
            }
        }
        return tempNode;
    }

    public void setPath(String path) throws AugeasTreeException {
        this.path = path;

    }

    public void updateFromParent() {
        /* AugeasNode node = this.getParentNode();
         if (!this.path.equals(node.getFullPath())){
                this.path = node.getFullPath();
            }*/
    }
}
