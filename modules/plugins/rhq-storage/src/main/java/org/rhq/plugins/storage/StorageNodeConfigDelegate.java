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

        config.put(new PropertySimple("minHeapSize", heapMinProp.substring(4)));
        config.put(new PropertySimple("maxHeapSize", heapMaxProp.substring(4)));
        config.put(new PropertySimple("heapNewSize", heapNewProp.substring(4)));
        config.put(new PropertySimple("threadStackSize", threadStackSizeProp.substring(4)));

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        try {
            PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(jvmOptsFile.getAbsolutePath());
            Properties properties = propertiesUpdater.loadExistingProperties();

            Configuration config = configurationUpdateReport.getConfiguration();

            // We want min and max heap to be the same
            properties.setProperty("heap_min", "-Xms" + config.getSimpleValue("maxHeapSize"));
            properties.setProperty("heap_max", "-Xmx" + config.getSimpleValue("maxHeapSize"));
            properties.setProperty("heap_new", "-Xmn" + config.getSimpleValue("heapNewSize"));
            properties.setProperty("thread_stack_size", "-Xss" + config.getSimpleValue("threadStackSize"));

            propertiesUpdater.update(properties);

            configurationUpdateReport.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IOException e) {
            configurationUpdateReport.setErrorMessageFromThrowable(e);
        }

    }
}
