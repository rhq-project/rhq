/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.jboss.as.jdr.commands;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Utils;

/**
 * Collects RHQ-specific files
 * @author Heiko W. Rupp
 */
public class RHQFilesCollector extends JdrCommand {

    private static final String RHQ_PREFIX = "rhq" + File.separator;
    private String subdirectory;
    private String fileName;

    public RHQFilesCollector(String subdirectory, String fileName) {
        this.subdirectory = subdirectory;
        this.fileName = fileName;
    }


    @Override
    public void execute() throws Exception {
        String basedir = System.getProperty("rhq.server.home");
        if (basedir==null) {
            throw new IllegalStateException("Did not find 'rhq.server.home'");
        }

        File dir = new File(basedir,subdirectory);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Directory " + dir.getAbsolutePath() + " does not exist");
        }
        if (!dir.canExecute() || !dir.canRead()) {
            throw new IllegalArgumentException("Directory " + dir.getAbsolutePath() + " can not be accessed");
        }


        File file = new File(dir,fileName);
        FileInputStream fis=null;
        try {
            fis = new FileInputStream(file);
            this.env.getZip().add(fis, RHQ_PREFIX + file.getName());
        } finally {
            Utils.safelyClose(fis);
        }
    }
}
