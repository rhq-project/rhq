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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * Defines a task that can take any number of file sets and file lists.
 */
public abstract class BaseFileSetTask extends Task {
    private static final Msg MSG = DbAntI18NFactory.getMsg();

    private List<Object> m_fileSets = new ArrayList<Object>();

    /**
     * Adds the given file set to this task.
     *
     * @param set
     */
    public void addFileset(FileSet set) {
        m_fileSets.add(set);
    }

    /**
     * Adds the given file list to this task.
     *
     * @param list
     */
    public void addFilelist(FileList list) {
        m_fileSets.add(list);
    }

    /**
     * Makes sure at least one file set/list was added.
     *
     * @throws BuildException
     */
    protected void validateAttributes() throws BuildException {
        if (m_fileSets.size() == 0) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.BASEFILESET_NEED_A_FILE));
        }
    }

    /**
     * Returns all specified file sets.
     *
     * @return the list of files that this task should work with
     */
    protected List<SimpleFileSet> getAllFileSets() {
        List<SimpleFileSet> sets = new ArrayList<SimpleFileSet>();

        // Iterate the FileSet collection.
        for (Object obj : m_fileSets) {
            if (obj instanceof FileSet) {
                sets.add(new SimpleFileSet(getProject(), (FileSet) obj));
            } else {
                sets.add(new SimpleFileSet(getProject(), (FileList) obj));
            }
        }

        return sets;
    }

    /**
     * Returns a list of all files in all sets.
     *
     * @return list of all files
     */
    protected List<File> getAllFiles() {
        Set<File> files = new HashSet<File>();
        List<SimpleFileSet> all_file_sets = getAllFileSets();

        for (SimpleFileSet file_set : all_file_sets) {
            // Create a list of absolute paths for the src files
            int len = file_set.files.length;

            for (int i = 0; i < len; i++) {
                File current = new File(file_set.baseDir, file_set.files[i]);

                // Make sure the file exists. This will rarely fail when using file sets,
                // but it could be rather common when using file lists.
                if (!current.exists()) {
                    log(MSG.getMsg(DbAntI18NResourceKeys.BASEFILESET_FILE_DOES_NOT_EXIST, current), Project.MSG_WARN);
                } else {
                    files.add(current);
                }
            }
        }

        // We need to return a list, not a set
        List<File> fileList = new ArrayList<File>();
        fileList.addAll(files);
        return fileList;
    }
}