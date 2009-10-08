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
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.ConditionalTagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * Evaluate the nested body content of this tag if the specified permission is granted for the specified resource type.
 *
 * @deprecated
 */
@Deprecated
public class PermissionGrantedTag extends ConditionalTagSupport {
    //----------------------------------------------------instance variables

    /* the scoped attribute that holds our permission */
    protected String permName = null;

    /* the scoped attribute that holds our resource type */
    protected String rtName = null;

    /* the scoped attribute that holds our role operation map */
    protected String mapName = null;

    //----------------------------------------------------constructors

    public PermissionGrantedTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the attribute in page, request, session or application scope that holds the <code>
     * Permission</code> object.
     *
     * @param name the name of the scoped attribute
     */
    public void setPerm(String name) {
        this.permName = name;
    }

    /**
     * Set the name of the attribute in page, request, session or application scope that holds the <code>
     * ResourceTypeValue</code> object.
     *
     * @param name the name of the scoped attribute
     */
    public void setRt(String name) {
        this.rtName = name;
    }

    /**
     * Set the name of the attribute in page, request, session or application scope that holds the <code>Map</code>
     * object.
     *
     * @param name the name of the scoped attribute
     */
    public void setMap(String name) {
        this.mapName = name;
    }

    /**
     * Check to see if the specified operation exists for the specified resource type in the role operation map.
     *
     * @exception JspTagException if a JSP tag exception occurs
     */
    @Override
    protected boolean condition() throws JspTagException {
        //        Permission perm = null;
        //        ResourceType rt = null;
        //        Map map = null;
        //
        //        try {
        //            perm = (Permission) evalAttr("perm", this.permName,
        //                                         Permission.class);
        //        }
        //        catch (NullAttributeException ne) {
        //            throw new JspTagException("bean " + this.permName + " not found");
        //        }
        //        catch (JspException je) {
        //            throw new JspTagException(je.toString());
        //        }
        //
        //        try {
        //            rt = (ResourceType) evalAttr("rt", this.rtName,
        //                                              ResourceType.class);
        //        }
        //        catch (NullAttributeException ne) {
        //            throw new JspTagException("bean " + this.rtName + " not found");
        //        }
        //        catch (JspException je) {
        //            throw new JspTagException(je.toString());
        //        }
        //
        //        try {
        //            map = (Map) evalAttr("map", this.mapName, Map.class);
        //        }
        //        catch (NullAttributeException e) {
        //            throw new JspTagException("bean " + this.mapName + " not found");
        //        }
        //        catch (JspException je) {
        //            throw new JspTagException(je.toString());
        //        }
        //
        //        String rtName = rt.getName();
        //
        //        OldOperation op = (OldOperation) perm.getOperation(rtName);
        //        if (op != null) {
        //            Integer opId = op.getId();
        //            List ops = (List) map.get(rtName);
        //            if (ops != null) {
        //                Iterator i = ops.iterator();
        //                while (i.hasNext()) {
        //                    OldOperation o = (OldOperation) i.next();
        //                    Integer oId = o.getId();
        //                    if (opId.intValue() == oId.intValue()) {
        //                        return true;
        //                    }
        //                }
        //            }
        //        }
        //
        return false;
    }

    /**
     * Release tag state.
     */
    @Override
    public void release() {
        permName = null;
        rtName = null;
        mapName = null;
        super.release();
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("permissionGranted", name, value, type, this, pageContext);
    }
}