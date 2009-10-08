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

import javax.servlet.jsp.tagext.TagSupport;
import org.rhq.enterprise.gui.legacy.util.TaglibUtils;

/**
 * Subclass of <code>TagSupport</code> that acts as a base class for tags that set scoped variables.
 */
public class VarSetterBaseTag extends TagSupport {
    //----------------------------------------------------instance variables

    private String scope;
    private String var;

    //----------------------------------------------------constructors

    public VarSetterBaseTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getVar() {
        return var;
    }

    /**
     */
    public void setVar(String var) {
        this.var = var;
    }

    /**
     */
    protected void setScopedVariable(Object value) {
        TaglibUtils.setScopedVariable(pageContext, scope, var, value);
    }

    /**
     * Release tag state.
     */
    public void release() {
        scope = null;
        var = null;
        super.release();
    }
}