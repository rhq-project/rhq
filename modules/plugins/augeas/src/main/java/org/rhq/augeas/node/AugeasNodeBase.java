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

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.tree.AugeasTree;

/**
 * Base class to store the data of nodes.
 * Inherited by {@link AugeasNodeLazy}, {@link AugeasNodeReal}
 * and {@link AugeasRootNode}.
 * 
 * @author Filip Drabek
 *
 */
public abstract class AugeasNodeBase {

    protected String path;
    protected String label;
    protected String value;
    protected AugeasTree ag;
    protected int seq;
    protected AugeasNode parentNode;
    protected List<AugeasNode> childNodes;

    protected AugeasNodeBase() {
        childNodes = new ArrayList<AugeasNode>();
    }

    public String getPath() {
        return path;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public AugeasNode getParentNode() {
        return parentNode;
    }

    public List<AugeasNode> getChildNodes() {
        return childNodes;
    }
}
