package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;

@Stateless
public class ContentMetadataManagerBean implements ContentMetadataManagerLocal {

    private static final Log log = LogFactory.getLog(ContentMetadataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityMgr;

    @EJB
    private ConfigurationMetadataManagerLocal configurationMetadataMgr;

    @EJB
    private BundleManagerLocal bundleMgr;

    @Override
    public void updateMetadata(ResourceType existingType, ResourceType newType) {
        log.debug("Updating bundle type and package types for " + existingType);

        existingType = entityMgr.find(ResourceType.class, existingType.getId());

        // set the bundle type if one is defined
        BundleType newBundleType = newType.getBundleType();
        if (newBundleType != null) {
            BundleType existingBundleType = existingType.getBundleType();
            newBundleType.setResourceType(existingType);
            if (existingBundleType != null) {
                newBundleType.setId(existingBundleType.getId());
                newBundleType = entityMgr.merge(newBundleType);
            }
            if (log.isDebugEnabled()) {
                log.debug("Updating bundle type to " + newBundleType);
            }
            existingType.setBundleType(newBundleType);

            // If bundleType is not null then this in a bundle plugin and we do not need to do any further
            // processing because a bundle plugin cannot define any other content.
            return;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Removing bundle type");
            }
            existingType.setBundleType(null);
        }

        // Easy case: If there are no package definitions in the new type, null out any in the existing and return
        if (newType.getPackageTypes().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Removing all package types for " + existingType);
            }
            for (PackageType packageType : existingType.getPackageTypes()) {
                entityMgr.remove(packageType);
            }
            existingType.getPackageTypes().clear();
            return;
        }

        // The new type has package definitions

        // Easy case: If the existing type did not have any package definitions, simply use the new type defs and return
        if (existingType.getPackageTypes().isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(existingType + " previously did not define any package types. Adding " +
                        newType.getPackageTypes());
            }
            for (PackageType newPackageType : newType.getPackageTypes()) {
                newPackageType.setResourceType(existingType);
                entityMgr.persist(newPackageType);
            }

            existingType.setPackageTypes(newType.getPackageTypes());
            return;
        }

        // Both the new and existing types have definitions, so merge
        Set<PackageType> existingPackageTypes = existingType.getPackageTypes();
        Map<String, PackageType> newPackageTypeDefinitions = new HashMap<String, PackageType>(newType.getPackageTypes()
            .size());
        for (PackageType newPackageType : newType.getPackageTypes()) {
            newPackageTypeDefinitions.put(newPackageType.getName(), newPackageType);
        }

        // Remove all definitions that are in the existing type but not in the new type
        List<PackageType> removedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        removedPackageTypes.removeAll(newType.getPackageTypes());
        for (PackageType removedPackageType : removedPackageTypes) {
            existingType.removePackageType(removedPackageType);
            entityMgr.remove(removedPackageType);
        }

        // Merge definitions that were already in the existing type and again in the new type
        List<PackageType> mergedPackageTypes = new ArrayList<PackageType>(existingType.getPackageTypes());
        mergedPackageTypes.retainAll(newType.getPackageTypes());

        if (log.isDebugEnabled()) {
            log.debug("Updating package types: " + mergedPackageTypes);
        }

        for (PackageType mergedPackageType : mergedPackageTypes) {
            updatePackageConfigurations(mergedPackageType,
                    newPackageTypeDefinitions.get(mergedPackageType.getName()));
            mergedPackageType.update(newPackageTypeDefinitions.get(mergedPackageType.getName()));
            entityMgr.merge(mergedPackageType);
        }

        // Persist all new definitions
        List<PackageType> newPackageTypes = new ArrayList<PackageType>(newType.getPackageTypes());
        newPackageTypes.removeAll(existingType.getPackageTypes());

        if (log.isDebugEnabled()) {
            log.debug("Adding package types: " + newPackageTypes);
        }

        for (PackageType newPackageType : newPackageTypes) {
            newPackageType.setResourceType(existingType);
            entityMgr.persist(newPackageType);
            existingPackageTypes.add(newPackageType);
        }
    }

    void updatePackageConfigurations(PackageType existingType, PackageType newType) {
        ConfigurationDefinition newConfigurationDefinition = newType.getDeploymentConfigurationDefinition();
        if (newConfigurationDefinition != null) {
            if (existingType.getDeploymentConfigurationDefinition() == null) {
                // everything new
                entityMgr.persist(newConfigurationDefinition);
                existingType.setDeploymentConfigurationDefinition(newConfigurationDefinition);
            } else {
                // update existing
                ConfigurationDefinition existingDefinition = existingType.getDeploymentConfigurationDefinition();
                configurationMetadataMgr.updateConfigurationDefinition(newConfigurationDefinition,
                    existingDefinition);
            }
        } else {
            // newDefinition == null
            if (existingType.getDeploymentConfigurationDefinition() != null) {
                existingType.setDeploymentConfigurationDefinition(null);
            }
        }

        // The only place in the code base where I see that the PackageType.packageExtraPropertiesDefinition gets set
        // is here in this method. The code in ContentMetadataParser that creates the PackageType objects from a
        // plugin descriptor never references the property. Can the packageExtraPropertiesDefinition property be
        // altogether removed from the code base?
        //
        // jsanda - 11/3/2010
//        newConfigurationDefinition = newType.getPackageExtraPropertiesDefinition();
//        if (newConfigurationDefinition != null) {
//            if (existingType.getPackageExtraPropertiesDefinition() == null) {
//                // everything new
//                entityMgr.persist(newConfigurationDefinition);
//                existingType.setPackageExtraPropertiesDefinition(newConfigurationDefinition);
//            } else {
//                // update existing
//                ConfigurationDefinition existingDefinition = existingType.getPackageExtraPropertiesDefinition();
//                configurationMetadataMgr.updateConfigurationDefinition(newConfigurationDefinition,
//                    existingDefinition);
//            }
//        } else {
//            // newDefinition == null
//            if (existingType.getPackageExtraPropertiesDefinition() != null) {
//                existingType.setPackageExtraPropertiesDefinition(null);
//            }
//        }
    }

    @Override
    public void deleteMetadata(Subject subject, ResourceType resourceType) {
        log.debug("Deleting bundle type and bundles for " + resourceType);

        // Currently PackageType deletion is handled via a cascading delete when the
        // owning ResourceType is deleted.
        try {
            deleteBundles(subject, resourceType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete bundles for " + resourceType, e);
        }
    }

    private void deleteBundles(Subject subject, ResourceType resourceType) throws Exception {
        BundleType bundleType = resourceType.getBundleType();

        if (bundleType == null) {
            return;
        }

        BundleCriteria criteria = new BundleCriteria();
        criteria.addFilterBundleTypeId(bundleType.getId());

        List<Bundle> bundles = bundleMgr.findBundlesByCriteria(subject, criteria);
        for (Bundle bundle : bundles) {
            bundleMgr.deleteBundle(subject, bundle.getId());
        }
    }

}
