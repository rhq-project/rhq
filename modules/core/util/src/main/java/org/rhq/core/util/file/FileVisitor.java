/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.util.file;

import java.io.File;

/**
 * A callback interface that is used with {@link FileUtil#forEachFile(java.io.File, FileVisitor)}
 * to iterate over files in a directory tree.
 */
public interface FileVisitor {

    /**
     * Invoked for each file in a directory tree.
     *
     * @param file The current in the iteration
     */
    void visit(File file);

}
