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

/**
 * @author Greg Hinkle
 */

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

public class ResourceTreeNode implements Comparable {

    private static ResourceTreeNode[] CHILDREN_ABSENT = new ResourceTreeNode[0];

    private List<ResourceTreeNode> children;

    private Object level;

    private String shortPath;

    public ResourceTreeNode(Object level) {
        this.level = level;
    }

    public synchronized List<ResourceTreeNode> getNodes() {
        if (children == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();

            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            children = new ArrayList<ResourceTreeNode>();

            if (level instanceof Resource) {
                List<AutoGroupComposite> comps = resourceManager.getChildrenAutoGroups(EnterpriseFacesContextUtility.getSubject(), ((Resource) level).getId());
                for (AutoGroupComposite comp : comps) {
                    children.add(new ResourceTreeNode(comp));
                }
            } else if (level instanceof AutoGroupComposite) {
                AutoGroupComposite comp = (AutoGroupComposite) level;
                List resources = comp.getResources();
                if (resources != null) {
                    for (Object res : resources) {
                        children.add(new ResourceTreeNode(res));
                    }
                }
            } else if (level instanceof ResourceWithAvailability) {
                List<AutoGroupComposite> comps = resourceManager.getChildrenAutoGroups(EnterpriseFacesContextUtility.getSubject(), ((ResourceWithAvailability) level).getResource().getId());
                for (AutoGroupComposite comp : comps) {
                    children.add(new ResourceTreeNode(comp));
                }
            } else {
                children = Collections.EMPTY_LIST;
            }
            Collections.sort(children);
        }

        return children;
    }

    public Object getData() {
        return level;
    }

    public String toString() {
        if (level instanceof AutoGroupComposite) {
            AutoGroupComposite composite = ((AutoGroupComposite) level) ;
            ResourceType type = composite.getResourceType();
            if (type != null) {
                return type.getName() + " (" + composite.getMemberCount() + ")";
            } else {
                return composite.getSubcategory().getName() + " (" + composite.getMemberCount() + ")";
            }
        } else if (level instanceof ResourceWithAvailability) {
            return ((ResourceWithAvailability) level).getResource().getName();
        } else if (level instanceof Resource) {
            return ((Resource) level).getName();
        }
        return level.toString();
    }

    public int compareTo(Object o) {
        return toString().compareTo(((ResourceTreeNode)o).toString());
    }
}