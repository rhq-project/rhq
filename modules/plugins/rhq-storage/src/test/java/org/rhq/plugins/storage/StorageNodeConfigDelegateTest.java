package org.rhq.plugins.storage;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.cassandra.util.ConfigEditor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class StorageNodeConfigDelegateTest {

    private File basedir;

    private File cassandraYamlFile;

    private StorageNodeConfigDelegate configDelegate;

    @BeforeMethod
    public void initDirs(Method test) throws Exception {
        File dir = new File(getClass().getResource(".").toURI());
        basedir = new File(dir, getClass().getSimpleName() + "/" + test.getName());
        FileUtil.purge(basedir, true);
        configDelegate = new StorageNodeConfigDelegate(basedir);

        cassandraYamlFile = new File(confDir(), "cassandra.yaml");
        InputStream inputStream = getClass().getResourceAsStream("/cassandra.yaml");
        FileOutputStream outputStream = new FileOutputStream(cassandraYamlFile);
        StreamUtil.copy(inputStream, outputStream);
    }


    @Test
    public void loadValidConfig() throws Exception {
        createDefaultConfig();

        Configuration config = configDelegate.loadResourceConfiguration();

        assertEquals(config.getSimpleValue("minHeapSize"), "512M", "Failed to load property [minHeapSize]");
        assertEquals(config.getSimpleValue("maxHeapSize"), "512M", "Failed to load property [maxHepSize]");
        assertEquals(config.getSimpleValue("heapNewSize"), "128M", "Failed to load property [heapNewSize]");
        assertEquals(config.getSimpleValue("threadStackSize"), "180", "Failed to load property [threadStackSize]");
        assertEquals(config.getSimple("heapDumpOnOOMError").getBooleanValue(), (Boolean) true,
            "Failed to load property [heapDumpOnOOMError]");
        assertEquals(new File(config.getSimpleValue("heapDumpDir")), binDir(), "Failed to load property [heapDumpDir]");
    }

    @Test
    public void updateValidConfig() throws Exception {
        createDefaultConfig();

        Configuration config = Configuration.builder()
            .addSimple("minHeapSize", "1024M")
            .addSimple("maxHeapSize", "1024M")
            .addSimple("heapNewSize", "256M")
            .addSimple("threadStackSize", "240")
            .addSimple("heapDumpOnOOMError", true)
            .addSimple("heapDumpDir", confDir())
            .addSimple("cqlPort", 9595)
            .addSimple("gossipPort", 9696)
            .build();
//        config.put(new PropertySimple("minHeapSize", "1024M"));
//        config.put(new PropertySimple("maxHeapSize", "1024M"));
//        config.put(new PropertySimple("heapNewSize", "256M"));
//        config.put(new PropertySimple("threadStackSize", "240"));
//        config.put(new PropertySimple("heapDumpOnOOMError", true));
//        config.put(new PropertySimple("heapDumpDir", confDir()));
//        config.put(new PropertySimple("cqlPort", 9595));
//        config.put(new PropertySimple("gossipPort", 9696));

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

        ConfigEditor yamlEditor = new ConfigEditor(cassandraYamlFile);
        yamlEditor.load();

        assertEquals(yamlEditor.getNativeTransportPort(), (Integer) 9595, "Failed to update native_transport_port");
        assertEquals(yamlEditor.getStoragePort(), (Integer) 9696, "Failed to update storage_port");
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

    @Test
    public void disableHeapDumps() throws Exception {
        createDefaultConfig();

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(Configuration.builder()
            .addSimple("heapDumpOnOOMError", false).build());

        configDelegate.updateResourceConfiguration(report);

        Properties properties = loadCassandraJvmProps();

        assertEquals(properties.getProperty("heap_dump_on_OOMError"), "", "Failed to disable property " +
            "[heapDumpOnOOMError]");
    }

    @Test
    public void updateShouldFailWhenMaxHeapSizeIsInvalid() throws Exception {
        createDefaultConfig();

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(Configuration.builder()
            .addSimple("maxHeapSize", "256GB").build());

        configDelegate.updateResourceConfiguration(report);

        assertEquals(report.getStatus(), ConfigurationUpdateStatus.FAILURE, "The configuration update should fail " +
            "when [maxHeapSize] has an invalid value.");
    }

    @Test
    public void updateShouldFailWhenHeapNewSizeIsInvalid() throws Exception {
        createDefaultConfig();

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(Configuration.builder()
            .addSimple("heapNewSize", "25^G").build());

        configDelegate.updateResourceConfiguration(report);

        assertEquals(report.getStatus(), ConfigurationUpdateStatus.FAILURE, "The configuration update should fail " +
            "when [heapNewSize] has an invalid value.");
    }

    @Test
    public void updateShouldFailWhenThreadStackSizeIsInvalid() throws Exception {
        createDefaultConfig();

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(Configuration.builder()
            .addSimple("threadStackSize", "128M").build());

        configDelegate.updateResourceConfiguration(report);

        assertEquals(report.getStatus(), ConfigurationUpdateStatus.FAILURE, "The configuration update should fail " +
            "when [threadStackSize] has an invalid value.");
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
