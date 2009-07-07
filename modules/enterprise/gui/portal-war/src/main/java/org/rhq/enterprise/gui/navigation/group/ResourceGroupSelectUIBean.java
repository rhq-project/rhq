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
package org.rhq.enterprise.gui.navigation.group;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Used to support searching for groups.
 * 
 * @author John Mazzitelli
 */
public class ResourceGroupSelectUIBean {

    private ResourceGroup resourceGroup;

    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

    private String searchString;

    public ResourceGroup getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<ResourceGroupComposite> autocomplete(Object suggest) {
        String pref = (String) suggest;
        ArrayList<ResourceGroupComposite> result;

        PageControl pc = new PageControl();
        pc.setPageSize(50);

        result = resourceGroupManager.findResourceGroupComposites(EnterpriseFacesContextUtility.getSubject(), null, null,
            null, pref, null, null, pc);

        return result;
    }
}
