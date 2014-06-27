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
package org.rhq.server.rhaccess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.rhq.enterprise.server.support.SupportManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceJdrReportRunner {
    private final int resourceId;
    private final static Logger log = Logger.getLogger(ResourceJdrReportRunner.class);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yy-mm-dd_hh-mm-ss");

    public ResourceJdrReportRunner(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getReport() throws Exception {
        log.info("Obtaining JDR report for resourceId=" + resourceId);
        SupportManagerLocal supportMgr = LookupUtil.getSupportManager();
        InputStream is = null;
        try {
            is = supportMgr.getSnapshotReportStream(LookupUtil.getSubjectManager().getOverlord(), resourceId,
            "jdr", null);
        } catch (Exception e) {
            throw new JdrReportFailedException(e.getMessage());
        }
        if (is == null) {
            throw new JdrReportFailedException("Failed to obtain JDR report for resourceId=" + resourceId
                + " - no data recieved");
        }
        File tmpDir = new File(System.getProperty("jboss.server.temp.dir"));
        if (tmpDir.exists() && tmpDir.canWrite()) {
            File tmp = new File(tmpDir, "jdr_" + dateFormat.format(new Date()) + "_" + resourceId + ".zip");
            FileOutputStream fos = new FileOutputStream(tmp);
            IOUtils.copy(is, fos);
            fos.close();
            log.info("Obtained JDR report written to " + tmp.getAbsolutePath());
            return tmp.getAbsolutePath();
        }
        throw new IOException("Cannot save JDR report file, unable to write to temp dir " + tmpDir.getAbsolutePath());
    }
}
