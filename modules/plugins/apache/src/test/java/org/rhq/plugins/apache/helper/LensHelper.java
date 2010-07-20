/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.apache.helper;

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
     
     public static String getTempDirectoryPath(){
           return (String)System.getProperties().get(TEMP_DIRECTORY);
     }
     
     public static File cpFileFromPluginToTemp(ClassLoader loader,File tempDirectory,String fileName) throws IOException,Exception{
            
        File destinationFile = new File(tempDirectory,fileName);
        if (!destinationFile.exists())
           {
            destinationFile.createNewFile();
            
            InputStream input  = loader.getResourceAsStream(fileName);
            copyFile(input, destinationFile);
            }
        
        return destinationFile;
     }
     
}