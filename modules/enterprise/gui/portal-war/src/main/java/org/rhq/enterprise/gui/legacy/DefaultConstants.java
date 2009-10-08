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

import org.rhq.core.domain.resource.ResourceCategory;

/**
 * Default values used in the UI
 */
public interface DefaultConstants {
    //---------------------------------------default values

    /**
     * Default size for add to list tables
     */
    public static final Integer ADDTOLIST_SIZE_DEFAULT = new Integer(15);

    /**
     * Availabiity check timeout defaults to 20 seconds
     */
    public static final long AVAILABILITY_DEFAULT_TIMEOUT = 20000;

    /**
     * Default number of data points to show on chart
     */
    public static final int DEFAULT_CHART_POINTS = 60;

    /**
     * Default resource category
     */
    public static final ResourceCategory RESOURCE_CATEGORY_DEFAULT = ResourceCategory.PLATFORM;

    /**
     * Maximum nuber of pages(dots) to be rendered
     */
    public static final Integer MAX_PAGES = new Integer(15);

    /**
     * Default page number for list tables
     */
    public static final Integer PAGENUM_DEFAULT = new Integer(0);

    /**
     * Display all items in list
     */
    public static final Integer PAGESIZE_ALL = new Integer(-1);

    /**
     * Default number of rows for list tables
     */
    public static final Integer PAGESIZE_DEFAULT = new Integer(15);

    /**
     * Default sort column for list tables
     */
    public static final Integer SORTCOL_DEFAULT = new Integer(1);

    /**
     * ascending sort order for list tables
     */
    public static final String SORTORDER_ASC = "asc";

    /**
     * ascending sort order for list tables
     */
    public static final String SORTORDER_DEC = "desc";

    /**
     * default sort order for list tables
     */
    public static final String SORTORDER_DEFAULT = SORTORDER_ASC;
}