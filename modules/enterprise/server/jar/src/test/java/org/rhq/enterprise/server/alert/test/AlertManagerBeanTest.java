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
package org.rhq.enterprise.server.alert.test;

import java.util.List;
import java.util.Random;

import javax.persistence.EntityManager;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.alert.notification.ResultState;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Test for {@link AlertManagerLocal} SLSB.
 */
@Test
public class AlertManagerBeanTest extends AbstractEJB3Test {
    private AlertManagerLocal alertManager;
    private Subject superuser;
    private Resource newResource;


    @BeforeMethod
    public void beforeMethod() throws Exception {
        alertManager = LookupUtil.getAlertManager();
        superuser = LookupUtil.getSubjectManager().getOverlord();

        IDatabaseConnection connection = new DatabaseConnection(getConnection());
        DatabaseOperation.CLEAN_INSERT.execute(connection, getDataSet());

        newResource = getEntityManager().find(Resource.class, 1);
    }

    @AfterClass
    public void cleanupDB() throws Exception {
        if ("true".equals(System.getProperty("clean.db"))) {
            IDatabaseConnection connection = new DatabaseConnection(getConnection());
            DatabaseOperation.DELETE_ALL.execute(connection, getDataSet());    
        }
    }

    IDataSet getDataSet() throws Exception {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass()
            .getResourceAsStream("AlertManagerBeanTest.xml")));
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

        List<AlertConditionLog> alertConditionLogs = getEntityManager().createQuery(
            "from AlertConditionLog log where log.id = :id")
            .setParameter("id", 2)
            .getResultList();

        List<AlertNotificationLog> notificationLogs = getEntityManager().createQuery(
            "from AlertNotificationLog log where log.id = :id")
            .setParameter("id", 2)
            .getResultList();

        assertEquals("Failed to delete alerts by template", 1, deletedCount);
        assertEquals("Failed to delete alert condition logs when deleting alerts by template", 0,
            alertConditionLogs.size());
        assertEquals("Failed to delete alert notification logs when deleting alerts by template", 0,
            notificationLogs.size());
    }

    public void testAlertDeleteInRange() {
        assert 2 == alertManager.deleteAlerts(0L, System.currentTimeMillis() + 600000L); // go out into the future to make sure we get our alert
    }


}