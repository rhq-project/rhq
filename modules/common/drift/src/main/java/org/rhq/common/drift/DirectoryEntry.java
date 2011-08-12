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

package org.rhq.common.drift;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DirectoryEntry implements Iterable<FileEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private String directory;

    private List<FileEntry> files = new ArrayList<FileEntry>();

    public DirectoryEntry(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }

    public DirectoryEntry add(FileEntry entry) {
        files.add(entry);
        return this;
    }

    public DirectoryEntry remove(FileEntry entry) {
        FileEntry entryToRemove = null;
        int i = 0;
        for (FileEntry fileEntry : files) {
            if (entry.getFile().equals(fileEntry.getFile())) {
                entryToRemove = fileEntry;
                break;
            }
            ++i;
        }
        if (entryToRemove != null) {
            files.remove(i);
        }
        return this;
    }

    public int getNumberOfFiles() {
        return files.size();
    }

    public Iterator<FileEntry> iterator() {
        return files.iterator();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[directory: " + directory + "]";
    }
}
