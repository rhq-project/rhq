package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@Stateless
public class ContentMetadataManagerBean implements ContentMetadataManagerLocal {

    @EJB
    private BundleManagerLocal bundleMgr;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void deleteMetadata(Subject subject, ResourceType resourceType) {
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
