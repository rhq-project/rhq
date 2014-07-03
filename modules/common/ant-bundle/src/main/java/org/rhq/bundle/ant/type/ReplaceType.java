/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.bundle.ant.type;

import java.util.ArrayList;
import java.util.List;

/**
 * File(s) that should be replaced (e.g. server/default/*.properties) by the enclosing rhq:deployment task. The files
 * are specified by nested includes-only filesets
 *
 * @author Ian Springer
 */
public class ReplaceType extends AbstractBundleType {
    private List<FileSet> fileSets = new ArrayList<FileSet>();

    @SuppressWarnings("unused")
    public void addConfigured(FileSet fileSet) {
        this.fileSets.add(fileSet);
    }

    public List<FileSet> getFileSets() {
        return this.fileSets;
    }
}
