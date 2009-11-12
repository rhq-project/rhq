/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc.perspective;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PerspectivePluginDescriptorType;

/**
 * Manages perspective server plugins.
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class PerspectiveServerPluginContainer extends AbstractTypeServerPluginContainer {

    public PerspectiveServerPluginContainer(MasterServerPluginContainer master) {
        super(master);
    }

    @Override
    public ServerPluginType getSupportedServerPluginType() {
        return new ServerPluginType(PerspectivePluginDescriptorType.class);
    }

    /* At load-time ensure that any WAR files packaged with the server plugin are deployed to the RHQ
     * Server.  Note, perspective apps are logically deployed along-side, not inside the rhq.ear (although physically
     * under the default/work). They should have access to the same shared components.
     *  
     * @see org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer#loadPlugin(org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment)
     */
    @Override
    public synchronized void loadPlugin(ServerPluginEnvironment env) throws Exception {
        String name = null;
        try {
            JarFile plugin = new JarFile(env.getPluginUrl().getFile());
            for (JarEntry entry : Collections.list(plugin.entries())) {
                name = entry.getName();
                if (name.toLowerCase().endsWith(".war")) {
                    deployWar(entry.getName(), plugin.getInputStream(entry));
                }
            }
        } catch (Exception e) {
            getLog().error("Failed to deploy " + env.getPluginName() + "#" + name, e);
        }

        super.loadPlugin(env);
    }

    private void deployWar(String name, InputStream iStream) {
        try {
            // Save the war file to a temp area
            File tempFile = writeWarToTempFile(name, iStream);

            // get reference to MBean server
            Context ic = new InitialContext();
            MBeanServerConnection server = (MBeanServerConnection) ic.lookup("jmx/invoker/RMIAdaptor");

            // get reference to MainDeployer MBean
            ObjectName mainDeployer = new ObjectName("jboss.system:service=MainDeployer");

            server.invoke(mainDeployer, "deploy", new Object[] { tempFile.getAbsolutePath() },
                new String[] { String.class.getName() });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File writeWarToTempFile(String name, InputStream iStream) throws Exception {

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File tempFile = new File(tempDir.getAbsolutePath(), name);

        // Delete any previous instance of the file
        if (tempFile.exists()) {
            getLog().debug("Existing temporary file found and will be deleted at: " + tempFile);
            tempFile.delete();
        }

        FileUtil.writeFile(iStream, tempFile);

        if (!tempFile.exists()) {
            String err = "Temporary file for application update not written to: " + tempFile;
            getLog().error(err);
            throw new Exception(err);
        }

        return tempFile;
    }

    /* At unload-time ensure that any WAR files packaged with the server plugin are un-deployed on
     * the RHQ Server.
     * 
     * @see org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer#unloadPlugin(org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment)
     */
    @Override
    public synchronized void unloadPlugin(ServerPluginEnvironment env) throws Exception {
        String name = null;
        try {
            JarFile plugin = new JarFile(env.getPluginUrl().getFile());
            for (JarEntry entry : Collections.list(plugin.entries())) {
                name = entry.getName();
                if (name.toLowerCase().endsWith(".war")) {
                    undeployWar(entry.getName());
                }
            }

        } catch (Exception e) {
            getLog().error("Failed to deploy " + env.getPluginName() + "#" + name, e);
        }

        super.unloadPlugin(env);
    }

    private void undeployWar(String name) {
        try {
            // get reference to MBean server
            Context ic = new InitialContext();
            MBeanServerConnection server = (MBeanServerConnection) ic.lookup("jmx/invoker/RMIAdaptor");

            // get reference to MainDeployer MBean
            ObjectName mainDeployer = new ObjectName("jboss.system:service=MainDeployer");

            Boolean isDeployed = (Boolean) server.invoke(mainDeployer, "isDeployed", new Object[] { name },
                new String[] { String.class.getName() });

            if (isDeployed) {
                server.invoke(mainDeployer, "undeploy", new Object[] { name }, new String[] { String.class.getName() });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
