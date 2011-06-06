package org.rhq.enterprise.server.rest;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

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
@Stateless
public class ResourceHandlerBean implements ResourceHandlerLocal {

    @EJB
    ResourceManagerLocal resMgr;
    @EJB
    AvailabilityManagerLocal availMgr;

    @Override
    public Resource getResource( int id) {


        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Resource res = resMgr.getResource(subject, id);
        // TODO we need to figure out what to do with lazy load fields, as marshalling into application/{xml,json} fails otherwise
        return res;
    }

    @Override
    public List<Resource> getPlatforms() {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO


        PageControl pc = new PageControl();
        List<Resource> ret = resMgr.findResourcesByCategory(subject, ResourceCategory.PLATFORM, InventoryStatus.COMMITTED, pc) ;
        return ret;
    }

    @Override
    public List<Resource> getServersForPlatform(int id) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        PageControl pc = new PageControl();
        Resource parent = resMgr.getResource(subject,id);
        List<Resource> ret = resMgr.findResourceByParentAndInventoryStatus(subject,parent,InventoryStatus.COMMITTED,pc);

        return ret;
    }

    @Override
    public Availability getAvailability(int resourceId) {

        Subject subject = LookupUtil.getSubjectManager().getOverlord(); // TODO

        Availability avail = availMgr.getCurrentAvailabilityForResource(subject, resourceId);
        return avail;
    }
}
