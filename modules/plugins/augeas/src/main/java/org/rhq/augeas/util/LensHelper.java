package org.rhq.augeas.util;

import java.io.File;
import java.io.FileInputStream;
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
	    	   String tempDir = (String)System.getProperties().get(TEMP_DIRECTORY);
	    	   File tempDirectory = new File(tempDir);
	    	   File [] lens = tempDirectory.listFiles(new LensFilter(param));
	    	   File sourceFile = new File(param);
	    	   
	    	   File tempFile;
	    	   
	    	   if (lens.length==0){	    		  
	    	     tempFile = File.createTempFile(param, ".aug");
	    	     tempFile.deleteOnExit();
                 copyFile(sourceFile,tempFile); 	    	  
	    	   }else{
	    		   tempFile = lens[0];
	    	   }
	  	      lensPath = tempFile.getAbsolutePath();
	    	}else
	    		lensPath = param;
	    	return lensPath;
	    	
	    }
	 
	 private static void copyFile(File source,File destination) throws Exception{
        
		 if (!destination.canWrite())
        	throw new Exception("Creating of temporary file for lens failed. Destination file "
        			+ destination.getAbsolutePath()+" is not accessible.");

        if (!source.canRead())
        	throw new Exception("Creating of temporary file for lens failed. Destination file "
        			+ destination.getAbsolutePath()+" is not accessible.");

        
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(destination);
    
        byte[] buf = new byte[1024];
        int length;
 
        while ((length = in.read(buf)) > 0) {
            out.write(buf, 0, length);
        }
        
        in.close();
        out.close();

	 }
	 
}
