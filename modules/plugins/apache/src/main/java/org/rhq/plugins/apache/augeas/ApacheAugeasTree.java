package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasNodeBuffer;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;

public class ApacheAugeasTree implements AugeasTree {

    private AugeasModuleConfig moduleConfig;
    private Augeas ag;
    private AugeasNode rootNode;
    private AugeasNode rootConfigNode;
    private AugeasNodeBuffer nodeBuffer;
    private String[] errorNodes = { "pos", "line", "char", "lens", "message" };
    public static String AUGEAS_DATA_PATH = File.separatorChar + "files";
    private Map<AugeasNode, List<String>> includes;

    public ApacheAugeasTree(Augeas ag, AugeasModuleConfig moduleConfig) {
        nodeBuffer = new AugeasNodeBuffer();
        this.moduleConfig = moduleConfig;
        this.ag = ag;
    }

    public void update() {
    }

    public void save() {
        ag.save();
    }

    public Map<AugeasNode, List<String>> getIncludes() {
        return includes;
    }

    public void setIncludes(Map<AugeasNode, List<String>> includes) {

        this.includes = includes;
    }

    private AugeasNode getLoadedNode(String path) throws AugeasTreeException {
        if (nodeBuffer.isNodeLoaded(path))
            return nodeBuffer.getNode(path);

        throw new AugeasTreeException("Node not found.");
    }

    public AugeasNode getNode(String path) throws AugeasTreeException {
        AugeasNode node;
        try {
            node = getLoadedNode(path);
        } catch (AugeasTreeException e) {
            node = createNode(path);
        }

        return node;
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

    public AugeasNode createNode(String fullPath) throws AugeasTreeException {
        AugeasNode node = null;
        try {
            node = getLoadedNode(fullPath);
            return node;
        } catch (Exception e) {
            List<String> list = ag.match(fullPath);
            if (!list.isEmpty())
                return new ApacheAugeasNode(fullPath, this);
        }
        ag.set(fullPath, null);
        node = new ApacheAugeasNode(fullPath, this);
        nodeBuffer.addNode(node);
        return node;
    }

    public AugeasNode createNode(AugeasNode parentNode, String name, String value, int seq) throws AugeasTreeException {
        AugeasNode nd = createNode(parentNode.getFullPath() + File.separatorChar + name + "[" + String.valueOf(seq)
            + "]");
        nd.setValue(value);

        return nd;
    }

    public String get(String expr) {
        return (ag.get(expr));
    }

    public AugeasNode getRootNode() {
        return rootNode;
    }

    public void removeNode(AugeasNode node, boolean updateSeq) throws AugeasTreeException {
        int res = ag.remove(node.getFullPath());
        nodeBuffer.removeNode(node, updateSeq, true);

    }

    public void setValue(AugeasNode node, String value) {
        ag.set(node.getFullPath(), value);
    }

    public String summarizeAugeasError() {

        String nodePrefix = "/augeas/files";
        List<String> str = moduleConfig.getIncludedGlobs();
        StringBuilder builder = new StringBuilder();

        for (String path : str) {
            String name = nodePrefix + path + File.separatorChar + "error";
            if (ag.exists(name)) {
                builder.append("Error " + ag.get(name) + '\n');
                for (String errNd : errorNodes) {
                    String pathToMessage = name + File.separatorChar + errNd;
                    if (ag.exists(pathToMessage)) {
                        builder.append(errNd + " " + ag.get(pathToMessage) + '\n');
                    }
                }
            }
        }

        return builder.toString();
    }

    public void setRootNode(AugeasNode node) {
        this.rootNode = node;

    }
}
