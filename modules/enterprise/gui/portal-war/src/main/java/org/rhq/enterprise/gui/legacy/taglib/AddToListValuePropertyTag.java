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
package org.rhq.enterprise.gui.legacy.taglib;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Define a property of a spider object to be displayed in a column of each of the "from" and "to" tables in the Add To
 * List widget.
 */
public class AddToListValuePropertyTag extends TagSupport {
    //----------------------------------------------------instance variables

    private String name;
    private String value;

    //----------------------------------------------------constructors

    public AddToListValuePropertyTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     */
    public int doEndTag() throws JspException {
        Class clazz = AddToListValueTag.class;
        AddToListValueTag ancestor = (AddToListValueTag) findAncestorWithClass(this, clazz);
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + clazz);
        }

        try {
            String value = (String) evalAttr("value", this.value, String.class);
            ancestor.addProperty(name, value);
            return EVAL_PAGE;
        } catch (NullAttributeException ne) {
            throw new JspException("bean " + this.value + " not found");
        }
    }

    /**
     * Release tag state.
     */
    public void release() {
        name = null;
        value = null;
        super.release();
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("addToListValueProperty", name, value, type, this, pageContext);
    }
}