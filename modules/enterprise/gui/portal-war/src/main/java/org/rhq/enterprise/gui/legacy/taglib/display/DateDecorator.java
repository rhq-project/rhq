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

import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.rhq.core.clientapi.util.units.DateFormatter;
import org.rhq.core.clientapi.util.units.FormattedNumber;
import org.rhq.core.clientapi.util.units.ScaleConstants;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * This class decorates longs representing dates to dates.
 */
public class DateDecorator extends BaseDecorator {
    private static Log log = LogFactory.getLog(DateDecorator.class.getName());

    /**
     * Holds value of property isElapsedTime.
     */
    private Boolean isElapsedTime;

    /**
     * Holds value of property isGroup.
     */
    private Boolean isGroup = false;

    // el version
    private String isGroupEl;

    /**
     * Holds value of property active.
     */
    private String active;

    /**
     * Holds the format of for the conversion of the input
     *
     * @see java.text.SimpleDateFormat
     */
    private String format;

    /**
     * Holds key into resource bundle for the property which should be shown if the date being displayed is null
     */
    private String resourceKeyForNull;

    public static final String defaultKey = "resource.hub.metric.not.applicable";
    private PageContext context;
    protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;

    /**
     * Decorates a date represented as a long.
     *
     * @param  obj a long representing the time as a long
     *
     * @return formatted date
     */
    @Override
    public String decorate(Object obj) {
        Long newDate = null;

        if (getName() != null) {
            String tmpName = getName();
            try {
                tmpName = (String) evalAttr("name", this.getName(), String.class);
                newDate = new Long(Long.parseLong(tmpName));
            } catch (NumberFormatException nfe) {
                log.debug("number format exception parsing long for: " + tmpName);
                return "";
            } catch (NullAttributeException ne) {
                log.debug("bean " + this.getName() + " not found");
                return "";
            } catch (JspException je) {
                log.debug("can't evaluate name [" + this.getName() + "]: ", je);
                return "";
            }
        } else {
            newDate = (Long) obj;
        }

        if (getActive() != null) {
            try {
                String tmpActive = (String) evalAttr("active", this.getActive(), String.class);
                int tmpIntActive = Integer.parseInt(tmpActive);
                if (tmpIntActive == 0) {
                    return "";
                }
            } catch (NumberFormatException nfe) {
                log.debug("invalid property");
            } catch (NullAttributeException ne) {
                log.debug("bean " + this.getActive() + " not found");
            } catch (JspException je) {
                log.debug("can't evaluate name [" + this.getActive() + "]: ", je);
            }
        }

        if (isGroupEl != null) {
            try {
                Boolean tmp = (Boolean) evalAttr("isGroupEl", isGroupEl, Boolean.class);
                if (tmp != null) {
                    isGroup = tmp;
                }
            } catch (NullAttributeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JspException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();

        if ((newDate != null) && newDate.equals(new Long(0))) {
            String resString;
            if ((this.getIsGroup() != null) && isGroup.booleanValue()) {
                resString = RequestUtils.message(request, "resource.common.monitor.visibility.config.DIFFERENT");
            } else {
                resString = RequestUtils.message(request, "resource.common.monitor.visibility.config.NONE");
            }

            return resString;
        }

        StringBuffer buf = new StringBuffer(512);

        if (obj == null) {
            // there may be cases where we have no date set when rendering a
            // table, so just show n/a (see PR 8443)

            // infact let it be overridable what we show
            String resourceKey = getResourceKeyForNull();
            if ((resourceKey == null) || "".equals(resourceKey)) {
                resourceKey = DateDecorator.defaultKey;
            }

            buf.append(RequestUtils.message(request, bundle, request.getLocale().toString(), resourceKey));
            return buf.toString();
        }

        Boolean b = getIsElapsedTime();
        if (null == b) {
            b = Boolean.FALSE;
        }

        UnitsConstants unit = b.booleanValue() ? UnitsConstants.UNIT_DURATION : UnitsConstants.UNIT_DATE;
        String formatString;
        if ((format == null) || "".equals(format)) // old case with implicit format string
        {
            formatString = RequestUtils.message((HttpServletRequest) getPageContext().getRequest(),
                Constants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis");
        } else {
            formatString = format;
        }

        DateFormatter.DateSpecifics dateSpecs;

        dateSpecs = new DateFormatter.DateSpecifics();
        dateSpecs.setDateFormat(new SimpleDateFormat(formatString));
        FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(newDate.doubleValue(), unit,
            ScaleConstants.SCALE_MILLI), getPageContext().getRequest().getLocale(), dateSpecs);
        buf.append(fmtd.toString());
        return buf.toString();
    }

    /**
     * Getter for property isElapsedTime.
     *
     * @return Value of property isElapsedTime.
     */
    public Boolean getIsElapsedTime() {
        return this.isElapsedTime;
    }

    /**
     * Setter for property isElapsedTime.
     *
     * @param isElapsedTime New value of property isElapsedTime.
     */
    public void setIsElapsedTime(Boolean isElapsedTime) {
        this.isElapsedTime = isElapsedTime;
    }

    /**
     * If this is a group, display "DIFFERENT" if the metric interval value is "0".
     *
     * @return Value of property isGroup.
     */
    public String getIsGroup() {
        return isGroupEl;
    }

    /**
     * Setter for property isGroup.
     *
     * @param isGroup New value of property isGroup.
     */
    public void setIsGroup(String el) {
        isGroupEl = el;
    }

    public PageContext getContext() {
        return context;
    }

    public void setContext(PageContext context) {
        this.context = context;
    }

    /**
     * @return Returns the active.
     */
    public String getActive() {
        return active;
    }

    /**
     * @param active The active to set.
     */
    public void setActive(String active) {
        this.active = active;
    }

    public String getResourceKeyForNull() {
        return resourceKeyForNull;
    }

    public void setResourceKeyForNull(String resourceKeyForNull) {
        this.resourceKeyForNull = resourceKeyForNull;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}