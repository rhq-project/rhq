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
        if (resourceId % 2 == 0) {
            list.add(getEven());
        } else {
            list.add(getOdd());
        }
        return list;
    }

    /**
     * Get certificate for even numbered resource ids.
     * @return The cert.
     * @note This is a prototype hack.
     */
    private EntitlementCertificate getOdd() {
        DummyLoader loader = new DummyLoader();
        try {
            return loader.getOdd();
        } catch (Exception ex) {
            log.error("Certificate (ODD), not-found", ex);
        }
        return null;
    }

    /**
     * Get certificate for odd numbered resource ids.
     * @return The cert.
     * @note This is a prototype hack.
     */
    private EntitlementCertificate getEven() {
        DummyLoader loader = new DummyLoader();
        try {
            return loader.getEven();
        } catch (Exception ex) {
            log.error("Certificate (EVEN), not-found", ex);
        }
        return null;
    }
}

class DummyLoader {

    private static final String dir = "/etc/pki/yum/rhq";

    EntitlementCertificate getOdd() throws IOException {
        String key = read("odd.key");
        String pem = read("odd.pem");
        return new EntitlementCertificate(key, pem);
    }

    EntitlementCertificate getEven() throws IOException {
        String key = read("even.key");
        String pem = read("even.pem");
        return new EntitlementCertificate(key, pem);
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
