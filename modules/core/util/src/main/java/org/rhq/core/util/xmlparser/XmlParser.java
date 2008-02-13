/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.util.xmlparser;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.rhq.core.clientapi.util.StringUtil;

/**
 * The main entry point && bulk of XmlParser. The parsing routine takes an entry-point tag, which provides information
 * about subtags, attributes it takes, etc. Tags can implement various interfaces to tell the parser to call back when
 * certain conditions are met. This class takes the role of both a minimal validator as well as a traversal mechanism
 * for building data objects out of XML.
 *
 * @deprecated see http://jira.jboss.com/jira/browse/JBNADM-2932
 */
@Deprecated
public class XmlParser {
    private XmlParser() {
    }

    private static void checkAttributes(Element elem, XmlTagHandler tag, XmlFilterHandler filter)
        throws XmlAttrException {
        boolean handlesAttrs = tag instanceof XmlAttrHandler;
        XmlAttr[] attrs;

        if (handlesAttrs) {
            attrs = ((XmlAttrHandler) tag).getAttributes();
        } else {
            attrs = new XmlAttr[0];
        }

        // Ensure out all the required && optional attributes
        for (int i = 0; i < attrs.length; i++) {
            Attribute a = null;
            boolean found = false;

            for (Iterator j = elem.getAttributes().iterator(); j.hasNext();) {
                a = (Attribute) j.next();

                if (a.getName().equalsIgnoreCase(attrs[i].getName())) {
                    found = true;
                    break;
                }
            }

            if (!found && (attrs[i].getType() == XmlAttr.REQUIRED)) {
                throw new XmlRequiredAttrException(elem, attrs[i].getName());
            }

            if (found && handlesAttrs) {
                String val;

                val = filter.filterAttrValue(tag, a.getName(), a.getValue());

                ((XmlAttrHandler) tag).handleAttribute(i, val);
            }
        }

        // Second loop to handle unknown attributes
        for (Iterator i = elem.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            boolean found = false;

            for (int j = 0; j < attrs.length; j++) {
                if (a.getName().equalsIgnoreCase(attrs[j].getName())) {
                    found = true;
                    break;
                }
            }

            if (found) {
                continue;
            }

            if (tag instanceof XmlUnAttrHandler) {
                XmlUnAttrHandler handler;
                String val;

                val = filter.filterAttrValue(tag, a.getName(), a.getValue());

                handler = (XmlUnAttrHandler) tag;
                handler.handleUnknownAttribute(a.getName(), val);
            } else {
                throw new XmlUnknownAttrException(elem, a.getName());
            }
        }

        if (tag instanceof XmlEndAttrHandler) {
            ((XmlEndAttrHandler) tag).endAttributes();
        }
    }

    private static void checkSubNodes(Element elem, XmlTagHandler tag, XmlFilterHandler filter)
        throws XmlAttrException, XmlTagException {
        XmlTagInfo[] subTags = tag.getSubTags();
        Map hash;

        hash = new HashMap();

        // First, count how many times each sub-tag is referenced
        for (Iterator i = elem.getChildren().iterator(); i.hasNext();) {
            Element e = (Element) i.next();
            String name;
            Integer val;

            name = e.getName().toLowerCase();
            if ((val = (Integer) hash.get(name)) == null) {
                val = new Integer(0);
            }

            val = new Integer(val.intValue() + 1);
            hash.put(name, val);
        }

        for (int i = 0; i < subTags.length; i++) {
            String name = subTags[i].getTag().getName().toLowerCase();
            Integer iVal = (Integer) hash.get(name);
            int threshold = 0;
            int val;

            val = (iVal == null) ? 0 : iVal.intValue();

            switch (subTags[i].getType()) {
            case XmlTagInfo.REQUIRED: {
                if (val == 0) {
                    throw new XmlMissingTagException(elem, name);
                } else if (val != 1) {
                    throw new XmlTooManyTagException(elem, name);
                }

                break;
            }

            case XmlTagInfo.OPTIONAL: {
                if (val > 1) {
                    throw new XmlTooManyTagException(elem, name);
                }

                break;
            }

            case XmlTagInfo.ONE_OR_MORE: {
                threshold++;
            }

            case XmlTagInfo.ZERO_OR_MORE: {
                if (val < threshold) {
                    throw new XmlMissingTagException(elem, name);
                }

                break;
            }
            }

            hash.remove(name);
        }

        // Now check for excess sub-tags
        if (hash.size() != 0) {
            Set keys = hash.keySet();

            throw new XmlTooManyTagException(elem, (String) keys.iterator().next());
        }

        // Recurse to all sub-tags
        for (Iterator i = elem.getChildren().iterator(); i.hasNext();) {
            Element child = (Element) i.next();

            for (int j = 0; j < subTags.length; j++) {
                XmlTagHandler subTag = subTags[j].getTag();
                String subName = subTag.getName();

                if (child.getName().equalsIgnoreCase(subName)) {
                    XmlParser.processNode(child, subTag, filter);
                    break;
                }
            }
        }
    }

    private static void processNode(Element elem, XmlTagHandler tag, XmlFilterHandler filter) throws XmlAttrException,
        XmlTagException {
        if (tag instanceof XmlTagEntryHandler) {
            ((XmlTagEntryHandler) tag).enter();
        }

        if (tag instanceof XmlFilterHandler) {
            filter = (XmlFilterHandler) tag;
        }

        XmlParser.checkAttributes(elem, tag, filter);
        if (tag instanceof XmlTextHandler) {
            ((XmlTextHandler) tag).handleText(elem.getText());
        }

        XmlParser.checkSubNodes(elem, tag, filter);

        if (tag instanceof XmlTagExitHandler) {
            ((XmlTagExitHandler) tag).exit();
        }
    }

    private static class DummyFilter implements XmlFilterHandler {
        public String filterAttrValue(XmlTagHandler tag, String attrName, String attrValue) {
            return attrValue;
        }
    }

    /**
     * Parse an input stream, otherwise the same as parsing a file
     */
    public static void parse(InputStream is, XmlTagHandler tag) throws XmlParseException {
        parse(is, tag, null);
    }

    public static void parse(InputStream is, XmlTagHandler tag, EntityResolver resolver) throws XmlParseException {
        SAXBuilder builder;
        Document doc;

        builder = new SAXBuilder();

        if (resolver != null) {
            builder.setEntityResolver(resolver);
        }

        try {
            if (resolver != null) {
                //WTF?  seems relative entity URIs are allowed
                //by certain xerces impls.  but fully qualified
                //file://... URLs trigger a NullPointerException
                //in others.  setting base here worksaround
                doc = builder.build(is, "");
            } else {
                doc = builder.build(is);
            }
        } catch (Exception e) {
            throw new XmlParseException(e.getMessage());
        }

        generalParse(tag, doc);
    }

    /**
     * Parse a file, which should have a root which is the associated tag.
     *
     * @param in  File to parse
     * @param tag Root tag which the parsed file should contain
     */
    public static void parse(File in, XmlTagHandler tag) throws XmlParseException {
        // JON: JDOM has a problem passing in a File that has spaces in dir name
        // (it escapes " " with "%20" and bombs out.
        // let's get the input stream ourselves and pass it to our parse() method that takes a stream
        try {
            InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(in));
            parse(is, tag);
        } catch (java.io.FileNotFoundException exc) {
            throw new XmlParseException(exc.getMessage());
        }
    }

    /**
     * General parsing used by both parse methods above
     */
    private static void generalParse(XmlTagHandler tag, Document doc) throws XmlParseException {
        Element root = doc.getRootElement();
        if (!root.getName().equalsIgnoreCase(tag.getName())) {
            throw new XmlParseException("Incorrect root tag.  Expected <" + tag.getName() + "> but got <"
                + root.getName() + ">");
        }

        XmlParser.processNode(root, tag, new DummyFilter());
    }

    private static void dumpAttrs(XmlAttr[] attrs, String typeName, int type, PrintStream out, int indent) {
        String printMsg;
        boolean printed = false;
        int lineBase;
        int lineLen;

        if (attrs.length == 0) {
            return;
        }

        lineLen = 0;
        printMsg = "- Has " + typeName + " attributes: ";
        lineBase = indent + printMsg.length();

        // Required attributes
        for (int i = 0; i < attrs.length; i++) {
            String toPrint;

            if (attrs[i].getType() != type) {
                continue;
            }

            if (!printed) {
                toPrint = StringUtil.repeatChars(' ', indent) + "- Has " + typeName + " attributes: ";
                out.print(toPrint);
                lineLen = toPrint.length();
                printed = true;
            }

            toPrint = attrs[i].getName() + ", ";
            lineLen += toPrint.length();
            out.print(toPrint);
            if (lineLen > 70) {
                out.println();
                out.print(StringUtil.repeatChars(' ', lineBase));
                lineLen = lineBase;
            }
        }

        if (printed) {
            out.println();
        }
    }

    private static void dumpNode(XmlTagHandler tag, PrintStream out, int indent) throws XmlTagException {
        out.println(StringUtil.repeatChars(' ', indent) + "Tag <" + tag.getName() + ">:");
        if (tag instanceof XmlAttrHandler) {
            XmlAttr[] attrs;

            attrs = ((XmlAttrHandler) tag).getAttributes();
            if (attrs.length == 0) {
                out.println(StringUtil.repeatChars(' ', indent) + "- has no required or optional attributes");
            }

            XmlParser.dumpAttrs(attrs, "REQUIRED", XmlAttr.REQUIRED, out, indent);
            XmlParser.dumpAttrs(attrs, "OPTIONAL", XmlAttr.OPTIONAL, out, indent);
        } else {
            out.println(StringUtil.repeatChars(' ', indent) + "- has no required or optional attributes");
        }

        if (tag instanceof XmlUnAttrHandler) {
            out.println(StringUtil.repeatChars(' ', indent) + "- handles arbitrary attributes");
        }

        XmlTagInfo[] subTags = tag.getSubTags();
        if (subTags.length == 0) {
            out.println(StringUtil.repeatChars(' ', indent) + "- has no subtags");
        } else {
            for (int i = 0; i < subTags.length; i++) {
                String name = subTags[i].getTag().getName();
                int type = subTags[i].getType();

                out.print(StringUtil.repeatChars(' ', indent) + "- has subtag <" + name + ">, which ");
                switch (type) {
                case XmlTagInfo.REQUIRED: {
                    out.println("is REQUIRED");
                    break;
                }

                case XmlTagInfo.OPTIONAL: {
                    out.println("is OPTIONAL");
                    break;
                }

                case XmlTagInfo.ONE_OR_MORE: {
                    out.println("is REQUIRED at least ONCE");
                    break;
                }

                case XmlTagInfo.ZERO_OR_MORE: {
                    out.println("can be specified any # of times");
                    break;
                }
                }

                XmlParser.dumpNode(subTags[i].getTag(), out, indent + 4);
            }
        }
    }

    public static void dump(XmlTagHandler root, PrintStream out) {
        try {
            XmlParser.dumpNode(root, out, 0);
        } catch (XmlTagException exc) {
            out.println("Error traversing tags: " + exc.getMessage());
        }
    }

    private static String bold(String text) {
        return "<emphasis role=\"bold\">" + text + "</emphasis>";
    }

    private static String tag(String name) {
        return bold("&lt;" + name + "&gt;");
    }

    private static String listitem(String name, String desc) {
        String item = "<listitem><para>" + name + "</para>";
        if (desc != null) {
            item += "<para>" + desc + "</para>";
        }

        return item;
    }

    private static void dumpAttrsSGML(XmlAttr[] attrs, String typeName, int type, PrintStream out, int indent) {
        boolean printed = false;
        if (attrs.length == 0) {
            return;
        }

        // Required attributes
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].getType() != type) {
                continue;
            }

            if (!printed) {
                out.println(StringUtil.repeatChars(' ', indent) + "<itemizedlist><listitem><para>" + typeName
                    + " attributes: </para>\n" + StringUtil.repeatChars(' ', indent) + "<itemizedlist>");
                printed = true;
            }

            out.println(StringUtil.repeatChars(' ', indent) + listitem(bold(attrs[i].getName()), null) + "</listitem>");
        }

        if (printed) {
            out.println(StringUtil.repeatChars(' ', indent) + "</itemizedlist></listitem></itemizedlist>");
        }
    }

    private static void dumpNodeSGML(XmlTagHandler tag, PrintStream out, int indent) throws XmlTagException {
        if (indent == 0) {
            out.println(StringUtil.repeatChars(' ', indent) + "<para>Tag " + tag(tag.getName()) + ":</para>");
        }

        if (tag instanceof XmlAttrHandler) {
            XmlAttr[] attrs;

            attrs = ((XmlAttrHandler) tag).getAttributes();

            dumpAttrsSGML(attrs, bold("REQUIRED"), XmlAttr.REQUIRED, out, indent);
            dumpAttrsSGML(attrs, bold("OPTIONAL"), XmlAttr.OPTIONAL, out, indent);
        }

        XmlTagInfo[] subTags = tag.getSubTags();
        if (subTags.length != 0) {
            out.println(StringUtil.repeatChars(' ', indent) + "<itemizedlist>");

            for (int i = 0; i < subTags.length; i++) {
                String name = subTags[i].getTag().getName();
                int type = subTags[i].getType();
                String desc = "";

                switch (type) {
                case XmlTagInfo.REQUIRED: {
                    desc = "REQUIRED";
                    break;
                }

                case XmlTagInfo.OPTIONAL: {
                    desc = "OPTIONAL";
                    break;
                }

                case XmlTagInfo.ONE_OR_MORE: {
                    desc = "REQUIRED at least ONCE";
                    break;
                }

                case XmlTagInfo.ZERO_OR_MORE: {
                    desc = "can be specified any # of times";
                    break;
                }
                }

                out.print(StringUtil.repeatChars(' ', indent) + listitem("Sub Tag " + tag(name) + " " + desc, null));

                dumpNodeSGML(subTags[i].getTag(), out, indent + 4);
                out.println(StringUtil.repeatChars(' ', indent) + "</listitem>");
            }

            out.println(StringUtil.repeatChars(' ', indent) + "</itemizedlist>");
        }
    }

    public static void dumpSGML(XmlTagHandler root, PrintStream out) {
        try {
            dumpNodeSGML(root, out, 0);
        } catch (XmlTagException exc) {
            out.println("Error traversing tags: " + exc.getMessage());
        }
    }
}