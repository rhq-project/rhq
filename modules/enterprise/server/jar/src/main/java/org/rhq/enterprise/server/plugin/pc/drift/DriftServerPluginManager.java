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
package org.rhq.enterprise.server.plugin.pc.drift;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

import static org.rhq.core.domain.util.StringUtils.getListAsString;
import static org.rhq.core.domain.util.StringUtils.getStringAsList;
import static org.rhq.enterprise.server.RHQConstants.ACTIVE_DRIFT_PLUGIN;
import static org.rhq.enterprise.server.RHQConstants.DRIFT_PLUGINS;

/**
 * This loads in all drift server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author Jay Shaughnessy 
 * @author John Sanda
 */
public class DriftServerPluginManager extends ServerPluginManager {

    private final Log log = LogFactory.getLog(this.getClass());

    public DriftServerPluginManager(DriftServerPluginContainer pc) {
        super(pc);
    }

    public DriftServerPluginFacet getDriftServerPluginComponent() {
        Properties sysConfig = getSysConfig();
        String value = sysConfig.getProperty(ACTIVE_DRIFT_PLUGIN);

        if (value == null) {
            throw new RuntimeException(ACTIVE_DRIFT_PLUGIN + " system configuration property is not set.");
        }

        return (DriftServerPluginFacet) getServerPluginComponent(value.substring(1, value.indexOf("$$")));
    }

    @Override
    protected void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        super.loadPlugin(env, enabled);

        String pluginName = env.getPluginKey().getPluginName();
        List<String> plugins = getDriftPlugins();

        int index = findPluginIndex(plugins, pluginName);
        if (index == -1) {
            plugins.add("[" + pluginName + "$$" + env.getPluginDescriptor().getDisplayName() + "]");
            saveDriftPlugins(plugins);
        }

    }

    @Override
    protected void unloadPlugin(String pluginName) throws Exception {
        super.unloadPlugin(pluginName);

        List<String> plugins = getDriftPlugins();
        int index = findPluginIndex(plugins, pluginName);

        if (index != -1) {
            plugins.remove(index);
            saveDriftPlugins(plugins);
        }
    }

    private List<String> getDriftPlugins() {
        Properties sysConfig = getSysConfig();
        return getStringAsList(sysConfig.getProperty(DRIFT_PLUGINS, ""), ",", true);
    }

    private void saveDriftPlugins(List<String> plugins) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        SystemManagerLocal systemMgr = LookupUtil.getSystemManager();

        Properties sysConfig = systemMgr.getSystemConfiguration(subjectMgr.getOverlord());
        sysConfig.put(DRIFT_PLUGINS, getListAsString(plugins, ","));
        systemMgr.setSystemConfiguration(subjectMgr.getOverlord(), sysConfig, false);
    }

    private Properties getSysConfig() {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        SystemManagerLocal systemMgr = LookupUtil.getSystemManager();

        return systemMgr.getSystemConfiguration(subjectMgr.getOverlord());
    }

    private int findPluginIndex(List<String> plugins, String name) {
        int i = 0;
        for (String value : plugins) {
            // value is of the form, [plugin_name$$plugin_display_name]
            if (name.equals(value.substring(1, value.indexOf("$$")))) {
                return i;
            }
            ++i;
        }
        return -1;
    }


}