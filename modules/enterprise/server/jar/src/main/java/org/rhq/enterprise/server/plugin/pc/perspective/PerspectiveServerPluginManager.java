/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.file.FileUtil;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.perspective.metadata.PerspectivePluginMetadataManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PerspectivePluginDescriptorType;

/**
 * This loads in all perspective server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author Jay Shaughnessy 
 * @author John Mazzitelli
 */
public class PerspectiveServerPluginManager extends ServerPluginManager {
    private final Log log = LogFactory.getLog(this.getClass());

    private PerspectivePluginMetadataManager metadataManager;

    public PerspectiveServerPluginManager(PerspectiveServerPluginContainer pc) {
        super(pc);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        this.metadataManager = new PerspectivePluginMetadataManager();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.metadataManager = null;
    }

    /**
     * An object that can be used to process and store all metadata from all perspective plugins. This object will contain all the
     * metadata found in all loaded perspective plugins.
     *
     * @return object to retrieve plugin metadata from
     */
    public PerspectivePluginMetadataManager getMetadataManager() {
        return this.metadataManager;
    }

    /* At load-time ensure that any WAR files packaged with the server plugin are deployed to the RHQ
     * Server.  Note, perspective apps are logically deployed along-side, not inside the rhq.ear (although physically
     * under the default/work). They should have access to the same shared components.
     *  
     * @see org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer#loadPlugin(org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment)
     */
    @Override
    public synchronized void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        super.loadPlugin(env, enabled);

        this.metadataManager.loadPlugin((PerspectivePluginDescriptorType) env.getPluginDescriptor());

        // Do this here and not startPlugin() because if the app does not deploy the perspective should
        // be considered invalid.
        deployEmbeddedWars(env);
    }

    private void deployEmbeddedWars(ServerPluginEnvironment env) {
        String name = null;
        try {
            JarFile pluginJarFile = new JarFile(env.getPluginUrl().getFile());
            for (JarEntry entry : Collections.list(pluginJarFile.entries())) {
                name = entry.getName();
                if (name.toLowerCase().endsWith(".war")) {
                    deployWar(env, entry.getName(), pluginJarFile.getInputStream(entry));
                }
            }
        } catch (Exception e) {
            Throwable t = (e instanceof MBeanException) ? e.getCause() : e;
            log.error("Failed to deploy " + env.getPluginKey().getPluginName() + "#" + name, t);
        }
    }

    private void deployWar(ServerPluginEnvironment env, String name, InputStream iStream) throws Exception {        
        // Save the war file to the plugins data directory. This survives restarts and will
        // act as our deploy directory.
        File deployFile = writeWarToFile(getDeployFile(env, name), iStream);

        // get reference to MBean server
        Context ic = new InitialContext();
        MBeanServerConnection server = (MBeanServerConnection) ic.lookup("jmx/invoker/RMIAdaptor");

        // get reference to MainDeployer MBean
        ObjectName mainDeployer = new ObjectName("jboss.system:service=MainDeployer");

        server.invoke(mainDeployer, "deploy", new Object[] { deployFile.getAbsolutePath() },
            new String[] { String.class.getName() });
    }

    private File getDeployFile(ServerPluginEnvironment env, String name) throws IOException {
        File dataDir = getServerPluginContext(env).getDataDirectory();
        dataDir.mkdirs();
        return new File(dataDir, name);
    }

    private File writeWarToFile(File destFile, InputStream iStream) throws Exception {
        // Delete any previous instance of the file
        if (destFile.exists()) {
            getLog().debug("Existing file found and will be deleted at: " + destFile);
            destFile.delete();
        }

        FileUtil.writeFile(iStream, destFile);

        if (!destFile.exists()) {
            String err = "Temporary file for application update not written to: " + destFile;
            getLog().error(err);
            throw new Exception(err);
        }

        return destFile;
    }

    @Override
    protected void startPlugin(String pluginName) {
        super.startPlugin(pluginName);

        // If the metadata manager is started, then this is a hot deploy (or redeploy) of the perspective, so restart
        // the metadata manage to merge the metadata from this perspective and all other previously loaded perspectives.
        // If the metadata manager is *not* started, then this is the initial load of the perspective, and the master PC
        // will call startPlugins() once all perspectives have been loaded, which will take care of starting the
        // metadata manager.
        if (isPluginEnabled(pluginName) && this.metadataManager.isStarted()) {
            this.metadataManager.stop();
            this.metadataManager.start();
        }

        // TODO: execute An MBean Start on any perspective webapps?        
    }

    /**
     * All of the plugins have been loaded, so now let the metadata manager sort through the definitions.
     *
     * @see org.rhq.enterprise.server.plugin.pc.ServerPluginManager#startPlugins()
     */
    @Override
    public synchronized void startPlugins() {
        super.startPlugins();
        this.metadataManager.start();
    }

    @Override
    protected void stopPlugin(String pluginName) {
        // TODO: This method can be called in three cases:
        //       1) hot-undeploy of a plugin
        //       2) hot-redeploy of a plugin (stopPlugin() is called, then startPlugin() is called)
        //       3) during shutdown of the plugin container
        // We only want to restart our metadata manager in case 2, but there's no way for us to tell which case we're
        // dealing with...
        /*if (case2 && this.metadataManager.isStarted()) {
            this.metadataManager.stop();
            this.metadataManager.start();
        }*/

        // TODO: execute An MBean Stop on any perspective webapps?

        super.stopPlugin(pluginName);
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.plugin.pc.ServerPluginManager#stopPlugins()
     */
    @Override
    public synchronized void stopPlugins() {
        this.metadataManager.stop();
        super.stopPlugins();
    }

    /* At unload-time ensure that any WAR files packaged with the server plugin are un-deployed on
     * the RHQ Server.
     * 
     * @see org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer#unloadPlugin(org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment)
     */
    @Override
    public synchronized void unloadPlugin(String pluginName) throws Exception {
        ServerPluginEnvironment env = getPluginEnvironment(pluginName);
        // Do this here and not stopPlugin(), treat this as a precursor to a possible perspectve deletion 
        undeployWars(env);
        this.metadataManager.unloadPlugin((PerspectivePluginDescriptorType) env.getPluginDescriptor());
        super.unloadPlugin(pluginName);
    }

    private void undeployWars(ServerPluginEnvironment env) {
        String name = null;
        try {
            JarFile plugin = new JarFile(env.getPluginUrl().getFile());
            for (JarEntry entry : Collections.list(plugin.entries())) {
                name = entry.getName();
                if (name.toLowerCase().endsWith(".war")) {
                    undeployWar(getDeployFile(env, entry.getName()));
                }
            }
        } catch (Exception e) {
            this.log.error("Failed to deploy " + env.getPluginKey().getPluginName() + "#" + name, e);
        }
    }

    private void undeployWar(File deployFile) {
        try {
            // get reference to MBean server
            Context ic = new InitialContext();
            MBeanServerConnection server = (MBeanServerConnection) ic.lookup("jmx/invoker/RMIAdaptor");

            // get reference to MainDeployer MBean
            ObjectName mainDeployer = new ObjectName("jboss.system:service=MainDeployer");

            URL fileUrl = deployFile.toURI().toURL();

            Boolean isDeployed = (Boolean) server.invoke(mainDeployer, "isDeployed", new Object[] { fileUrl },
                new String[] { URL.class.getName() });

            if (isDeployed) {
                server.invoke(mainDeployer, "undeploy", new Object[] { fileUrl }, new String[] { URL.class.getName() });
            }
        } catch (Exception e) {
            log.error("Failed to undeploy perspective WAR [" + deployFile + "].", e);
        }
    }
}