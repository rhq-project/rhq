package org.rhq.enterprise.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Class that deals with getting data about resources
 * @author Heiko W. Rupp
 */
@Produces({"application/json","application/xml","text/plain"})
@Path("/resource")
public class ResourceHandler {


    @GET
    @Path("/r/{id}")
    public Resource getResource(@PathParam("id") int id) {

        ResourceManagerLocal resMgr = LookupUtil.getResourceManager();

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Resource res = resMgr.getResource(subject,id);

        return res;
    }

    @GET
    @Path("/p")
    public List<Resource> getPlatforms() {
        ResourceManagerLocal resMgr = LookupUtil.getResourceManager();

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO


        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(subject, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        return ret;
    }

    @GET
    @Path("/p/{id}/s")
    public List<Resource> getServersForPlatform(@PathParam("id") int id) {
        ResourceManagerLocal resMgr = LookupUtil.getResourceManager();

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        PageControl pc = new PageControl();
        Resource parent = resMgr.getResource(subject,id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(subject,parent,InventoryStatus.COMMITTED,pc);

        return ret;
    }

    @GET
    @Path("/a/{id}")
    public Availability getAvailability(@PathParam("id")int resourceId) {
        AvailabilityManagerLocal availMgr = LookupUtil.getAvailabilityManager();

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Availability avail = availMgr.getCurrentAvailabilityForResource(subject, resourceId);
        return avail;
    }
}
