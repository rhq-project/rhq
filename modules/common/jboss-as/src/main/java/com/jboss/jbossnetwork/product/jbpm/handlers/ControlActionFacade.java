 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import org.rhq.core.pluginapi.operation.OperationServicesResult;

/**
 * Facade to the rest of the plugin classes necessary to perform control actions on the resource. This is largely to
 * facilitate testing, allowing the test to substitute in a mock implementation. When running live, an implementation
 * will simply forward the calls to the appropriate class in the plugin or in the plugin container APIs.
 *
 * @author Jason Dobies
 */
public interface ControlActionFacade {
    /**
     * Starts the underlying JBoss AS instance.
     *
     * @return result of executing the operation, which will indicate success or failure
     */
    OperationServicesResult start();

    /**
     * Stops the underlying JBoss AS instance. This method will throw an exception if the AS server is not currently
     * running.
     *
     * @return result of executing the operation, which will indicate success or failure
     */
    OperationServicesResult stop();

    /**
     * Indicates if the AS instance is running.
     *
     * @return <code>true</code> if the instance is running; <code>false</code> otherwise.
     */
    boolean isRunning();
}