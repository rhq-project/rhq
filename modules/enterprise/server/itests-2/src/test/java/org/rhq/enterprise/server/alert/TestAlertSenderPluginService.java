/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.enterprise.server.alert;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.TestServerPluginService;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class TestAlertSenderPluginService extends TestServerPluginService {

    protected TestAlertSenderPluginService(File tmpdir) {
        super(tmpdir);
    }

    @Override
    protected List<AbstractTypeServerPluginContainer> createPluginContainers(MasterServerPluginContainer master) {
        return Collections.<AbstractTypeServerPluginContainer>singletonList(new TestAlertServerPluginContainer(master));
    }

    class TestAlertServerPluginContainer extends AlertServerPluginContainer {
        public TestAlertServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ServerPluginManager createPluginManager() {
            return new TestAlertServerPluginManager(this);
        }
    }

    class TestAlertServerPluginManager extends AlertSenderPluginManager {
        public TestAlertServerPluginManager(AlertServerPluginContainer pc) {
            super(pc);
        }

        @Override
        protected ServerPlugin getPlugin(ServerPluginEnvironment env) {
            return TestServerPluginService.getPlugin(env);
        }
    }    
}
