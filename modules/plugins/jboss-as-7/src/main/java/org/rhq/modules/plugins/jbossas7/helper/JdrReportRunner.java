/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.modules.plugins.jbossas7.ASConnection;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

public class JdrReportRunner {

    public static final int JDR_OPERATION_TIMEOUT = 60 * 10;

    final Log log = LogFactory.getLog(JdrReportRunner.class);

    private final Address path;
    private final ASConnection connection;

    public JdrReportRunner(Address path, ASConnection con) {
        this.path = new Address(path);
        this.connection = con;
        this.path.addSegment("subsystem=jdr");
    }

    public InputStream getReport() throws Exception {
        log.info("Obtaining JDR Report form " + this.path);
        Operation operation = new Operation("generate-jdr-report", this.path);
        Result res = this.connection.execute(operation, false, JDR_OPERATION_TIMEOUT); // 10minutes should be enough
        if (res.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) res.getResult();
            File report = new File(map.get("report-location").toString());
            if (report.exists() && report.canRead()) {
                log.info("JDR Report created in " + report.getAbsolutePath());
                return new FileInputStream(report);
            } else {
                throw new IOException("Failed to read generated JDR Report file " + report.getAbsolutePath());
            }
        }
        log.error("Failed to generate JDR Report : " + res.getFailureDescription());
        return null;
    }
}
