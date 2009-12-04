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

package org.rhq.augeas.tree;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
/**
 * 
 * @author Filip Drabek
 *
 */
public interface AugeasTree {
       public void update();
       public void save();
       public AugeasNode getNode(String path) throws AugeasTreeException;
       public List<AugeasNode> match(String expression) throws AugeasTreeException;
       public List<AugeasNode> matchRelative(AugeasNode node,String expression) throws AugeasTreeException;
       public AugeasNode createNode(String fullPath) throws AugeasTreeException;
       public AugeasNode createNode(AugeasNode parentNode,String name ,String value,int seq)throws AugeasTreeException;
       public String get(String expr);
       public AugeasNode getRootNode();
       public void removeNode(AugeasNode node,boolean updateSeq) throws Exception;
       public void setValue(AugeasNode node,String value);
       public String summarizeAugeasError();
       public void setRootNode(AugeasNode node);
}
