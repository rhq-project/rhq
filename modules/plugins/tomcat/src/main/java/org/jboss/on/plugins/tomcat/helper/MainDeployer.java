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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.catalina.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

/**
 * @author Ian Springer
 */
public class MainDeployer {
    static public final String DEPLOYER_BEAN = "Catalina:type=Deployer,host=localhost";

    private final Log log = LogFactory.getLog(this.getClass());

    private EmsOperation deployOperation;
    private EmsOperation redeployOperation;
    private EmsOperation undeployOperation;

    public MainDeployer(EmsConnection connection) throws NoSuchMethodException {
        EmsBean mainDeployer = connection.getBean(DEPLOYER_BEAN);
        if (mainDeployer == null) {
            throw new IllegalStateException("MBean named [" + DEPLOYER_BEAN + "] does not exist.");
        }
        this.deployOperation = EmsUtility.getOperation(mainDeployer, "manageApp", Context.class);
        //this.redeployOperation = EmsUtility.getOperation(mainDeployer, "manageApp", URL.class);
        this.undeployOperation = EmsUtility.getOperation(mainDeployer, "unmanageApp", URL.class);
    }

    public void deploy(File file) throws DeployerException {
        log.debug("Deploying " + file + "...");
        try {
            URL url = toURL(file);
            this.deployOperation.invoke(new Object[] { url });
        } catch (RuntimeException e) {
            throw new DeployerException("Failed to deploy " + file, e);
        }
    }

    public void redeploy(File file) throws DeployerException {
        log.debug("Redeploying " + file + "...");
        try {
            URL url = toURL(file);
            this.redeployOperation.invoke(new Object[] { url });
        } catch (RuntimeException e) {
            throw new DeployerException("Failed to redeploy " + file, e);
        }
    }

    public void undeploy(File file) throws DeployerException {
        log.debug("Undeploying " + file + "...");
        try {
            URL url = toURL(file);
            this.undeployOperation.invoke(new Object[] { url });
        } catch (RuntimeException e) {
            throw new DeployerException("Failed to undeploy " + file, e);
        }
    }

    private static URL toURL(File file) {
        URL url;
        try {
            url = file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        return url;
    }

    public class DeployerException extends Exception {
        DeployerException(String message) {
            super(message);
        }

        DeployerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
