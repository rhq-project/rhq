/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.control;

import java.io.File;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * @author John Sanda
 */
public class StorageControl extends RHQServiceControl {

    public StorageControl(ControlCommand command) {
        super(command);
    }

    public int install(File basedir) throws Exception {
        log.debug("Installing RHQ storage node");

        command.putProperty("rhq.storage.basedir", basedir.getAbsolutePath());

        org.apache.commons.exec.CommandLine commandLine = new org.apache.commons.exec.CommandLine(
            "./rhq-storage-installer." + getExtension())
            .addArgument("--dir")
            .addArgument(basedir.getAbsolutePath())
            .addArgument("--commitlog")
            .addArgument("./storage/commit_log")
            .addArgument("--data")
            .addArgument("./storage/data")
            .addArgument("--saved-caches")
            .addArgument("./storage/saved_caches");

        Executor executor = new DefaultExecutor();
        executor.setWorkingDirectory(command.binDir);
        executor.setStreamHandler(new PumpStreamHandler());

        return executor.execute(commandLine);
    }

    private String getExtension() {
        if (isWindows()) {
            return "bat";
        }
        return "sh";
    }

}
