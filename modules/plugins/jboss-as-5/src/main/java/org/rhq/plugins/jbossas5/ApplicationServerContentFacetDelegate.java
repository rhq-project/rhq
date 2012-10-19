package org.rhq.plugins.jbossas5;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.common.jbossas.AbstractJBossASContentFacetDelegate;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.plugins.jbossas5.util.FileContentDelegate;
import org.rhq.plugins.jbossas5.util.JarContentDelegate;

public class ApplicationServerContentFacetDelegate extends AbstractJBossASContentFacetDelegate {

    private final Log log = LogFactory.getLog(this.getClass());

    private final Map<PackageType, FileContentDelegate> contentDelegates = new HashMap<PackageType, FileContentDelegate>();

    private File configurationPath;

    private ContentContext contentContext;

    private void setConfigurationPath(File configurationPath) {
        this.configurationPath = configurationPath;
    }

    private File getConfigurationPath() {
        return this.configurationPath;
    }

    private void setContentContext(ContentContext contentContext) {
        this.contentContext = contentContext;
    }

    private ContentContext getContentContext() {
        return this.contentContext;
    }

    protected ApplicationServerContentFacetDelegate(JBPMWorkflowManager workflowManager, File configurationPath,
        ContentContext contentContext) {
        super(workflowManager);
        this.setConfigurationPath(configurationPath);
        this.setContentContext(contentContext);
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

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        ContentResponseResult overallResult = ContentResponseResult.SUCCESS;
        List<DeployIndividualPackageResponse> individualResponses = new ArrayList<DeployIndividualPackageResponse>(
            packages.size());

        for (ResourcePackageDetails pkg : packages) {
            log.info("Attempting to deploy package: " + pkg);

            String packageTypeName = pkg.getPackageTypeName();
            if (packageTypeName.equals(PACKAGE_TYPE_PATCH)) {

                if (packages.size() > 1) {
                    log.warn("Attempt to install more than one patch at a time, installation aborted.");

                    DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
                    response
                        .setOverallRequestErrorMessage("When deploying a patch, no other packages may be deployed at the same time.");
                    return response;
                }

                try {
                    DeployIndividualPackageResponse response = getWorkflowManager().run(pkg);

                    if (response.getResult() == ContentResponseResult.FAILURE) {
                        overallResult = ContentResponseResult.FAILURE;
                    }

                    // just in case response is null, it would throw NPE on the getResult() check above but the item
                    // would already be a member in individualResponses; moving the add below the check ensures that
                    // only non-null instances of individualResponses will ever make it into the List
                    individualResponses.add(response);
                } catch (Throwable throwable) {
                    log.error("Error deploying package: " + pkg, throwable);

                    // Don't forget to provide an individual response for the failed package.
                    DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(pkg.getKey(),
                        ContentResponseResult.FAILURE);
                    response.setErrorMessageFromThrowable(throwable);
                    individualResponses.add(response);

                    overallResult = ContentResponseResult.FAILURE;
                }
            } else if (packageTypeName.equals(PACKAGE_TYPE_LIBRARY)) {
                if (packages.size() > 1) {
                    log.warn("Attempt to install more than one patch at a time, installation aborted.");

                    DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
                    response
                        .setOverallRequestErrorMessage("When deploying a patch, no other packages may be deployed at the same time.");
                    return response;
                } else {
                    deployJarLibrary(pkg, contentServices);
                }
            }
        }

        DeployPackagesResponse response = new DeployPackagesResponse(overallResult);
        response.getPackageResponses().addAll(individualResponses);

        return response;
    }

    public DeployPackagesResponse deployJarLibrary(ResourcePackageDetails packageDetails,
        ContentServices contentServices) {
        ContentResponseResult overallResult = ContentResponseResult.SUCCESS;
        List<DeployIndividualPackageResponse> individualResponses = new ArrayList<DeployIndividualPackageResponse>(1);
        String deployDir = this.getConfigurationPath() + File.separator + "lib";
        String destinationFileLocation = deployDir + File.separator + packageDetails.getKey().getName() + ".jar";
        //get the name of the uploaded file.

        try {
            downloadBits(packageDetails.getKey(), destinationFileLocation);
        } catch (Throwable throwable) {
            log.error("Error deploying package: " + packageDetails, throwable);
            DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(packageDetails.getKey(),
                ContentResponseResult.FAILURE);
            response.setErrorMessageFromThrowable(throwable);
            individualResponses.add(response);
            overallResult = ContentResponseResult.FAILURE;
        }

        DeployPackagesResponse response = new DeployPackagesResponse(overallResult);
        response.getPackageResponses().addAll(individualResponses);
        return response;
    }

    public void downloadBits(PackageDetailsKey key, String destinationFileLocation) throws IOException,
        EmptyFileException {
        ContentServices contentServices = this.getContentContext().getContentServices();

        // Open a stream to where the downloaded file should go
        FileOutputStream output = new FileOutputStream(destinationFileLocation);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output, 4096);

        // Request the bits from the server
        try {
            contentServices.downloadPackageBits(contentContext, key, bufferedOutput, true);
            bufferedOutput.close();

            // Verify the file was created correctly
            File downloadedFile = new File(destinationFileLocation);
            if (!downloadedFile.exists()) {
                throw new FileNotFoundException("File to download [" + destinationFileLocation + "] does not exist");
            }

            if (downloadedFile.length() == 0) {
                throw new EmptyFileException("Downloaded file [" + destinationFileLocation + "] is empty");
            }
        } finally {
            // Close the stream if there was an error thrown from downloadPackageBits
            try {
                bufferedOutput.close();
            } catch (IOException e1) {
                log.error("Error closing output stream to [" + destinationFileLocation + "] after exception", e1);
            }
        }
    }

}
