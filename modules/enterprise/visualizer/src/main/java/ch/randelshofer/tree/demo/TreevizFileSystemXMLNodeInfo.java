/*
 * @(#)XMLNodeInfo.java  1.0  23. Juni 2008
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.tree.demo;

import ch.randelshofer.tree.Colorizer;
import ch.randelshofer.tree.NodeInfo;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import ch.randelshofer.tree.Weighter;
import java.awt.Color;
import java.awt.Image;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XMLNodeInfo produces information about generic XML files.
 * It attempts to interpret the XML attrMap 'name', 'size', and 'created'
 * like file system attrMap.
 *
 * @author  Werner Randelshofer
 * @version 1.0 23. Juni 2008 Created.
 */
public class TreevizFileSystemXMLNodeInfo implements NodeInfo {

    private Colorizer colorizer;
    private Weighter colorWeighter;
    private Weighter weighter;

    public static enum AttributeType {

        TEXT, BOOLEAN, NUMBER, DATE
    }
    private HashMap<String, HashSet<String>> attributes;
    private HashMap<String, AttributeType> types;
    private HashMap<String, HashSet<String>> userAttributes;
    private HashMap<String, XMLNode> userMap;
    private HashMap<String, AttributeType> userTypes;
    private String nameAttribute;
    private String weightAttribute;
    private String colorAttribute;
    private TreevizFileSystemXMLTree tree;

    /** Creates a new instance. */
    public TreevizFileSystemXMLNodeInfo(TreevizFileSystemXMLTree tree) {
        colorizer = new RGBColorizer();
        //weighter = new LastModifiedWeighter();
        attributes = new HashMap<String, HashSet<String>>();
        this.tree = tree;
    }

    public void init(TreeNode root) {
        init((XMLNode) root);
    }

    public String getName(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        if (nameAttribute != null && node.getAttribute(nameAttribute) != null) {
            return node.getAttribute(nameAttribute);
        }
        return node.getName();
    }

    public Color getColor(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        return (colorizer == null || colorWeighter == null) ? Color.WHITE : colorizer.get(colorWeighter.getWeight(path));
    }

    public long getWeight(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        if (weightAttribute != null && node.getAttribute(weightAttribute) != null) {
            return Math.max(1, Long.valueOf(node.getAttribute(weightAttribute)).longValue());
        }
        return 1;
    }

    public long getAccumulatedWeight(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        return node.getCumulatedWeight();
    }

    public String getTooltip(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();

        StringBuilder buf = new StringBuilder();
        TreePath<TreeNode> parentPath = path;
        do {
            buf.insert(0, "<br>");
            buf.insert(0, getName(parentPath));
            parentPath = parentPath.getParentPath();
        } while (parentPath != null && parentPath.getPathCount() > 1);
        buf.insert(0, "<html>");
        buf.append("<br>");

        if (!node.isLeaf()) {
            buf.append(DecimalFormat.getIntegerInstance().format(node.children().size()));
            buf.append(" Files");
            buf.append("<br>");
        }

        buf.append(formatSize(getAccumulatedWeight(path)));

        Map<String, String> attr = node.getAttributes();
        for (Map.Entry<String, String> entry : attr.entrySet()) {
            buf.append("<br>" + entry.getKey() + ": ");
            if (entry.getKey().toLowerCase().endsWith("size") && types.get(entry.getKey()) == AttributeType.NUMBER) {
                buf.append(formatSize(Long.valueOf(entry.getValue())));
            } else {
                buf.append(entry.getValue());

            }
        }

        String ownerAttr = "ownerRef";
        if (node.getAttribute("ownerRef") != null) {
            XMLNode userNode = userMap.get(node.getAttribute("ownerRef"));
            if (userNode != null) {
                buf.append("<br><br>Owner:");
                attr = userNode.getAttributes();
                for (Map.Entry<String, String> entry : attr.entrySet()) {
                    buf.append("<br>" + entry.getKey() + ": ");
                    if (entry.getKey().toLowerCase().endsWith("size") && types.get(entry.getKey()) == AttributeType.NUMBER) {
                        buf.append(formatSize(Long.valueOf(entry.getValue())));
                    } else {
                        buf.append(entry.getValue());

                    }
                }
                buf.append("<br>Owners disk usage: ");
                        buf.append(formatSize(userNode.getCumulatedWeight()));
            }
        }
        return buf.toString();
    }

    private static String formatSize(long w) {
        double scaledW = w;
        String scaledUnit = "bytes";
        if (scaledW > 1024) {
            scaledW /= 1024;
            scaledUnit = "KB";
            if (scaledW > 1024) {
                scaledW /= 1024;
                scaledUnit = "MB";
            }
            if (scaledW > 1024) {
                scaledW /= 1024;
                scaledUnit = "GB";
            }
        }
        StringBuilder buf = new StringBuilder();
        buf.append(DecimalFormat.getNumberInstance().format(scaledW));
        buf.append(' ');
        buf.append(scaledUnit);
        if (scaledUnit != "bytes") { // string literals get interned
            buf.append(" (");
            buf.append(DecimalFormat.getIntegerInstance().format(w));
            buf.append(" bytes)");
        }
        return buf.toString();
    }

    public Image getImage(TreePath<TreeNode> path) {
        XMLNode node = (XMLNode) path.getLastPathComponent();
        return null;
    }

    public Weighter getWeighter() {
        return colorWeighter;
    }

    public Colorizer getColorizer() {
        return colorizer;
    }
    /*
    public void putAttribute(String key, String value) {
    HashSet<String> valueSet;
    if (attrMap.containsKey(key)) {
    valueSet = attrMap.get(key);
    } else {
    valueSet = new HashSet<String>();
    attrMap.put(key, valueSet);
    }
    valueSet.add(value);
    }*/

    public void init(XMLNode root) {
        userTypes = new HashMap<String, AttributeType>();
        userAttributes = new HashMap<String, HashSet<String>>();
        types = new HashMap<String, AttributeType>();
        attributes = new HashMap<String, HashSet<String>>();
        computeBasicStats(tree.getUsersRoot(), userTypes, userAttributes);
        computeBasicStats(root, types, attributes);
        computeFilesStats(root);
        computeUserStats(tree.getUsersRoot(), root);
    }

    public void computeBasicStats(XMLNode root, HashMap<String, AttributeType> typeMap, HashMap<String, HashSet<String>> attrMap) {
        collectAttributesRecursive(root, attrMap);

        Matcher numberMatcher = Pattern.compile("^-?\\d+$").matcher("");
        Matcher dateMatcher = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}(T|\\s)\\d{2}:\\d{2}:\\d{2}$").matcher("");
        for (Map.Entry<String, HashSet<String>> entry : attrMap.entrySet()) {
            AttributeType type;
            String firstValue = entry.getValue().iterator().next();
            if (firstValue.equals("true") || firstValue.equals("false")) {
                type = AttributeType.BOOLEAN;
            } else {
                numberMatcher.reset(firstValue);
                if (numberMatcher.matches()) {
                    type = AttributeType.NUMBER;
                } else {
                    dateMatcher.reset(firstValue);
                    if (dateMatcher.matches()) {
                        type = AttributeType.DATE;
                    } else {
                        type = AttributeType.TEXT;
                    }
                }
            }
            if (type != AttributeType.TEXT) {
                TypeLoop:
                for (String value : entry.getValue()) {
                    switch (type) {
                        case TEXT:
                            break TypeLoop;
                        case BOOLEAN:
                            if (!value.equals("true") && !value.equals("false")) {
                                type = AttributeType.TEXT;
                                break TypeLoop;
                            }
                            break;
                        case NUMBER:
                            numberMatcher.reset(value);
                            if (!numberMatcher.matches()) {
                                type = AttributeType.TEXT;
                                break TypeLoop;
                            }
                            break;
                        case DATE:
                            dateMatcher.reset(value);
                            if (!dateMatcher.matches()) {
                                type = AttributeType.TEXT;
                                break TypeLoop;
                            }
                            break;
                    }
                }
            }
            typeMap.put(entry.getKey(), type);
        }

        // Determine the name attribute
        nameAttribute = null;
        if (typeMap.get("name") != null) {
            nameAttribute = "name";
        } else {
            for (Map.Entry<String, AttributeType> entry : typeMap.entrySet()) {
                if (entry.getValue() == AttributeType.TEXT) {
                    if (nameAttribute == null || nameAttribute.compareTo(entry.getKey()) > 0) {
                        nameAttribute = entry.getKey();
                    }
                }
            }
        }


    }

    private void computeFilesStats(XMLNode root) {
        // Determine the weight attribute
        weightAttribute = null;
        if (types.get("size") == AttributeType.NUMBER) {
            weightAttribute = "size";
        } else {
            for (Map.Entry<String, AttributeType> entry : types.entrySet()) {
                if (entry.getValue() == AttributeType.NUMBER) {
                    if (weightAttribute == null || weightAttribute.compareTo(entry.getKey()) > 0) {
                        weightAttribute = entry.getKey();
                    }
                }
            }
        }
        weighter = new TreevizFileSystemXMLInfoWeighter(this, weightAttribute);
        root.accumulateWeights(this, null);

        // Determine the color attribute
        colorAttribute = null;
        if (types.get("created") == AttributeType.DATE) {
            colorAttribute = "created";
        } else {
            for (Map.Entry<String, AttributeType> entry : types.entrySet()) {
                if (entry.getValue() == AttributeType.DATE) {
                    if (colorAttribute == null || colorAttribute.compareTo(entry.getKey()) > 0) {
                        colorAttribute = entry.getKey();
                    }
                }
            }
        }
        if (colorAttribute == null) {
            weighter = null;
        } else {
            colorWeighter = new TreevizFileSystemXMLInfoWeighter(this, colorAttribute);
            colorWeighter.init(root);
        }
        colorizer = new RGBColorizer(new float[]{0f, ((TreevizFileSystemXMLInfoWeighter) colorWeighter).getMedianWeight(), 1f}, new Color[]{
            new Color(0x64c8ff),
            new Color(0xf5f5f5),
            new Color(0xff9946)
        });

    }

    public void collectAttributesRecursive(XMLNode node, HashMap<String, HashSet<String>> attrMap) {
        for (Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            HashSet<String> valueSet;
            if (attrMap.containsKey(key)) {
                valueSet = attrMap.get(key);
            } else {
                valueSet = new HashSet<String>();
                attrMap.put(key, valueSet);
            }
            valueSet.add(value);
        }
        for (TreeNode child : node.children()) {
            collectAttributesRecursive((XMLNode) child, attrMap);
        }
    }

    public void computeUserStats(XMLNode usersRoot, XMLNode filesRoot) {
        userMap = new HashMap<String, XMLNode>();

        String idAttr = "id";
        for (TreeNode child : usersRoot.children()) {
            XMLNode userNode = (XMLNode) child;
            if (userNode.getAttribute(idAttr) != null) {
                userMap.put(userNode.getAttribute(idAttr), userNode);
            }
        }

        computeUserFileStatsRecursive(filesRoot);
    }

    public void computeUserFileStatsRecursive(XMLNode filesNode) {
        if (filesNode.getAttribute("ownerRef") != null) {
            XMLNode userNode = userMap.get(filesNode.getAttribute("ownerRef"));
            if (userNode != null && filesNode.children().size() == 0) {
                userNode.setCumulatedWeight(userNode.getCumulatedWeight() + filesNode.getCumulatedWeight());
            }
        }

        for (TreeNode child : filesNode.children()) {
            computeUserFileStatsRecursive((XMLNode) child);
        }
    }

    public TreevizFileSystemXMLNodeInfo.AttributeType getType(String key) {
        return types.get(key);
    }

    public Set<String> getValues(String key) {
        return attributes.get(key);
    }
}
