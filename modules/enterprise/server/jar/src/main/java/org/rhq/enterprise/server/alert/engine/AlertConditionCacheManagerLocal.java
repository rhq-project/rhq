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
package org.rhq.enterprise.server.alert.engine;

/**
 * @author Joseph Marques
 */

import javax.ejb.Local;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.operation.OperationHistory;

/**
 * This is the SLSB interface for interacting with the clustered cache that stores and computes whether AlertDefinition
 * conditions have become true for which Resources in the system. It consequently calls the condition listener MDB,
 * which stores the fired conditions to the backing store. A separate process later comes along and decides - depending
 * on the recovery property, enablement actions, and enablement filtering options that were set on the AlertDefinition
 * this condition was triggered against - whether an alert should fire or not.
 */
@Local
public interface AlertConditionCacheManagerLocal {
    /**
     * A MeasurementReport is full of MeasurementData objects. Each of these could potentially match against one of the
     * cache's conditions. So, each must be checked against it, to see whether it fires against any of the conditions.
     *
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(MeasurementData... measurementData);

    /**
     * A MeasurementReport is full of CallTimeData objects. Each of these could potentially match against one of the
     * cache's conditions. So, each must be checked against it, to see whether it fires against any of the conditions.
     *
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(CallTimeData... callTimeData);

    /**
     * Operation history occurs in two distinct phases. The first is when the operation is first triggered. An
     * OperationHistory element gets persisted whose status is INPROGRESS. The request is sent down to some agent that
     * carries out the requested work. Each agent will send a response back out-of-band and update the operation
     * accordingly. This method needs to be called in BOTH circumstances. Since the current implementation of the alerts
     * subsystem allows you to generate an alert based on ANY of the valid states an operation can be in, we must check
     * the conditions when the OperationHistory is in the only INPROGRESS state as well as any of the various
     * 'resultant' states.
     *
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(OperationHistory operationHistory);

    /**
     * Since Availability is no longer a measurement, it must be checked differently. As a side note, since Availability
     * is RLE users can ONLY be notified when the Availability changes. If the Availability for a resource doesn't
     * change between two consecutive reports, this method can be safely called, but it will not trigger an event.
     *
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(Availability... availability);

    /**
     * An EventReport is full of Event objects.  Each of these could potentially match against one of the cache's
     * conditions. So, each must be checked against it, to see whether it fires against any of the conditions.
     * 
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(EventSource source, Event... events);

    /**
     * ResourceConfiguration changes can be made through the UI or on the managed resource.  Whenever a new
     * ResourceConfigurationUpdate is created, it should be passed to this method
     * 
     * @return the number of conditions that were true against this argument
     */
    AlertConditionCacheStats checkConditions(ResourceConfigurationUpdate update);

    void reloadCachesForAgent(int agentId);

    void reloadGlobalCache();

    void reloadAllCaches();
}