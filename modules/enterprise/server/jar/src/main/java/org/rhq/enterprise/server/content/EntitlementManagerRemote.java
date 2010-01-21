package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.transfer.EntitlementCertificate;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface EntitlementManagerRemote {
    /**
     * Get a list of entitlement certificates for the specified resources.
     * @param subject    The logged in user's subject.
     * @param resourceId The resource id.
     * @return A list of certificates.
     */
    @WebMethod
    List<EntitlementCertificate> getCertificates( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);
}
