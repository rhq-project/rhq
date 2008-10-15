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
package org.rhq.plugins.jbossas.helper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.rhq.plugins.jbossas.util.JBossMBeans;
import org.rhq.plugins.jbossas.util.EmsUtility;

/**
 * @author Ian Springer
 */
public class MainDeployer {
    private final Log log = LogFactory.getLog(this.getClass());

    private EmsOperation deployOperation;
    private EmsOperation redeployOperation;

    public MainDeployer(EmsConnection connection) throws NoSuchMethodException {
        EmsBean mainDeployer = connection.getBean(JBossMBeans.MAIN_DEPLOYER);
        if (mainDeployer == null) {
            throw new IllegalStateException("MBean named [" + JBossMBeans.MAIN_DEPLOYER + "] does not exist.");
        }
        this.deployOperation = EmsUtility.getOperation(mainDeployer, "deploy", URL.class);
        this.redeployOperation = EmsUtility.getOperation(mainDeployer, "redeploy", URL.class);
    }

    public void deploy(File file) throws Exception {
        log.debug("Deploying " + file + "...");
        try {
            URL url = toURL(file);
            this.deployOperation.invoke(new Object[]{url});
        }
        catch (RuntimeException e) {
            throw new Exception("Failed to deploy " + file, e);
        }
    }

    public void redeploy(File file) throws Exception {
        log.debug("Redeploying " + file + "...");
        try {
            URL url = toURL(file);
            this.redeployOperation.invoke(new Object[]{url});
        }
        catch (RuntimeException e) {
            throw new Exception("Failed to redeploy " + file, e);
        }
    }

    private static URL toURL(File file) {
        URL url;
        try {
            url = file.toURI().toURL();
        }
        catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
        return url;
    }
}
