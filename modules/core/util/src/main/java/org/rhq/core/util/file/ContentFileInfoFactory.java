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
import java.util.ArrayList;
import java.util.List;

public class ContentFileInfoFactory {
    /**
     * This static list contains the different types of {@link ContentFileInfo} types
     * that this factory can process. 
     *
     * In the future, we can provide an API for clients to add their own types,
     * but we have to be careful we don't leak memory by adding types that are
     * loaded in some other classloader that might later get destroyed.
     */
    private static final List<Class<? extends ContentFileInfo>> contentFileInfoTypes;
    static {
        contentFileInfoTypes = new ArrayList<Class<? extends ContentFileInfo>>();
        contentFileInfoTypes.add(JarContentFileInfo.class);
        // ... add more here, e.g. RpmContentFileInfo...
    }

    /**
     * The factory method that creates a {@link ContentFileInfo} object for the given file.
     * 
     * @param file
     * @return the object that can provide information on the given file
     * @throws IllegalArgumentException if there is no known {@link ContentFileInfo} type
     *                                  that is able to process the file.
     */
    public static ContentFileInfo createContentFileInfo(File file) {
        ContentFileInfo fileInfo;

        for (Class<? extends ContentFileInfo> clazz : ContentFileInfoFactory.contentFileInfoTypes) {
            try {
                fileInfo = clazz.getConstructor(File.class).newInstance(file);
                if (fileInfo.isValid()) {
                    return fileInfo;
                }
            } catch (Exception ignore) {
                // this should never happen, all content file info's have the constructor we want
            }
        }

        // this file is not a valid kind of file that we know about, just use the generic implementation
        fileInfo = new GenericContentFileInfo(file);
        if (!fileInfo.isValid()) {
            throw new IllegalArgumentException("Cannot get info from file [" + file + "]. Does it exist?");
        }

        return fileInfo;
    }
}
