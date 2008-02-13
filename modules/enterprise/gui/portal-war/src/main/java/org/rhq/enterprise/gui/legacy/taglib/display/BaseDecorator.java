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
import javax.servlet.jsp.tagext.Tag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is an abstract class for decorators to inherit from for implementing decorators for columns.
 */
public abstract class BaseDecorator extends ColumnDecorator implements Tag {
    private static Log log = LogFactory.getLog(BaseDecorator.class.getName());

    /**
     * The main method to override here. This should look something like this: <code>String name = null; try { name =
     * (String) evalAttr("name", this.name, String.class); } catch (NullAttributeException ne) { log.debug("bean " +
     * this.name + " not found"); return ""; } catch (JspException je) { log.debug("can't evaluate name [" + this.name +
     * "]: ", je); return ""; } StringBuffer buf = new StringBuffer(1024); buf.append("
     * <td>"); buf.append(obj.toString()); buf.append("</td>"); return buf.toString()</code>
     */
    public abstract String decorate(Object obj);

    public int doStartTag() throws JspTagException {
        Object parent = getParent();

        if ((parent == null) || !(parent instanceof ColumnTag)) {
            throw new JspTagException("A BaseDecorator must be used within a ColumnTag.");
        }

        ((ColumnTag) parent).setDecorator(this);

        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    /**
     * The name="foo" property.
     */
    private Tag parent;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public Tag getParent() {
        return parent;
    }

    public void setParent(Tag t) {
        this.parent = t;
    }

    public void release() {
        super.release();
        name = null;
        parent = null;
    }

    protected Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("spiderdecorator", name, value, type, this, getPageContext());
    }

    protected String generateErrorComment(String exc, String attrName, String attrValue, Throwable t) {
        log.debug(attrName + " expression [" + attrValue + "] not evaluated", t);
        StringBuffer sb = new StringBuffer("<!-- ");
        sb.append(" failed due to ");
        sb.append(exc);
        sb.append(" on ");
        sb.append(attrName);
        sb.append(" = ");
        sb.append(attrValue);
        sb.append(" -->");
        return sb.toString();
    }
}