package org.rhq.plugins.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.error.YAMLException;

import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.cassandra.util.ConfigEditorException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegate implements ConfigurationFacet {

    private Log log = LogFactory.getLog(StorageNodeConfigDelegate.class);

    private File jvmOptsFile;
    private File wrapperEnvFile;
    private File cassandraYamlFile;
    private OperationFacet invoker;

    public StorageNodeConfigDelegate(File basedir, OperationFacet invoker) {
        File confDir = new File(basedir, "conf");
        jvmOptsFile = new File(confDir, "cassandra-jvm.properties");
        cassandraYamlFile = new File(confDir, "cassandra.yaml");
        this.invoker = invoker;

        // for windows, config props also get propagated to the wrapper env
        if (isWindows()) {
            File wrapperDir = new File(basedir, "../bin/wrapper");
            wrapperEnvFile = new File(wrapperDir, "rhq-storage-wrapper.env");
        }
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(jvmOptsFile));

        Configuration config = new Configuration();

        String heapDumpOnOOMError = properties.getProperty("heap_dump_on_OOMError");
        String heapDumpDir = properties.getProperty("heap_dump_dir");

        config.put(new PropertySimple("minHeapSize", getHeapMinProp(properties)));
        config.put(new PropertySimple("maxHeapSize", getHeapMaxProp(properties)));
        config.put(new PropertySimple("heapNewSize", getHeapNewProp(properties)));
        config.put(new PropertySimple("threadStackSize", getStackSizeProp(properties)));

        if (!StringUtil.isEmpty(heapDumpOnOOMError)) {
            config.put(new PropertySimple("heapDumpOnOOMError", true));
        } else {
            config.put(new PropertySimple("heapDumpOnOOMError", false));
        }

        if (!StringUtil.isEmpty(heapDumpDir)) {
            config.put(new PropertySimple("heapDumpDir", heapDumpDir));
        } else {
            File basedir = jvmOptsFile.getParentFile().getParentFile();
            config.put(new PropertySimple("heapDumpDir", new File(basedir, "bin").getAbsolutePath()));
        }

        ConfigEditor yamlEditor = new ConfigEditor(cassandraYamlFile);
        yamlEditor.load();
        config.put(new PropertySimple("cqlPort", yamlEditor.getNativeTransportPort()));
        config.put(new PropertySimple("gossipPort", yamlEditor.getStoragePort()));

        // Read data directories here..
        config.put(new PropertySimple("CommitLogLocation", yamlEditor.getCommitLogDirectory()));
        config.put(new PropertySimple("SavedCachesLocation", yamlEditor.getSavedCachesDirectory()));
        PropertyList dataFileLocations = new PropertyList("AllDataFileLocations");
        for (String s : yamlEditor.getDataFileDirectories()) {
            dataFileLocations.add(new PropertySimple("directory", s));
        }
        config.put(dataFileLocations);

        return config;
    }

    /**
     * Ensure that the path uses only forward slash.
     * @param path
     * @return forward-slashed path, or null if path is null
     */
    private static String useForwardSlash(String path) {

        return (null != path) ? path.replace('\\', '/') : null;
    }

    private String getHeapMinProp(Properties properties) {
        String value = properties.getProperty("heap_min");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xms")) {
            return value;
        }

        return value.substring(4);
    }

    private String getHeapMaxProp(Properties properties) {
        String value = properties.getProperty("heap_max");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xmx")) {
            return value;
        }

        return value.substring(4);
    }

    private String getHeapNewProp(Properties properties) {
        String value = properties.getProperty("heap_new");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!value.startsWith("-Xmn")) {
            return value;
        }

        return value.substring(4);
    }

    private String getStackSizeProp(Properties properties) {
        String value = properties.getProperty("thread_stack_size");

        if (StringUtil.isEmpty(value)) {
            return "";
        }

        if (!(value.startsWith("-Xss") || value.endsWith("k") || value.length() > 5)) {
            return value;
        }

        return value.substring(4, value.length() - 1);
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        updateResourceConfigurationAndRestartIfNecessary(configurationUpdateReport, false);
    }

    public void updateResourceConfigurationAndRestartIfNecessary(ConfigurationUpdateReport configurationUpdateReport,
        boolean restartIfNecessary) {
        try {
            Configuration config = configurationUpdateReport.getConfiguration();

            updateCassandraJvmProps(config);
            updateCassandraYaml(config);

            String dataFilesChangedString = config.getSimpleValue("dataDirectoriesChanged");
            boolean dataFilesChanged = dataFilesChangedString != null && Boolean.parseBoolean(dataFilesChangedString);

            if(dataFilesChanged && invoker != null) {
                try {
                    OperationResult moveDataFilesResult = invoker.invokeOperation("moveDataFiles", config);
                    if(moveDataFilesResult.getErrorMessage() != null) {
                        configurationUpdateReport.setErrorMessage(moveDataFilesResult.getErrorMessage());
                    }
                    restartIfNecessary = false; // We have already restarted the storage node, don't do it twice
                } catch (Exception e) {
                    configurationUpdateReport.setErrorMessage(e.getMessage());
                }
            }

            if (isWindows()) {
                updateWrapperEnv(config);
            }
            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IllegalArgumentException e) {
            configurationUpdateReport.setErrorMessage("No configuration update was applied: " + e.getMessage());
        } catch (IOException e) {
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        } catch (ConfigEditorException e) {
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        }
        if (restartIfNecessary) {
            restartIfNecessary(configurationUpdateReport);
        }
    }

    private void restartIfNecessary(ConfigurationUpdateReport configurationUpdateReport) {
        boolean restartIsRequired = false;
        Configuration params = configurationUpdateReport.getConfiguration();
        if (configurationUpdateReport.getStatus().equals(ConfigurationUpdateStatus.SUCCESS)) {
            if (params.getSimpleValue("maxHeapSize") != null
                || params.getSimpleValue("heapNewSize") != null
                || params.getSimpleValue("threadStackSize") != null) {
                restartIsRequired = true;
            }
        }
        if (restartIsRequired && invoker != null) {
            try {
                OperationResult restartResult = invoker.invokeOperation("restart", null);
                if (restartResult.getErrorMessage() != null) {
                    configurationUpdateReport.setErrorMessage(restartResult.getErrorMessage());
                }
            } catch (Exception e) {
                configurationUpdateReport.setErrorMessage(e.getMessage());
            }
        }
    }

    private void updateCassandraJvmProps(Configuration newConfig) throws IOException {
        PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(jvmOptsFile.getAbsolutePath());
        Properties properties = propertiesUpdater.loadExistingProperties();

        String jmxPort = newConfig.getSimpleValue("jmxPort");
        if (!StringUtil.isEmpty(jmxPort)) {
            validateIntegerArg("jmx_port", jmxPort);
            properties.setProperty("jmx_port", jmxPort);
        }

        String maxHeapSize = newConfig.getSimpleValue("maxHeapSize");
        if (!StringUtil.isEmpty(maxHeapSize)) {
            validateHeapArg("maxHeapSize", maxHeapSize);
            // We want min and max heap to be the same
            properties.setProperty("heap_min", "-Xms" + maxHeapSize);
            properties.setProperty("heap_max", "-Xmx" + maxHeapSize);
        }

        String heapNewSize = newConfig.getSimpleValue("heapNewSize");
        if (!StringUtil.isEmpty(heapNewSize)) {
            validateHeapArg("heapNewSize", heapNewSize);
            properties.setProperty("heap_new", "-Xmn" + heapNewSize);
        }

        String threadStackSize = newConfig.getSimpleValue("threadStackSize");
        if (!StringUtil.isEmpty(threadStackSize)) {
            validateIntegerArg("threadStackSize", threadStackSize);
            properties.setProperty("thread_stack_size", "-Xss" + threadStackSize + "k");
        }

        PropertySimple heapDumpOnOMMError = newConfig.getSimple("heapDumpOnOOMError");
        if (heapDumpOnOMMError != null) {
            if (heapDumpOnOMMError.getBooleanValue()) {
                properties.setProperty("heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
            } else {
                properties.setProperty("heap_dump_on_OOMError", "");
            }
        }

        String heapDumpDir = useForwardSlash(newConfig.getSimpleValue("heapDumpDir"));
        if (!StringUtil.isEmpty(heapDumpDir)) {
            properties.setProperty("heap_dump_dir", heapDumpDir);
        }

        propertiesUpdater.update(properties);
    }

    private void updateCassandraYaml(Configuration newConfig) {
        ConfigEditor editor = new ConfigEditor(cassandraYamlFile);
        try {
            editor.load();

            PropertySimple cqlPortProperty = newConfig.getSimple("cqlPort");
            if (cqlPortProperty != null) {
                editor.setNativeTransportPort(cqlPortProperty.getIntegerValue());
            }

            PropertySimple gossipPortProperty = newConfig.getSimple("gossipPort");
            if (gossipPortProperty != null) {
                editor.setStoragePort(gossipPortProperty.getIntegerValue());
            }

            editor.save();
        } catch (ConfigEditorException e) {
            if (e.getCause() instanceof YAMLException) {
                log.error("Failed to update " + cassandraYamlFile);
                log.info("Attempting to restore " + cassandraYamlFile);
                try {
                    editor.restore();
                    throw e;
                } catch (ConfigEditorException e1) {
                    log.error("Failed to restore " + cassandraYamlFile + ". A copy of the file prior to any " +
                        "modifications can be found at " + editor.getBackupFile());
                    throw new ConfigEditorException("There was an error updating " + cassandraYamlFile + " and " +
                        "undoing the changes failed. A copy of the file can be found at " + editor.getBackupFile() +
                        ". See the agent logs for more details.", e);
                }
            } else {
                log.error("No updates were made to " + cassandraYamlFile + " due to an unexpected error", e);
                throw e;
            }
        }
    }

    private void updateWrapperEnv(Configuration config) throws IOException {
        PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(wrapperEnvFile.getAbsolutePath());
        Properties properties = propertiesUpdater.loadExistingProperties();

        String maxHeapSize = config.getSimpleValue("maxHeapSize");
        if (!StringUtil.isEmpty(maxHeapSize)) {
            validateHeapArg("maxHeapSize", maxHeapSize);
            // We want min and max heap to be the same
            properties.setProperty("set.heap_min", "-Xms" + maxHeapSize);
            properties.setProperty("set.heap_max", "-Xmx" + maxHeapSize);
        }

        String heapNewSize = config.getSimpleValue("heapNewSize");
        if (!StringUtil.isEmpty(heapNewSize)) {
            validateHeapArg("heapNewSize", heapNewSize);
            properties.setProperty("set.heap_new", "-Xmn" + heapNewSize);
        }

        String threadStackSize = config.getSimpleValue("threadStackSize");
        if (!StringUtil.isEmpty(threadStackSize)) {
            validateIntegerArg("threadStackSize", threadStackSize);
            properties.setProperty("set.thread_stack_size", "-Xss" + threadStackSize + "k");
        }

        PropertySimple heapDumpOnOMMError = config.getSimple("heapDumpOnOOMError");
        if (heapDumpOnOMMError != null) {
            if (heapDumpOnOMMError.getBooleanValue()) {
                properties.setProperty("set.heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
            } else {
                properties.setProperty("set.heap_dump_on_OOMError", "");
            }
        }

        String heapDumpDir = useForwardSlash(config.getSimpleValue("heapDumpDir"));
        if (!StringUtil.isEmpty(heapDumpDir)) {
            properties.setProperty("set.heap_dump_dir", "-XX:HeapDumpPath=" + heapDumpDir);
        }

        propertiesUpdater.update(properties);
    }

    private void validateHeapArg(String name, String value) {
        if (value.length() < 2) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }

        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length - 1; ++i) {
            if (!Character.isDigit(chars[i])) {
                throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
            }
        }

        char lastChar = Character.toUpperCase(chars[chars.length - 1]);
        if (!(lastChar == 'M' || lastChar == 'G')) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }
    }

    private void validateIntegerArg(String name, String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(value + " is not a legal value for the property [" + name + "]");
        }
    }

    private boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
