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
    private final Map<String, String> realizedFiles = new HashMap<String, String>();
    private final Map<String, String> errors = new HashMap<String, String>();

    public void addIgnoredFile(String path) {
        this.ignoredFiles.add(convertPath(path));
    }

    public void addDeletedFile(String path) {
        this.deletedFiles.add(convertPath(path));
    }

    public void addAddedFile(String path) {
        this.addedFiles.add(convertPath(path));
    }

    public void addChangedFile(String path) {
        this.changedFiles.add(convertPath(path));
    }

    public void addBackedUpFile(String originalPath, String backupPath) {
        this.backedUpFiles.put(convertPath(originalPath), convertPath(backupPath));
    }

    public void addRealizedFile(String path, String content) {
        this.realizedFiles.put(convertPath(path), content);
    }

    public void addError(String path, String errorMsg) {
        this.errors.put(convertPath(path), errorMsg);
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("added=").append(this.addedFiles).append('\n');
        str.append("deleted=").append(this.deletedFiles).append('\n');
        str.append("changed=").append(this.changedFiles).append('\n');
        str.append("ignored=").append(this.ignoredFiles).append('\n');
        str.append("backed-up=").append(this.backedUpFiles).append('\n');
        str.append("realized=").append(this.realizedFiles.keySet()).append('\n');
        str.append("errors=").append(this.errors);
        return str.toString();
    }

    private String convertPath(String path) {
        return new File(path).getPath(); // makes sure e.g. the file separators are correct for this platform
    }
}
