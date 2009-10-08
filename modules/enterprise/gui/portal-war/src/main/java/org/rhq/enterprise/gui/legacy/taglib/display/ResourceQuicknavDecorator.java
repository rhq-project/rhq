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

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceFacets;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.server.resource.ResourceFacetsCache;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A {@link QuicknavDecorator} for {@link Resource}s.
 *
 * @author Ian Springer
 */
public class ResourceQuicknavDecorator extends QuicknavDecorator {
    private static final String MONITOR_URL = "/rhq/resource/monitor/graphs.xhtml";
    private static final String EVENTS_URL = "/rhq/resource/events/history.xhtml";
    private static final String INVENTORY_URL = "/rhq/resource/inventory/view.xhtml";
    private static final String CONFIGURE_URL = "/rhq/resource/configuration/view.xhtml";
    private static final String OPERATIONS_URL = "/rhq/resource/operation/resourceOperationScheduleNew.xhtml";
    private static final String ALERT_URL = "/rhq/resource/alert/listAlertDefinitions.xhtml";
    private static final String CONTENT_URL = "/rhq/resource/content/view.xhtml?mode=view";

    private ResourceComposite resourceComposite;
    private ResourceFacets resourceFacets;

    @Override
    public String decorate(Object columnValue) throws Exception {
        this.resourceComposite = (ResourceComposite) columnValue;

        ResourceType type = this.resourceComposite.getResource().getResourceType();
        if (type == null) {
            this.resourceFacets = ResourceFacets.NONE;
        } else {
            this.resourceFacets = ResourceFacetsCache.getSingleton().getResourceFacets(type.getId());
        }
        return getOutput();
    }

    @Override
    protected String getFullURL(String url) {
        HttpServletRequest request = (HttpServletRequest) getPageContext().getRequest();
        return request.getContextPath() + url + ((url.indexOf("?") == -1) ? "?" : "&")
            + ParamConstants.RESOURCE_ID_PARAM + "=" + this.resourceComposite.getResource().getId();
    }

    @Override
    protected String getTagName() {
        return "resource-quicknav-decorator";
    }

    @Override
    protected boolean isMonitorSupported() {
        return true;
    }

    @Override
    protected boolean isEventsSupported() {
        return this.resourceFacets.isEvent();
    }

    @Override
    protected boolean isInventorySupported() {
        return true;
    }

    @Override
    protected boolean isConfigureSupported() {
        return this.resourceFacets.isConfiguration();
    }

    @Override
    protected boolean isOperationsSupported() {
        return this.resourceFacets.isOperation();
    }

    @Override
    protected boolean isAlertSupported() {
        return true;
    }

    @Override
    protected boolean isContentSupported() {
        return this.resourceFacets.isContent();
    }

    // TODO: For now, all icons are "allowed", but this may change in the future.
    //       (see http://jira.jboss.com/jira/browse/JBNADM-1616)

    @Override
    protected boolean isMonitorAllowed() {
        return LookupUtil.getSystemManager().isMonitoringEnabled();
    }

    @Override
    protected boolean isEventsAllowed() {
        return true;
    }

    @Override
    protected boolean isInventoryAllowed() {
        return true;
    }

    @Override
    protected boolean isConfigureAllowed() {
        return true;
    }

    @Override
    protected boolean isOperationsAllowed() {
        return true;
    }

    @Override
    protected boolean isAlertAllowed() {
        return LookupUtil.getSystemManager().isMonitoringEnabled();
    }

    @Override
    protected boolean isContentAllowed() {
        return true;
    }

    @Override
    protected String getMonitorURL() {
        return MONITOR_URL;
    }

    @Override
    protected String getEventsURL() {
        return EVENTS_URL;
    }

    @Override
    protected String getInventoryURL() {
        return INVENTORY_URL;
    }

    @Override
    protected String getConfigureURL() {
        return CONFIGURE_URL;
    }

    @Override
    protected String getOperationsURL() {
        return OPERATIONS_URL;
    }

    @Override
    protected String getAlertURL() {
        return ALERT_URL;
    }

    @Override
    protected String getContentURL() {
        return CONTENT_URL;
    }
}