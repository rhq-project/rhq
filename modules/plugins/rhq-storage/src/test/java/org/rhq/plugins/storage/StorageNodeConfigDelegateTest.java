package org.rhq.plugins.storage;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegateTest {

    private File basedir;

    private StorageNodeConfigDelegate configDelegate;

    @BeforeMethod
    public void initDirs(Method test) throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        configDelegate = new StorageNodeConfigDelegate(basedir);
    }


    @Test
    public void loadValidConfig() throws Exception {
        createDefaultConfig();

        Configuration config = configDelegate.loadResourceConfiguration();

        assertEquals(config.getSimpleValue("minHeapSize"), "512M", "Failed to load property [minHeapSize]");
        assertEquals(config.getSimpleValue("maxHeapSize"), "512M", "Failed to load property [maxHepSize]");
        assertEquals(config.getSimpleValue("heapNewSize"), "128M", "Failed to load property [heapNewSize]");
        assertEquals(config.getSimpleValue("threadStackSize"), "180k", "Failed to load property [threadStackSize]");
        assertEquals(config.getSimple("heapDumpOnOOMError").getBooleanValue(), (Boolean) true,
            "Failed to load property [heapDumpOnOOMError]");
        assertEquals(new File(config.getSimpleValue("heapDumpDir")), binDir(), "Failed to load property [heapDumpDir]");
    }

    @Test
    public void updateValidConfig() throws Exception {
        createDefaultConfig();

        Configuration config = new Configuration();
        config.put(new PropertySimple("minHeapSize", "1024M"));
        config.put(new PropertySimple("maxHeapSize", "1024M"));
        config.put(new PropertySimple("heapNewSize", "256M"));
        config.put(new PropertySimple("threadStackSize", "240k"));
        config.put(new PropertySimple("heapDumpOnOOMError", true));
        config.put(new PropertySimple("heapDumpDir", confDir()));

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(config);

        configDelegate.updateResourceConfiguration(report);

        Properties properties = loadCassandraJvmProps();

        assertEquals(properties.getProperty("heap_min"), "-Xms1024M", "Failed to update property [minHeapSize]");
        assertEquals(properties.getProperty("heap_max"), "-Xmx1024M", "Failed to update property [maxHeapSize]");
        assertEquals(properties.getProperty("heap_new"), "-Xmn256M", "Failed to update property [heapNewSize]");
        assertEquals(properties.getProperty("thread_stack_size"), "-Xss240k",
            "Failed to update property [threadStackSize]");
        assertEquals(properties.getProperty("heap_dump_on_OOMError"), "-XX:+HeapDumpOnOutOfMemoryError",
            "Failed to update property [heap_dump_on_OOMError]");
        assertEquals(properties.getProperty("heap_dump_dir"), confDir().getAbsolutePath(),
            "Failed to update property [heap_dump_dir]");
    }

    @Test
    public void minHeapSizeShouldBeTheSameAsMaxHeapSize() throws Exception {
        createDefaultConfig();

        Configuration config = new Configuration();
        config.put(new PropertySimple("minHeapSize", "512M"));
        config.put(new PropertySimple("maxHeapSize", "768M"));

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(config);

        configDelegate.updateResourceConfiguration(report);

        Properties properties = loadCassandraJvmProps();

        assertEquals(properties.getProperty("heap_max"), "-Xmx768M", "Failed to update property [maxHeapSize]");
        assertEquals(properties.getProperty("heap_min"), "-Xms768M", "Failed to update property [maxHeapSize]. It " +
            "should be the same as [maxHeapSize].");
    }

    private void createDefaultConfig() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("heap_min", "-Xms512M");
        properties.setProperty("heap_max", "-Xmx512M");
        properties.setProperty("heap_new", "-Xmn128M");
        properties.setProperty("thread_stack_size", "-Xss180k");
        properties.setProperty("heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
        properties.setProperty("heap_dump_dir", binDir().getAbsolutePath());

        properties.store(new FileOutputStream(new File(confDir(), "cassandra-jvm.properties")), "");
    }

    private Properties loadCassandraJvmProps() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(confDir(), "cassandra-jvm.properties")));

        return properties;
    }

    private File confDir() {
        return mkdirIfNecessary(basedir, "conf");
    }

    private File binDir() {
        return mkdirIfNecessary(basedir, "bin");
    }

    private File mkdirIfNecessary(File parent, String path) {
        File dir = new File(parent, path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

}
