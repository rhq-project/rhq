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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.domain.alert.builder;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.builder.condition.AbsoluteValueCondition;
import org.rhq.core.domain.alert.builder.condition.AvailabilityCondition;
import org.rhq.core.domain.alert.builder.condition.DriftCondition;
import org.rhq.core.domain.alert.builder.condition.ResourceConfigurationCondition;
import org.rhq.core.domain.alert.builder.notifier.SystemUserNotifier;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Michael Burman
 */
public class AlertBuilderTest {

    private AlertDefinitionTemplate createEmptyTemplate() {
        AlertDefinitionTemplate template = new AlertDefinitionTemplate(1, "testName")
                .enabled(true)
                .description("description")
                .priority(AlertPriority.MEDIUM)
                .alertProtocol(BooleanExpression.ANY);
        return template;
    }

    private AlertDefinitionTemplate addConditions(AlertDefinitionTemplate template) {
        // Create and add conditions
        AvailabilityCondition availabilityCondition = new AvailabilityCondition()
                .availability(AlertConditionOperator.AVAIL_GOES_DOWN);

        DriftCondition driftCondition = new DriftCondition()
                .expression(".*")
                .name(".*");

        ResourceConfigurationCondition resourceConfigurationCondition = new ResourceConfigurationCondition();

        AbsoluteValueCondition absoluteValueCondition = new AbsoluteValueCondition()
                .metric(10001)
                .value(0.5)
                .comparator(AlertConditionOperator.LESS_THAN);

        template.addCondition(availabilityCondition)
                .addCondition(driftCondition)
                .addCondition(resourceConfigurationCondition)
                .addCondition(absoluteValueCondition);

        return template;
    }

    @Test
    public void testTemplateToDefinitionBasics() {
        AlertDefinition alertDefinitionFromTemplate = createEmptyTemplate().getAlertDefinition();
        assertNotNull(alertDefinitionFromTemplate.getEnabled());
        assertEquals(AlertPriority.MEDIUM, alertDefinitionFromTemplate.getPriority());
        assertEquals("name", alertDefinitionFromTemplate.getName());
        assertEquals("description", alertDefinitionFromTemplate.getDescription());
        assertEquals(BooleanExpression.ANY, alertDefinitionFromTemplate.getConditionExpression());
    }

    @Test
    public void testDampening() {
        // Test empty without dampening rules
        AlertDefinitionTemplate template = createEmptyTemplate();
        AlertDefinition alertDefinition = template.getAlertDefinition();
        Assert.assertEquals(AlertDampening.Category.NONE, alertDefinition.getAlertDampening().getCategory());

        int occurrences = 2;
        int period = 3;

        // Add dampening rules
        AlertDampeningTemplate dampeningTemplate = new AlertDampeningTemplate()
                .category(AlertDampening.Category.DURATION_COUNT)
                .occurences(occurrences)
                .period(period)
                .time(AlertDampening.TimeUnits.MINUTES);

        template.dampening(dampeningTemplate);

        alertDefinition = template.getAlertDefinition();
        AlertDampening alertDampening = alertDefinition.getAlertDampening();
        assertEquals(occurrences, alertDampening.getValue());
        Assert.assertEquals(period, alertDampening.getPeriod());
    }

    @Test
    public void testRecovery() {
        // Test without recoveryDefinitions and with recoveryDefinitions
        AlertDefinitionTemplate alertDefinitionTemplate = createEmptyTemplate();
        alertDefinitionTemplate.recovery()
                .disableWhenFired(true);

        AlertDefinition alertDefinition = alertDefinitionTemplate.getAlertDefinition();
        assertEquals(true, alertDefinition.getWillRecover());
        assertEquals(Integer.valueOf(0), alertDefinition.getRecoveryId());

        alertDefinitionTemplate.recovery()
                .recoverAlert(1)
                .disableWhenFired(true);
        try {
            alertDefinition = alertDefinitionTemplate.getAlertDefinition();
            fail("Should have thrown IllegalStateException");
        } catch(IllegalStateException e) {
            // ok
        }

        alertDefinitionTemplate = createEmptyTemplate();
        alertDefinitionTemplate.recovery()
                .recoverAlert(1);
        alertDefinition = alertDefinitionTemplate.getAlertDefinition();
        Assert.assertEquals(Integer.valueOf(1), alertDefinition.getRecoveryId());
    }

    private void testSystemUserNotifier(AlertNotification notification) {
        assertEquals("SubjectsSender", notification.getSenderName());
        assertEquals("rhqadmin", notification.getConfiguration().getSimpleValue("subjectId"));
    }

    @Test
    public void testNotifications() {
        // Test through generic gateway
        AlertDefinitionTemplate alertDefinitionTemplate = createEmptyTemplate();

        Configuration recipients = new Configuration();
        recipients.setSimpleValue("subjectId", "rhqadmin");
        AlertNotificationTemplate notificationTemplate = new AlertNotificationTemplate()
                .sender("SubjectsSender")
                .configuration(recipients);
        alertDefinitionTemplate.addNotification(notificationTemplate);

        AlertDefinition alertDefinition = alertDefinitionTemplate.getAlertDefinition();
        assertEquals(1, alertDefinition.getAlertNotifications().size());
        AlertNotification notification = alertDefinition.getAlertNotifications().get(0);
        testSystemUserNotifier(notification);

        // Test helpers
        alertDefinitionTemplate = createEmptyTemplate();
        SystemUserNotifier notifier = new SystemUserNotifier()
                .addRecipient("rhqadmin");
        alertDefinitionTemplate.addNotification(notifier);
        assertEquals(1, alertDefinition.getAlertNotifications().size());
        notification = alertDefinition.getAlertNotifications().get(0);
        testSystemUserNotifier(notification);
    }

    @Test
    public void testConditions() {
        AlertDefinition alertDefinitionFromTemplate = addConditions(createEmptyTemplate()).getAlertDefinition();
        assertEquals(4, alertDefinitionFromTemplate.getConditions().size());

        for (AlertCondition alertCondition : alertDefinitionFromTemplate.getConditions()) {
            switch (alertCondition.getCategory()) {
                case DRIFT:
                    assertEquals(".*", alertCondition.getName());
                    assertEquals(".*", alertCondition.getOption());
                    break;
                case THRESHOLD:
                    assertEquals("<", alertCondition.getComparator());
                    assertEquals(0.5, alertCondition.getThreshold());
                    break;
                case AVAILABILITY:
                    assertEquals("AVAIL_GOES_DOWN", alertCondition.getOption());
                    break;
                case RESOURCE_CONFIG:
                    assertNull(alertCondition.getOption());
                    break;
                case AVAIL_DURATION:
                    break;
                case CHANGE:
                    break;
                case BASELINE:
                    break;
                case RANGE:
                    break;
                case TRAIT:
                    break;
                default:
                    fail("We did not store anything else than those defined earlier");
            }
        }

    }
}
