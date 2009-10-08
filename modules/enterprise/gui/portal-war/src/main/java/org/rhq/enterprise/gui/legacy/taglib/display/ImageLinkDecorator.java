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
 * The ImageButtonDecorator is nice for when the images are submitting a form from within a table but when the table is
 * populated with links that are orthogonal to the form's purpose and link to another set of functionality, this
 * decorator is the ticket.
 */
public class ImageLinkDecorator extends BaseDecorator {
    private static Log log = LogFactory.getLog(ImageLinkDecorator.class.getName());
    private static final String DEFAULT_BORDER = "0";
    private static final String TAG = "imagelinkdecorator";

    // tag attrs
    private String href_el;
    private String src_el;
    private String border_el;
    private String id_el;

    // attrs are optional
    private boolean borderIsSet = false;
    private boolean idIsSet = false;
    private boolean hrefIsSet = false;

    // values post evaluation by el engine
    private String href_val;
    private String src_val;
    private String border_val;
    private String id_val;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.legacy.taglib.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        String contextPath = ((HttpServletRequest) getPageContext().getRequest()).getContextPath();
        StringBuffer error = new StringBuffer();

        if (hrefIsSet) {
            try {
                setHrefVal(contextPath + (String) evalAttr("href", getHref(), String.class));
            } catch (NullAttributeException e) {
                error.append(generateErrorComment(e.getClass().getName(), "href_el", getHref(), e));
            } catch (JspException e) {
                error.append(generateErrorComment(e.getClass().getName(), "href_el", getHref(), e));
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

        if (idIsSet) {
            try {
                setIdVal((String) evalAttr("id", getId(), String.class));
            } catch (NullAttributeException e) {
                error.append(generateErrorComment(e.getClass().getName(), "id_el", getId(), e));
            } catch (JspException e) {
                error.append(generateErrorComment(e.getClass().getName(), "id_el", getId(), e));
            }
        }

        if (error.length() > 0) {
            return error.toString();
        }

        return generateOutput();
    }

    public void release() {
        super.release();
        href_el = null;
        src_el = null;
        border_el = null;
        id_el = null;
        href_val = null;
        src_val = null;
        border_val = null;
        id_val = null;
        borderIsSet = false;
        idIsSet = false;
        hrefIsSet = false;
    }

    private String generateOutput() {
        StringBuffer sb = new StringBuffer();
        if (hrefIsSet) {
            sb.append("<a ");
            if (idIsSet) {
                sb.append("id=\"");
                sb.append(getIdVal());
                sb.append("\" ");
            }

            sb.append("href=\"").append(getHrefVal()).append("\">");
        }

        sb.append("<img src=\"");
        sb.append(getSrcVal()).append("\" border=\"");
        sb.append(getBorderVal()).append("\">");
        if (hrefIsSet) {
            sb.append("</a>");
        }

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
     * Returns the href_el.
     *
     * @return String
     */
    public String getHref() {
        return href_el;
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
     * Sets the href_el.
     *
     * @param href_el The href_el to set
     */
    public void setHref(String href_el) {
        hrefIsSet = true;
        this.href_el = href_el;
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
     * Returns the href_val.
     *
     * @return String
     */
    private String getHrefVal() {
        return href_val;
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
     * Sets the href_val.
     *
     * @param href_val The href_val to set
     */
    private void setHrefVal(String href_val) {
        this.href_val = href_val;
    }

    /**
     * Sets the src_val.
     *
     * @param src_val The src_val to set
     */
    private void setSrcVal(String src_val) {
        this.src_val = src_val;
    }

    /**
     * Returns the id_el.
     *
     * @return String
     */
    public String getId() {
        return id_el;
    }

    /**
     * Returns the id_val.
     *
     * @return String
     */
    public String getIdVal() {
        return id_val;
    }

    /**
     * Sets the id_el.
     *
     * @param id_el The id_el to set
     */
    public void setId(String id_el) {
        idIsSet = true;
        this.id_el = id_el;
    }

    /**
     * Sets the id_val.
     *
     * @param id_val The id_val to set
     */
    public void setIdVal(String id_val) {
        this.id_val = id_val;
    }
}