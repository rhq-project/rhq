package org.rhq.plugins.apache.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApacheDirectiveTree implements Cloneable {

    private ApacheDirective rootNode;

    public ApacheDirectiveTree() {
        rootNode = new ApacheDirective();
        rootNode.setRootNode(true);
    }

    public ApacheDirective getRootNode() {
        return rootNode;
    }

    public void setRootNode(ApacheDirective rootNode) {
        this.rootNode = rootNode;
    }

    public List<ApacheDirective> search(ApacheDirective nd, String name) {
        return parseExpr(nd, name);
    }

    public List<ApacheDirective> search(String name) {
        if (name.startsWith("/"))
            return parseExpr(rootNode, name.substring(1));
        else
            return parseExpr(rootNode, name);
    }

    /**
     * Search can be provided a path-like expression to look for specific children.
     * This method merely looks for children with specified name.
     * <p/>
     * If recursive is <code>false</code>, this method is equivalent to {@link ApacheDirective#getChildByName(String)}.
     *
     * @param nd the root node to start the search from
     * @param name the name of the child node
     * @param recursively whether to recurse down the child tree (in the depth first search manner)
     * @return the list of directives with given name.
     */
    public List<ApacheDirective> findByName(ApacheDirective nd, String name, boolean recursively) {
        List<ApacheDirective> ret = new ArrayList<ApacheDirective>();
        findByName(nd, name, recursively, ret);
        return ret;
    }

    /**
     * Same as {@link #findByName(ApacheDirective, String, boolean, java.util.List)} with the root node of this tree
     * provided as the node parameter.
     * @param name the name of the child nodes
     * @param recursively whether to recurse down the child tree (in the depth first search manner)
     * @return the list of directives with given name.
     */
    public List<ApacheDirective> findByName(String name, boolean recursively) {
        return findByName(rootNode, name, recursively);
    }

    /**
     * @return the paths of all config files that contribute to this config tree
     */
    public Set<String> getAllPaths() {
        Set<String> paths = new HashSet<String>();
        //this will leave out the path of the root node. But that's OK, because rootNode
        //doesn't correspond to any real directive anyway.
        addPath(rootNode, paths);
        return paths;
    }

    private void addPath(ApacheDirective directive, Set<String> paths) {
        for (ApacheDirective c : directive.getChildDirectives()) {
            paths.add(c.getFile());
            addPath(c, paths);
        }
    }

    private void findByName(ApacheDirective root, String name, boolean recursively, List<ApacheDirective> results) {
        for (ApacheDirective child : root.getChildDirectives()) {
            if (name.equals(child.getName())) {
                results.add(child);
            }

            if (recursively) {
                findByName(child, name, true, results);
            }
        }
    }

    private List<ApacheDirective> parseExpr(ApacheDirective nd, String expr) {
        int index = expr.indexOf("/");
        String name;

        if (index == -1)
            name = expr;
        else
            name = expr.substring(0, index);

        List<ApacheDirective> nds = new ArrayList<ApacheDirective>();

        for (ApacheDirective dir : nd.getChildByName(name)) {
            if (index == -1)
                nds.add(dir);
            else {
                List<ApacheDirective> tempNodes = parseExpr(dir, expr.substring(index + 1));
                if (tempNodes != null)
                    nds.addAll(tempNodes);
            }
        }

        return nds;
    }

    public ApacheDirective createNode(ApacheDirective parentNode, String name) {
        ApacheDirective dir = new ApacheDirective(name);
        dir.setParentNode(parentNode);
        parentNode.addChildDirective(dir);
        return dir;
    }

    @Override
    public ApacheDirectiveTree clone() {
        ApacheDirectiveTree copy = new ApacheDirectiveTree();
        copy.rootNode = rootNode.clone();

        return copy;
    }
    
}
