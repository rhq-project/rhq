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
package org.rhq.enterprise.gui.uibeans;

/**
 * Declares constants used by bizapp beans.
 */
public class UIConstants {
    /**
     * Identifies a <code>ResourceTypeDisplaySummary</code> representing two or more resources of like resource type.
     */
    public static final int SUMMARY_TYPE_AUTOGROUP = 1;

    /**
     * Identifies a <code>ResourceTypeDisplaySummary</code> representing a cluster of resources.
     */
    public static final int SUMMARY_TYPE_CLUSTER = 2;

    /**
     * Identifies a <code>ResourceTypeDisplaySummary</code> representing a single resource.
     */
    public static final int SUMMARY_TYPE_SINGLETON = 3;

    /**
     * The {@link WebUserPreferences#getPageRefreshPeriod()} value indicates no page refresh. 
     */
    public static final int DONT_REFRESH_PAGE = 0;
}