package org.rhq.cassandra.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * YAML configuration editor, not thread safe.
 *
 * @author John Sanda
 */
public class ConfigEditor {

    private final File configFile;

    private final File backupFile;

    private final Yaml yaml;

    private Map config;

    public ConfigEditor(File cassandraYamlFile)  {
        configFile = cassandraYamlFile;
        backupFile = new File(configFile.getParent(), "." + configFile.getName() + ".bak");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
    }

    public void load() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            config = (Map) yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            throw new ConfigEditorException("Failed to load " + configFile, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void save() {
        createBackup();
        try {
            yaml.dump(config, new FileWriter(configFile));
            backupFile.delete();
            config = null;
        } catch (Exception e) {
            throw new ConfigEditorException("Failed to save changes to " + configFile, e);
        }
    }

    public void restore() {
        try {
            copyFile(backupFile, configFile);
            backupFile.delete();
            config = null;
        } catch (IOException e) {
            throw new ConfigEditorException("Failed to restore " + configFile + " from " + backupFile, e);
        }
    }

    private void createBackup() {
        try {
            copyFile(configFile, backupFile);
        } catch (IOException e) {
            throw new ConfigEditorException("Failed to create " + backupFile, e);
        }
    }

    public File getBackupFile() {
        return backupFile;
    }

    public String getClusterName() {
        return (String) config.get("cluster_name");
    }

    public String getListenAddress() {
        return (String) config.get("listen_address");
    }

    public void setListenAddress(String address) {
        config.put("listen_address", address);
    }

    public String getRpcAddress() {
        return (String) config.get("rpc_address");
    }

    public void setRpcAddress(String address) {
        config.put("rpc_address", address);
    }

    public String getAuthenticator() {
        return (String) config.get("authenticator");
    }

    public String getCommitLogDirectory() {
        return (String) config.get("commitlog_directory");
    }

    public void setCommitLogDirectory(String dir) {
        config.put("commitlog_directory", dir);
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

    public void setSavedCachesDirectory(String dir) {
        config.put("saved_caches_directory", dir);
    }

    public void setSeeds(String... seeds) {
        List seedProviderList = (List) config.get("seed_provider");
        Map seedProvider = (Map) seedProviderList.get(0);
        List paramsList = (List) seedProvider.get("parameters");
        Map params = (Map) paramsList.get(0);

        StringBuilder seedsString = new StringBuilder();
        for (int i = 0; i < seeds.length; i++) {
            if (i > 0) {
                seedsString.append(",");
            }

            seedsString.append(seeds[i]);
        }
        params.put("seeds", seedsString.toString());
    }

    public Integer getNativeTransportPort() {
        return (Integer) config.get("native_transport_port");
    }

    public void setNativeTransportPort(Integer port) {
        config.put("native_transport_port", port);
    }

    public Integer getStoragePort() {
        return (Integer) config.get("storage_port");
    }

    public void setStoragePort(Integer port) {
        config.put("storage_port", port);
    }

    public String getInternodeAuthenticator() {
        return (String) config.get("internode_authenticator");
    }

    public void setInternodeAuthenticator(String clazz) {
        config.put("internode_authenticator", clazz);
    }

    public static void copyFile(File inFile, File outFile) throws FileNotFoundException, IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(inFile));
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

        int bufferSize = 32768;
        try {
            is = new BufferedInputStream(is, bufferSize);
            byte[] buffer = new byte[bufferSize];
            for (int bytesRead = is.read(buffer); bytesRead != -1; bytesRead = is.read(buffer)) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        } finally {
            os.close();
            is.close();
        }
    }

}
