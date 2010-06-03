package org.rhq.plugins.apache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.pluginapi.plugin.PluginContext;
import org.rhq.core.pluginapi.plugin.PluginLifecycleListener;

public class ApachePluginLifecycleListener implements PluginLifecycleListener{
    
    private static final Log log = LogFactory.getLog(ApachePluginLifecycleListener.class);
    protected String dataPath;
    private static String LENS_NAME="httpd.aug";
    
    public void initialize(PluginContext context) throws Exception {
        try {
       File tempDirectory = context.getDataDirectory();    
       tempDirectory.mkdir();
     
       //we need to have the path to temp directory because we need to delete these files in the end 
       //and shutdown has not param PluginContext
       dataPath = tempDirectory.getAbsolutePath();
       copyTheLens(dataPath);
        
        }catch(Exception e){
            log.error("Copy of augeas lens to temporary folder failed.",e);
        }
    }
    
    public void shutdown() {        
        File tempDirectory = new File(dataPath);
        File [] files  = tempDirectory.listFiles();
        for (File file : files){
            if (file.getName().matches(".*.aug")){
                file.delete();
            }
        }
    }
    
    private File cpFileFromPluginToTemp(ClassLoader loader,File tempDirectory,String fileName) throws IOException,Exception{        
        File destinationFile = new File(tempDirectory,fileName);
        if (!destinationFile.exists())
           {
            destinationFile.createNewFile();
            
            InputStream input  = loader.getResourceAsStream(fileName);
            copyFile(input, destinationFile);
            }
        
        return destinationFile;
     }
    
    public void copyFile(InputStream in,File destination) throws Exception{
        
        if (!destination.canWrite())
           throw new Exception("Creating of temporary file for lens failed. Destination file "
                   + destination.getAbsolutePath()+" is not accessible.");

       OutputStream out = new FileOutputStream(destination);
   
       byte[] buf = new byte[1024];
       int length;

       while ((length = in.read(buf)) > 0) {
           out.write(buf, 0, length);
       }
       
       in.close();
       out.close();

    }

    public void copyTheLens(String tempDirectory) throws Exception{
       URL url = this.getClass().getClassLoader().getResource(LENS_NAME);
       String tempFile = url.getFile();
       File file = new File(tempFile);           
       String modName = Character.toLowerCase(file.getName().charAt(0))+file.getName().substring(1);
      
       File destinationFile = new File(tempDirectory,modName);
       if (!destinationFile.exists())
          {
           destinationFile.createNewFile();
           
           InputStream input  = this.getClass().getClassLoader().getResourceAsStream(LENS_NAME);
           copyFile(input, destinationFile);
           }
       }
 }
    


