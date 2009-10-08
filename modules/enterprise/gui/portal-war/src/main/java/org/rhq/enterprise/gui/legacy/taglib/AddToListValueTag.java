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

import java.util.HashMap;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Define a bundle of properties of a spider object corresponding to the columns of each of the "from" and "to" tables
 * in the Add To List widget. After this tag and its nested body are evaluated, its ancestor <code>
 * AddToListValueTag</code> will have added a <code><code>Map</code> object (representing the spider object) with
 * properties as defined by nested <code>&lt;spider:addToListValueProperty&gt;</code> tags.
 */
public class AddToListValueTag extends TagSupport {
    //----------------------------------------------------instance variables

    private String id;
    private HashMap properties;

    //----------------------------------------------------constructors

    public AddToListValueTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     */
    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    /**
     */
    public int doStartTag() throws JspException {
        properties = new HashMap();
        return EVAL_BODY_INCLUDE;
    }

    /**
     */
    public int doEndTag() throws JspException {
        Class clazz = AddToListValuesTag.class;
        AddToListValuesTag ancestor = (AddToListValuesTag) findAncestorWithClass(this, clazz);
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + clazz);
        }

        try {
            String id = (String) evalAttr("id", this.id, String.class);
            properties.put("id", id);
            ancestor.addValue(properties);
            return EVAL_PAGE;
        } catch (NullAttributeException ne) {
            throw new JspException("bean " + this.id + " not found");
        }
    }

    /**
     * Release tag state.
     */
    public void release() {
        id = null;
        properties = null;
        super.release();
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("addToListValue", name, value, type, this, pageContext);
    }
}