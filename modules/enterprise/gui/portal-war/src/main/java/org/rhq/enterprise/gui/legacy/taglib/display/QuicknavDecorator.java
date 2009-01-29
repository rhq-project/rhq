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
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * This class is a two-in-one decorator/tag for use within the <code>TableTag</code>; it is a <code>
 * ColumnDecorator</code> tag that that creates a row of quicknav icons (i.e. [M][I][C][A] etc.) for the resource hub.
 *
 * @author Ian Springer
 */
public abstract class QuicknavDecorator extends ColumnDecorator implements Tag {
    protected static final String DATA_TABLE_STYLE_CLASS = "data-table";
    protected static final String QUICKNAV_CELL_STYLE_CLASS = "quicknav-cell";
    protected static final String QUICKNAV_BLOCK_STYLE_CLASS = "quicknav-block";

    private static final IconInfo MONITOR_ICON_INFO = new IconInfo("/images/icon_hub_m.gif", "Monitor");

    private static final IconInfo EVENT_ICON_INFO = new IconInfo("/images/icon_hub_e.gif", "Events");

    private static final IconInfo INVENTORY_ICON_INFO = new IconInfo("/images/icon_hub_i.gif", "Inventory");

    private static final IconInfo CONFIGURE_ICON_INFO = new IconInfo("/images/icon_hub_c.gif", "Configure");

    private static final IconInfo OPERATIONS_ICON_INFO = new IconInfo("/images/icon_hub_o.gif", "Operations");

    private static final IconInfo ALERT_ICON_INFO = new IconInfo("/images/icon_hub_a.gif", "Alerts");

    private static final IconInfo CONTENT_ICON_INFO = new IconInfo("/images/icon_hub_p.gif", "Packages");

    private static final String ICON_SRC_LOCKED = "/images/icon_hub_locked.gif";

    private static final String ICON_WIDTH = "13";
    private static final String ICON_HEIGHT = "13";

    protected static final Log LOG = LogFactory.getLog(QuicknavDecorator.class.getName());

    private Tag parentTag;

    protected String getOutput() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<table class=\"").append(DATA_TABLE_STYLE_CLASS).append("\"><tr>");

        // MONITOR (M) icon
        appendCell(stringBuilder, isMonitorSupported(), isMonitorAllowed(), getMonitorURL(), MONITOR_ICON_INFO);

        // EVENTS (E) icon
        appendCell(stringBuilder, isEventsSupported(), isEventsAllowed(), getEventsURL(), EVENT_ICON_INFO);

        // INVENTORY (I) icon
        appendCell(stringBuilder, isInventorySupported(), isInventoryAllowed(), getInventoryURL(), INVENTORY_ICON_INFO);

        // CONFIGURE (C) icon
        appendCell(stringBuilder, isConfigureSupported(), isConfigureAllowed(), getConfigureURL(), CONFIGURE_ICON_INFO);

        // OPERATIONS (O) icon
        appendCell(stringBuilder, isOperationsSupported(), isOperationsAllowed(), getOperationsURL(),
            OPERATIONS_ICON_INFO);

        // ALERTS (A) icon
        appendCell(stringBuilder, isAlertSupported(), isAlertAllowed(), getAlertURL(), ALERT_ICON_INFO);

        // CONTENT (T) icon
        appendCell(stringBuilder, isContentSupported(), isContentAllowed(), getContentURL(), CONTENT_ICON_INFO);

        stringBuilder.append("</tr></table>");

        return stringBuilder.toString();
    }

    private void appendCell(StringBuilder stringBuilder, boolean facetSupported, boolean facetAllowed, String tabUrl,
        IconInfo iconInfo) {
        stringBuilder.append("<td class=\"").append(QUICKNAV_CELL_STYLE_CLASS).append("\">");
        stringBuilder.append("<div class=\"").append(QUICKNAV_BLOCK_STYLE_CLASS).append("\">");
        if (facetSupported) {
            if (facetAllowed) {
                appendLinkedIcon(stringBuilder, tabUrl, iconInfo);
            } else {
                appendLockedIcon(stringBuilder);
            }
        }

        stringBuilder.append("</div></td>");
    }

    protected String getNA() {
        return "";
    }

    protected void appendLockedIcon(StringBuilder stringBuilder) {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        stringBuilder.append("<img src=\"").append(request.getContextPath()).append(ICON_SRC_LOCKED).append(
            "\" width=\"").append(ICON_WIDTH).append("\" height=\"").append(ICON_HEIGHT).append(
            "\" alt=\"Locked\" />\n");
    }

    protected void appendLinkedIcon(StringBuilder stringBuilder, String url, IconInfo iconInfo) {
        String fullTargetURL = getFullURL(url);
        makeLinkedIconWithRef(stringBuilder, fullTargetURL, iconInfo.getSrc(), iconInfo.getTitle());
    }

    protected abstract String getFullURL(String relativeTargetURL);

    //   protected boolean isMonitoringEnabled()
    //   {
    //      return ContextUtils.monitoringEnabled(getPageContext().getServletContext());
    //   }

    private void makeLinkedIconWithRef(StringBuilder stringBuilder, String targetURL, String imageURL, String imageTitle) {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();

        stringBuilder.append("<a href=\"").append(targetURL).append("\">");

        stringBuilder.append("<img src=\"").append(request.getContextPath()).append(imageURL).append("\" width=\"")
            .append(ICON_WIDTH).append("\" height=\"").append(ICON_HEIGHT).append("\" title=\"").append(imageTitle)
            .append("\" alt=\"").append(imageTitle).append("\" />");

        stringBuilder.append("</a>\n");
    }

    public void setParent(Tag tag) {
        this.parentTag = tag;
    }

    public Tag getParent() {
        return this.parentTag;
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A " + getTagName() + " tag must be used within a ColumnTag.");
        }

        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        setParent(null);
        setPageContext(null);
    }

    protected abstract String getTagName();

    protected Object evalAttr(String name, String value, Class<?> type) throws JspException {
        return ExpressionUtil.evalNotNull(getTagName(), name, value, type, this, getPageContext());
    }

    protected abstract boolean isMonitorSupported();

    protected abstract boolean isEventsSupported();

    protected abstract boolean isInventorySupported();

    protected abstract boolean isConfigureSupported();

    protected abstract boolean isOperationsSupported();

    protected abstract boolean isAlertSupported();

    protected abstract boolean isContentSupported();

    protected abstract boolean isMonitorAllowed();

    protected abstract boolean isEventsAllowed();

    protected abstract boolean isInventoryAllowed();

    protected abstract boolean isConfigureAllowed();

    protected abstract boolean isOperationsAllowed();

    protected abstract boolean isAlertAllowed();

    protected abstract boolean isContentAllowed();

    protected abstract String getMonitorURL();

    protected abstract String getEventsURL();

    protected abstract String getInventoryURL();

    protected abstract String getConfigureURL();

    protected abstract String getOperationsURL();

    protected abstract String getAlertURL();

    protected abstract String getContentURL();

    protected static class IconInfo {
        private String src;
        private String title;

        public IconInfo(String src, String title) {
            this.src = src;
            this.title = title;
        }

        public String getSrc() {
            return src;
        }

        public String getTitle() {
            return title;
        }
    }
}