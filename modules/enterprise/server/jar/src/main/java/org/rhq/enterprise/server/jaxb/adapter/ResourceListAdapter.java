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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;

/**This adapter is a JAXB wrapper for the Resource class.  
 * 
 * Problematic field on resource: Resource.parentResource
 *                                Resource.List<ResourceErrors>
 * 
 * @author Simeon Pinder
 *
 */
public class ResourceListAdapter extends XmlAdapter<WsResourceListWrapper, List<Resource>> {

    /**Converts a Resource type back to marshallable JAXB type.
     * 
     */
    public WsResourceListWrapper marshal(List<Resource> opaque) throws Exception {

        WsResourceListWrapper resources = null;

        if (opaque != null) {
            resources = new WsResourceListWrapper(opaque);
        } else {
            throw new IllegalArgumentException("The resource passed in was null.");
        }
        return resources;
    }

    /**
     * Converts the WsResource type back into familiar Resource type on server side.
     * TODO: not sure we need this at all as there would have to be a reason to turn 
     * WsResource back into a List<Resource>.  Problem is one way for now.
     */
    public List<Resource> unmarshal(WsResourceListWrapper marshallable) throws Exception {

        List<Resource> resource = null;
        if (marshallable != null) {

            //create new Config instance to be returned
            resource = new ArrayList<Resource>();

        } else {
            throw new IllegalArgumentException("The WsResource type passed in was null.");
        }
        return resource;
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
class WsResourceListWrapper extends Resource {

    //FIELDS : BEGIN
    //Following fields are not populated by default because the fields in core/domain
    // object had to be marked as XmlTransient to eliminate cycles.  These fields are
    // added back here on the return type so that the client still has the appropriate 
    // id(s) to look up if they do need the data that was contained in the XMLTransient 
    // fields.
    private List<Resource> lineageList = new LinkedList<Resource>();
    private int parentResourceId = -1;
    private List<Integer> resourceErrorIds = new ArrayList<Integer>();

    //FIELDS: END

    //default no args constructor for JAXB and bean requirement
    public WsResourceListWrapper() {
        //    public WsResource() {
        super();
    }

    public WsResourceListWrapper(List<Resource> opaque) {
        //    public WsResource(Resource opaque) {
        //store the resourceList as embedded property
        if (opaque != null) {
            for (Resource r : opaque) {
                //make copy and save that copy
                WsResourceListWrapper copy = new WsResourceListWrapper();
                copyResource(copy, r);
                lineageList.add(copy);
            }
        }
    }

    /**Copies all the values from the original Server side
     * component to a different reference. JAXB will use 
     * reflection to null out those references that are 
     * annotated as XmlTransient.  In this case the parentResources.
     * 
     * parentResource is used in lineage type returned
     * 
     * @param destination
     * @param source
     */
    private void copyResource(WsResourceListWrapper destination, Resource source) {

        if ((destination == null) || (source == null)) {
            throw new IllegalArgumentException("Neither source or destination references can be null.");
        }
        destination.setAgent(source.getAgent());
        destination.setAlertDefinitions(source.getAlertDefinitions());
        destination.setDescription(source.getDescription());
        destination.setChildResources(source.getChildResources());
        destination.setConnected(source.isConnected());
        destination.setContentServiceRequests(source.getContentServiceRequests());
        destination.setCreateChildResourceRequests(source.getCreateChildResourceRequests());
        destination.setCurrentAvailability(source.getCurrentAvailability());
        destination.setDeleteResourceRequests(source.getDeleteResourceRequests());
        destination.setExplicitGroups(source.getExplicitGroups());
        destination.setImplicitGroups(source.getImplicitGroups());
        destination.setInstalledPackageHistory(source.getInstalledPackageHistory());
        destination.setInstalledPackages(source.getInstalledPackages());
        destination.setInventoryStatus(source.getInventoryStatus());
        destination.setItime(source.getItime());
        destination.setLocation(source.getLocation());
        destination.setModifiedBy(source.getModifiedBy());
        destination.setMtime(source.getMtime());
        destination.setOperationHistories(source.getOperationHistories());
        destination.setPluginConfiguration(source.getPluginConfiguration());
        destination.setPluginConfigurationUpdates(source.getPluginConfigurationUpdates());
        destination.setProductVersion(source.getProductVersion());
        destination.setResourceConfiguration(source.getResourceConfiguration());
        destination.setResourceConfigurationUpdates(source.getResourceConfigurationUpdates());
        destination.setSchendules(source.getSchedules());
        destination.setUuid(source.getUuid());
        //handle the problematic references that would cause cycles if used by storing only ids.
        if (source.getParentResource() != null) {
            destination.parentResourceId = source.getParentResource().getId();
        }
        if ((source.getResourceErrors() != null) && (!source.getResourceErrors().isEmpty())) {
            for (ResourceError error : source.getResourceErrors()) {
                destination.resourceErrorIds.add(error.getId());
            }
        }
        destination.setId(source.getId());
        destination.setName(source.getName());
        destination.setResourceKey(source.getResourceKey());
        destination.setResourceType(source.getResourceType());
        destination.setVersion(source.getVersion());

    }
    //    //METHODS :END
}