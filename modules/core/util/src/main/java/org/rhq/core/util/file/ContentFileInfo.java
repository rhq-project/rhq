/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.util.file;

import java.io.File;

/**
 * Subclasses are responsible for examining a file to determine information
 * about its content - like its version string and a description of the content.
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public abstract class ContentFileInfo {

    private final File file;

    /**
     * The constructor that sets the file that this object will examine.
     * 
     * @param file the file whose info is managed by this object
     */
    public ContentFileInfo(File file) {
        this.file = file;
    }

    /**
     * Get the file that this object examines.
     *  
     * @return the file
     */
    public File getContentFile() {
        return this.file;
    }

    /**
     * Determines if the {@link #getContentFile() file} can be processed
     * by the implementation. In other words, if the implementation of this
     * interface can properly parse the file content to determine
     * its {@link #getVersion() version} (or other information),
     * this returns <code>true</code>. If the file is of an unknown
     * content type or is not valid for some reason, <code>false</code> is returned.
     * 
     * @return <code>true</code> if this object can process the content
     *         to determine information about it
     */
    public abstract boolean isValid();

    /**
     * Returns an appropriate version of the file content.
     * If the implementation cannot determine what version the content
     * is, then the default is returned.
     * 
     * @param defaultValue the version string if it cannot be determined
     * @return the content version, or the default value if it cannot be determined
     */
    public abstract String getVersion(String defaultValue);

    /**
     * Returns an appropriate description for the file content.
     * If the implementation cannot determine an appropriate
     * description, then the default value will be returned.
     * 
     * @param defaultValue the version string if it cannot be determined
     * @return the description of the content, or the default value if it cannot be determined
     */
    public abstract String getDescription(String defaultValue);
}
