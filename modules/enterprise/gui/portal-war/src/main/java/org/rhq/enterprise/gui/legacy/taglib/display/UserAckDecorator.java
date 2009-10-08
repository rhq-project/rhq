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
package org.rhq.enterprise.gui.legacy.taglib.display;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a ColumnDecorator tag that that
 * creates either the name of someone that acknowledged the event
 * or a message with an onclick() handler on it.
 * 
 * @author Heiko W. Rupp
 */
public class UserAckDecorator extends ColumnDecorator implements Tag {

    private static Log log = LogFactory.getLog(UserAckDecorator.class);

    /**
     * The class property of the checkbox.
     */
    private String styleClass = null;

    /**
     * The onClick attribute of the checkbox.
     */
    private String onclick = null;

    /**
     * The id of the div we want to insert
     */
    private String id = null;

    // ctors
    public UserAckDecorator() {
        styleClass = "listMember";
        onclick = "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStyleClass() {
        return this.styleClass;
    }

    public void setStyleClass(String c) {
        this.styleClass = c;
    }

    public String getOnclick() {
        return this.onclick;
    }

    public void setOnclick(String o) {
        this.onclick = o;
    }

    @Override
    public String decorate(Object obj) {
        String click = "";

        try {
            value = (String) evalAttr("value", this.value, String.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + this.value + " not found");
            return "";
        } catch (JspException je) {
            log.debug("can't evaluate value [" + this.value + "]: ", je);
            return "";
        }

        try {
            click = (String) evalAttr("onclick", this.getOnclick(), String.class);
        } catch (NullAttributeException e) {
            // Onclick is empty
        } catch (JspException e) {
            // Onclick is empty
        }

        ColumnTag ct = getColumnTag();
        if (id != null) {
            if (id.contains("_property:")) {
                id = ct.replacePropertyIfPossible(id, context);
            }
        }
        if (click != null) {
            click = ct.replacePropertyIfPossible(click, context);
        }

        boolean ackDone;
        if ("".equals(obj))
            ackDone = false;
        else
            ackDone = true;

        StringBuffer buf = new StringBuffer();
        buf.append("<div");
        if (id != null)
            buf.append(" id=" + id);
        if (!ackDone) {
            buf.append(" onclick=\"");
            buf.append(click);
            buf.append("\"");
        }
        if (getStyleClass() != null) {
            buf.append(" class=\"");
            buf.append(getStyleClass());
            buf.append("\"");
        }
        // change cursor when hovering over the link
        // onmouseout, onmouseover
        buf.append(" style=\"cursor:pointer;\"");

        buf.append("\">");
        if (ackDone) {
            buf.append(obj.toString());
        } else
            buf.append("<u>Click to Acknowledge!</u>");

        buf.append("</div>");

        return buf.toString();
    }

    public int doStartTag() throws javax.servlet.jsp.JspException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A UserAckDecorator must be used within a ColumnTag.");
        }

        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    // the JSP tag interface for this decorator
    Tag parent;
    PageContext context;

    /**
     * Holds value of property value.
     */
    private String value;

    public Tag getParent() {
        return parent;
    }

    public void setParent(Tag t) {
        this.parent = t;
    }

    @Override
    public void setPageContext(PageContext pc) {
        this.context = pc;
    }

    @Override
    public void release() {
        styleClass = null;
        onclick = null;
        parent = null;
        context = null;
    }

    private Object evalAttr(String name, String val, Class<String> type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("userackdecorator", name, val, type, this, context);
    }

    /**
     * Getter for property value.
     *
     * @return Value of property value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Setter for property value.
     *
     * @param value New value of property value.
     */
    public void setValue(String value) {
        this.value = value;
    }
}