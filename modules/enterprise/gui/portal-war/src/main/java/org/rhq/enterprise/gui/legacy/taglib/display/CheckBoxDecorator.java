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
 * This class is a two in one decorator/tag for use within the display:table tag, it is a ColumnDecorator tag that that
 * creates a column of checkboxes.
 */
public class CheckBoxDecorator extends ColumnDecorator implements Tag {
    //----------------------------------------------------static variables

    private static Log log = LogFactory.getLog(CheckBoxDecorator.class.getName());

    //----------------------------------------------------instance variables

    /**
     * The class property of the checkbox.
     */
    private String styleClass = null;

    /**
     * The name="foo" property of the checkbox.
     */
    private String name = null;

    /**
     * The onClick attribute of the checkbox.
     */
    private String onclick = null;

    /**
     * A flag indicating whether or not to suppress the checkbox
     */
    private String suppress = null;

    // ctors
    public CheckBoxDecorator() {
        styleClass = "listMember";
        name = "";
        onclick = "";
    }

    // accessors
    public String getStyleClass() {
        return this.styleClass;
    }

    public void setStyleClass(String c) {
        this.styleClass = c;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getOnclick() {
        return this.onclick;
    }

    public void setOnclick(String o) {
        this.onclick = o;
    }

    public String getSuppress() {
        return this.suppress;
    }

    public void setSuppress(String o) {
        this.suppress = o;
    }

    public String decorate(Object obj) {
        String name = null;
        String value = null;
        String click = "";
        Boolean suppress = Boolean.FALSE;
        try {
            name = (String) evalAttr("name", this.name, String.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + this.name + " not found");
            return "";
        } catch (JspException je) {
            log.debug("can't evaluate name [" + this.name + "]: ", je);
            return "";
        }

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
            suppress = (Boolean) evalAttr("suppress", this.suppress, Boolean.class);
        } catch (NullAttributeException ne) {
            suppress = Boolean.FALSE;
        } catch (JspException je) {
            log.debug("can't evaluate suppress [" + this.suppress + "]: ", je);
            return "";
        }

        if ((suppress != null) && suppress.booleanValue()) {
            return "";
        }

        try {
            click = (String) evalAttr("onclick", this.getOnclick(), String.class);
        } catch (NullAttributeException e) {
            // Onclick is empty
        } catch (JspException e) {
            // Onclick is empty
        }

        StringBuffer buf = new StringBuffer();
        buf.append("<input type=\"checkbox\" onclick=\"");
        buf.append(click);
        buf.append("\" class=\"");
        buf.append(getStyleClass());
        buf.append("\" name=\"");
        buf.append(name);
        buf.append("\" value=\"");
        if (value != null) {
            buf.append(value);
        } else {
            buf.append(obj.toString());
        }

        buf.append("\"");
        buf.append(">");

        return buf.toString();
    }

    public int doStartTag() throws javax.servlet.jsp.JspException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A CheckboxDecorator must be used within a ColumnTag.");
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

    public void setPageContext(PageContext pc) {
        this.context = pc;
    }

    public void release() {
        styleClass = null;
        name = null;
        onclick = null;
        suppress = null;
        parent = null;
        context = null;
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("checkboxdecorator", name, value, type, this, context);
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