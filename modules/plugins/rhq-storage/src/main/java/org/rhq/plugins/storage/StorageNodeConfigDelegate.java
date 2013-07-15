package org.rhq.plugins.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegate implements ConfigurationFacet {

    private File jvmOptsFile;

    public StorageNodeConfigDelegate(File basedir) {
        File confDir = new File(basedir, "conf");
        jvmOptsFile = new File(confDir, "cassandra-jvm.properties");
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(jvmOptsFile));

        Configuration config = new Configuration();

        String heapMinProp = properties.getProperty("heap_min");
        String heapMaxProp = properties.getProperty("heap_max");
        String heapNewProp = properties.getProperty("heap_new");
        String threadStackSizeProp = properties.getProperty("thread_stack_size");
        String heapDumpOnOOMError = properties.getProperty("heap_dump_on_OOMError");
        String heapDumpDir = properties.getProperty("heap_dump_dir");

        config.put(new PropertySimple("minHeapSize", heapMinProp.substring(4)));
        config.put(new PropertySimple("maxHeapSize", heapMaxProp.substring(4)));
        config.put(new PropertySimple("heapNewSize", heapNewProp.substring(4)));
        config.put(new PropertySimple("threadStackSize", threadStackSizeProp.substring(4)));

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

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        try {
            PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(jvmOptsFile.getAbsolutePath());
            Properties properties = propertiesUpdater.loadExistingProperties();

            Configuration config = configurationUpdateReport.getConfiguration();

            String maxHeapSize = config.getSimpleValue("maxHeapSize");
            if (!StringUtil.isEmpty(maxHeapSize)) {
                // We want min and max heap to be the same
                properties.setProperty("heap_min", "-Xms" + maxHeapSize);
                properties.setProperty("heap_max", "-Xmx" + maxHeapSize);
            }

            String heapNewSize = config.getSimpleValue("heapNewSize");
            if (!StringUtil.isEmpty(heapNewSize)) {
                properties.setProperty("heap_new", "-Xmn" + heapNewSize);
            }

            String threadStackSize = config.getSimpleValue("threadStackSize");
            if (!StringUtil.isEmpty(threadStackSize)) {
                properties.setProperty("thread_stack_size", "-Xss" + threadStackSize);
            }

            PropertySimple heapDumpOnOMMError = config.getSimple("heapDumpOnOOMError");
            if (heapDumpOnOMMError != null) {
                if (heapDumpOnOMMError.getBooleanValue()) {
                    properties.setProperty("heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
                } else {
                    properties.setProperty("heap_dump_on_OOMError", "");
                }
            }

            String heapDumpDir = config.getSimpleValue("heapDumpDir");
            if (!StringUtil.isEmpty(heapDumpDir)) {
                properties.setProperty("heap_dump_dir", heapDumpDir);
            }

            propertiesUpdater.update(properties);

            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IOException e) {
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        }

    }
}
