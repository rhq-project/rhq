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
package org.rhq.enterprise.gui.inventory.group;


import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.inventory.resource.ResourceTreeNode;

import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeNode implements Comparable {

    private static ResourceGroupTreeNode[] CHILDREN_ABSENT = new ResourceGroupTreeNode[0];

    private List<ResourceGroupTreeNode> children;

    private Object level;

    private String shortPath;

    public ResourceGroupTreeNode(Object level) {
        this.level = level;
    }

    public synchronized List<ResourceGroupTreeNode> getNodes() {
        if (children == null) {
            FacesContext facesContext = FacesContext.getCurrentInstance();

            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

            children = new ArrayList<ResourceGroupTreeNode>();

            /*if (level instanceof Resource) {
                List<AutoGroupComposite> comps = groupManager.getChildrenAutoGroups(EnterpriseFacesContextUtility.getSubject(), ((Resource) level).getId());
                for (AutoGroupComposite comp : comps) {
                    children.add(new ResourceGroupTreeNode(comp));
                }
            } else if (level instanceof AutoGroupComposite) {
                AutoGroupComposite comp = (AutoGroupComposite) level;
                List resources = comp.getResources();
                if (resources != null) {
                    for (Object res : resources) {
                        children.add(new ResourceGroupTreeNode(res));
                    }
                }
            } else if (level instanceof ResourceWithAvailability) {
                List<AutoGroupComposite> comps = groupManager.getChildrenAutoGroups(EnterpriseFacesContextUtility.getSubject(), ((ResourceWithAvailability) level).getResource().getId());
                for (AutoGroupComposite comp : comps) {
                    children.add(new ResourceGroupTreeNode(comp));
                }
            } else {
                children = Collections.EMPTY_LIST;
            }
            Collections.sort(children);*/
        }

        return children;
    }

    public Object getData() {
        return level;
    }

    public String toString() {
        if (level instanceof ResourceGroup) {
            ResourceGroup group = (ResourceGroup) level;
            return group.getName();
        }
        return level.toString();
    }

    public int compareTo(Object o) {
        return toString().compareTo(((ResourceGroupTreeNode)o).toString());
    }
}