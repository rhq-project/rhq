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
package org.rhq.core.clientapi.server.plugin.content.util;

import java.io.File;
import java.io.IOException;

import org.rhq.core.domain.util.MD5Generator;

/**
 * Handles any generic file. This is to be used as a fallback if no other
 * more specific {@link ContentFileInfo} implementation exists to handle a specific file.
 * 
 * @author John Mazzitelli
 */
public class GenericContentFileInfo extends ContentFileInfo {

    public GenericContentFileInfo(File file) {
        super(file);
    }

    /**
     * Any file is considered valid if it exists and is readable.
     * 
     * @return <code>true</code> iff the {@link #getContentFile() file} exists and is readable
     */
    public boolean isValid() {
        return getContentFile().exists() && getContentFile().canRead();
    }

    /**
     * If the caller provided a default value, it is used. If the default
     * value is <code>null</code>, this will generate a version string
     * equal to the file's MD5 hashcode. If this fails to calculate the
     * MD5, a runtime exception is thrown.
     * 
     * @param defaultValue the string to return, unless this is <code>null</code>
     * @return the default value if not <code>null</code>, otherwise, the MD5 of the file
     */
    public String getVersion(String defaultValue) {
        if (defaultValue != null) {
            return defaultValue;
        }

        try {
            return MD5Generator.getDigestString(getContentFile());
        } catch (IOException e) {
            throw new RuntimeException("Cannot generate version for file [" + getContentFile() + "]", e);
        }
    }

    /**
     * This method simply returns the default value. There is no description
     * for a generic file that this method can return.
     * 
     * @param defaultValue the value that will be returned
     * @return simply returns the default value
     */
    public String getDescription(String defaultValue) {
        return defaultValue;
    }
}
