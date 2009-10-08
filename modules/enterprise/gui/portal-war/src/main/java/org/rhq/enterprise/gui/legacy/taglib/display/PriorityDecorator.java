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

import java.util.Locale;
import javax.servlet.jsp.JspException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;

/**
 * usage <display:column property="priority" width=="10" title"alerts.alert.listheader.priority"/>
 * <display:prioritydecorator flagKey="application.properties.key.prefix"/> In the properties file, have these keys
 * present: application.properties.key.prefix.Low=Low application.properties.key.prefix.High=High
 * application.properties.key.prefix.Medium=Medium
 */
public class PriorityDecorator extends BaseDecorator {
    private static final Locale defaultLocale = Locale.getDefault();
    private static Log log = LogFactory.getLog(BooleanDecorator.class.getName());

    protected String flagKey;

    protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;
    protected String locale = org.apache.struts.Globals.LOCALE_KEY;

    // tag attribute setters

    /**
     * Sets the message prefix that respresents a boolean result
     *
     * @param theFlagKey a String that will have "true" or "false" appended to it to look up in the application
     *                   properties
     */
    public void setFlagKey(String theFlagKey) {
        flagKey = theFlagKey;
    }

    // our ColumnDecorator

    /* (non-Javadoc)
     * @see org.apache.taglibs.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        Integer priority = new Integer(0);
        try {
            priority = (Integer) columnValue;
        } catch (ClassCastException cce) {
            log.debug("class cast exception: ", cce);
        }

        String key = "alert.config.props.PB.Priority." + priority;
        try {
            return RequestUtils.message(getPageContext(), bundle, locale, key);
        } catch (JspException e) {
            return "???" + key + "???";
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    public void release() {
        super.release();
        bundle = org.apache.struts.Globals.MESSAGES_KEY;
        locale = org.apache.struts.Globals.LOCALE_KEY;
        flagKey = null;
    }
}