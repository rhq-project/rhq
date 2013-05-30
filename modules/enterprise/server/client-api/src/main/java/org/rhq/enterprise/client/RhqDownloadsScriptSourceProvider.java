/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.script.BaseRhqSchemeScriptSourceProvider;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Lukas Krejci
 */
public class RhqDownloadsScriptSourceProvider extends BaseRhqSchemeScriptSourceProvider {

    private static final Log LOG = LogFactory.getLog(RhqDownloadsScriptSourceProvider.class);

    private static final String AUTHORITY = "downloads";

    private CoreServerMBean coreServer;

    /**
     * A bunch of methods optionally enclosed in privileged actions to ensure that we can access the information
     * inside RHQ server even though it might not be accessible in the access control context the scripts run in
     * in the RHQ server.
     */
    private static class SecurityActions {
        private SecurityActions() {

        }

        private static CoreServerMBean lookupCoreServer() {
            if (System.getSecurityManager() == null) {
                return LookupUtil.getCoreServer();
            } else {
                return AccessController.doPrivileged(new PrivilegedAction<CoreServerMBean>() {
                    @Override
                    public CoreServerMBean run() {
                        return LookupUtil.getCoreServer();
                    }
                });
            }
        }

        private static File getDownloadDir(final CoreServerMBean coreServer) {
            File earDeployDir;
            if (System.getSecurityManager() == null) {
                earDeployDir = coreServer.getEarDeploymentDir();
            } else {
               earDeployDir = AccessController.doPrivileged(new PrivilegedAction<File>() {
                    @Override
                    public File run() {
                        return coreServer.getEarDeploymentDir();
                    }
                });
            }
            File downloadDir = new File(earDeployDir, "rhq-downloads");
            return downloadDir;
        }
    }

    public RhqDownloadsScriptSourceProvider() {
        //we need to do a safe JNDI lookup, but we may run in unprivileged context...
        this(SecurityActions.lookupCoreServer());
    }

    /**
     * This is meant only for testing purposes.
     *
     * @param coreServerMBean the CoreServer MBean to use
     */
    public RhqDownloadsScriptSourceProvider(CoreServerMBean coreServerMBean) {
        super(AUTHORITY);
        this.coreServer = coreServerMBean;
    }

    @Override
    protected Reader doGetScriptSource(URI scriptUri) {
        String path = scriptUri.getPath().substring(1); //remove the leading /

        //We're going to be doing an MBean call here which the scripts don't have privs for by default
        File downloadsDir = SecurityActions.getDownloadDir(coreServer);
        File scriptDownloads = new File(downloadsDir, "script-modules");
        File file = new File(scriptDownloads, path);

        try {
            return new InputStreamReader(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOG.debug("Failed to locate the download file: " + scriptUri, e);
            return null;
        }
    }
}
