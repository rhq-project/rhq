package org.rhq.plugins.apache.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ApacheConfigWriter {
    
    private Map<String,OutputStream> streams;
    
    public ApacheConfigWriter(ApacheDirectiveTree tree){
       streams = new HashMap<String,OutputStream>();
    }
    
    public boolean save(ApacheDirective dir){
        try {
            if (dir.isRootNode()){
                for (ApacheDirective directive : dir.getChildDirectives())
                    writeToFile(directive);
            }else
             writeToFile(dir);
            for (OutputStream stream : streams.values()){
                stream.flush();
                stream.close();
            }
        }catch(Exception e){
            return false;
        }finally{
            try {
              for (OutputStream stream : streams.values()){
                   stream.flush();
                   stream.close();
                }
            } catch(IOException ee){
                return false;
              }
            }
        
        return true;
    }
        
    private void writeToFile(ApacheDirective dir) throws Exception{
         String fileName = dir.getFile();
         OutputStream str;

         if (!streams.containsKey(fileName)){
             File fl = new File(fileName);
             if (!fl.exists())
                 fl.createNewFile();
             
             str = new FileOutputStream(fl);
             streams.put(fileName, str);
         }else
             str = streams.get(fileName);
                                          
         if (dir.isNested()){
             str.write((dir.getText()+">"+'\n').getBytes());
            for (ApacheDirective tempDir : dir.getChildDirectives()){
               writeToFile(tempDir);
                 }
             str.write(("</"+dir.getName()+">"+'\n').getBytes());
         }else
             str.write((dir.getText()+'\n').getBytes());
         
   }  
}
