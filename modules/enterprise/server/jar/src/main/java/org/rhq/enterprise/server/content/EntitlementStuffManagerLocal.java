package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.transfer.EntitlementCertificate;

/**
 *
 * @author jortel
 *
 */
@Local
public interface EntitlementStuffManagerLocal {

    /**
     * Get a list of entitlement certificates for the specified resources.
     * @param subject    The logged in user's subject.
     * @param resourceId The resource id.
     * @return A list of certificates.
     */
    List<EntitlementCertificate> getCertificates(Subject subject, int resourceId);
}
