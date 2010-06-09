/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.util.updater;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages information about deployment files such as which files
 * have been added, deleted, etc. The calls to these methods
 * don't necessarily come at a time when a file has actually been 
 * added, deleted, etc - these are only called when it has been determined
 * that a file is going to be added, deleted, etc. In other words, don't
 * use this as a realtime listener of events happening during a live
 * deployment; instead, use this as a final report of what happened
 * after a deployment has been completed.
 * 
 * @author John Mazzitelli
 */
public class DeployDifferences {
    private final Set<String> ignoredFiles = new HashSet<String>();
    private final Set<String> deletedFiles = new HashSet<String>();
    private final Set<String> addedFiles = new HashSet<String>();
    private final Set<String> changedFiles = new HashSet<String>();
    private final Map<String, String> backedUpFiles = new HashMap<String, String>();
    private final Map<String, String> restoredFiles = new HashMap<String, String>();
    private final Map<String, String> realizedFiles = new HashMap<String, String>();
    private final Map<String, String> errors = new HashMap<String, String>();
    private boolean cleaned = false;

    public void addIgnoredFile(String path) {
        this.ignoredFiles.add(convertPath(path));
    }

    public void addIgnoredFiles(Collection<String> paths) {
        for (String path : paths) {
            addIgnoredFile(path);
        }
    }

    public void removeIgnoredFile(String path) {
        this.ignoredFiles.remove(convertPath(path));
    }

    public boolean containsIgnoredFile(String path) {
        return this.ignoredFiles.contains(convertPath(path));
    }

    public void addDeletedFile(String path) {
        this.deletedFiles.add(convertPath(path));
    }

    public void addDeletedFiles(Collection<String> paths) {
        for (String path : paths) {
            addDeletedFile(path);
        }
    }

    public void removeDeletedFile(String path) {
        this.deletedFiles.remove(convertPath(path));
    }

    public boolean containsDeletedFile(String path) {
        return this.deletedFiles.contains(convertPath(path));
    }

    public void addAddedFile(String path) {
        this.addedFiles.add(convertPath(path));
    }

    public void addAddedFiles(Collection<String> paths) {
        for (String path : paths) {
            addAddedFile(path);
        }
    }

    public void removeAddedFile(String path) {
        this.addedFiles.remove(convertPath(path));
    }

    public boolean containsAddedFile(String path) {
        return this.addedFiles.contains(convertPath(path));
    }

    public void addChangedFile(String path) {
        this.changedFiles.add(convertPath(path));
    }

    public void addChangedFiles(Collection<String> paths) {
        for (String path : paths) {
            addChangedFile(path);
        }
    }

    public void removeChangedFile(String path) {
        this.changedFiles.remove(convertPath(path));
    }

    public boolean containsChangedFile(String path) {
        return this.changedFiles.contains(convertPath(path));
    }

    public void addBackedUpFile(String originalPath, String backupPath) {
        this.backedUpFiles.put(convertPath(originalPath), convertPath(backupPath));
    }

    public void addRestoredFile(String restoredPath, String backupPath) {
        this.restoredFiles.put(convertPath(restoredPath), convertPath(backupPath));
    }

    public void addRealizedFile(String path, String content) {
        this.realizedFiles.put(convertPath(path), content);
    }

    public void addError(String path, String errorMsg) {
        this.errors.put(convertPath(path), errorMsg);
    }

    public void setCleaned(boolean cleaned) {
        this.cleaned = cleaned;
    }

    /**
     * Returns the set of files that have been ignored.
     * 
     * @return the ignored files
     */
    public Set<String> getIgnoredFiles() {
        return this.ignoredFiles;
    }

    /**
     * Returns the set of files that have been deleted.
     * 
     * @return the deleted files
     */
    public Set<String> getDeletedFiles() {
        return this.deletedFiles;
    }

    /**
     * Returns the set of files that have been added.
     * 
     * @return the added files
     */
    public Set<String> getAddedFiles() {
        return this.addedFiles;
    }

    /**
     * Returns the set of files that have been changed.
     * 
     * @return the changed files
     */
    public Set<String> getChangedFiles() {
        return this.changedFiles;
    }

    /**
     * Returns the set of files that have been backed up.
     * The key is the original path of the file that was backed up; the value
     * is the path to the backup file itself.
     * 
     * @return the information on files that were backed up
     */
    public Map<String, String> getBackedUpFiles() {
        return this.backedUpFiles;
    }

    /**
     * Returns the set of files that have been restored from a backup copy.
     * The key is the restored path of the file (i.e. the location where the
     * file now resides after being restored); the value is the path where
     * the backup copy of the file is.
     * 
     * @return the information on files that were restored
     */
    public Map<String, String> getRestoredFiles() {
        return this.restoredFiles;
    }

    /**
     * Returns the set of files that have been realized.
     * When a file is said to be "realized", it means the file was original
     * a template with replacement tokens but those replacement tokens have
     * been replaced with actual, real values.
     * 
     * The key is the path of the file that was realized; the value
     * is the actual content of the file after all replacement tokens have
     * been replaced.
     * 
     * @return the information on files that were realized
     */
    public Map<String, String> getRealizedFiles() {
        return this.realizedFiles;
    }

    /**
     * Returns the set of files that caused an error during processing.
     * 
     * The key is the path of the file that caused an error; the value
     * is an error message to describe the error that occurred.
     * 
     * @return the information on files that caused errors during processing
     */
    public Map<String, String> getErrors() {
        return this.errors;
    }

    /**
     * Returns <code>true</code> if the delpoyment's destination directory was
     * wiped of all files/directories before the new deployment files were
     * copied to it. This means any ignored files or directories that were
     * in the deployment's destination directory will have been deleted.
     *  
     * @return the cleaned flag
     */
    public boolean wasCleaned() {
        return cleaned;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("added=").append(this.addedFiles).append('\n');
        str.append("deleted=").append(this.deletedFiles).append('\n');
        str.append("changed=").append(this.changedFiles).append('\n');
        str.append("ignored=").append(this.ignoredFiles).append('\n');
        str.append("backed-up=").append(this.backedUpFiles).append('\n');
        str.append("restored=").append(this.restoredFiles).append('\n');
        str.append("realized=").append(this.realizedFiles.keySet()).append('\n');
        str.append("cleaned=[").append(this.cleaned).append(']').append('\n');
        str.append("errors=").append(this.errors);
        return str.toString();
    }

    /**
     * Converts the path to the form that will be stored internally.
     * 
     * @param path a filepath to be converted
     * 
     * @return the converted path that is to be used to store in the internal sets.
     */
    public String convertPath(String path) {
        if (File.separatorChar != '/') {
            if (path != null) {
                path = path.replace(File.separatorChar, '/');
            }
        }
        return path;
    }
}
