package org.rhq.enterprise.server.resource.metadata;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceType;

@Local
public interface ContentMetadataManagerLocal {

    /**
     * Updates the database with new package definitions found in the new resource type. Any definitions not found in
     * the new type but were previously in the existing resource type will be removed. Any definitions common to both
     * will be merged.
     *
     * @param newType      new resource type containing updated package definitions
     * @param existingType old resource type with existing package definitions
     */
    void updateMetadata(ResourceType existingType, ResourceType newType);

    void deleteMetadata(Subject subject, ResourceType resourceType);

}
