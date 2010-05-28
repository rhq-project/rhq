package org.rhq.plugins.apache.parser;

import java.util.List;

public class ApacheTest {

    public static void main(String[] args) {        
        String path = "/home/fdrabek/html/httpd.conf";
        String serverRoot = "/home/fdrabek/html/";
        try {
               
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree,serverRoot);
        ApacheConfigReader.buildTree(path, parser);
        List<ApacheDirective> dir = tree.search("/<Directory/Options");
        for (ApacheDirective d : dir)
          System.out.println(d.getName());
        
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static void print(ApacheDirective dir){
        System.out.println(dir.getName());
        for (ApacheDirective dr : dir.getChildDirectives()){
            print(dr);
        }
    }

}
