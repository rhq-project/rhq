package org.rhq.enterprise.gui.coregui.client.util;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * @author Ian Springer
 */
public class TreeUtility {
    public static void printTree(Tree tree) {
        TreeNode rootNode = tree.getRoot();
        printTreeNode(tree, rootNode);
    }

    private static void printTreeNode(Tree tree, TreeNode node) {
        int level = tree.getLevel(node);
        for (int i = 0; i < level; i++) {
            System.out.print(' ');
        }
        System.out.println("* " + node);
        TreeNode[] childNodes = tree.getChildren(node);
        for (TreeNode childNode : childNodes) {
            printTreeNode(tree, childNode);
        }
    }

    private TreeUtility() {
    }
}
