/**
 * 
 */
package org.rhq.plugins.jbossas.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.on.common.jbossas.AbstractJBossASContentFacetDelegate;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;

import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;

/**
 * Content facet delegate for JBoss AS 4.
 * 
 * @author Lukas Krejci
 */
public class JBossASContentFacetDelegate extends AbstractJBossASContentFacetDelegate {

    private Map<PackageType, FileContentDelegate> contentDelegates = new HashMap<PackageType, FileContentDelegate>();

    private File configurationPath;

    public JBossASContentFacetDelegate(JBPMWorkflowManager workflowManager, File configurationPath) {
        super(workflowManager);
        this.configurationPath = configurationPath;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        FileContentDelegate contentDelegate = getContentDelegate(type);

        Set<ResourcePackageDetails> details = null;
        if (contentDelegate != null) {
            details = contentDelegate.discoverDeployedPackages();
        }

        return details;
    }

    private FileContentDelegate getContentDelegate(PackageType type) {
        FileContentDelegate contentDelegate = contentDelegates.get(type);
        if (contentDelegate == null) {
            if (type.getName().equals("library")) {
                File deployLib = new File(configurationPath, "lib");
                contentDelegate = new JarContentDelegate(deployLib, type.getName());
            }

            contentDelegates.put(type, contentDelegate);
        }

        return contentDelegate;
    }
}
