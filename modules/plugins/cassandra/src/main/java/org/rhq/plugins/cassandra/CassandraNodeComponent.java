/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.plugins.cassandra;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.system.OperatingSystemType.WINDOWS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.SigarException;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.ProcessInfo.ProcessInfoSnapshot;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.cassandra.util.KeyspaceService;
import org.rhq.plugins.cassandra.util.TakeSnapshotOperation;
import org.rhq.plugins.jmx.JMXServerComponent;

/**
 * @author John Sanda
 */
public class CassandraNodeComponent extends JMXServerComponent<ResourceComponent<?>> implements OperationFacet {
    private static final Log log = LogFactory.getLog(CassandraNodeComponent.class);

    private String host;
    private ProcessInfo processInfo;
    private InetAddress hostAddress;

    @SuppressWarnings("rawtypes")
    @Override
    public void start(ResourceContext context) throws Exception {
        super.start(context);

        processInfo = context.getNativeProcess();
        host = context.getPluginConfiguration().getSimpleValue("host", "localhost");

        try {
            hostAddress = InetAddress.getByName(this.host);
        } catch (UnknownHostException e) {
            log.error("Unable to convert hostname[" + this.host + "] into IP address for " + context.getResourceKey(),
                e);
        }
    };

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    @Override
    public void stop() {
        processInfo = null;
        super.stop();
    }

    @Override
    public AvailabilityType getAvailability() {
        long start = System.nanoTime();
        try {
            if (isStorageServiceReachable()) {
                return AvailabilityType.UP;
            }
            return AvailabilityType.DOWN;
        } finally {
            long totalTimeMillis = NANOSECONDS.toMillis(System.nanoTime() - start);
            if (log.isDebugEnabled()) {
                log.debug("Finished availability check in " + totalTimeMillis + " ms");
            }
            if (totalTimeMillis > SECONDS.toMillis(5)) {
                log.warn("Availability check exceeded five seconds. Total time was " + totalTimeMillis + " ms");
            }
        }
    }

    private boolean isStorageServiceReachable() {
        JMXConnector connector = null;
        try {
            Configuration pluginConfig = getResourceContext().getPluginConfiguration();
            String url = pluginConfig.getSimpleValue("connectorAddress");
            JMXServiceURL serviceURL = new JMXServiceURL(url);
            connector = JMXConnectorFactory.connect(serviceURL, null);

            MBeanServerConnection serverConnection = connector.getMBeanServerConnection();
            ObjectName storageService = new ObjectName("org.apache.cassandra.db:type=StorageService");

            // query an attribute to make sure it is in fact available
            serverConnection.getAttribute(storageService, "NativeTransportRunning");

            return true;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to make JMX connection to StorageService", e);
            }
            return false;
        } finally {
            if (connector != null) {
                try {
                    connector.close();
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("An error occurred closing the JMX connector", e);
                    }
                }
            }
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("shutdown")) {
            OperationResult operationResult = shutdownNode();
            waitForNodeToGoDown();
            return operationResult;
        } else if (name.equals("start")) {
            return startNode();
        } else if (name.equals("restart")) {
            return restartNode();
        } else if (name.equals("updateSeedsList")) {
            return updateSeedsList(parameters);
        } else if (name.equals("takeSnapshot")) {
            if (isStorageServiceReachable()) {
                return new TakeSnapshotOperation(new KeyspaceService(getEmsConnection()), parameters).invoke();
            } else {
                OperationResult result = new OperationResult();
                result.setErrorMessage("Unable to take snaphost, Storage Node is not available");
                return result;
            }

        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    protected OperationResult shutdownNode() {
        ResourceContext<?> context = getResourceContext();

        if (log.isInfoEnabled()) {
            log.info("Starting shutdown operation on " + CassandraNodeComponent.class.getName() +
                " with resource key " + context.getResourceKey());
        }
        EmsConnection emsConnection = getEmsConnection();
        EmsBean storageService = emsConnection.getBean("org.apache.cassandra.db:type=StorageService");
        Class[] emptyParams = new Class[0];

        if (log.isDebugEnabled()) {
            log.debug("Disabling thrift...");
        }
        EmsOperation operation = storageService.getOperation("stopRPCServer", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Disabling gossip...");
        }
        operation = storageService.getOperation("stopGossiping", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Initiating drain...");
        }
        operation = storageService.getOperation("drain", emptyParams);
        operation.invoke((Object[]) emptyParams);

        return stopNode();
    }

    protected OperationResult stopNode() {
        ProcessInfo process = getResourceContext().getNativeProcess();

        if (process == null) {
            log.warn("Failed to obtain process info. It appears Cassandra is already shutdown.");
            return new OperationResult("Failed to obtain process info. It appears Cassandra is already shutdown.");
        }

        long pid = process.getPid();
        try {
            process.kill("KILL");

            Configuration pluginConfig = getResourceContext().getPluginConfiguration();
            File basedir = new File(pluginConfig.getSimpleValue("baseDir"));
            File binDir = new File(basedir, "bin");
            File pidFile = new File(binDir, "cassandra.pid");

            pidFile.delete();

            return new OperationResult("Successfully shut down Cassandra daemon with pid " + pid);
        } catch (SigarException e) {
            log.warn("Failed to shut down Cassandra node with pid " + pid, e);
            OperationResult failure = new OperationResult("Failed to shut down Cassandra node with pid " + pid);
            failure.setErrorMessage(ThrowableUtil.getAllMessages(e));
            return failure;
        }
    }

    protected void waitForNodeToGoDown() throws InterruptedException {
        if (OperatingSystem.getInstance().getName().equals(OperatingSystem.NAME_MACOSX)) {
            // See this thread on VMWare forum: http://communities.vmware.com/message/2187972#2187972
            // Unfortunately there is no work around for this failure on Mac OSX so the method will silently return on
            // this platform.
            return;
        }
        for (ProcessInfoSnapshot processInfoSnapshot = getProcessInfoSnapshot();; processInfoSnapshot = getProcessInfoSnapshot()) {
            if (processInfoSnapshot == null || !processInfoSnapshot.isRunning()) {
                // Process not found, so it died, that's fine
                // OR
                // Process info says process is no longer running, that's fine as well
                break;
            }
            if (getResourceContext().getComponentInvocationContext().isInterrupted()) {
                // Operation canceled or timed out
                throw new InterruptedException();
            }
            // Process is still running, wait a second and check again
            Thread.sleep(SECONDS.toMillis(2));
        }
    }

    private ProcessInfoSnapshot getProcessInfoSnapshot() {
        ProcessInfoSnapshot processInfoSnapshot = (processInfo == null) ? null : processInfo.freshSnapshot();
        if (processInfoSnapshot == null || !processInfoSnapshot.isRunning()) {
            processInfo = getResourceContext().getNativeProcess();
            // Safe to get prior snapshot here, we've just recreated the process info instance
            processInfoSnapshot = (processInfo == null) ? null : processInfo.priorSnaphot();
        }
        return processInfoSnapshot;
    }

    protected OperationResult startNode() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String baseDir = pluginConfig.getSimpleValue("baseDir");
        File binDir = new File(baseDir, "bin");
        if (!startScriptExists(binDir)) {
            OperationResult failure = new OperationResult("Failed to start Cassandra daemon");
            failure.setErrorMessage("Start script does not exists");
            return failure;
        }
        ProcessExecution scriptExe = getProcessExecution(binDir);
        SystemInfo systemInfo = getResourceContext().getSystemInformation();
        ProcessExecutionResults results = systemInfo.executeProcess(scriptExe);
        if (results.getError() == null) {
            return new OperationResult("Successfully started Cassandra daemon");
        } else {
            OperationResult failure = new OperationResult("Failed to start Cassandra daemon");
            failure.setErrorMessage(ThrowableUtil.getAllMessages(results.getError()));
            return failure;
        }
    }

    private boolean startScriptExists(File binDir) {
        File file = new File(binDir, getStartScript());
        return file.exists() && !file.isDirectory();
    }

    private ProcessExecution getProcessExecution(File binDir) {
        ProcessExecution scriptExe;
        if (OperatingSystem.getInstance().getName().equals(OperatingSystem.NAME_WIN32)) {
            File startScript = new File(binDir, getStartScript());
            scriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        } else {
            // On Linux, when Cassandra is started with an absolute path, the command line is too long and is truncated
            // in /proc/pid/cmdline (beacuse of a long CLASSPATH made of absolute paths)
            // This prevents the process from being later discovered because the process query argument criteria
            // expects org.apache.cassandra.service.CassandraDaemon to be found
            File startScript = new File("./" + getStartScript());
            scriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
            scriptExe.setCheckExecutableExists(false);
        }
        scriptExe.setWorkingDirectory(binDir.getAbsolutePath());
        scriptExe.addArguments(asList("-p", "cassandra.pid"));
        return scriptExe;
    }

    protected OperationResult restartNode() {
        OperationResult result = shutdownNode();

        if (result.getErrorMessage() == null) {
            result = startNode();
        }

        return result;
    }

    protected OperationResult updateSeedsList(Configuration params) {
        PropertyList list = params.getList("seedsList");
        List<String> addresses = getAddresses(list);

        OperationResult result = new OperationResult();
        try {
            updateSeedsList(addresses);
        }  catch (Exception e) {
            log.error("An error occurred while updating the seeds list property", e);
            Throwable rootCause = ThrowableUtil.getRootCause(e);
            result.setErrorMessage(ThrowableUtil.getStackAsString(rootCause));
        }
        return result;
    }

    protected List<String> getAddresses(PropertyList seedsList) {
        List<String> addresses = new ArrayList<String>();
        for (Property property : seedsList.getList()) {
            PropertySimple simple = (PropertySimple) property;
            addresses.add(simple.getStringValue());
        }
        return addresses;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void updateSeedsList(List<String> seeds) throws IOException {
        ResourceContext<?> context = getResourceContext();
        Configuration pluginConfig = context.getPluginConfiguration();

        String yamlProp = pluginConfig.getSimpleValue("yamlConfiguration");
        if (yamlProp == null || yamlProp.isEmpty()) {
            throw new IllegalStateException("Plugin configuration property [yamlConfiguration] is undefined. This " +
                "property must specify be set and specify the location of cassandra.yaml in order to complete " +
                "this operation");
        }

        File yamlFile = new File(yamlProp);
        if (!yamlFile.exists()) {
            throw new IllegalStateException("Plug configuration property [yamlConfiguration] has as its value a " +
                "non-existent file.");
        }

        // Cassandra uses strong typing when reading and parsing cassandra.yaml. The
        // document is parsed into a org.apache.cassandra.config.Config object. I tried
        // using the config classes but ran into a couple different problems. When writing
        // the config back out to cassandra.yaml, the generated yaml is not correct for the
        // seed_provider property. The snakeyaml parser cannot even load the document
        // because the SeedProviderDef class cannot be instantiated since it does not define
        // a no-args constructor. Once I fixed that, I still was not able to get past the
        // problems with the yaml generated for the seed_provider provider. Subsequent reads
        // of cassandra.yaml would result in parsing errors. Given these problems, I decided
        // to go with the untyped approach for updating cassandra.yaml for now.
        //
        // jsanda

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Map cassandraConfig = (Map) yaml.load(new FileInputStream(yamlFile));

        List seedProviderList = (List) cassandraConfig.get("seed_provider");
        Map seedProvider = (Map) seedProviderList.get(0);
        List paramsList = (List) seedProvider.get("parameters");
        Map params = (Map) paramsList.get(0);
        params.put("seeds", StringUtil.listToString(seeds));

        // create a backup of the configuration file in preparation of writing out the changes
        File yamlFileBackup = new File(yamlProp + ".bak" + new Date().getTime());
        StreamUtil.copy(new FileInputStream(yamlFile), new FileOutputStream(yamlFileBackup), true);

        if (!yamlFile.delete()) {
            String msg = "Failed to delete [" + yamlFile + "] in preparation of writing updated configuration. The " +
                "changes will be aborted.";
            log.error(msg);
            deleteYamlBackupFile(yamlFileBackup);
            throw new IOException(msg);
        }

        FileWriter writer = new FileWriter(yamlFile);
        try {
            yaml.dump(cassandraConfig, writer);
            deleteYamlBackupFile(yamlFileBackup);
        } catch (Exception e) {
            log.error("An error occurred while trying to write the updated configuration back to " + yamlFile, e);
            log.error("Reverting changes to " + yamlFile);

            if (yamlFile.delete()) {
                StreamUtil.copy(new FileInputStream(yamlFileBackup), new FileOutputStream(yamlFile));
                deleteYamlBackupFile(yamlFileBackup);
            } else {
                String msg = "Failed updates to " + yamlFile.getName() + " cannot be rolled back. The file cannot be " +
                    "deleted. " + yamlFile + " should be replaced by " + yamlFileBackup;
                log.error(msg);
                throw new IOException(msg);
            }
        } finally {
            writer.close();
        }
    }

    private void deleteYamlBackupFile(File yamlBackup) {
        if (!yamlBackup.delete()) {
            log.warn("Failed to delete Cassandra configuration backup file [" + yamlBackup + "]. This file " +
                    "should be deleted.");
        }
    }

    private String getStartScript() {
        ResourceContext<?> context = getResourceContext();
        SystemInfo systemInfo = context.getSystemInformation();

        if (systemInfo.getOperatingSystemType() == WINDOWS) {
            return "cassandra.bat";
        } else {
            return "cassandra";
        }
    }

    public String getHost() {
        return host;
    }
}
