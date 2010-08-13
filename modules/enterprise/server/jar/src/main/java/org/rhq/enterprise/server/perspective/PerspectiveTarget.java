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

package org.rhq.enterprise.server.perspective;

/**
 * @author jshaughnessy
 */
public enum PerspectiveTarget {
    AGENT("/rhq/ha/viewAgent.xhtml?mode=view&agentId="), //
    ALERT("/alerts/Alerts.do?mode=viewAlert&a=", true, false), //
    ALERT_DEFINITION("/alerts/Config.do?mode=viewRoles&ad=", true, false), //    
    ALERT_TEMPLATE("/alerts/Config.do?mode=viewRoles&ad=", false, true), //    
    CONTENT_PROVIDER("/rhq/content/contentProvider.xhtml?mode=view&id="), //
    GROUP_COMPATIBLE("/rhq/group/monitor/graphs.xhtml?category=COMPATIBLE&groupId="), //
    GROUP_DEFINITION("/rhq/definition/group/view.xhtml?groupDefinitionId="), //
    GROUP_MIXED("/rhq/group/inventory/view.xhtml?category=MIXED&groupId="), //
    METRIC_TEMPLATE("/admin/platform/monitor/Config.do?mode=configure&id=", false, true), //
    REPO("/rhq/content/repo.xhtml?mode=view&id="), // 
    RESOURCE("/rhq/resource/summary/summary.xhtml?id="), //
    ROLE("/admin/role/RoleAdmin.do?mode=view&r="), //
    SERVER("/rhq/ha/viewServer.xhtml?mode=view&serverId="), //
    USER("/admin/user/UserAdmin.do?mode=view&u=");

    private final String baseUrl;
    private final boolean isResourceTarget;
    private final boolean isTemplateTarget;

    PerspectiveTarget(String baseUrl) {
        this(baseUrl, false, false);
    }

    private PerspectiveTarget(String baseUrl, boolean isResourceTarget, boolean isTemplateTarget) {
        this.baseUrl = baseUrl;
        this.isResourceTarget = isResourceTarget;
        this.isTemplateTarget = isTemplateTarget;
    }

    /**
     * @param targetId The target id. For example, the group id.
     * @return The url path for the target.
     * @throws IllegalArgumentException If the PerspectiveTarget is resource qualified (requires a resource id).
     */
    public String getTargetUrl(int targetId) {
        if (this.isResourceTarget || this.isTemplateTarget) {
            throw new IllegalArgumentException(
                "This PerspectiveTarget requires resource or resource type information, use appropriate getter: "
                    + this);
        }

        return this.baseUrl + targetId;
    }

    /**
     * @param resourceId The target's resource. For example, the resource on which the alert fired.
     * @param targetId The resource target. For example, the alert for the specified resource.
     * @return The url path for the resource qualified target.
     * @throws IllegalArgumentException If the PerspectiveTarget is not resource qualified.
     */
    public String getResourceTargetUrl(int resourceId, int targetId) {
        if (!this.isResourceTarget) {
            throw new IllegalArgumentException(
                "This is not a resource qualified PerspectiveTarget. Use appropriate getter: " + this);
        }

        return this.baseUrl + targetId + "&id=" + resourceId;
    }

    /**
     * @param resourceTypeId The target's resource type. For example, the resource type for the alert template.
     * @param targetId The target. For example, the alert definition for the alert template.
     * @return The url path for the template target.
     * @throws IllegalArgumentException If the PerspectiveTarget is not a template target.
     */
    public String getTemplateTargetUrl(int resourceTypeId, int targetId) {
        if (!this.isTemplateTarget) {
            throw new IllegalArgumentException("This is not a template PerspectiveTarget. Use appropriate getter: "
                + this);
        }

        return this.baseUrl + targetId + "&type=" + resourceTypeId;
    }

    public boolean isResourceTarget() {
        return isResourceTarget;
    }

    public boolean isTemplateTarget() {
        return isTemplateTarget;
    }

}
