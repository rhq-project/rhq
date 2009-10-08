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

import java.util.ArrayList;
import java.util.Map;
import javax.servlet.jsp.JspException;

/**
 * Define the set of object values displayed in the "from" and "to" tables in the Add To List widget. Each object is
 * specified by a nested <code>&lt;spider:addToListValue&gt;</code> tag. After this tag and its nested body are
 * evaluated, a scoped attribute will contain a <code>List</code> of <code>Map</code> objects (each representing a
 * spider object) with the properties specified by <code>&lt;spider:addToListValueProperty&gt;</code> tags nested inside
 * the <code>&lt;spider:addToListValue&gt;</code> tag.
 */
public class AddToListValuesTag extends VarSetterBaseTag {
    //----------------------------------------------------instance variables

    private ArrayList values;

    //----------------------------------------------------constructors

    public AddToListValuesTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     */
    public void addValue(Map map) {
        values.add(map);
    }

    /**
     */
    public int doStartTag() throws JspException {
        values = new ArrayList();
        return EVAL_BODY_INCLUDE;
    }

    /**
     */
    public int doEndTag() throws JspException {
        setScopedVariable(values);
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     */
    public void release() {
        values = null;
        super.release();
    }
}