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
package org.rhq.enterprise.server.system;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class SystemManagerBeanTest extends AbstractEJB3Test {
    private Subject overlord;
    private SystemManagerLocal systemManager;

    @BeforeMethod
    public void beforeMethod() {
        overlord = LookupUtil.getSubjectManager().getOverlord();
        systemManager = LookupUtil.getSystemManager();
    }

    public void testGetSystemConfiguration() {
        assert null != systemManager.getSystemConfiguration();
    }

    public void testAnalyze() {
        systemManager.analyze(overlord);
    }

    public void testEnableHibernateStatistics() {
        systemManager.enableHibernateStatistics();
    }

    public void testGetDatabaseType() {
        assert systemManager.getDatabaseType() instanceof DatabaseType;
    }

    public void testReindex() {
        systemManager.reindex(overlord);
    }

    public void testVacuum() {
        systemManager.vacuum(overlord);
    }

    public void testVacuumAppdef() {
        systemManager.vacuumAppdef(overlord);
    }
}