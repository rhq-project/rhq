/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.w3c.dom.Element;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.CustomUi;

/**
 * Plugin manager that takes care of loading the plug-ins and instantiating
 * of {@link AlertSender} etc.
 * @author Heiko W. Rupp
 */
public class AlertSenderPluginManager extends ServerPluginManager {

    private final Log log = getLog();
    private Map<String,String> pluginClassByName = new HashMap<String, String>();
    private Map<String,ServerPluginEnvironment> pluginEnvByName = new HashMap<String, ServerPluginEnvironment>();
    private Map<String,AlertSenderInfo> senderInfoByName = new HashMap<String, AlertSenderInfo>();

    public AlertSenderPluginManager(AbstractTypeServerPluginContainer pc) {
        super(pc);
    }

    /**
     * Postprocess the loading of the plugin - the actual load is done
     * in the super class.
     * Here we verify that the passed &lt;plugin-class&gt; is valid and build the
     * list of plugins that can be queried by the UI etc.
     * @param env the environment of the plugin to be loaded
     *
     * @throws Exception
     */
    @Override
    public void loadPlugin(ServerPluginEnvironment env,boolean enable) throws Exception {
        log.info("Start loading alert plugin " + env.getPluginKey().getPluginName());
        super.loadPlugin(env,enable);

        AlertPluginDescriptorType type = (AlertPluginDescriptorType) env.getPluginDescriptor();

        String className = type.getPluginClass();
        if (!className.contains(".")) {
            className = type.getPackage() + "." + className;
        }
        try {
            Class.forName(className,false,env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.error("Can't find pluginClass " + className + ". Plugin " + env.getPluginKey().getPluginName() + " will be ignored");
            try {
                unloadPlugin(env.getPluginKey().getPluginName());
            }
            catch (Throwable t) {
                log.warn("  +--> unload failed too " + t.getMessage());
            }
            return;
        }

        // The short name is basically the key into the plugin
        String shortName = type.getShortName();
        pluginClassByName.put(shortName,className);

        //
        // Ok, we have a valid plugin class, so we can look for other things
        // and store the info
        //

        String uiSnippetPath;
        URL uiSnippetUrl = null;
        CustomUi customUI = type.getCustomUi();
        if (customUI!=null) {
            uiSnippetPath = customUI.getUiSnippetName();

            try {
                uiSnippetUrl = env.getPluginClassLoader().getResource(uiSnippetPath);
                log.info("UI snipped for " + shortName + " is at " + uiSnippetUrl);
            }
            catch (Exception e) {
                log.error("No valid ui snippet provided, but <custom-ui> given for sender plugin " + shortName + "Error is "+ e.getMessage());
                log.error("Plugin will be ignored");
                return;
            }

            // Get the backing bean class
            className = customUI.getBackingBeanName();
            if (!className.contains(".")) {
                className = type.getPackage() + "." + className;
            }
            try {
                Class.forName(className,true,env.getPluginClassLoader()); // TODO how make this available to Seam and the Web-CL ?
            }
            catch (Throwable t ) {
                log.error("Backing bean " + className + " not found for plugin " + shortName);
            }
        }

        AlertSenderInfo info = new AlertSenderInfo(shortName, type.getDescription(), env.getPluginKey());
        info.setUiSnippetUrl(uiSnippetUrl);
        senderInfoByName.put(shortName, info);

        pluginEnvByName.put(shortName,env);

    }

    @Override
    protected void unloadPlugin(String pluginName) throws Exception {
        log.info("Unloading plugin " + pluginName );
        super.unloadPlugin(pluginName);
        String shortName=null;
        for (AlertSenderInfo info: senderInfoByName.values()) {
            if (info.getPluginName().equals(pluginName)) {
                shortName = info.getShortName();
                break;
            }
        }
        pluginClassByName.remove(shortName);
        senderInfoByName.remove(shortName);
        pluginEnvByName.remove(shortName);
    }

    /**
     * Instantiate an AlertSender for the passed shortName, which is the name you have provided
     * in the plugin descriptor in the &lt;shortName&gt; element
     * @param notification The alert notification we need the sender for. Wanted sender is in notification.getSenderName()
     * @return a new AlertSender with preferences set
     * @see AlertSender
     */
    public AlertSender getAlertSenderForNotification(AlertNotification notification) {

        String className = pluginClassByName.get(notification.getSenderName());
        ServerPluginEnvironment env = pluginEnvByName.get(notification.getSenderName());
        Class clazz;
        try {
            clazz = Class.forName(className,true,env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.error(e); // TODO
            return null;
        }

        AlertSender sender;
        try {
            sender = (AlertSender) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return null;
        }

        // We have no entityManager lying around here, which means
        // Configuration is an uninitialized Proxy and we'd get a LazyInit
        // Exception later.
        // So lets get it from the SessionBeans
        ServerPluginContext ctx = getServerPluginContext(env);
        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();


        sender.alertParameters = mgr.getAlertPropertiesConfiguration(notification);
        if (sender.alertParameters == null)
            sender.alertParameters = new Configuration(); // Safety measure

        ServerPluginsLocal pluginsMgr = LookupUtil.getServerPlugins();

        PluginKey key = ctx.getPluginEnvironment().getPluginKey();
        ServerPlugin plugin = pluginsMgr.getServerPlugin(key);
        plugin = pluginsMgr.getServerPluginRelationships(plugin);

        sender.preferences = plugin.getPluginConfiguration();
        if (sender.preferences==null)
            sender.preferences = new Configuration(); // Safety measure

        return sender;
    }

    /**
     * Return the list of deployed alert sender plug-ins by their &lt;shortName&gt;
     * @return List of plugin names
     */
    public List<String> getPluginList() {
        return new ArrayList<String>(pluginClassByName.keySet());
    }

    public AlertSenderInfo getAlertSenderInfo(String shortName) {
        return senderInfoByName.get(shortName);
    }
}
