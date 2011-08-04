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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import static org.rhq.enterprise.server.RHQConstants.ACTIVE_DRIFT_PLUGIN;

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
        String pluginName = sysConfig.getProperty(ACTIVE_DRIFT_PLUGIN);

        if (pluginName == null) {
            throw new RuntimeException(ACTIVE_DRIFT_PLUGIN + " system configuration property is not set.");
        }

        return (DriftServerPluginFacet) getServerPluginComponent(pluginName);
    }

    private Properties getSysConfig() {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        SystemManagerLocal systemMgr = LookupUtil.getSystemManager();

        return systemMgr.getSystemConfiguration(subjectMgr.getOverlord());
    }

}