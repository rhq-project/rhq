package org.rhq.enterprise.server.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.rest.ResourceWithType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Class that deals with getting data about resources
 * @author Heiko W. Rupp
 */
@Stateless
public class ResourceHandlerBean implements ResourceHandlerLocal {

    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    AvailabilityManagerLocal availMgr;

    @Override
    public ResourceWithType getResource(int id) {


        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Resource res = resMgr.getResource(subject, id);

        ResourceWithType rwt = fillRWT(res);

        return rwt;
    }


    @Override
    public List<ResourceWithType> getPlatforms() {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO


        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(subject, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }
        return rwtList;
    }

    @Override
    public List<ResourceWithType> getServersForPlatform(int id) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        PageControl pc = new PageControl();
        Resource parent = resMgr.getResource(subject,id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(subject,parent,InventoryStatus.COMMITTED,pc);
        List<ResourceWithType> rwtList = new ArrayList<ResourceWithType>(ret.size());
        for (Resource r: ret) {
            ResourceWithType rwt = fillRWT(r);
            rwtList.add(rwt);
        }

        return rwtList;
    }

    @Override
    public Availability getAvailability(int resourceId) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Availability avail = availMgr.getCurrentAvailabilityForResource(subject, resourceId);
        return avail;
    }

    private ResourceWithType fillRWT(Resource res) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(),res.getId(), resourceType.getName(),
                resourceType.getId(), resourceType.getPlugin());
        Resource parent = res.getParentResource();
        if (parent!=null) {
            rwt.setParentId(parent.getId());
            rwt.setParentName(parent.getName());
        }
        return rwt;
    }

}
