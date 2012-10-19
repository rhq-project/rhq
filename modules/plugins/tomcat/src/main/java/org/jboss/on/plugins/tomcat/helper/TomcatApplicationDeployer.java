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
package org.jboss.on.plugins.tomcat.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

/**
 * @author Fady Matar
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class TomcatApplicationDeployer {
    private final Log log = LogFactory.getLog(this.getClass());

    private EmsOperation deployOperation;
    private EmsOperation undeployOperation;

    public TomcatApplicationDeployer(EmsConnection connection, String vhostName) throws NoSuchMethodException {
        String deployerBean = getDeployerBeanName(vhostName);
        EmsBean mainDeployer = connection.getBean(deployerBean);
        if (mainDeployer == null) {
            throw new IllegalStateException("MBean named [" + deployerBean + "] does not exist.");
        }

        connection.getConnectionProvider().getConnectionSettings().getClassPathEntries();
        this.deployOperation = EmsUtility.getOperation(mainDeployer, "addServiced", String.class);
        this.undeployOperation = EmsUtility.getOperation(mainDeployer, "removeServiced", String.class);
    }

    private String getDeployerBeanName(String vhostName) {
        return "Catalina:type=Deployer,host=" + ((null == vhostName) ? "localhost" : vhostName);
    }

    public void deploy(String contextPath) throws DeployerException {
        log.debug("Servicing " + contextPath + "...");
        try {
            this.deployOperation.invoke(new Object[] { contextPath });
        } catch (RuntimeException e) {
            throw new DeployerException("Failed to service " + contextPath, e);
        }
    }

    public void undeploy(String contextPath) throws DeployerException {
        log.debug("Undeploying " + contextPath + "...");
        try {
            this.undeployOperation.invoke(new Object[] { contextPath });
        } catch (RuntimeException e) {
            throw new DeployerException("Failed to undeploy " + contextPath, e);
        }
    }

    public class DeployerException extends Exception {
        private static final long serialVersionUID = 1L;

        DeployerException(String message) {
            super(message);
        }

        DeployerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
