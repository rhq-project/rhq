package org.rhq.plugins.apache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

public class ApachePluginLifecycleListener implements PluginLifecycleListener {

    private static final Log log = LogFactory.getLog(ApachePluginLifecycleListener.class);
    private static final String LENS_NAME = "httpd.aug";

    protected String dataPath;

    public void initialize(PluginContext context) throws Exception {
        try {
            File tempDirectory = context.getDataDirectory();
            if (!tempDirectory.exists()) {
                if (!tempDirectory.mkdir()) {
                    log.error("Failed to create a temporary folder for augeas lens.");
                }
            }

            //we need to have the path to temp directory because we need to delete these files in the end
            //and shutdown has not param PluginContext
            dataPath = tempDirectory.getAbsolutePath();
            copyTheLens(dataPath);

        } catch (Exception e) {
            log.error("Copy of augeas lens to temporary folder failed.", e);
        }
    }

    public void shutdown() {
        File tempDirectory = new File(dataPath);
        File[] files = tempDirectory.listFiles();
        for (File file : files) {
            if (file.getName().matches(".*.aug")) {
                if (!file.delete()) {
                    log.error("Failed to delete augeas lens: " + file.getAbsolutePath());
                }
            }
        }
    }

    public void copyFile(InputStream in, File destination) throws Exception {

        if (!destination.canWrite()) {
            throw new Exception("Creating of temporary file for lens failed. Destination file "
                    + destination.getAbsolutePath() + " is not accessible.");
        }

        OutputStream out = new FileOutputStream(destination);
        try {
            byte[] buf = new byte[1024];
            int length;

            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        } finally {
            out.close();
        }
    }

    public void copyTheLens(String tempDirectory) throws Exception {
        URL url = this.getClass().getClassLoader().getResource(LENS_NAME);
        String tempFile = url.getFile();
        File file = new File(tempFile);
        String modName = Character.toLowerCase(file.getName().charAt(0)) + file.getName().substring(1);

        File destinationFile = new File(tempDirectory, modName);
        if (!destinationFile.exists()) {
            if (destinationFile.createNewFile()) {
                InputStream input = this.getClass().getClassLoader().getResourceAsStream(LENS_NAME);
                try {
                    copyFile(input, destinationFile);
                } finally {
                    input.close();
                }
            }
        }
    }
}
    


