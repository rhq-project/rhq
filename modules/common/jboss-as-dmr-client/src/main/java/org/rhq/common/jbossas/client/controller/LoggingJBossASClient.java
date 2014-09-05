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
package org.rhq.common.jbossas.client.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides management of the logging subsystem.
 *
 * @author John Mazzitelli
 */
public class LoggingJBossASClient extends JBossASClient {

    public static final String LOGGING = "logging";
    public static final String LOGGER = "logger";
    public static final String FILE_HANDLER = "periodic-rotating-file-handler";

    public LoggingJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a logger with the given name.
     *
     * @param loggerName the name to check (this is also known as the category name)
     * @return true if there is a logger/category with the given name already in existence
     */
    public boolean isLogger(String loggerName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, LOGGING, LOGGER, loggerName);
        return null != readResource(addr);
    }

    /**
     * Returns the level of the given logger.
     *
     * @param loggerName the name of the logger (this is also known as the category name)
     * @return level of the logger
     * @throws Exception if the log level could not be obtained (typically because the logger doesn't exist)
     */
    public String getLoggerLevel(String loggerName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, LOGGING, LOGGER, loggerName);
        return getStringAttribute("level", addr);
    }

    /**
     * Sets the logger to the given level.
     * If the logger does not exist yet, it will be created.
     *
     * @param loggerName the logger name (this is also known as the category name)
     * @param level the new level of the logger (e.g. DEBUG, INFO, ERROR, etc.)
     * @throws Exception
     */
    public void setLoggerLevel(String loggerName, String level) throws Exception {

        final Address addr = Address.root().add(SUBSYSTEM, LOGGING, LOGGER, loggerName);
        final ModelNode request;

        if (isLogger(loggerName)) {
            request = createWriteAttributeRequest("level", level, addr);
        } else {
            final String dmrTemplate = "" //
                + "{" //
                + "\"category\" => \"%s\" " //
                + ", \"level\" => \"%s\" " //
                + ", \"use-parent-handlers\" => \"true\" " //
                + "}";
            final String dmr = String.format(dmrTemplate, loggerName, level);

            request = ModelNode.fromString(dmr);
            request.get(OPERATION).set(ADD);
            request.get(ADDRESS).set(addr.getAddressNode());
        }

        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    public void setFilterSpec(String filterSpec) throws Exception {

        final Address addr = Address.root().add(SUBSYSTEM, LOGGING, FILE_HANDLER, "FILE");
        final ModelNode request;

        request = createWriteAttributeRequest("filter-spec", filterSpec, addr);

        final ModelNode response = execute(request);
        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

}
