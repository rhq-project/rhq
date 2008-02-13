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

import javax.servlet.jsp.JspException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;

/**
 * This class acts as a decorator for tables, displaying "YES" "NO" or "SOME", depending if total is 0, equal to, or
 * less than active.
 */
public class GroupMetricsDecorator extends BaseDecorator {
    protected String locale = org.apache.struts.Globals.LOCALE_KEY;
    protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;

    private static Log log = LogFactory.getLog(GroupMetricsDecorator.class.getName());

    /**
     * Holds value of property active.
     */
    private String active;

    /**
     * Holds value of property total.
     */
    private String total;

    /**
     * Compares active and total, and outputs "YES" "NO" or "SOME"
     *
     * @param  obj Does not use this value
     *
     * @return formatted date
     */
    public String decorate(Object obj) {
        String tmpActive = null;
        int tmpIntActive = 0;
        int tmpIntTotal = 0;
        if ((getActive() != null) && (getTotal() != null)) {
            String tmpName = getActive();
            try {
                tmpActive = (String) evalAttr("active", this.getActive(), String.class);
                tmpIntActive = Integer.parseInt(tmpActive);
                tmpActive = (String) evalAttr("total", this.getTotal(), String.class);
                tmpIntTotal = Integer.parseInt(tmpActive);
            } catch (NumberFormatException nfe) {
                log.debug("invalid property");
                return "";
            } catch (NullAttributeException ne) {
                log.debug("bean " + this.getActive() + " not found");
                return "";
            } catch (JspException je) {
                log.debug("can't evaluate name [" + this.getActive() + "]: ", je);
                return "";
            }
        }

        try {
            if (tmpIntActive == 0) {
                return RequestUtils.message(this.getPageContext(), bundle, locale,
                    "resource.common.monitor.visibility.config.NO");
            } else if (tmpIntActive < tmpIntTotal) {
                return RequestUtils.message(this.getPageContext(), bundle, locale,
                    "resource.common.monitor.visibility.config.SOME");
            } else {
                return RequestUtils.message(this.getPageContext(), bundle, locale,
                    "resource.common.monitor.visibility.config.YES");
            }
        } catch (JspException je) {
            log.debug("could not look up message: " + je);
        }

        return "";
    }

    public void release() {
        super.release();
        active = null;
        total = null;
    }

    /**
     * Getter for property active.
     *
     * @return Value of property active.
     */
    public String getActive() {
        return this.active;
    }

    /**
     * Setter for property active.
     *
     * @param active New value of property active.
     */
    public void setActive(String active) {
        this.active = active;
    }

    /**
     * Getter for property total.
     *
     * @return Value of property total.
     */
    public String getTotal() {
        return this.total;
    }

    /**
     * Setter for property total.
     *
     * @param total New value of property total.
     */
    public void setTotal(String total) {
        this.total = total;
    }
}