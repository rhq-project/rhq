/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * File(s) that should be ignored (e.g. server/default/work/**) by the enclosing rhq:deployment task. The files are
 * specified by nested includes-only filesets, whose 'dir' attributes must specify relative paths (relative to the
 * ${rhq.deploy.dir}).
 *
 * @author Ian Springer
 */
public class IgnoreType extends AbstractBundleType {
    private List<FileSet> fileSets = new ArrayList<FileSet>();

    public void addConfigured(FileSet fileSet) {
        File dir = fileSet.getDir();
        if (dir != null && dir.isAbsolute()) {
            throw new BuildException("The 'dir' attribute on the rhq:ignore type must be a relative path (relative to the ${rhq.deploy.dir}).");
        }
        this.fileSets.add(fileSet);
    }

    public List<FileSet> getFileSets() {
        return this.fileSets;
    }
}
