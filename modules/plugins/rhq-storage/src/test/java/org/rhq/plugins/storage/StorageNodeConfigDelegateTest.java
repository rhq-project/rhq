package org.rhq.plugins.storage;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegateTest {



    @Test
    public void loadValidConfig() throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        File basedir = new File(dir, getClass().getSimpleName() + "/loadValidConfig");
        File confDir = new File(basedir, "conf");

        confDir.mkdirs();
        createDefaultConfig(confDir);

        StorageNodeConfigDelegate configDelegate = new StorageNodeConfigDelegate(basedir);
        Configuration config = configDelegate.loadResourceConfiguration();

        assertEquals(config.getSimpleValue("minHeapSize"), "512M", "Failed to load property [minHeapSize]");
        assertEquals(config.getSimpleValue("maxHeapSize"), "512M", "Failed to load property [maxHepSize]");
        assertEquals(config.getSimpleValue("heapNewSize"), "128M", "Failed to load property [heapNewSize]");
        assertEquals(config.getSimpleValue("threadStackSize"), "180k", "Failed to load property [threadStackSize]");
    }

    @Test
    public void updateValidConfig() throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        File basedir = new File(dir, getClass().getSimpleName() + "/updateValidConfig");
        File confDir = new File(basedir, "conf");

        confDir.mkdirs();
        createDefaultConfig(confDir);

        Configuration config = new Configuration();
        config.put(new PropertySimple("minHeapSize", "1024M"));
        config.put(new PropertySimple("maxHeapSize", "1024M"));
        config.put(new PropertySimple("heapNewSize", "256M"));
        config.put(new PropertySimple("threadStackSize", "240k"));

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(config);

        StorageNodeConfigDelegate configDelegate = new StorageNodeConfigDelegate(basedir);
        configDelegate.updateResourceConfiguration(report);

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(confDir, "cassandra-jvm.properties")));

        assertEquals(properties.getProperty("heap_min"), "-Xms1024M", "Failed to update property [minHeapSize]");
        assertEquals(properties.getProperty("heap_max"), "-Xmx1024M", "Failed to update property [maxHeapSize]");
        assertEquals(properties.getProperty("heap_new"), "-Xmn256M", "Failed to update property [heapNewSize]");
        assertEquals(properties.getProperty("thread_stack_size"), "-Xss240k",
            "Failed to update property [threadStackSize]");
    }

    private void createDefaultConfig(File confDir) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("heap_min", "-Xms512M");
        properties.setProperty("heap_max", "-Xmx512M");
        properties.setProperty("heap_new", "-Xmn128M");
        properties.setProperty("thread_stack_size", "-Xss180k");

        properties.store(new FileOutputStream(new File(confDir, "cassandra-jvm.properties")), "");
    }

}
