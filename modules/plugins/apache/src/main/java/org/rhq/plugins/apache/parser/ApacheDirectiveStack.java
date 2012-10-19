package org.rhq.plugins.apache.parser;

import java.util.ArrayList;
import java.util.List;

public class ApacheDirectiveStack {

    protected List<ApacheDirective> stack;
    
    public ApacheDirectiveStack(){
        stack= new ArrayList<ApacheDirective>();
    }
    
    public void addDirective(ApacheDirective dir){
        stack.add(dir);
    }
    
    public ApacheDirective getLastDirective(){
        return stack.get(stack.size()-1);
    }
    
    public void removeLastDirective(){
        stack.remove(stack.size()-1);
    }
    
    public ApacheDirectiveStack copy(){
        ApacheDirectiveStack st = new ApacheDirectiveStack();
        for (ApacheDirective dir : stack){
            st.addDirective(dir);
        }
        
        return st;
    }
    
    public boolean isEmpty(){
        if (stack.size()==0)
            return true;
        else
            return false;
    }
}
