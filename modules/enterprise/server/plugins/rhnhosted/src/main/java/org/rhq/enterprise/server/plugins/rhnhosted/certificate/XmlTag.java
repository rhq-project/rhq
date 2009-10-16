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

package org.rhq.enterprise.server.plugins.rhnhosted.certificate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * @author pkilambi
 *
 */
/**
 * XmlTag a simple class to render an XML tag
 */
public class XmlTag {

    private String tag;
    private Map attribs;
    private List body;
    private boolean spaceBeforeEndTag;
    private List keys;

    /**
     * Standard xml header with utf-8 encoding. Example usage:<br />
     * <code>
     * StringBuffer buf = new StringBuffer();
     * buf.append(XmlTag.XML_HDR_UTF8);
     * buf.append(new XmlTag("foo").render());
     * </code>
     */
    public static final String XML_HDR_UTF8 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    /**
     * Standard xml header with no encoding. Example usage:<br />
     * <code>
     * StringBuffer buf = new StringBuffer();
     * buf.append(XmlTag.XML_HDR);
     * buf.append(new XmlTag("foo").render());
     * </code>
     */
    public static final String XML_HDR = "<?xml version=\"1.0\"?>";

    /**
     * Public constructor
     * @param tagIn the name of the tag
     */
    protected XmlTag(String tagIn) {
        this(tagIn, true);
    }

    /**
     * Constructs an XmlTag. The <code>spaceBefore</code> attribute controls
     * whether a space is inserted before the closing tag of a single line
     * element.<br />
     * For example, a true value for spaceBefore and a tagIn of "foo" will
     * render &lt;foo /&gt;. A spaceBefore value of false would've rendered
     * &lt;foo/&gt;.
     * @param tagIn the name of the tag
     * @param spaceBefore true if you want a space before the closing tag.
     */

    protected XmlTag(String tagIn, boolean spaceBefore) {
        attribs = new HashMap();
        tag = tagIn;
        body = new ArrayList();
        keys = new ArrayList();
        spaceBeforeEndTag = spaceBefore;
    }

    /**
     * set an attribute of the html tag
     * @param name the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String name, String value) {
        attribs.put(name, value);
        keys.add(name);
    }

    /**
     * Removes an attribute of the html tag.
     * @param name the attribute name to be removed.
     */
    public void removeAttribute(String name) {
        attribs.remove(name);
        keys.remove(name);
    }

    /**
     * set the body of the tag
     * @param bodyIn the new body
     */
    public void addBody(String bodyIn) {
        body.add(bodyIn);
    }

    /**
     * Adds the given tag to the body of this tag.
     * @param bodyTag Tag to be added to the body of this tag.
     */
    public void addBody(XmlTag bodyTag) {
        body.add(bodyTag);
    }

    /**
     * render the tag into a string
     * @return the string version
     */
    public String render() {
        StringBuffer ret = new StringBuffer();
        ret.append(renderOpenTag());
        if (!hasBody()) {
            ret.deleteCharAt(ret.length() - 1);
            if (spaceBeforeEndTag) {
                ret.append(" />");
            }
            else {
                ret.append("/>");
            }
        }
        else {
            ret.append(renderBody());
            ret.append(renderCloseTag());
        }
        return ret.toString();
    }

    /**
     * render the open tag and attributes
     * @return the open tag as a string
     */
    public String renderOpenTag() {
        StringBuffer ret = new StringBuffer("<");
        ret.append(tag);

        Iterator i = keys.iterator();
        while (i.hasNext()) {
            String attrib = (String) i.next();
            ret.append(" ");
            ret.append(attrib);
            ret.append("=\"");
            ret.append((String) attribs.get(attrib));
            ret.append("\"");
        }
        ret.append(">");

        return ret.toString();
    }

    /**
     * render the tag body
     * @return the tag body as a string
     */
    public String renderBody() {
        StringBuffer buf = new StringBuffer();

        for (Iterator itr = body.iterator(); itr.hasNext();) {
            buf.append(convertToString(itr.next()));
        }

        return buf.toString();
    }

    private String convertToString(Object o) {
        if (o instanceof XmlTag) {
            return ((XmlTag) o).render();
        }
        else if (o instanceof String) {
            return (String) o;
        }
        else {
            return o.toString();
        }
    }

    /**
     * render the close tag
     * @return the close tag as a string
     */
    public String renderCloseTag() {
        return "</" + tag + ">";
    }

    /**
     * Returns true if this tag has a body defined.
     * @return true if this tag has a body defined.
     */
    public boolean hasBody() {
        return (body.size() > 0);
    }
}
