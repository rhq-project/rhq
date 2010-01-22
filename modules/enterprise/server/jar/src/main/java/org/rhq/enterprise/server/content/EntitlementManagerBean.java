package org.rhq.enterprise.server.content;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
public class EntitlementManagerBean implements EntitlementManagerLocal, EntitlementManagerRemote {

    private final Log log = LogFactory.getLog(EntitlementManagerBean.class);

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

class DummyLoader {

    static final String dir = "/etc/pki/rhq";

    EntitlementCertificate load(int resourceId) throws IOException {
        String name = String.valueOf(resourceId);
        String key = read(name + ".key");
        String pem = read(name + ".pem");
        return new EntitlementCertificate(name, key, pem);
    }

    private String read(String fn) throws IOException {
        String path = dir + File.separator + fn;
        File f = new File(path);
        byte[] buffer = new byte[(int) f.length()];
        BufferedInputStream istr = new BufferedInputStream(new FileInputStream(f));
        istr.read(buffer);
        String result = new String(buffer);
        istr.close();
        return result;
    }
}
