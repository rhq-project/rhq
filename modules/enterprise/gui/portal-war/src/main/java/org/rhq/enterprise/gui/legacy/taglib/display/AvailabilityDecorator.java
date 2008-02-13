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
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * This class is a two in one decorator/tag for use within the <code>TableTag</code>; it is a <code>
 * ColumnDecorator</code> tag that that creates a column of availability icons.
 *
 * <p/>One of these days, when the whole DependencyNode thing is cleaned up, a lot of this stuff should just move to
 * it's own decorator just for DependencyNodes
 */
public class AvailabilityDecorator extends ColumnDecorator implements Tag {
    private static final String ICON_WIDTH = "15";
    private static final String ICON_HEIGHT = "15";
    private static final String ICON_BORDER = "0";

    private static final String ICON_UP = "/images/icon_available_green.png";
    private static final String ICON_DOWN = "/images/icon_available_red.png";
    private static final String ICON_WARN = "/images/icon_available_yellow.png";
    private static final String ICON_ERR = "/images/icon_available_grey.png";

    private PageContext context;
    private Tag parent;

    public AvailabilityDecorator() {
        super();
    }

    public String decorate(Object obj) throws Exception {
        return getOutputByValue(obj);
    }

    private String getOutputByValue(Object availability) throws JspException {
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        StringBuilder iconURL = new StringBuilder(req.getContextPath());

        String availabilityMessage;
        if (availability instanceof AvailabilityType) {
            availabilityMessage = ((AvailabilityType) availability).name();
            switch ((AvailabilityType) availability) {
            case UP: {
                iconURL.append(ICON_UP);
                availabilityMessage = "Up";
                break;
            }

            case DOWN: {
                iconURL.append(ICON_DOWN);
                availabilityMessage = "Down";
                break;
            }
            }
        } else if (availability instanceof Number) {
            double a = ((Number) availability).doubleValue();
            if (a == 1) {
                iconURL.append(ICON_UP);
                availabilityMessage = "Up";
            } else if (a == 0) {
                iconURL.append(ICON_DOWN);
                availabilityMessage = "Down";
            } else {
                iconURL.append(ICON_WARN);
                availabilityMessage = "Mixed";
            }
        } else if (availability == null) {
            iconURL.append(ICON_ERR);
            availabilityMessage = "Unknown";
        } else {
            throw new IllegalStateException(
                "Value of property attribute must be of type AvailabilityType or Number - its type is "
                    + availability.getClass().getName() + ".");
        }

        StringBuilder buf = new StringBuilder();
        buf.append("<img src=\"").append(iconURL).append("\" ");
        buf.append("width=\"").append(ICON_WIDTH).append("\" ");
        buf.append("height=\"").append(ICON_HEIGHT).append("\" ");
        buf.append("alt=\"").append(availabilityMessage).append("\" ");
        buf.append("title=\"").append(availabilityMessage).append("\" ");
        buf.append("border=\"").append(ICON_BORDER).append("\">");
        return buf.toString();
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("An AvailabilityDecorator must be used within a ColumnTag.");
        }

        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    public Tag getParent() {
        return parent;
    }

    public void setParent(Tag t) {
        this.parent = t;
    }

    public void setPageContext(PageContext pc) {
        this.context = pc;
    }

    public void release() {
        parent = null;
        context = null;
    }
}