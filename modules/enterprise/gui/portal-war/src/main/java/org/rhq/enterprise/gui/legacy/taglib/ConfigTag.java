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

import java.util.Properties;
import javax.servlet.jsp.JspException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSP tag that looks up a named CAM config property and returns the value as a scoped variable.
 */
public class ConfigTag extends VarSetterBaseTag {
    private static Log log = LogFactory.getLog(ConfigTag.class.getName());

    //----------------------------------------------------instance variables

    /* the property to look up */
    private String prop = null;

    /* the value to test against */
    private String value = null;

    //----------------------------------------------------constructors

    public ConfigTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the property to look up
     *
     * @param prop the property name
     */
    public void setProp(String prop) {
        this.prop = prop;
    }

    /**
     * Set the value to look up
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Process the tag, looking up the config property and setting the scoped variable.
     *
     * @exception JspException if the scripting variable can not be found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        try {
            SystemManagerLocal systemManager = LookupUtil.getSystemManager();

            log.trace("getting CAM config property [" + prop + "]");
            Properties conf = systemManager.getSystemConfiguration();
            String propVal = conf.getProperty(prop);

            if (value != null) {
                setScopedVariable(new Boolean(propVal.equals(value)));
            } else {
                setScopedVariable(propVal);
            }
        } catch (Exception e) {
            log.error("config properties lookup failed", e);
        }

        return SKIP_BODY;
    }

    /**
     * Release tag state.
     */
    public void release() {
        prop = null;
        value = null;
        super.release();
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("config", name, value, type, this, pageContext);
    }
}