/*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */

package org.rhq.enterprise.server.jaxb.adapter;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**This adapter is a JAXB wrapper for the ResourceGroup class.  
 * 
 * Problematic fields on resource: ResourceGroup.groupCategory
 * 
 * @author Simeon Pinder
 *
 */
public class ResourceGroupAdapter extends XmlAdapter<WsResourceGroupWrapper, ResourceGroup> {

    /**Converts a ResourceGroup type back to marshallable JAXB type.
     * 
     */
    public WsResourceGroupWrapper marshal(ResourceGroup opaque) throws Exception {
        WsResourceGroupWrapper resources = null;
        //no implementation now. Needed if server needs to re-populate some reference
        //before returning it to the client.
        if (opaque != null) {
            resources = new WsResourceGroupWrapper(opaque);
        }
        return resources;
    }

    /**
     * Converts the WsResourceGroupWrapper type back into familiar ResourceGroup type on server side.
     */
    public ResourceGroup unmarshal(WsResourceGroupWrapper marshallable) throws Exception {
        ResourceGroup group = null;
        if (marshallable != null) {
            //create new ResourceGroup instance to be returned
            group = new ResourceGroup("");
            //Copy all relevant fields over
            copy(marshallable, group);
        } else {
            throw new IllegalArgumentException("The WsConfiguration type passed in was null.");
        }
        return group;
    }

    /**Copies all the values from the client side
     * component to create equivalent server side element. 
     * 
     * groupCategory is null in client type
     * 
     * @param destination
     * @param source
     */
    private void copy(WsResourceGroupWrapper source, ResourceGroup destination) {
        //        destination.setAgent(source.getAgent());
        destination.setAlertDefinitions(source.getAlertDefinitions());
        destination.setResourceType(source.getResourceType());
        destination.setClusterBackingGroups(source.getClusterBackingGroups());
        destination.setClusterResourceGroup(source.getClusterResourceGroup());
        destination.setConfigurationUpdates(source.getConfigurationUpdates());
        destination.setCtime(source.getCtime());
        destination.setDescription(source.getDescription());
        destination.setExplicitResources(source.getExplicitResources());
        destination.setGroupByClause(source.getGroupByClause());
        destination.setGroupDefinition(source.getGroupDefinition());
        destination.setId(source.getId());
        destination.setImplicitResources(source.getImplicitResources());
        destination.setModifiedBy(source.getModifiedBy());
        destination.setMtime(source.getMtime());
        destination.setName(source.getName());
        destination.setOperationHistories(source.getOperationHistories());
        destination.setRecursive(source.isRecursive());
        destination.setVisible(source.isVisible());
    }
}

/**Purpose of this class is to create a JAXB marshallable class for Configuration
 * that does not have the same problems being serialized as Configuration.
 * 
 * @author Simeon Pinder
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
class WsResourceGroupWrapper extends ResourceGroup {

    //FIELDS : BEGIN
    private List<Resource> lineageList = new LinkedList<Resource>();

    //FIELDS: END

    //default no args constructor for JAXB and bean requirement
    public WsResourceGroupWrapper() {
        //        super();
    }

    public WsResourceGroupWrapper(ResourceGroup rg) {
        //TODO:add implementation?
    }

    //    public WsResourceListWrapper(List<Resource> opaque) {
    //        //store the resourceList as embedded property
    //        if (opaque != null) {
    //            for (Resource r : opaque) {
    //                //                lineageList.add(r);
    //                //make copy and save that copy
    //                Resource copy = new Resource();
    //                copyResource(copy, r);
    //                lineageList.add(copy);
    //            }
    //        }
    //    }

    //    //METHODS :END
}