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

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;

public class HubConstants {
    public static final String DEFAULT_GROUP_CATEGORY = GroupCategory.COMPATIBLE.name();
    public static final String DEFAULT_GROUP_NAME = null;

    public static final String PARAM_GROUP = "group";
    public static final String PARAM_GROUP_ID = "groupId";
    public static final String PARAM_GROUP_CATEGORY = "category";

    public static final String HIERARCHY_SEPARATOR = " > ";
    public static final String VIEW_ATTRIB = "Resource Hub View";

    public static final ResourceType ALL_RESOURCE_TYPES = null;
}