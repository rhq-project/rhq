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
    FORMAT(true, "/bin/hadoop", "namenode -format"),
    FSCK(true, "/bin/hadoop", "fsck /"),
    LS(true, "/bin/hadoop", "fs -ls"),
    START(true, "/bin/hadoop-daemon.sh", "start "),
    STOP(true, "/bin/hadoop-daemon.sh", "stop "),
    QUEUE_LIST(true, "/bin/hadoop", "queue -list"),
    JOB_LIST_RUNNING(true, "/bin/hadoop", "job -list"),
    JOB_LIST_ALL(true, "/bin/hadoop", "job -list all"),
    REBALANCE_DFS(true, "/bin/hadoop", "balancer"),
    KILL(true, "/bin/hadoop", "job -kill", "pid"),
    JAR(false, "/bin/hadoop", "jar",  "args");

    private final String relativePathToExecutable;

    private final String args;
    
    private final String[] paramNames;
    
    private final boolean killOnTimeout;
    
    private HadoopSupportedOperations(boolean killOnTimeout, String relativePathToExecutable, String args, String... paramNames) {
        this.killOnTimeout = killOnTimeout;
        this.relativePathToExecutable = relativePathToExecutable;
        this.args = args;
        this.paramNames = paramNames;
    }

    public String getRelativePathToExecutable() {
        return relativePathToExecutable;
    }

    public String getArgs() {
        return args;
    }

    public String[] getParamsNames() {
        return paramNames;
    }
    
    public boolean isKillOnTimeout() {
        return killOnTimeout;
    }
}
