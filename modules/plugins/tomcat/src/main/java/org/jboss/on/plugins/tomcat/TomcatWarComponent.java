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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.plugins.tomcat.helper.TomcatApplicationDeployer;
import org.jetbrains.annotations.Nullable;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.core.clientapi.server.plugin.content.util.JarContentFileInfo;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.version.PackageVersions;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * A resource component for managing a web application (WAR) deployed to a Tomcat server.
 *
 * @author Jay Shaughnessy
 * @author Fady Matar
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class TomcatWarComponent extends MBeanResourceComponent<TomcatVHostComponent> implements ContentFacet, DeleteResourceFacet {

    private static final String METRIC_PREFIX_APPLICATION = "Application.";
    private static final String METRIC_PREFIX_SERVLET = "Servlet.";
    private static final String METRIC_PREFIX_SESSION = "Session.";
    private static final String METRIC_PREFIX_VHOST = "VHost.";

    private static final String METRIC_RESPONSE_TIME = "ResponseTime";

    private static final String METRIC_MAX_SERVLET_TIME = METRIC_PREFIX_SERVLET + "MaxResponseTime";
    private static final String METRIC_MIN_SERVLET_TIME = METRIC_PREFIX_SERVLET + "MinResponseTime";
    private static final String METRIC_AVG_SERVLET_TIME = METRIC_PREFIX_SERVLET + "AvgResponseTime";
    private static final String METRIC_NUM_SERVLET_REQUESTS = METRIC_PREFIX_SERVLET + "NumRequests";
    private static final String METRIC_NUM_SERVLET_ERRORS = METRIC_PREFIX_SERVLET + "NumErrors";
    private static final String METRIC_TOTAL_TIME = METRIC_PREFIX_SERVLET + "TotalTime";

    private static final String TRAIT_EXPLODED = METRIC_PREFIX_APPLICATION + "exploded";
    private static final String TRAIT_VHOST_NAMES = METRIC_PREFIX_VHOST + "name";

    // Uppercase variables are filled in prior to searching, lowercase are matched by the returned beans
    private static final String QUERY_TEMPLATE_SERVLET = "Catalina:j2eeType=Servlet,J2EEApplication=none,J2EEServer=none,WebModule=%WEBMODULE%,name=%name%";
    private static final String QUERY_TEMPLATE_SESSION = "Catalina:type=Manager,host=%HOST%,path=%PATH%";
    private static final String QUERY_TEMPLATE_HOST = "Catalina:type=Manager,path=%PATH%,host=%host%";

    protected static final String PROPERTY_NAME = "name";
    protected static final String PROPERTY_CONTEXT_ROOT = "contextRoot";
    protected static final String PROPERTY_FILENAME = "filename";
    protected static final String PROPERTY_RESPONSE_TIME_LOG_FILE = ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP;
    protected static final String PROPERTY_RESPONSE_TIME_URL_EXCLUDES = ResponseTimeConfiguration.RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP;
    protected static final String PROPERTY_RESPONSE_TIME_URL_TRANSFORMS = ResponseTimeConfiguration.RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP;
    protected static final String PROPERTY_VHOST = "vHost";

    protected static final String RESOURCE_TYPE_NAME = "Tomcat Web Application (WAR)";

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Name of the backing package type that will be used when discovering packages. This corresponds to the name
     * of the package type defined in the plugin descriptor. For simplicity, the package type for both EARs and
     * WARs is simply called "file". This is still unique within the context of the parent resource type and lets
     * this class use the same package type name in both cases.
     */
    private static final String PKG_TYPE_FILE = "file";

    /**
     * Architecture string used in describing discovered packages.
     */
    private static final String ARCHITECTURE = "noarch";
    private PackageVersions versions;

    private EmsBean webModuleMBean;
    private ResponseTimeLogParser logParser;

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType availability;

        if (null == this.webModuleMBean) {
            this.webModuleMBean = getWebModuleMBean();
        }

        if (null != this.webModuleMBean) {
            int state;

            try {
                // check to see if the mbean is truly active
                state = (Integer) this.webModuleMBean.getAttribute("state").refresh();
            } catch (Exception e) {
                // if not active an exception may be thrown
                state = WarMBeanState.STOPPED;
            }

            availability = (WarMBeanState.STARTED == state) ? AvailabilityType.UP : AvailabilityType.DOWN;

            if (AvailabilityType.DOWN == availability) {
                // if availability is down then ensure we use a new mbean on the next try, in case we have
                // a totally new EMS connection. This is creating a limitation on the stop operation.
                this.webModuleMBean = null;
            }
        } else {
            availability = AvailabilityType.DOWN;
        }

        return availability;
    }

    @Override
    public void start(ResourceContext<TomcatVHostComponent> resourceContext) {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.webModuleMBean = getWebModuleMBean();

        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile != null) {
            this.logParser = new ResponseTimeLogParser(logFile);
            this.logParser.setExcludes(responseTimeConfig.getExcludes());
            this.logParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {

        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            try {
                if (metricName.equals(METRIC_RESPONSE_TIME)) {
                    if (this.logParser != null) {
                        try {
                            CallTimeData callTimeData = new CallTimeData(schedule);
                            this.logParser.parseLog(callTimeData);
                            report.addData(callTimeData);
                        } catch (Exception e) {
                            log.error("Failed to retrieve HTTP call-time data.", e);
                        }
                    } else {
                        log.error("The '" + METRIC_RESPONSE_TIME + "' metric is enabled for WAR resource '" + getApplicationName()
                            + "', but no value is defined for the '" + PROPERTY_RESPONSE_TIME_LOG_FILE + "' connection property.");
                        // TODO: Communicate this error back to the server for display in the GUI.
                    }
                } else if (metricName.startsWith(METRIC_PREFIX_SERVLET)) {
                    Double value = getServletMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                    report.addData(metric);
                } else if (metricName.startsWith(METRIC_PREFIX_SESSION)) {
                    Double value = getSessionMetric(metricName);
                    MeasurementDataNumeric metric = new MeasurementDataNumeric(schedule, value);
                    report.addData(metric);
                } else if (metricName.startsWith(METRIC_PREFIX_VHOST)) {
                    if (metricName.equals(TRAIT_VHOST_NAMES)) {
                        List<EmsBean> beans = getVHosts();
                        String value = "";
                        Iterator<EmsBean> iter = beans.iterator();
                        while (iter.hasNext()) {
                            EmsBean eBean = iter.next();
                            value += eBean.getBeanName().getKeyProperty("host");
                            if (iter.hasNext())
                                value += ",";
                        }
                        MeasurementDataTrait trait = new MeasurementDataTrait(schedule, value);
                        report.addData(trait);
                    }
                } else if (metricName.startsWith(METRIC_PREFIX_APPLICATION)) {
                    if (metricName.equals(TRAIT_EXPLODED)) {
                        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
                        String filename = pluginConfig.getSimpleValue(PROPERTY_FILENAME, null);
                        boolean exploded = new File(filename).isDirectory();
                        MeasurementDataTrait trait = new MeasurementDataTrait(schedule, (exploded) ? "yes" : "no");
                        report.addData(trait);
                    }
                } else {
                    log.warn("Unexpected Tomcat WAR metric schedule: " + metricName);
                }
            } catch (Exception e) {
                log.debug("Failed to gather Tomcat WAR metric: " + metricName + ", " + e);
            }
        }
    }

    private Double getSessionMetric(String metricName) {
        EmsConnection jmxConnection = getEmsConnection();
        String servletMBeanNames = QUERY_TEMPLATE_SESSION;
        Configuration config = getResourceContext().getPluginConfiguration();
        servletMBeanNames = servletMBeanNames.replace("%PATH%", config.getSimpleValue(PROPERTY_CONTEXT_ROOT, ""));
        servletMBeanNames = servletMBeanNames.replace("%HOST%", config.getSimpleValue(PROPERTY_VHOST, ""));
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());
        String property = metricName.substring(METRIC_PREFIX_SESSION.length());
        Double ret = Double.NaN;

        if (mBeans.size() > 0) { // TODO flag error if != 1 ?
            EmsBean eBean = mBeans.get(0);
            eBean.refreshAttributes();
            EmsAttribute att = eBean.getAttribute(property);
            if (att != null) {
                Integer i = (Integer) att.getValue();
                ret = Double.valueOf(i);
            }

        }
        return ret;
    }

    private Double getServletMetric(String metricName) {

        EmsConnection jmxConnection = getEmsConnection();

        String servletMBeanNames = QUERY_TEMPLATE_SERVLET;
        Configuration config = getResourceContext().getPluginConfiguration();
        servletMBeanNames = servletMBeanNames.replace("%WEBMODULE%", config.getSimpleValue(PROPERTY_NAME, ""));
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(servletMBeanNames);
        List<EmsBean> mBeans = jmxConnection.queryBeans(queryUtility.getTranslatedQuery());

        long min = Long.MAX_VALUE;
        long max = 0;
        long processingTime = 0;
        int requestCount = 0;
        int errorCount = 0;
        Double result;

        for (EmsBean mBean : mBeans) {
            mBean.refreshAttributes();
            if (metricName.equals(METRIC_MIN_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("minTime");
                Long l = (Long) att.getValue();
                if (l < min)
                    min = l;
            } else if (metricName.equals(METRIC_MAX_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("maxTime");
                Long l = (Long) att.getValue();
                if (l > max)
                    max = l;
            } else if (metricName.equals(METRIC_AVG_SERVLET_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
                att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(METRIC_NUM_SERVLET_REQUESTS)) {
                EmsAttribute att = mBean.getAttribute("requestCount");
                Integer i = (Integer) att.getValue();
                requestCount += i;
            } else if (metricName.equals(METRIC_NUM_SERVLET_ERRORS)) {
                EmsAttribute att = mBean.getAttribute("errorCount");
                Integer i = (Integer) att.getValue();
                errorCount += i;
            } else if (metricName.equals(METRIC_TOTAL_TIME)) {
                EmsAttribute att = mBean.getAttribute("processingTime");
                Long l = (Long) att.getValue();
                processingTime += l;
            }
        }
        if (metricName.equals(METRIC_AVG_SERVLET_TIME)) {
            result = (requestCount > 0) ? ((double) processingTime / (double) requestCount) : Double.NaN;
        } else if (metricName.equals(METRIC_MIN_SERVLET_TIME)) {
            result = (min != Long.MAX_VALUE) ? (double) min : Double.NaN;
        } else if (metricName.equals(METRIC_MAX_SERVLET_TIME)) {
            result = (max != 0) ? (double) max : Double.NaN;
        } else if (metricName.equals(METRIC_NUM_SERVLET_ERRORS)) {
            result = (double) errorCount;
        } else if (metricName.equals(METRIC_NUM_SERVLET_REQUESTS)) {
            result = (double) requestCount;
        } else if (metricName.equals(METRIC_TOTAL_TIME)) {
            result = (double) processingTime;
        } else {
            // fallback
            result = Double.NaN;
        }

        return result;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        WarOperation operation = getOperation(name);
        if (name.equalsIgnoreCase("start")) {
            this.webModuleMBean = getWebModuleMBean();
        }

        if (null == this.webModuleMBean) {
            throw new IllegalStateException("Could not find MBean for WAR '" + getApplicationName() + "'.");
        }

        EmsOperation mbeanOperation = this.webModuleMBean.getOperation(name);
        if (mbeanOperation == null) {
            throw new IllegalStateException("Operation [" + name + "] not found on bean [" + this.webModuleMBean.getBeanName() + "]");
        }

        // NOTE: None of the supported operations have any parameters or return values, which makes our job easier.
        Object[] paramValues = new Object[0];
        mbeanOperation.invoke(paramValues);
        int state = (Integer) this.webModuleMBean.getAttribute("state").refresh();
        int expectedState = getExpectedPostExecutionState(operation);
        if (state != expectedState) {
            throw new Exception("Failed to " + name + " webapp (value of the 'state' attribute of MBean '" + this.webModuleMBean.getBeanName()
                + "' is " + state + ", not " + expectedState + ").");
        }

        return new OperationResult();
    }

    private static int getExpectedPostExecutionState(WarOperation operation) {
        int expectedState;
        switch (operation) {
        case START:
        case RELOAD: {
            expectedState = WarMBeanState.STARTED;
            break;
        }

        case STOP: {
            expectedState = WarMBeanState.STOPPED;
            break;
        }

        default: {
            throw new IllegalStateException("Unsupported operation: " + operation); // will never happen
        }
        }

        return expectedState;
    }

    private WarOperation getOperation(String name) {
        try {
            return WarOperation.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation name: " + name);
        }
    }

    /**
     * Returns the Catalina WebModule MBean associated with this WAR (e.g.
     * Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=//localhost/jmx-console), or null if the
     * WAR has no corresponding WebModuleMBean.
     *
     * <p/>This will return null only in the rare case that a deployed WAR has no context root associated with it. An
     * example of this is ROOT.war in the RHQ Server. rhq.ear maps rhq-portal.war to "/" and overrides ROOT.war's
     * association with "/".
     *
     * @return the Catalina WebModule MBean associated with this WAR
     */
    @Nullable
    private EmsBean getWebModuleMBean() {
        String webModuleMBeanName = getWebModuleMBeanName();
        EmsBean result = null;

        if (webModuleMBeanName != null) {
            ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(webModuleMBeanName);
            List<EmsBean> mBeans = getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());
            // There should only be one mBean for this match.
            if (mBeans.size() == 1) {
                result = mBeans.get(0);
            }
        }

        return result;
    }

    @Nullable
    private String getWebModuleMBeanName() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String name = pluginConfig.getSimpleValue(PROPERTY_NAME, null);
        String webModuleMBeanName = "Catalina:j2eeType=WebModule,J2EEApplication=none,J2EEServer=none,name=" + name;
        return webModuleMBeanName;
    }

    private enum WarOperation {
        START, STOP, RELOAD
    }

    private interface WarMBeanState {
        int STOPPED = 0;
        int STARTED = 1;
    }

    private List<EmsBean> getVHosts() {
        EmsConnection emsConnection = getEmsConnection();
        String query = QUERY_TEMPLATE_HOST;
        query = query.replace("%PATH%", getResourceContext().getPluginConfiguration().getSimpleValue(PROPERTY_CONTEXT_ROOT, ""));
        ObjectNameQueryUtility queryUtil = new ObjectNameQueryUtility(query);
        List<EmsBean> mBeans = emsConnection.queryBeans(queryUtil.getTranslatedQuery());
        return mBeans;
    }

    /**
     * Returns the name of the application.
     *
     * @return application name
     */
    private String getApplicationName() {
        String resourceKey = getResourceContext().getResourceKey();
        String appName = resourceKey.substring(resourceKey.lastIndexOf('=') + 1);
        return appName;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        // You can only update the one application file referenced by this resource
        if (packages.size() != 1) {
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("When deploying WAR files only one can be updated at a time.");
            return response;
        }
        ResourcePackageDetails packageDetails = packages.iterator().next();

        // Find location of existing application
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        File appFile = new File(pluginConfig.getSimple(PROPERTY_FILENAME).getStringValue());
        if (!appFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + appFile, packageDetails);
        }
        boolean isExploded = appFile.isDirectory();

        // save the new version of the app to a temp location
        File tempFile;
        try {
            tempFile = writeAppBitsToTempFile(appFile, contentServices, packageDetails);
        } catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e, packageDetails);
        }

        // Undeploy (delete) the current app.  Back up the bits in case we need to restore if the new app fails to deploy
        File backupFile = null;
        try {
            backupFile = this.deleteApp(pluginConfig, appFile, true);
        } catch (Exception e) {
            if (appFile.exists()) {
                return failApplicationDeployment("Error undeploying existing app - cause: " + e, packageDetails);
            }
            // log but proceed with no backup
            log.warn("Failed to create app backup but proceeding with redeploy of " + appFile.getPath() + ": " + e);
        }

        try {
            // Write the new bits for the application. If successful Tomcat will pick it up and complete the deploy.
            moveTempFileToDeployLocation(tempFile, appFile, isExploded);
        } catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                FileUtils.purge(appFile, true);
                moveTempFileToDeployLocation(backupFile, appFile, isExploded);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            } catch (Exception e1) {
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: " + ThrowableUtil.getAllMessages(e1);
            }
            return failApplicationDeployment(errorMessage, packageDetails);
        }

        // Deploy was successful!

        deleteBackupOfOriginalFile(backupFile);
        persistApplicationVersion(packageDetails, appFile);

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        return response;
    }

    private void moveTempFileToDeployLocation(File tempFile, File appFile, boolean isExploded) throws IOException {
        InputStream tempIs = null;
        if (isExploded) {
            tempIs = new BufferedInputStream(new FileInputStream(tempFile));
            appFile.mkdir();
            ZipUtil.unzipFile(tempIs, appFile);
        } else {
            tempFile.renameTo(appFile);
        }
    }

    private File backupAppBitsToTempFile(File appFile) throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), appFile.getName() + System.currentTimeMillis());

        // The temp file shouldn't be there, but check and delete it if it is
        if (tempFile.exists()) {
            log.warn("Existing temporary file found and will be deleted at: " + tempFile);
            tempFile.delete();
        }

        try {
            ZipUtil.zipFileOrDirectory(appFile, tempFile);
        } catch (IOException e) {
            log.error("Error backing up app " + appFile.getPath() + " to " + tempFile, e);
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw e;
        } finally {
            if (!tempFile.exists()) {
                log.error("Temporary file for application update not written to: " + tempFile);
                throw new Exception();
            }
        }

        return tempFile;
    }

    private File writeAppBitsToTempFile(File file, ContentServices contentServices, ResourcePackageDetails packageDetails) throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), file.getName() + System.currentTimeMillis());

        // The temp file shouldn't be there, but check and delete it if it is
        if (tempFile.exists()) {
            log.warn("Existing temporary file found and will be deleted at: " + tempFile);
            tempFile.delete();
        }
        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            contentServices.downloadPackageBits(getResourceContext().getContentContext(), packageDetails.getKey(), tempOutputStream, true);
        } finally {
            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException e) {
                    log.error("Error closing temporary output stream", e);
                }
            }
        }
        if (!tempFile.exists()) {
            log.error("Temporary file for application update not written to: " + tempFile);
            throw new Exception();
        }
        return tempFile;
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimpleValue(PROPERTY_FILENAME, null);

        if (fullFileName == null) {
            throw new IllegalStateException("Plugin configuration does not contain the full file name of the WAR file.");
        }

        // If the parent WAR resource was found, this file should exist
        File file = new File(fullFileName);
        if (file.exists()) {
            // Package name and file name of the application are the same
            String fileName = new File(fullFileName).getName();

            PackageVersions versions = loadApplicationVersions();
            String version = versions.getVersion(fileName);

            // First discovery of this WAR
            if (null == version) {
                JarContentFileInfo info = new JarContentFileInfo(file);
                version = info.getVersion("1.0");
                versions.putVersion(fileName, version);
                versions.saveToDisk();
            }

            PackageDetailsKey key = new PackageDetailsKey(fileName, version, PKG_TYPE_FILE, ARCHITECTURE);
            ResourcePackageDetails details = new ResourcePackageDetails(key);
            details.setFileName(fileName);
            details.setLocation(file.getPath());
            if (!file.isDirectory())
                details.setFileSize(file.length());
            details.setFileCreatedDate(null); // TODO: get created date via SIGAR

            packages.add(details);
        }

        return packages;
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove the package backing a WAR resource.");
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            if (packageFile.isDirectory()) {
                fileToSend = File.createTempFile("jopr-tomcat", ".zip");
                ZipUtil.zipFileOrDirectory(packageFile, fileToSend);
            } else
                fileToSend = packageFile;
            return new BufferedInputStream(new FileInputStream(fileToSend));
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve package bits for " + packageDetails, e);
        }
    }

    private DeployPackagesResponse failApplicationDeployment(String errorMessage, ResourcePackageDetails packageDetails) {
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);

        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);

        response.addPackageResponse(packageResponse);

        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {
        String packageName = appFile.getName();
        PackageVersions versions = loadApplicationVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        try {
            FileUtils.purge(backupOfOriginalFile, true);
        } catch (Exception e) {
            // not critical.
            log.warn("Failed to delete backup of original file: " + backupOfOriginalFile);
        }
    }

    public TomcatVHostComponent getParentResourceComponent() {
        return getResourceContext().getParentResourceComponent();
    }

    private PackageVersions loadApplicationVersions() {
        if (versions == null) {
            ResourceType resourceType = getResourceContext().getResourceType();
            String pluginName = resourceType.getPlugin();

            File dataDirectoryFile = getResourceContext().getDataDirectory();

            if (!dataDirectoryFile.exists()) {
                dataDirectoryFile.mkdir();
            }

            String dataDirectory = dataDirectoryFile.getAbsolutePath();

            log.debug("Creating application versions store with plugin name [" + pluginName + "] and data directory [" + dataDirectory + "]");

            versions = new PackageVersions(pluginName, dataDirectory);
            versions.loadFromDisk();
        }

        return versions;
    }

    public void deleteResource() throws Exception {
        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimple(PROPERTY_FILENAME).getStringValue();
        File file = new File(fullFileName);
        if (!file.exists()) {
            log.warn("Could not delete web application files (perhaps removed manually?). Proceeding with resource removal for: " + fullFileName);
        } else {
            deleteApp(pluginConfiguration, file, false);
        }
    }

    private File deleteApp(Configuration pluginConfiguration, File appFile, boolean keepBackup) throws Exception {
        File backupFile = null;
        String contextRoot = pluginConfiguration.getSimple(PROPERTY_CONTEXT_ROOT).getStringValue();

        try {
            // this will release locked files. In particular, the .war when deployed as an archive (this may be a windows issue only)
            invokeOperation("stop", null);
            getParentResourceComponent().undeployWar(contextRoot);

            if (keepBackup) {
                try {
                    backupFile = this.backupAppBitsToTempFile(appFile);
                } catch (Exception e) {
                    log.warn("Failed to create backup while deleting app " + appFile.getPath());
                }
            }
        } catch (TomcatApplicationDeployer.DeployerException e) {
            log.warn("Failed to undeploy WAR (may have been undeployed manually). Proceeding with resource delete for  [" + contextRoot + "].", e);
        } catch (Exception e) {
            log.error("Failed to undeploy WAR [" + contextRoot + "].", e);
            throw e;
        } finally {
            File associatedWarFile = null;
            if (appFile.isDirectory()) {
                associatedWarFile = new File(appFile.getAbsolutePath() + ".war");
            }

            try {
                if ((null != associatedWarFile) && associatedWarFile.exists()) {
                    FileUtils.purge(associatedWarFile, true);
                }
            } catch (IOException e) {
                // don't fail on this but warn, since the app may get redeployed on the next Tomcat startup
                log.warn("Failed to delete file [" + associatedWarFile + "].", e);
            }

            try {
                FileUtils.purge(appFile, true);
            } catch (IOException e) {
                log.error("Failed to delete file [" + appFile + "].", e);
                // if the undeploy also failed that exception will be lost
                // and this one will be seen by the caller instead.
                // arguably both these conditions indicate failure, since
                // not being able to delete the file will mean that it will
                // likely get picked up again by the deployment scanner
                throw e;
            }
        }

        return backupFile;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);

        // If all went well, persist the changes to the Tomcat user Database
        try {
            storeConfig();
        } catch (Exception e) {
            report
                .setErrorMessage("Failed to persist configuration change.  Changes will not survive Tomcat restart unless a successful Save operation is performed.");
        }
    }

    /** Persist local changes to the server.xml */
    void storeConfig() throws Exception {
        this.getResourceContext().getParentResourceComponent().storeConfig();
    }

}
