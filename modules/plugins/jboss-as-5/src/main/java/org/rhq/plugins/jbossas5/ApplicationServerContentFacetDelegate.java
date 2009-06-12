package org.rhq.plugins.jbossas5;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.on.common.jbossas.AbstractJBossASContentFacetDelegate;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.plugins.jbossas5.util.FileContentDelegate;
import org.rhq.plugins.jbossas5.util.JarContentDelegate;

public class ApplicationServerContentFacetDelegate extends AbstractJBossASContentFacetDelegate {

    private final Map<PackageType, FileContentDelegate> contentDelegates = new HashMap<PackageType, FileContentDelegate>();

    private File configurationPath;

    private void setConfigurationPath(File configurationPath) {
        this.configurationPath = configurationPath;
    }

    private File getConfigurationPath() {
        return this.configurationPath;
    }

    protected ApplicationServerContentFacetDelegate(JBPMWorkflowManager workflowManager, File configurationPath) {
        super(workflowManager);
        this.setConfigurationPath(configurationPath);
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
                File deployLib = new File(this.getConfigurationPath(), "lib");
                contentDelegate = new JarContentDelegate(deployLib, type.getName());
            }

            contentDelegates.put(type, contentDelegate);
        }

        return contentDelegate;
    }

}
