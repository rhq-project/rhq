package org.rhq.cassandra.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.rhq.core.util.StringUtil;
import org.rhq.core.util.file.FileUtil;

/**
 * @author John Sanda
 */
public class ConfigEditor {

    private File configFile;

    private File backupFile;

    private Yaml yaml;

    private Map config;

    public ConfigEditor(File cassandraYamlFile)  {
        configFile = cassandraYamlFile;
    }

    public void load() {
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yaml = new Yaml(options);
            config = (Map) yaml.load(new FileInputStream(configFile));
            createBackup();
        } catch (FileNotFoundException e) {
            throw new ConfigEditorException("Failed to load " + configFile, e);
        }
    }

    public void save() {
        try {
            yaml.dump(config, new FileWriter(configFile));
            backupFile.delete();
            yaml = null;
            config = null;
            backupFile = null;
        } catch (Exception e) {
            throw new ConfigEditorException("Failed to save changes to " + configFile, e);
        }
    }

    public void restore() {
        try {
            FileUtil.copyFile(backupFile, configFile);
            backupFile.delete();
            yaml = null;
            config = null;
            backupFile = null;
        } catch (IOException e) {
            throw new ConfigEditorException("Failed to restore " + configFile + " from " + backupFile, e);
        }
    }

    private void createBackup() {
        backupFile = new File(configFile.getParent(), "." + configFile.getName() + ".bak");
        try {
            FileUtil.copyFile(configFile, backupFile);
        } catch (IOException e) {
            throw new ConfigEditorException("Failed to create " + backupFile, e);
        }
    }

    public File getBackupFile() {
        return backupFile;
    }

    public String getCommitLogDirectory() {
        return (String) config.get("commitlog_directory");
    }

    public List<String> getDataFileDirectories() {
        return (List<String>) config.get("data_file_directories");
    }

    public void setDataFileDirectories(List<String> dirs) {
        config.put("data_file_directories", dirs);
    }

    public String getSavedCachesDirectory() {
        return (String) config.get("saved_caches_directory");
    }

    public void setSeeds(String... seeds) {
        List seedProviderList = (List) config.get("seed_provider");
        Map seedProvider = (Map) seedProviderList.get(0);
        List paramsList = (List) seedProvider.get("parameters");
        Map params = (Map) paramsList.get(0);
        params.put("seeds", StringUtil.arrayToString(seeds));
    }

    public Integer getNativeTransportPort() {
        return (Integer) config.get("native_transport_port");
    }

    public void setNativeTransportPort(int port) {
        config.put("native_transport_port", port);
    }

    public Integer getStoragePort() {
        return (Integer) config.get("storage_port");
    }

    public void setStoragePort(int port) {
        config.put("storage_port", port);
    }

}
