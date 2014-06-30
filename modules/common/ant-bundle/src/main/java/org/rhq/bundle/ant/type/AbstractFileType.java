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

import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * A base class for the functionality shared by {@link FileType} and {@link ArchiveType}.
 *
 * @author Ian Springer
 */
public abstract class AbstractFileType extends AbstractBundleType {
    private String name;
    private File source;

    // TODO: We currently do not call this method. Do we want to or should we just let the Deployer utility handle
    //       validation of specified files?
    public void init() throws BuildException {
        if (!this.source.exists()) {
            throw new BuildException("File path specified by 'name' attribute (" + this.source + ") does not exist.");
        }
        if (this.source.isDirectory()) {
            throw new BuildException("File path specified by 'name' attribute (" + this.source
                + ") is a directory - it must be a regular file.");
        }
    }

    public File getSource() {
        return this.source;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        File file = new File(name);
        if (file.isAbsolute()) {
            throw new BuildException("Path specified by 'name' attribute (" + name
                + ") is not relative - it must be a relative path, relative to the Ant basedir.");
        }
        this.source = getProject().resolveFile(name);
    }

    public void setSource(File source) {
        this.source = source;
    }
}
