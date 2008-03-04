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
package org.rhq.core.pluginapi.event.log;

import java.util.Map;

import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.DirStat;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarFileNotFoundException;
import org.hyperic.sigar.Sigar;

/**
 * @author Ian Springer
 */
public class LogFileInfo extends FileInfo {
    private FileInfo fileInfo;

    public LogFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public LogFileInfo() {
        super();
    }

    public String getTypeString() {
        return this.fileInfo.getTypeString();
    }

    public char getTypeChar() {
        return this.fileInfo.getTypeChar();
    }

    public String getName() {
        return this.fileInfo.getName();
    }

    public int hashCode() {
        return this.fileInfo.hashCode();
    }

    public boolean equals(Object o) {
        return this.fileInfo.equals(o);
    }

    public String getPermissionsString() {
        return this.fileInfo.getPermissionsString();
    }

    public int getMode() {
        return this.fileInfo.getMode();
    }

    public void enableDirStat(boolean value) {
        this.fileInfo.enableDirStat(value);
    }

    public String diff() {
        return this.fileInfo.diff();
    }

    public String diff(DirStat stat) {
        return this.fileInfo.diff(stat);
    }

    public String diff(FileInfo info) {
        return this.fileInfo.diff(info);
    }

    public FileInfo getPreviousInfo() {
        return this.fileInfo.getPreviousInfo();
    }

    public boolean modified() throws SigarException, SigarFileNotFoundException {
        return this.fileInfo.modified();
    }

    public boolean changed() throws SigarException, SigarFileNotFoundException {
        return this.fileInfo.changed() || this.fileInfo.getSize() != getPreviousInfo().getSize();
    }

    public void stat() throws SigarException, SigarFileNotFoundException {
        this.fileInfo.stat();
    }

    public void gather(Sigar sigar, String s) throws SigarException {
        this.fileInfo.gather(sigar, s);
    }

    public long getPermissions() {
        return this.fileInfo.getPermissions();
    }

    public int getType() {
        return this.fileInfo.getType();
    }

    public long getUid() {
        return this.fileInfo.getUid();
    }

    public long getGid() {
        return this.fileInfo.getGid();
    }

    public long getInode() {
        return this.fileInfo.getInode();
    }

    public long getDevice() {
        return this.fileInfo.getDevice();
    }

    public long getNlink() {
        return this.fileInfo.getNlink();
    }

    public long getSize() {
        return this.fileInfo.getSize();
    }

    public long getAtime() {
        return this.fileInfo.getAtime();
    }

    public long getCtime() {
        return this.fileInfo.getCtime();
    }

    public long getMtime() {
        return this.fileInfo.getMtime();
    }

    public Map toMap() {
        return this.fileInfo.toMap();
    }

    public String toString() {
        return this.fileInfo.toString();
    }
}
