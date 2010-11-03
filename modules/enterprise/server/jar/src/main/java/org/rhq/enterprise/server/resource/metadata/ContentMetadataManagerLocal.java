package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceType;

import javax.ejb.Local;

@Local
public interface ContentMetadataManagerLocal {

    void deleteMetadata(Subject subject, ResourceType resourceType);

}
