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

import java.util.List;
/**
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasNode {

       public String getPath();

       public void setPath(String path) throws Exception;
       
       public String getLabel() ;

       public void setLabel(String label);

       public String getValue();

       public void setValue(String value) ;

       public int getSeq();

       public void setSeq(int seq);

       public AugeasNode getParentNode();

       public List<AugeasNode> getChildNodes() ;
       
       public boolean equals(Object obj);
       
       public String getFullPath();
       
       public void addChildNode(AugeasNode node);
       
       public List<AugeasNode> getChildByLabel(String labelName);
       
       public void remove(boolean updateSeq) throws Exception;
       
       public void updateFromParent();
}
