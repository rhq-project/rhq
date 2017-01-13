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
package org.rhq.enterprise.gui.common.paging;

import javax.faces.model.SelectItem;

/**
 * @author Joseph Marques
 */
public class PageControlSettingsUIBean {
    private SelectItem[] pageSizes;
    private static String MAX_ITEMS_PER_PAGE = "rhq.server.gui.max-items-per-page";

    public PageControlSettingsUIBean() {
        try {
            String maxItemsPerPage = System.getProperty(MAX_ITEMS_PER_PAGE);
            int maxItemsPerPageInt;
            if (maxItemsPerPage != null && (maxItemsPerPageInt = Integer.parseInt(maxItemsPerPage)) >= 45) {
                // Scale default page sizes
                String tier0 = String.valueOf(Math.max(15, (int) (maxItemsPerPageInt * 0.25)));
                String tier1 = String.valueOf(Math.max(30, (int) (maxItemsPerPageInt * 0.50)));
                String tier2 = String.valueOf(Math.max(45, (int) (maxItemsPerPageInt * 0.75)));
                pageSizes = new SelectItem[] { new SelectItem(tier0, tier0), new SelectItem(tier1, tier1),
                        new SelectItem(tier2, tier2), new SelectItem(maxItemsPerPage, maxItemsPerPage) };
            }
        } catch (NumberFormatException nfe) {
            // Ignore this exception.
        }
        if (pageSizes == null) {
            pageSizes = new SelectItem[]{new SelectItem("15", "15"), new SelectItem("30", "30"),
                    new SelectItem("45", "45")};
        }

    }

    public SelectItem[] getPageSizes() {
        return pageSizes;
    }

    public int getMinimumPageSize() {
        return 15;
    }
}