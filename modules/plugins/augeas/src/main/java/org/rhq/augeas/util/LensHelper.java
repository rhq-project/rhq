package org.rhq.augeas.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LensHelper {
	 public static final String TEMP_DIRECTORY = "java.io.tmpdir"; 
	 public static final String TEMP_FILE_SUFFIX = ".aug";
		
	
	 public static String getLensPath(String param) throws IOException,Exception{
	    	String lensPath=null;
	    		    	
	    	if (param.indexOf(File.separatorChar)==-1){    	   
	    	  
	    	}else
	    		lensPath = param;
	    	return lensPath;
	    	
	    }
	 
	 public static void copyFile(InputStream in,File destination) throws Exception{
        
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
	 
	 public static File createTempDir(String name) throws IOException{

	   String tempDir = (String)System.getProperties().get(TEMP_DIRECTORY);
  	   
	   File tempDirectory = new File(tempDir);
  	   File [] lens = tempDirectory.listFiles(new LensFilter(name));
  	   
  	   File lensDirectory;
  	   
  	   if (lens.length==0){	    		  
  	     File tempFile = File.createTempFile(name, "");
  	     String nm = tempFile.getName();
  	     tempFile.delete();
  	     lensDirectory = new File(tempDirectory,nm);
  	     lensDirectory.mkdir();
  	     lensDirectory.deleteOnExit(); 	    	  
  	   }else{
  		   lensDirectory = lens[0];
  	   }
  	   
  	   return lensDirectory;
	      
	 }
	 
	 public static File cpFileFromPluginToTemp(File tempDirectory,String fileName) throws IOException,Exception{
	    	
	    File destinationFile = new File(tempDirectory,fileName);
	    if (!destinationFile.exists())
	       {
	    	destinationFile.createNewFile();
	    	InputStream input  = tempDirectory.getClass().getClassLoader().getResourceAsStream(fileName);
	    	copyFile(input, destinationFile);
	    	}
	    
	    return destinationFile;
	 }
	 
}
