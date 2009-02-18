/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.jboss.on.plugins.tomcat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.on.plugins.tomcat.helper.CreateResourceHelper;
import org.jboss.on.plugins.tomcat.helper.FileContentDelegate;
import org.jboss.on.plugins.tomcat.helper.TomcatApplicationDeployer;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ApplicationServerComponent;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Handle generic information about a virtual host in tomcat
 * 
 * @author Jay Shaughnessy
 * @author Heiko W. Rupp
 *
 */
public class TomcatVHostComponent extends MBeanResourceComponent<TomcatServerComponent> implements ApplicationServerComponent, CreateChildResourceFacet {

    protected static final String PROP_APP_BASE = "appBase";

    protected static final String CONTENT_PROP_EXPLODE_ON_DEPLOY = "explodeOnDeploy";

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) {
        TomcatServerComponent parentComponent = getResourceContext().getParentResourceComponent();
        parentComponent.getEmsConnection(); // first make sure the connection is loaded

        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();

            String attributeName = name.substring(name.lastIndexOf(':') + 1);

            try {
                EmsAttribute attribute = getEmsBean().getAttribute(attributeName);

                Object valueObject = attribute.refresh();

                if (attributeName.equals("aliases")) {
                    String[] vals = (String[]) valueObject;
                    MeasurementDataTrait mdt = new MeasurementDataTrait(request, Arrays.toString(vals));
                    report.addData(mdt);
                }
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    public File getInstallationPath() {
        return getResourceContext().getParentResourceComponent().getInstallationPath();
    }

    public File getConfigurationPath() {
        String appBase = getResourceContext().getPluginConfiguration().getSimpleValue(PROP_APP_BASE, "webapps");

        return new File(getInstallationPath(), appBase);
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        String resourceTypeName = report.getResourceType().getName();
        try {
            if (TomcatWarComponent.RESOURCE_TYPE_NAME.equals(resourceTypeName)) {
                warCreate(report);
            } else {
                throw new UnsupportedOperationException("Unsupported Resource type: " + resourceTypeName);
            }
        } catch (Exception e) {
            CreateResourceHelper.setErrorOnReport(report, e);
        }
        return report;
    }

    private void warCreate(CreateResourceReport report) throws Exception {
        ResourcePackageDetails details = report.getPackageDetails();
        PackageDetailsKey key = details.getKey();
        String archiveName = key.getName();

        if (!archiveName.toLowerCase().endsWith(".war")) {
            CreateResourceHelper.setErrorOnReport(report, "Deployed file must have a .war extension");
            return;
        }

        Configuration deployTimeConfiguration = details.getDeploymentTimeConfiguration();
        PropertySimple explodeOnDeployProp = deployTimeConfiguration.getSimple(CONTENT_PROP_EXPLODE_ON_DEPLOY);

        if (explodeOnDeployProp == null || explodeOnDeployProp.getBooleanValue() == null) {
            CreateResourceHelper.setErrorOnReport(report, "Explode On Deploy property is required.");
            return;
        }
        boolean explodeOnDeploy = explodeOnDeployProp.getBooleanValue();

        // Perform the deployment        
        File deployDir = getConfigurationPath();
        FileContentDelegate fileContent = new FileContentDelegate(deployDir, details.getPackageTypeName());

        if (explodeOnDeploy) {
            // trim off the .war suffix because we want to deploy into a root directory named after the app name
            archiveName = archiveName.substring(0, archiveName.length() - 4);
        }

        File path = new File(deployDir, archiveName);
        if (path.exists()) {
            CreateResourceHelper.setErrorOnReport(report, "A web application named " + path.getName() + " is already deployed with path " + path + ".");
            return;
        }

        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), "tomcat-war.bin");
        OutputStream osForTempDir = new BufferedOutputStream(new FileOutputStream(tempFile));
        ContentContext contentContext = getResourceContext().getContentContext();

        ContentServices contentServices = contentContext.getContentServices();
        contentServices.downloadPackageBitsForChildResource(contentContext, TomcatWarComponent.RESOURCE_TYPE_NAME, key, osForTempDir);

        osForTempDir.close();

        // check for content
        boolean valid = isWebApplication(tempFile);
        if (!valid) {
            CreateResourceHelper.setErrorOnReport(report, "Expected a " + TomcatWarComponent.RESOURCE_TYPE_NAME + " file, but its format/content did not match");
            return;
        }

        InputStream isForTempDir = new BufferedInputStream(new FileInputStream(tempFile));
        fileContent.createContent(path, isForTempDir, explodeOnDeploy);

        // Resource key should match the following:        
        // Catalina:j2eeType=WebModule,name=//localhost/<archiveName>,J2EEApplication=none,J2EEServer=none        

        String resourceKey = "Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=//localhost/" + archiveName;

        report.setResourceName(archiveName);
        report.setResourceKey(resourceKey);
        report.setStatus(CreateResourceStatus.SUCCESS);
    }

    private boolean isWebApplication(File file) {
        JarFile jfile = null;
        try {
            jfile = new JarFile(file);
            JarEntry entry = jfile.getJarEntry("WEB-INF/web.xml");

            return (null != entry);
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        } finally {
            if (jfile != null)
                try {
                    jfile.close();
                } catch (IOException e) {
                    log.info("Exception when trying to close the war file: " + e.getMessage());
                }
        }
    }

    public TomcatApplicationDeployer getDeployer() {
        TomcatApplicationDeployer deployer = null;
        EmsConnection connection = null;

        try {
            connection = getEmsConnection();
            if (null != connection) {
                deployer = new TomcatApplicationDeployer(connection);
            }
        } catch (Throwable e) {
            log.error("Unable to access MainDeployer MBean required for creation and deletion of managed resources - this should never happen. Cause: " + e);
        }

        return deployer;
    }

    void undeployWar(String contextRoot) throws TomcatApplicationDeployer.DeployerException {
        // As it stands Tomcat will respond to the placement or removal of the physical Web App itself. We
        // call removeServiced prior to the file delete to let TC know to stop servicing the app, hopefully
        // for a cleaner removal.
        // There is no additional MBean interaction required, the deploy is done in a file-based way.

        TomcatApplicationDeployer deployer = getDeployer();
        if (null == deployer) {
            throw new IllegalStateException("Unable to undeploy " + contextRoot + ", because MainDeployer MBean could " + "not be accessed - this should never happen.");
        }

        deployer.undeploy(contextRoot);
    }

}
