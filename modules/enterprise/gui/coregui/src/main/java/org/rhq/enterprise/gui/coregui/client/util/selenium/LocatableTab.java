/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.tab.Tab;

/**
 * Wrapper for com.smartgwt.client.widgets.tab.Tab that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableTab extends Tab implements Locatable {

    private String locatorId;

    /** 
     * <pre>
     * ID Format: "Tab-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableTab(String locatorId, String title) {
        super(title);
        init(locatorId);
    }

    /** 
     * <pre>
     * ID Format: "Tab-locatorId"
     * </pre>
     * @param locatorId not null or empty.
     */
    public LocatableTab(String locatorId, String title, String icon) {
        super(title, icon);
        init(locatorId);
    }

    private void init(String locatorId) {
        this.locatorId = locatorId;
        String unsafeId = "Tab-" + locatorId;
        setID(SeleniumUtility.getSafeId(unsafeId));
    }

    public String getLocatorId() {
        return locatorId;
    }

    public String extendLocatorId(String extension) {
        return this.locatorId + "-" + extension;
    }

}
