package org.rhq.enterprise.server.jaxb.adapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.resource.Resource;

/**This adapter is a JAXB wrapper for the Resource class.  
 * 
 * Problematic field on resource: Resource.parentResource
 * 
 * @author Simeon Pinder
 *
 */
public class ResourceAdapter extends XmlAdapter<WsResourceListWrapper, List<Resource>> {

    /**Converts a Configuration type back to marshallable JAXB type.
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
            throw new IllegalArgumentException("The WsConfiguration type passed in was null.");
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
    private List<Resource> lineageList = new LinkedList<Resource>();

    //FIELDS: END

    //default no args constructor for JAXB and bean requirement
    public WsResourceListWrapper() {
        super();
    }

    public WsResourceListWrapper(List<Resource> opaque) {
        //store the resourceList as embedded property
        if (opaque != null) {
            for (Resource r : opaque) {
                //                lineageList.add(r);
                //make copy and save that copy
                Resource copy = new Resource();
                copyResource(copy, r);
                lineageList.add(copy);
            }
        }
    }

    /**Copies all the values from the original Server side
     * component to a different reference. JAXB will use 
     * reflection to null out those references that are 
     * annotated as XmlTransient.  In this case parentResource.
     * 
     * parentResource is used in lineage type returned
     * 
     * @param destination
     * @param source
     */
    private void copyResource(Resource destination, Resource source) {
        destination.setAgent(source.getAgent());
        destination.setAlertDefinitions(source.getAlertDefinitions());
        destination.setDescription(source.getDescription());
        destination.setContentServiceRequests(source.getContentServiceRequests());
        destination.setCreateChildResourceRequests(source.getCreateChildResourceRequests());
        //        destination.get
        //TODO: spinder 9/4/08. Not sure if remainder of properties not set here are needed? can get rest as id provided
        // and separate getResource call can be made.
        destination.setParentResource(source.getParentResource());
        destination.setId(source.getId());
        destination.setName(source.getName());
        destination.setResourceKey(source.getResourceKey());
        destination.setResourceType(source.getResourceType());
        destination.setVersion(source.getVersion());
    }
    //    //METHODS :END
}