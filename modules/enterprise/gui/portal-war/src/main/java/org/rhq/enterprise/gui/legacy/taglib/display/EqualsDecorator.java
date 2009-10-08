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
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;

/**
 * Lookup in the context messages for common boolean items for textual display or HTML tag building i.e. "Yes" - "No"
 * "On" - "Off" "/image/icon_on.gif" - "/image/icon_off.gif" You can compare a property to the value and return a
 * Boolean, you can do <display:equalsdecorator value="10001" flagKey="resource.application.inventory.service.id"/> In
 * the properties file have these keys present: resource.application.inventory.service.id.true=ID is 10001
 * resource.application.inventory.service.id.false=ID is not 10001
 */
public class EqualsDecorator extends BaseDecorator implements Cloneable {
    private static final Locale defaultLocale = Locale.getDefault();
    private static Log log = LogFactory.getLog(EqualsDecorator.class.getName());

    private String value;
    private String flagKey;

    private String bundle = org.apache.struts.Globals.MESSAGES_KEY;
    private String locale = org.apache.struts.Globals.LOCALE_KEY;

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

    public void setValue(String theValue) {
        value = theValue;
    }

    // our ColumnDecorator

    /* (non-Javadoc)
     * @see org.apache.taglibs.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        Boolean b = Boolean.FALSE;
        String msg = "";
        try {
            if (value.compareToIgnoreCase("null") == 0) {
                b = new Boolean(columnValue == null);
            } else {
                if (columnValue != null) {
                    b = new Boolean(value.equals(columnValue.toString()));
                }
            }

            String key = flagKey + '.' + b.toString();
            msg = TagUtils.getInstance().message(getPageContext(), bundle, locale, key);
        } catch (ClassCastException cce) {
            log.debug("class cast exception: ", cce);
        } catch (JspException je) {
            log.debug("can't evaluate resource [" + columnValue + "]: ", je);
        }

        if (msg.length() == 0) {
            msg = (b.booleanValue()) ? "Yes" : "No";
        }

        return msg;
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);

        if (ancestorTag == null) {
            throw new JspTagException("A BooleanDecorator must be used within a ColumnTag.");
        }

        // You have to make a clone, otherwise, if there are more than one
        // boolean decorator in this table, then we'll end up with only one
        // boolean decorator object
        EqualsDecorator clone;
        try {
            clone = (EqualsDecorator) this.clone();
        } catch (CloneNotSupportedException e) {
            // Then just use this
            clone = this;
        }

        ancestorTag.setDecorator(clone);
        return SKIP_BODY;
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