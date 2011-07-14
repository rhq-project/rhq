/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.server.plugins.drift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginFacet;

/**
 * A drift server-side plugin component that the server uses to process drift files.
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class DriftServerPluginComponent implements ServerPluginComponent, DriftServerPluginFacet, ControlFacet {

    private final Log log = LogFactory.getLog(DriftServerPluginComponent.class);

    @SuppressWarnings("unused")
    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.debug("The RHQ Drift plugin has been initialized!!! : " + this);
    }

    public void start() {
        log.debug("The RHQ Drift plugin has started!!! : " + this);
    }

    public void stop() {
        log.debug("The RHQ Drift plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        log.debug("The RHQ Drift plugin has been shut down!!! : " + this);
    }

    @Override
    public DriftFile fetchDriftFile(String sha256) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void storeDriftFile(DriftFile driftFile) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public ControlResults invoke(String name, Configuration parameters) {
        // TODO Auto-generated method stub
        return null;
    }

}
