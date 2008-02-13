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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;

/**
 * The ImageDecorator is nice for when the images are within a table and may have attributes that would need to reflect
 * the changing values in iteration loop
 */
public class ImageDecorator extends BaseDecorator {
    private static Log log = LogFactory.getLog(ImageDecorator.class.getName());
    private static final String DEFAULT_BORDER = "0";
    private static final String TAG = "imagelinkdecorator";

    // tag attrs
    private String onmouseover_el;
    private String onmouseout_el;
    private String src_el;
    private String border_el;

    // attrs are optional
    private boolean borderIsSet = false;
    private boolean onmouseoverIsSet = false;
    private boolean onmouseoutIsSet = false;

    // values post evaluation by el engine
    private String onmouseover_val;
    private String onmouseout_val;
    private String src_val;
    private String border_val;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.legacy.taglib.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        String contextPath = ((HttpServletRequest) getPageContext().getRequest()).getContextPath();
        StringBuffer error = new StringBuffer();

        if (onmouseoverIsSet) {
            try {
                setOnmouseoverVal((String) evalAttr("onmouseover", getOnmouseover(), String.class));
            } catch (NullAttributeException e) {
                error.append(generateErrorComment(e.getClass().getName(), "onmouseover_el", getOnmouseover(), e));
            } catch (JspException e) {
                error.append(generateErrorComment(e.getClass().getName(), "onmouseover_el", getOnmouseover(), e));
            }
        }

        if (onmouseoutIsSet) {
            try {
                setOnmouseoutVal((String) evalAttr("onmouseout", getOnmouseout(), String.class));
            } catch (NullAttributeException e) {
                error.append(generateErrorComment(e.getClass().getName(), "onmouseout_el", getOnmouseout(), e));
            } catch (JspException e) {
                error.append(generateErrorComment(e.getClass().getName(), "onmouseout_el", getOnmouseout(), e));
            }
        }

        try {
            setSrcVal(contextPath + (String) evalAttr("src", getSrc(), String.class));
        } catch (NullAttributeException e) {
            error.append(generateErrorComment(e.getClass().getName(), "src_el", getSrc(), e));
        } catch (JspException e) {
            error.append(generateErrorComment(e.getClass().getName(), "src_el", getSrc(), e));
        }

        if (borderIsSet) {
            try {
                setBorderVal((String) evalAttr("border", getBorder(), String.class));
            } catch (NullAttributeException e) {
                error.append(generateErrorComment(e.getClass().getName(), "border_el", getBorder(), e));
            } catch (JspException e) {
                error.append(generateErrorComment(e.getClass().getName(), "border_el", getBorder(), e));
            }
        } else {
            setBorderVal(DEFAULT_BORDER);
        }

        if (error.length() > 0) {
            return error.toString();
        }

        return generateOutput();
    }

    public void release() {
        super.release();
        onmouseover_el = null;
        onmouseout_el = null;
        src_el = null;
        border_el = null;
        onmouseover_val = null;
        onmouseout_val = null;
        src_val = null;
        border_val = null;
        borderIsSet = false;
        onmouseoverIsSet = false;
        onmouseoutIsSet = false;
    }

    private String generateOutput() {
        StringBuffer sb = new StringBuffer();
        sb.append("<img src=\"");
        sb.append(getSrcVal()).append("\"");

        if (borderIsSet) {
            sb.append(" border=\"").append(getBorderVal()).append("\"");
        }

        if (onmouseoverIsSet) {
            sb.append(" onmouseover=\"").append(getOnmouseoverVal()).append("\"");
        }

        if (onmouseoutIsSet) {
            sb.append(" onmouseout=\"").append(getOnmouseoutVal()).append("\"");
        }

        sb.append("\">");
        return sb.toString();
    }

    /**
     * Returns the border_el.
     *
     * @return String
     */
    public String getBorder() {
        return border_el;
    }

    /**
     * Returns the onmouseover_el.
     *
     * @return String
     */
    public String getOnmouseover() {
        return onmouseover_el;
    }

    /**
     * Returns the onmouseout_el.
     *
     * @return String
     */
    public String getOnmouseout() {
        return onmouseout_el;
    }

    /**
     * Returns the src_el.
     *
     * @return String
     */
    public String getSrc() {
        return src_el;
    }

    /**
     * Sets the border_el.
     *
     * @param border_el The border_el to set
     */
    public void setBorder(String border_el) {
        borderIsSet = true;
        this.border_el = border_el;
    }

    /**
     * Sets the onmouseover_el.
     *
     * @param onmouseover_el The onmouseover_el to set
     */
    public void setOnmouseover(String onmouseover_el) {
        onmouseoverIsSet = true;
        this.onmouseover_el = onmouseover_el;
    }

    /**
     * Sets the onmouseout_el.
     *
     * @param onmouseout_el The onmouseout_el to set
     */
    public void setOnmouseout(String onmouseout_el) {
        onmouseoutIsSet = true;
        this.onmouseout_el = onmouseout_el;
    }

    /**
     * Sets the src_el.
     *
     * @param src_el The src_el to set
     */
    public void setSrc(String src_el) {
        this.src_el = src_el;
    }

    /**
     * Returns the border_val.
     *
     * @return String
     */
    private String getBorderVal() {
        return border_val;
    }

    /**
     * Returns the onmouseover_val.
     *
     * @return String
     */
    private String getOnmouseoverVal() {
        return onmouseover_val;
    }

    /**
     * Returns the onmouseout_val.
     *
     * @return String
     */
    private String getOnmouseoutVal() {
        return onmouseout_val;
    }

    /**
     * Returns the src_val.
     *
     * @return String
     */
    private String getSrcVal() {
        return src_val;
    }

    /**
     * Sets the border_val.
     *
     * @param border_val The border_val to set
     */
    private void setBorderVal(String border_val) {
        this.border_val = border_val;
    }

    /**
     * Sets the onmouseover_val.
     *
     * @param onmouseover_val The onmouseover_val to set
     */
    private void setOnmouseoverVal(String onmouseover_val) {
        this.onmouseover_val = onmouseover_val;
    }

    /**
     * Sets the onmouseout_val.
     *
     * @param onmouseout_val The onmouseout_val to set
     */
    private void setOnmouseoutVal(String onmouseout_val) {
        this.onmouseout_val = onmouseout_val;
    }

    /**
     * Sets the src_val.
     *
     * @param src_val The src_val to set
     */
    private void setSrcVal(String src_val) {
        this.src_val = src_val;
    }
}