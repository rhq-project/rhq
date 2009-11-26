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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;

/**
 * TODO
 * @author Heiko W. Rupp
 */
public class AlertSenderPluginManager extends ServerPluginManager {

    private final Log log = getLog();
    private Map<String,String> pluginNameToType = new HashMap<String, String>();
    private Map<String,String> pluginClassByName = new HashMap<String, String>();
    private Map<String,ServerPluginEnvironment> pluginEnvByName = new HashMap<String, ServerPluginEnvironment>();

    public AlertSenderPluginManager(AbstractTypeServerPluginContainer pc) {
        super(pc);
    }

    @Override
    public void loadPlugin(ServerPluginEnvironment env) throws Exception {
        super.loadPlugin(env);    // TODO: Customise this generated block

        AlertPluginDescriptorType type = (AlertPluginDescriptorType) env.getPluginDescriptor();

        String className = ((Element)type.getPluginClass()).getTextContent();
        if (!className.contains(".")) {
            className = type.getPackage() + "." + className;
        }
        try {
            Class.forName(className,false,env.getPluginClassLoader());
        }
        catch (Exception e) {
            log.warn("Can't find pluginClass " + className + ". Plugin will be ignored");
            return;
        }
        String shortName = ((Element) type.getShortName()).getTextContent();
        pluginClassByName.put(shortName,className);
        pluginNameToType.put(shortName,env.getPluginName());
        pluginEnvByName.put(shortName,env);

    }


    public AlertSender getAlertSenderForType(String shortName) {

        String className = pluginClassByName.get(shortName);
        ServerPluginEnvironment env = pluginEnvByName.get(shortName);
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

        sender.alertParameters = new Configuration(); // TODO
        sender.preferences = new Configuration();  // TODO

        return sender;
    }

    /**
     * Return the list of deployed alert sender plug-ins by their &lt;shortName&gt;
     * @return List of plugin names
     */
    public List<String> getPluginList() {
        return new ArrayList<String>(pluginClassByName.keySet());
    }
}
