/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.hadoop;

/**
 * Supported operations for Hadoop plugin
 * 
 * @author Jirka Kremser
 */
public enum HadoopSupportedOperations {
    FORMAT("/bin/hadoop", "namenode -format"),
    FSCK("/bin/hadoop", "fsck /"),
    LS("/bin/hadoop", "fs -ls"),
    START("/bin/hadoop-daemon.sh", "start "),
    STOP("/bin/hadoop-daemon.sh", "stop "),
    QUEUE_LIST("/bin/hadoop", "queue -list");

    private final String relativePathToExecutable;

    private final String args;

    private HadoopSupportedOperations(String relativePathToExecutable, String args) {
        this.relativePathToExecutable = relativePathToExecutable;
        this.args = args;
    }

    public String getRelativePathToExecutable() {
        return relativePathToExecutable;
    }

    public String getArgs() {
        return args;
    }
}
