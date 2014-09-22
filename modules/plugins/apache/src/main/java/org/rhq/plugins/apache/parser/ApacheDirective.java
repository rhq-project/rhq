package org.rhq.plugins.apache.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApacheDirective implements Cloneable {

    private static final String IF_MODULE = "<IfModule";
    private static final String IF_DEFINE = "<IfDefine";
    private static final String IF_VERSION = "<IfVersion";

    private String name;
    private List<String> values;
    private boolean isNested;
    private boolean isRootNode;
    private boolean isComment;
    private static final String WS = "[ \t]*";
    private static final String WORD = "\"(?:[^\"\n]|\\\")*\"|'(?:[^'\n]|\\\')*'|[^'\" \t\n]+";
    private static final String DIRECTIVE_REGEX = WS + "(" + WORD + ")" + WS;
    private static final String COMMENT_REGEX = "^[\t ]*#.*+$";
    private boolean updated = false;

    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile(DIRECTIVE_REGEX);
    private static final Pattern COMMENT_PATTERN = Pattern.compile(COMMENT_REGEX);
    private List<ApacheDirective> childNodes;
    private ApacheDirective parentNode;
    private String file;

    public static boolean isConditionalDirectiveName(String name) {
        return IF_MODULE.equalsIgnoreCase(name) || IF_DEFINE.equalsIgnoreCase(name) || IF_VERSION.equalsIgnoreCase(name);
    }

    public ApacheDirective() {
        values = new ArrayList<String>();
        childNodes = new ArrayList<ApacheDirective>();
    }

    public ApacheDirective(String directive) {
        values = new ArrayList<String>();
        childNodes = new ArrayList<ApacheDirective>();

        Matcher matcher = COMMENT_PATTERN.matcher(directive);
        if (matcher.matches()) {
            isComment = true;
            values.add(directive);
            name = "#";
        } else {
            int startIndex = 0;
            boolean updated = true;
            while (updated && startIndex < directive.length()) {
                updated = false;
                Matcher m = DIRECTIVE_PATTERN.matcher(directive);
                while (m.find(startIndex)) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String val = m.group(i);
                        values.add(val);
                    }
                    updated = true;
                    startIndex = m.end();
                }
            }
            if (values.isEmpty())
                throw new RuntimeException("Directive " + directive + "is not in valid format.");

            String lastVal = values.get(values.size() - 1);
            if (lastVal.endsWith(">")) {
                lastVal = lastVal.substring(0, lastVal.length() - 1);
                values.set(values.size() - 1, lastVal);
            }

            name = values.get(0);
            values.remove(0);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public boolean isNested() {
        return isNested;
    }

    public void setNested(boolean isNested) {
        this.isNested = isNested;
    }

    public String getValuesAsString() {
        StringBuilder buf = new StringBuilder();
        for (String val : values) {
            buf.append(val);
        }
        return buf.toString();
    }

    public void addChildDirective(ApacheDirective directive) {
        childNodes.add(directive);
    }

    public List<ApacheDirective> getChildDirectives() {
        return childNodes;
    }

    public List<ApacheDirective> getChildByName(String name) {
        List<ApacheDirective> kids = new ArrayList<ApacheDirective>();
        for (ApacheDirective dir : childNodes) {
            if (dir.getName().equals(name)) {
                kids.add(dir);
            }
        }
        return kids;
    }

    public ApacheDirective getParentNode() {
        return parentNode;
    }

    public void setParentNode(ApacheDirective parent) {
        this.parentNode = parent;
    }

    public boolean isRootNode() {
        return isRootNode;
    }

    public void setRootNode(boolean isRootNode) {
        this.isRootNode = isRootNode;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public String getFile() {
        if (isRootNode)
            return null;

        if (file == null) {
            List<ApacheDirective> dir = parentNode.getChildByName(name);
            for (int i = 0; i < dir.size(); i++)
                if (dir.get(i) == this) {
                    file = dir.get(i - 1).getFile();
                    break;
                }
        }
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void removeChilDirective(ApacheDirective dir) {
        if (childNodes.contains(dir))
            childNodes.remove(dir);
    }

    public void remove() {
        parentNode.removeChilDirective(this);
    }

    public String getText() {
        StringBuilder builder = new StringBuilder();
        builder.append(name + " ");
        for (String temp : values) {
            builder.append(temp + " ");
        }
        builder.deleteCharAt(builder.length() - 1);
        if (isNested)
            builder.append(">");

        return builder.toString();
    }

    public boolean isConditional() {
        return isConditionalDirectiveName(name);
    }

    public int getSeq() {
        List<ApacheDirective> directives = parentNode.getChildByName(name);
        for (int i = 0; i < directives.size(); i++) {
            if (directives.get(i) == this) {
                return i;
            }
        }
        return 0;
    }

    public void addValue(String val) {
        values.add(val);
    }

    @Override
    public ApacheDirective clone() {
        try {
            ApacheDirective copy = (ApacheDirective) super.clone();

            List<ApacheDirective> newChildNodes = new ArrayList<ApacheDirective>(childNodes.size());
            for (ApacheDirective child : childNodes) {
                ApacheDirective childCopy = child.clone();
                childCopy.parentNode = copy;
                newChildNodes.add(childCopy);
            }
            copy.childNodes = newChildNodes;

            return copy;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("ApacheDirective not cloneable even though it is declared as such.", e);
        }
    }
}
