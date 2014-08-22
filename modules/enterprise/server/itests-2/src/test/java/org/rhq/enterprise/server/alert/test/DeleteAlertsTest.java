/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.alert.test;

import java.sql.Connection;
import java.util.List;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link AlertManagerLocal} SLSB.
 */
@Test(enabled = false)
public class DeleteAlertsTest extends AbstractEJB3Test {
    private AlertManagerLocal alertManager;
    private PurgeManagerLocal purgeManager;
    private Subject superuser;
    private Resource newResource;

    @Override
    protected void beforeMethod() throws Exception {
        alertManager = LookupUtil.getAlertManager();
        purgeManager = LookupUtil.getPurgeManager();
        superuser = LookupUtil.getSubjectManager().getOverlord();

        Connection connection = null;

        try {
            connection = getConnection();
            IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
            DatabaseOperation.REFRESH.execute(dbUnitConnection, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        newResource = getEntityManager().find(Resource.class, 1);
    }

    @Override
    protected void afterMethod() throws Exception {
        if ("true".equals(System.getProperty("clean.db"))) {
            Connection connection = null;

            try {
                connection = getConnection();
                IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
                DatabaseOperation.DELETE.execute(dbUnitConnection, getDataSet());
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }

    IDataSet getDataSet() throws Exception {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(
            "AlertManagerBeanTest.xml")));
        xmlProducer.setColumnSensing(true);
        return new FlatXmlDataSet(xmlProducer);
    }

    public void deleteAlertsForResource() {
        assert 1 == alertManager.deleteAlertsByContext(superuser, EntityContext.forResource(newResource.getId()));
    }

    @SuppressWarnings("unchecked")
    public void deleteAlertsForResourceTemplate() {
        int resourceTypeId = 1;
        int deletedCount = alertManager.deleteAlertsByContext(superuser, EntityContext.forTemplate(resourceTypeId));

        List<AlertConditionLog> alertConditionLogs = getEntityManager()
            .createQuery("from AlertConditionLog log where log.id = :id").setParameter("id", 2).getResultList();

        List<AlertNotificationLog> notificationLogs = getEntityManager()
            .createQuery("from AlertNotificationLog log where log.id = :id").setParameter("id", 2).getResultList();

        assertEquals("Failed to delete alerts by template", 1, deletedCount);
        assertEquals("Failed to delete alert condition logs when deleting alerts by template", 0,
            alertConditionLogs.size());
        assertEquals("Failed to delete alert notification logs when deleting alerts by template", 0,
            notificationLogs.size());
    }

    public void testAlertDeleteInRange() {
        // go out into the future to make sure we get our alert
        assert 2 == purgeManager.deleteAlerts(0L, System.currentTimeMillis() + 600000L);
    }

}
