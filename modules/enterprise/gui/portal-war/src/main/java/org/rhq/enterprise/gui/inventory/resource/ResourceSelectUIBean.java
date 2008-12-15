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
package org.rhq.enterprise.gui.inventory.resource;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class ResourceSelectUIBean {


    private Resource resource;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();



    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }


    public List<ResourceComposite> autocomplete(Object suggest) {
        String pref = (String)suggest;
        ArrayList<ResourceComposite> result;

        PageControl pc = new PageControl();
        pc.setPageSize(50);

        result = resourceManager.findResourceComposites(
                EnterpriseFacesContextUtility.getSubject(),
                null,
                null,
                null,
                pref,
                true,
                pc);

        return result;
    }
}
