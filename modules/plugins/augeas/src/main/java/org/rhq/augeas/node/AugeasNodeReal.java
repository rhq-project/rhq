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

package org.rhq.augeas.node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.rhq.augeas.tree.AugeasTree;
/**
 * 
 * @author Filip Drabek
 *
 */
public class AugeasNodeReal extends AugeasNodeParent implements AugeasNode{

       public AugeasNodeReal()
       {
              super();
       }
       
       public AugeasNodeReal(AugeasNode parentNode,AugeasTree tree,String label,int seq)
       {
              super();
              ag = tree;
              this.parentNode = parentNode;
              path = parentNode.getPath()+ File.separatorChar + parentNode.getLabel() + File.separatorChar;
              this.seq = seq;
       }
       
       public AugeasNodeReal(AugeasNode parentNode,AugeasTree ag,String fullPath)
       {
              super();
              this.parentNode = parentNode;
              if (fullPath == null)
                     return ;
              
               this.path = fullPath.substring(0,fullPath.lastIndexOf(File.separatorChar)+1);
               String val = fullPath.substring(fullPath.lastIndexOf(File.separatorChar)+1,fullPath.length());
                  
               int firstB = val.indexOf("[");
               
               if (firstB != -1){                     
                    seq = Integer.valueOf(val.substring(firstB+1,val.indexOf(']')));
                    label = val.substring(0,firstB);
                  }
               else{
                          seq = 0;
                         label = fullPath.substring(fullPath.lastIndexOf(File.separatorChar)+1);
                  }    
       }
       
       public AugeasNodeReal(String path,String label,int seq,String value)
       {
              childNodes = new ArrayList<AugeasNode>();
              
              if (path == null)
                     return ;
              
              this.path = path;
              this.value = value;
              this.label = label;
              this.seq = seq;
       }

       
        public boolean equals(Object obj) {
               if (this == obj)
                   return true;
               if (obj == null || getClass() != obj.getClass())
                   return false;

               AugeasNode that = (AugeasNode) obj;

               if (!this.getFullPath().equals(that.getFullPath()))
                             return false;

               return true;
           }

       
       public String getFullPath() {
              return path + label + (seq !=0 ? "["+String.valueOf(seq)+"]" : "" );
       }
       
       public void addChildNode(AugeasNode node) {
              //TODO kontrola jestli sem patri
              childNodes.add(node);
       }
       
       public List<AugeasNode> getChildByLabel(String labelName){
              List<AugeasNode> nodes = getChildNodes();
              List<AugeasNode> tempNode = new ArrayList<AugeasNode>();

              for (AugeasNode node : nodes){
                     if (node.getLabel().equals(labelName))
                            tempNode.add(node);
              }
              return tempNode;
       }
       
       public void remove(boolean updateSeq) throws Exception
       {
              ag.removeNode(this,updateSeq);
       }

       public void setPath(String path) throws Exception{
              int end;
              if (path.lastIndexOf(File.separatorChar) == path.length())
                         {
                          end = path.length()-1;
                         this.path = path.substring(0,path.length()-1);
                         }
                     else
                            {
                            end = path.length();
                            this.path = path;
                            }
              
              AugeasNode parentNode = ag.getNode(path.substring(0,end));
              List<AugeasNode> nodes = parentNode.getChildByLabel(this.label);
              if (!nodes.isEmpty()){
                     int indexes = nodes.size();
                     this.setSeq(indexes+1);
              }
              parentNode.addChildNode(this);
               for (AugeasNode nd : this.getChildNodes())
                 {
                      nd.updateFromParent(); 
                 }
       }

       public void updateFromParent() {
              AugeasNode node = this.getParentNode();
              if (!this.path.equals(node.getFullPath())){
                     this.path = node.getFullPath();
                  }
              
              for (AugeasNode nd : this.getChildNodes())
              {
             nd.updateFromParent();              
              }
       }
}
