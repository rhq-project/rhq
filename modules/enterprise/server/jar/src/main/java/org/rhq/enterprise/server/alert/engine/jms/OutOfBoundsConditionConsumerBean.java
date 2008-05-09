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
package org.rhq.enterprise.server.alert.engine.jms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.oob.MeasurementOutOfBounds;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.jms.model.OutOfBoundsConditionMessage;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Use the default message provider
 * 
 * @author Joseph Marques
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/OutOfBoundsConditionQueue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
    @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "NonDurable") })
public class OutOfBoundsConditionConsumerBean implements MessageListener {
    private final Log log = LogFactory.getLog(OutOfBoundsConditionConsumerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    @SuppressWarnings("unused")
    private EntityManager entityManager;

    public void onMessage(Message message) {
        OutOfBoundsConditionMessage outOfBoundsMessage = null;
        MeasurementOutOfBounds oob = null;
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            Object object = objectMessage.getObject();

            outOfBoundsMessage = (OutOfBoundsConditionMessage) object;

            MeasurementSchedule schedule = new MeasurementSchedule();
            schedule.setId(outOfBoundsMessage.getScheduleId());

            oob = new MeasurementOutOfBounds(schedule, outOfBoundsMessage.getTimestamp(), outOfBoundsMessage
                .getOobValue());

            // JBNADM-2772 (Make sure the txn doesn't get rolled back for constraint violations)
            entityManager.persist(oob);
            entityManager.flush();
            //problemResourceManager.addMeasurementOutOfBounds(oob); // the exception flowing cross bean call boundaries was setting rollback only
        } catch (Exception e) {
            log.error("Error persisting OOB, Message: " + e.getMessage());
            log.error("Is cache in a valid state? -- "
                + (LookupUtil.getAlertConditionCacheManager().isCacheValid() ? "yes" : "no"));

            if (outOfBoundsMessage != null) {
                DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");

                log.error("for scheduleId: " + outOfBoundsMessage.getScheduleId());
                log.error("Original @ " + df.format(new Date(outOfBoundsMessage.getTimestamp())) + " - "
                    + outOfBoundsMessage.getOobValue());
            }
            if (oob != null) {
                log.error("Duplicate OOB?: " + oob.toString());
            }
        }
    }
}