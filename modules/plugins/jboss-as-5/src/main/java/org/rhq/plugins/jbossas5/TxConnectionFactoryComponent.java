/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.SimpleValue;

/**
 * A Resource component for JBoss AS 5 Tx Connection Factories.
 *
 * @author Ian Springer
 */
public class TxConnectionFactoryComponent extends ManagedComponentComponent
{
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception
    {
        Set<MeasurementScheduleRequest> uncollectedMetrics = new HashSet();
        for (MeasurementScheduleRequest request : metrics)
        {
            try
            {
                if (request.getName().equals("custom.transactionType"))
                {
                    ManagedProperty xaTransactionProp = getManagedProperties().get("xa-transaction");
                    SimpleValue xaTransactionMetaValue = (SimpleValue)xaTransactionProp.getValue();
                    Boolean xaTransactionValue = (xaTransactionMetaValue != null)
                            ? (Boolean)xaTransactionMetaValue.getValue() : null;
                    boolean isXa = (xaTransactionValue != null && xaTransactionValue);
                    String transactionType = (isXa) ? "XA" : "Local";
                    report.addData(new MeasurementDataTrait(request, transactionType));
                }
                else
                {
                    uncollectedMetrics.add(request);
                }
            }
            catch (Exception e)
            {
                log.error("Failed to collect metric for " + request, e);
            }
        }
        super.getValues(report, uncollectedMetrics);
    }
}
