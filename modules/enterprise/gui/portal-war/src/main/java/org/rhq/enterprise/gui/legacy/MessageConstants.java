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
package org.rhq.enterprise.gui.legacy;

/**
 * Error Messages defined in Application.Properties file
 */

public interface MessageConstants {
    //---------------------------------------error messages

    public static final String ERR_CAM_GENERAL = "errors.cam.general";

    public static final String ERR_ROLE_NOT_FOUND = "admin.role.error.RoleNotFound";

    public static final String ERR_ROLE_CREATION = "admin.role.error.BadRole";

    public static final String ERR_PERM_USER_REMOVAL = "admin.user.error.PermissionException.RemoveUser";

    public static final String ERR_RESOURCE_NOT_FOUND = "resource.common.error.ResourceNotFound";

    public static final String ERR_RESOURCE_GROUP_NOT_FOUND = "resource.common.error.ResourceGroupNotFound";

    public static final String ERR_RESOURCE_ID_FOUND = "resource.common.inventory.error.ResourceIdNotKnown";

    public static final String ERR_DUP_RESOURCE_FOUND = "resource.group.inventory.error.DuplicateResourceName";

    public static final String ERR_DUP_CLUSTER_ASSIGNMENT = "resource.group.inventory.error.DuplicateClusterAssignment";

    public static final String ERR_RESOURCE_VIEW_PERM = "resource.common.error.ResourceViewPermission";

    public static final String ERR_RESOURCE_LIST_VIEW_PERM = "resource.common.error.ResourceListViewPermission";

    public static final String ERR_PLATFORM_NOT_FOUND = "resource.platform.error.PlatformNotFound";

    public static final String ERR_PLATFORM_NOT_COMMITTED = "resource.platform.error.PlatformNotCommitted";

    public static final String ERR_URL_NOT_SPECIFIED = "resource.common.error.UrlNotFound";

    //---------------------------------------error attrs

    public static final String ERR_PLATFORM_HEALTH_ATTR = "resource.common.error.PlatformHealth";

    public static final String ERR_SERVER_HEALTH_ATTR = "resource.common.error.ServerHealth";

    //---------------------------------------confirmation messages

    public static final String CNF_METRIC_DISPLAY_RANGE_CHANGED = "resource.common.monitor.confirm.MetricDisplayRangeChanged";

    public static final String CNF_FAVORITE_METRICS_ADDED = "resource.common.monitor.confirm.FavoriteMetricsAdded";

    public static final String CNF_FAVORITE_METRICS_REMOVED = "resource.common.monitor.confirm.FavoriteMetricsRemoved";

    public static final String CNF_METRICS_BASELINE_SET = "resource.common.monitor.confirm.MetricsBaselineSet";

    public static final String CNF_AUTO_BASELINE_SET = "resource.common.monitor.confirm.AutoBaselineSet";
}