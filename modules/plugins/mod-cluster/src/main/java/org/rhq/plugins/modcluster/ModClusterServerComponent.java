/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.modcluster;

import java.util.Set;

import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.modcluster.helper.JBossHelper;
import org.rhq.plugins.modcluster.model.ProxyInfo;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "rawtypes" })
public class ModClusterServerComponent extends MBeanResourceComponent {

    @Override
    public AvailabilityType getAvailability() {
        String rawProxyInfo = JBossHelper.getRawProxyInfo(getEmsBean());

        if (rawProxyInfo == null) {
            return AvailabilityType.DOWN;
        }

        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);
        if (proxyInfo.getAvailableNodes().size() == 0) {
            return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void getValues(MeasurementReport report, Set requests, EmsBean bean) {
        for (MeasurementScheduleRequest request : (Set<MeasurementScheduleRequest>) requests) {
            if (request.getName().equals("ProxyInformation")) {
                String rawProxyInfo = JBossHelper.getRawProxyInfo(bean);
                report.addData(new MeasurementDataTrait(request, rawProxyInfo));
                requests.remove(request);
                break;
            }
        }

        super.getValues(report, requests, bean);
    }
}
