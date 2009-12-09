package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.tree.impl.AugeasTreeLazy;

/**
 * For Apache, we need our own implementation of the tree
 * because we need to build the tree such that it transparently
 * handles the include directives (i.e. the call to {@link #match(String)}
 * or {@link #matchRelative(AugeasNode, String)} should return
 * nodes as if the Include directive was replaced by the contents
 * of the included file.
 * 
 * 
 * @author Lukas Krejci
 */
public class ApacheAugeasTree extends AugeasTreeLazy {

    private Map<AugeasNode, List<String>> includes;

    public ApacheAugeasTree(Augeas ag, AugeasModuleConfig moduleConfig) {
        super(ag, moduleConfig);
    }

    protected AugeasNode instantiateNode(String fullPath) {
        return new ApacheAugeasNode(fullPath, this);
    }
    
    public Map<AugeasNode, List<String>> getIncludes() {
        return includes;
    }

    public void setIncludes(Map<AugeasNode, List<String>> includes) {

        this.includes = includes;
    }

    public List<AugeasNode> match(String expression) throws AugeasTreeException {
        if (!expression.startsWith(AUGEAS_DATA_PATH))
            expression = AUGEAS_DATA_PATH + expression;
        /*AugeasNode node = null;
        int max = 0;
        for (AugeasNode nd : includes.keySet()) {
            for (String file : includes.get(nd)) {
                if (expression.startsWith(file)) {
                    if (file.length() > max) {
                        node = nd;
                        max = file.length();
                    }
                }
            }
        }

        if (node == null){
        	node = rootNode;
        	max = rootNode.getFullPath().length();
        }
        return matchRelative(node, expression.substring(max + 1));*/
        return matchInternal(expression);
    }

    public List<AugeasNode> matchRelative(AugeasNode node, String expression) throws AugeasTreeException {
        try {
            if (expression.indexOf(File.separatorChar) == 0)
                expression = expression.substring(1);

            return parseExpr(node, expression);
        } catch (Exception e) {
            throw new AugeasTreeException(e.getMessage());
        }
    }

    private int subExpressionIndex(String expr) {
        //we have to parse the expression carefully because of the 
        //potential xpath qualifier that can contain path separators.
        
        //0 = normal
        //1 = in xpath qualifier
        //2 = in double-quoted string (inside the qualifier)
        //3 = in single-quoted string (inside the qualifier)
        int state = 0;
        int idx = 0;
        boolean found = false;
        while (!found && idx < expr.length()) {
            char currentChar = expr.charAt(idx);
            switch (state) {
            case 0: //normal
                switch (currentChar) {
                case '[':
                    state = 1;
                    break;
                case '/':
                    found = true;
                    break;
                }
                break;
            case 1: //xpath qualifier
                switch (currentChar) {
                case ']':
                    state = 0;
                    break;
                case '"':
                    state = 2;
                    break;
                case '\'':
                    state = 3;
                    break;
                }
                break;
            case 2: //double quoted string
                switch (currentChar) {
                case '"':
                    state = 1;
                    break;
                case '\\':
                    idx++;
                    break;
                }
                break;
            case 3: //single quoted string
                switch (currentChar) {
                case '\'':
                    state = 1;
                    break;
                case '\\':
                    idx++;
                    break;
                }
                break;
            }
            idx++;
        }
        
        return idx == expr.length() ? -1 : idx;
    }
    
    private List<AugeasNode> parseExpr(AugeasNode nd, String expr) throws Exception {

        int index = subExpressionIndex(expr);
        if (index == -1)
            return search(nd, expr);

        String subExpr = expr.substring(0, index - 1);
        List<AugeasNode> nodes = search(nd, subExpr);

        List<AugeasNode> nds = new ArrayList<AugeasNode>();

        for (AugeasNode node : nodes) {
            List<AugeasNode> tempNodes = parseExpr(node, expr.substring(index));
            if (tempNodes != null)
                nds.addAll(tempNodes);
        }

        return nds;
    }

    private List<AugeasNode> search(AugeasNode nd, String expr) throws Exception {

        String fullExpr = nd.getFullPath() + File.separator + expr;

        List<AugeasNode> nodes = this.matchInternal(fullExpr);
        if (includes.containsKey(nd)) {
            List<String> files = includes.get(nd);
            for (String fileName : files) {
                List<AugeasNode> nds = this.matchInternal(fileName + File.separator + expr);
                for (AugeasNode node : nds) {
                    if (!nodes.contains(node))
                        nodes.add(node);
                }
            }
        }

        return nodes;
    }

    private List<AugeasNode> matchInternal(String expression) throws AugeasTreeException {
        if (!expression.startsWith(AUGEAS_DATA_PATH))
            expression = AUGEAS_DATA_PATH + expression;

        List<String> res = ag.match(expression);

        List<AugeasNode> nodes = new ArrayList<AugeasNode>();

        for (String name : res) {
            nodes.add(getNode(name));
        }
        return nodes;
    }
}
