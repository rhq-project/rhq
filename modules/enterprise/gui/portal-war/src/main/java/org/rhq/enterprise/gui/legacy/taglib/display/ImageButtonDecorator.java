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

import javax.servlet.http.HttpServletRequest;
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
 * This class is a two in one decorator/tag for use within the <code>TableTag</code>; it is a <code>
 * ColumnDecorator</code> tag that that creates a column of image buttons.
 */
public class ImageButtonDecorator extends ColumnDecorator implements Tag {
    //----------------------------------------------------static variables

    private static Log log = LogFactory.getLog(ImageButtonDecorator.class.getName());

    //----------------------------------------------------instance variables

    private PageContext context;
    private Tag parent;
    private String form = null;
    private String input = null;
    private String page = null;

    //----------------------------------------------------constructors

    public ImageButtonDecorator() {
        super();
    }

    //----------------------------------------------------public methods

    public String getForm() {
        return this.form;
    }

    public void setForm(String n) {
        this.form = n;
    }

    public String getInput() {
        return this.input;
    }

    public void setInput(String n) {
        this.input = n;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String decorate(Object obj) {
        String formName = null;
        try {
            formName = (String) evalAttr("form", this.form, String.class);
        } catch (NullAttributeException ne) {
            log.debug("bean " + this.form + " not found");
            return "";
        } catch (JspException je) {
            log.debug("can't evaluate form type [" + this.form + "]: ", je);
            return "";
        }

        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        String src = req.getContextPath() + page;

        StringBuffer buf = new StringBuffer();
        buf.append("<input type=\"image\" ");
        buf.append("src=\"");
        buf.append(src);
        buf.append("\" ");
        buf.append("border=\"0\" onClick=\"clickSelect('");
        buf.append(formName);
        buf.append("', '");
        buf.append(getInput());
        buf.append("', '");
        buf.append(obj.toString());
        buf.append("');\">");
        return buf.toString();
    }

    public int doStartTag() throws JspTagException {
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
        form = null;
        input = null;
        page = null;
        parent = null;
        context = null;
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("imagebuttondecorator", name, value, type, this, context);
    }
}