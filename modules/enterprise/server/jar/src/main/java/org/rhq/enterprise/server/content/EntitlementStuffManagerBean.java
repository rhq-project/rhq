package org.rhq.enterprise.server.content;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.transfer.EntitlementCertificate;
import org.rhq.enterprise.server.authz.RequiredPermission;

@Stateless
public class EntitlementStuffManagerBean implements EntitlementStuffManagerLocal, EntitlementStuffManagerRemote {

    private final Log log = LogFactory.getLog(EntitlementStuffManagerBean.class);

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<EntitlementCertificate> getCertificates(Subject subject, int resourceId) {
        List<EntitlementCertificate> list = new ArrayList<EntitlementCertificate>();
        DummyLoader loader = new DummyLoader();
        try {
            list.add(loader.load(resourceId));

        } catch (Exception ex) {
            log.warn("x.509 cert for: " //
                + resourceId //
                + " not-found in: " //
                + DummyLoader.dir);
        }
        return list;
    }

}

