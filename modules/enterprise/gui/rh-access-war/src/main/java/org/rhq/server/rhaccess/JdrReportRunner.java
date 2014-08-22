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

import org.apache.log4j.Logger;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class JdrReportRunner {
    final ModelControllerClient client;

    private final static Logger log = Logger.getLogger(JdrReportRunner.class);

    public JdrReportRunner() throws Exception {
        String host = System.getProperty("jboss.bind.address.management", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("jboss.management.native.port", "6999"));
        client = ModelControllerClient.Factory.create(host, port);
        log.debug("DMR Client created");
    }

    public String getReport() throws Exception {
        String report = null;
        ModelNode op = new ModelNode();
        ModelNode address = op.get("address").setEmptyList();
        address.add("subsystem", "jdr");
        op.get("operation").set("generate-jdr-report");
        log.info("Executing /subsystem=jdr/:generate-jdr-report()");
        ModelNode result = client.execute(op);
        log.debug("Operation executed");
        if (isSuccess(result)) {
            report = result.get("result").get("report-location").asString();
            log.info("JDR report written to " + report);
        } else {
            log.error("Failed to generate JDR report " + result.asString());
            throw new Exception("Failed to generate JDR report " + result.asString());
        }
        return report;
    }

    public static boolean isSuccess(ModelNode operationResult) {
        if (operationResult != null) {
            return operationResult.hasDefined("outcome") && operationResult.get("outcome").asString().equals("success");
        }
        return false;
    }
}
