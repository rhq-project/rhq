package org.rhq.plugins.apache.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApacheConfigWriter {
    
    public ApacheConfigWriter(ApacheDirectiveTree tree){
   
    }
    
    private void findUpdated(ApacheDirective dir,List<ApacheDirective> updatedNodes){
        if (dir.isUpdated()){
            updatedNodes.add(dir);
        }
        for (ApacheDirective directive : dir.getChildDirectives()){
            findUpdated(directive,updatedNodes);
        }
    }
    
    private Set<String> findUpdatedFiles(List<ApacheDirective> updatedNodes){
        Set<String> updatedFiles = new HashSet<String>();
        for (ApacheDirective dir : updatedNodes){
           updatedFiles.contains(dir.getFile());
        }
        return updatedFiles;
      }
    
    public boolean save(){
        return true;
    }
    
    private ApacheDirective findFirstFileDirective(ApacheDirective node,String file){
        if (node.getFile().equals(file)){
            return node.getParentNode();
        }
        for (ApacheDirective dir : node.getChildDirectives()){
          ApacheDirective dd = findFirstFileDirective(dir, file);
          if (dd!=null)
              return dd.getParentNode();
        }
        return null;
    }
    
    public void saveFile(String file,ApacheDirective dir) throws Exception{
        
        File fl = new File(file);
        if (!fl.exists())
            fl.createNewFile();
        
        OutputStream str = new FileOutputStream(fl);
        for (ApacheDirective d : dir.getChildDirectives()){
            if (dir.getFile().equals(file)){
                writeToFile(str,dir,file);
            }
        }
    }
    
    private void writeToFile(OutputStream str,ApacheDirective dir,String file) throws Exception{
         if (dir.getFile().equals(file)){                           
                 str.write(dir.getText().getBytes());
                 if (dir.isNested()){
                 for (ApacheDirective tempDir : dir.getChildDirectives()){
                     writeToFile(str, tempDir, file);
                 }
                 str.write(("</"+dir.getName()+">").getBytes());
                 }
             }else
               return;
         }  
}
