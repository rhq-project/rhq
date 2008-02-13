/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.db.ant.dbsetup;

import java.io.File;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;

/**
 * Maintains a simple set of files.
 */
public class SimpleFileSet {
    /**
     * The base directory where the files are located.
     */
    public File baseDir;

    /**
     * The names of the files in the set.
     */
    public String[] files;

    /**
     * Creates a new {@link SimpleFileSet} object given a file set.
     *
     * @param  project
     * @param  file_set
     *
     * @throws IllegalArgumentException
     */
    public SimpleFileSet(Project project, FileSet file_set) {
        init(project, file_set);
    }

    /**
     * Creates a new {@link SimpleFileSet} object given a file list.
     *
     * @param  project
     * @param  file_list
     *
     * @throws IllegalArgumentException
     */
    public SimpleFileSet(Project project, FileList file_list) {
        init(project, file_list);
    }

    /**
     * Initializes this object with the given file set.
     *
     * @param project
     * @param fileSet
     */
    public void init(Project project, FileSet fileSet) {
        DirectoryScanner scanner = fileSet.getDirectoryScanner(project);
        baseDir = fileSet.getDir(project);
        files = scanner.getIncludedFiles();
    }

    /**
     * Initializes this object with the given file set.
     *
     * @param project
     * @param fileList
     */
    public void init(Project project, FileList fileList) {
        baseDir = fileList.getDir(project);
        files = fileList.getFiles(project);
    }
}