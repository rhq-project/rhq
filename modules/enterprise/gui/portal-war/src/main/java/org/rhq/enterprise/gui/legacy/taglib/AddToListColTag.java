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

/**
 * Define a column for the "from" and "to" tables in the Add To List widget.
 */
public class AddToListColTag extends TagSupport {
    //----------------------------------------------------instance variables

    private String key;
    private String name;

    //----------------------------------------------------constructors

    public AddToListColTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     */
    public int doEndTag() throws JspException {
        Class clazz = AddToListColsTag.class;
        AddToListColsTag ancestor = (AddToListColsTag) findAncestorWithClass(this, clazz);
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + clazz);
        }

        ancestor.addCol(name, key);
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     */
    public void release() {
        key = null;
        name = null;
        super.release();
    }
}