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
package org.rhq.gui.webdav;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bradmcevoy.http.Resource;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Represents a managed resource along with its children resources.
 * 
 * @see BasicResource
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ResourceFolder extends BasicResource implements AuthenticatedCollectionResource {

    private List<Resource> children;

    public ResourceFolder(Subject subject, org.rhq.core.domain.resource.Resource managedResource) {
        super(subject, managedResource);
    }

    public String getUniqueId() {
        return String.valueOf(getManagedResource().getId());
    }

    public String getName() {
        return getManagedResource().getName();
    }

    public Date getModifiedDate() {
        return new Date(getManagedResource().getMtime());
    }

    public Date getCreateDate() {
        return new Date(getManagedResource().getCtime());
    }

    public Resource child(String childName) {
        List<? extends Resource> children = getChildren();
        for (Resource child : children) {
            if (childName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    public List<? extends Resource> getChildren() {
        return getChildren(getSubject());
    }

    public List<? extends Resource> getChildren(Subject subject) {
        if (this.children == null) {
            PageList<org.rhq.core.domain.resource.Resource> childs;
            ResourceManagerLocal rm = LookupUtil.getResourceManager();
            childs = rm.getChildResources(subject, getManagedResource(), PageControl.getUnlimitedInstance());

            this.children = new ArrayList<Resource>(childs.size());
            for (org.rhq.core.domain.resource.Resource child : childs) {
                if (child.getInventoryStatus() == InventoryStatus.COMMITTED) {
                    this.children.add(new ResourceFolder(subject, child));
                }
            }

            AvailabilityResource availabilityResource = new AvailabilityResource(subject, getManagedResource());
            this.children.add(availabilityResource);

            ConfigurationManagerLocal cm = LookupUtil.getConfigurationManager();
            Configuration config = cm.getActiveResourceConfiguration(getManagedResource().getId());
            if (config != null && !config.getProperties().isEmpty()) {
                this.children.add(new ConfigResource(subject, getManagedResource(), config));
            }

            MeasurementDataManagerLocal mdm = LookupUtil.getMeasurementDataManager();
            List<MeasurementDataTrait> traits = mdm.getCurrentTraitsForResource(getManagedResource().getId(), null);
            if (traits != null && traits.size() > 0) {
                this.children.add(new TraitsResource(subject, getManagedResource(), traits));
            }
        }
        return this.children;
    }
}
