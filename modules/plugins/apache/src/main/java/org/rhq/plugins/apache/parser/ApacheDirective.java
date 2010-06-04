package org.rhq.plugins.apache.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApacheDirective {

    private String name;
    private List<String> values;
    private boolean isNested;
    private boolean isRootNode;
    private boolean isComment;
    private static final String WS = "[ \t]*";
    private static final String WORD = "\"(?:[^\"\n]|\\\")*\"|'(?:[^'\n]|\\\')*'|[^'\" \t\n]+";
    private static final String DIRECTIVE_PATTERN =  WS + "(" + WORD + ")" + WS;
    private static final String COMMENT_PATTERN="^[\t ]*#.*+$";
    private boolean updated=false;
  
    private final Pattern directivePattern = Pattern.compile(DIRECTIVE_PATTERN);
    private final Pattern commentPattern = Pattern.compile(COMMENT_PATTERN);
    private List<ApacheDirective> childNodes;
    private ApacheDirective parentNode;
    private String file;
    
    public ApacheDirective(){
        values = new ArrayList<String>();
        childNodes = new ArrayList<ApacheDirective>();
    }
    
    public ApacheDirective(String directive){
        values = new ArrayList<String>();
        childNodes = new ArrayList<ApacheDirective>();
        
        Matcher matcher = commentPattern.matcher(directive);
        if (matcher.matches()){
            isComment = true;
            values.add(directive);
            name = "#";
        }else{         
        int startIndex = 0;
        boolean updated = true;
        while (updated & startIndex < directive.length()){
            updated = false;          
                Matcher m = directivePattern.matcher(directive);
                while (m.find(startIndex)) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String val = m.group(i);
                        values.add(val);
                    }
                    updated = true;
                    startIndex = m.end();
                }
            }
        if (values.isEmpty())
            throw new RuntimeException("Directive "+directive+"is not in valid format.");
            
        String lastVal =  values.get(values.size()-1);
        if (lastVal.endsWith(">")){
            lastVal = lastVal.substring(0,lastVal.length()-1);
            values.set(values.size()-1, lastVal);
        }
        
        name = values.get(0);
        values.remove(0);
        }
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public boolean isNested() {
        return isNested;
    }

    public void setNested(boolean isNested) {
        this.isNested = isNested;
    }
    
    public String getValuesAsString(){
        StringBuilder buf = new StringBuilder();
        for (String val : values){
            buf.append(val);
        }
        return buf.toString();
    }
    
    public void addChildDirective(ApacheDirective directive){
        childNodes.add(directive);
    }
    
    public List<ApacheDirective> getChildDirectives(){
        return childNodes;
    }
    
    public List<ApacheDirective> getChildByName(String name){
        List<ApacheDirective> kids = new ArrayList<ApacheDirective>();
        for (ApacheDirective dir : childNodes){
           if (dir.getName().equals(name)){
             kids.add(dir);             
           }            
        }
        return kids;
    }
    
    public ApacheDirective getParentNode(){
        return parentNode;
    }
    
    public void setParentNode(ApacheDirective parent){
     this.parentNode = parent;
    }

    public boolean isRootNode() {
        return isRootNode;
    }

    public void setRootNode(boolean isRootNode) {
        this.isRootNode = isRootNode;
    }
    
    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
    
    public String getFile() {
        if (isRootNode)
            return null;
        
        if (file == null){
            List<ApacheDirective> dir = parentNode.getChildByName(name);
            for (int i=0;i<dir.size();i++)
                if (dir.get(i) == this){
                    file = dir.get(i-1).getFile();
                    break;
                }
            }        
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }    
    
    public void removeChilDirective(ApacheDirective dir){
        if (childNodes.contains(dir))
            childNodes.remove(dir);        
    }
    
    public void remove(){
        parentNode.removeChilDirective(this);
    }
    
    public String getText(){
      StringBuilder builder = new StringBuilder();
      builder.append(name+" ");
      for (String temp : values){
          builder.append(temp+" ");
      }
      builder.deleteCharAt(builder.length()-1);
      if (isNested)
          builder.append(">");
    
    return builder.toString();
    }
    
    public int getSeq(){
      List<ApacheDirective> directives = parentNode.getChildByName(name);
       for (int i=0;i<directives.size();i++){
         if (directives.get(i)==this){
          return i;
          }   
       }
       return 0;
    }
    
    public void addValue(String val){
        values.add(val);
    }
}
