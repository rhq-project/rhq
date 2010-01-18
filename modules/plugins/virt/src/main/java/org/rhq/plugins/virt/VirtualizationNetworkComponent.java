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
package org.rhq.plugins.virt;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.libvirt.LibvirtException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Component for managing both virtual hosts and guests though some features are guest only and only
 * defined in the guest metadata.
 *
 * @author Greg Hinkle
 */
public class VirtualizationNetworkComponent implements ResourceComponent<VirtualizationHostComponent>,
    MeasurementFacet, ConfigurationFacet, CreateChildResourceFacet {

    private Log log = LogFactory.getLog(VirtualizationDomainComponent.class);
    private String networkName;
    private long cpuNanosLast;
    private long cpuCheckedLast;
    ResourceContext<VirtualizationHostComponent> resourceContext;

    public void start(ResourceContext<VirtualizationHostComponent> resourceContext)
        throws InvalidPluginConfigurationException, Exception {
        networkName = resourceContext.getResourceKey();
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            LibVirtConnection virt = getConnection();
            return virt.isNetworkActive(networkName) ? AvailabilityType.UP : AvailabilityType.DOWN;
        } catch (LibvirtException e) {
            log.error("Exception caught retriveing the domain info for " + networkName);
            throw new RuntimeException(e);
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        /*LibVirtConnection virt = this.getConnection();
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().equals("cpuTime")) {
                report.addData(new MeasurementDataNumeric(request,
                    (double) virt.getDomainInfo(domainName).domainInfo.cpuTime));
            } else if (request.getName().equals("cpuPercentage")) {
                long checked = System.nanoTime();
                long cpuNanos = virt.getDomainInfo(domainName).domainInfo.cpuTime;

                if (cpuCheckedLast != 0) {
                    long duration = checked - cpuCheckedLast;

                    long diff = cpuNanos - cpuNanosLast;

                    double percentage = ((double) diff) / ((double) duration);
                    report.addData(new MeasurementDataNumeric(request, percentage));
                }

                cpuCheckedLast = checked;
                cpuNanosLast = cpuNanos;
            } else if (request.getName().equals("memoryUsage")) {
                report.addData(new MeasurementDataNumeric(request,
                    (double) virt.getDomainInfo(domainName).domainInfo.memory));
            }
        }*/
    }

    public Configuration loadResourceConfiguration() throws LibvirtException {
        LibVirtConnection virt = getConnection();

        String xml = virt.getNetworkXML(networkName);
        boolean autostart = virt.getNetwork(networkName).getAutostart();
        return XMLEditor.getNetworkConfiguration(xml, autostart);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            LibVirtConnection virt = getConnection();

            Configuration config = report.getConfiguration();
            boolean autostart = config.getSimple("autostart").getBooleanValue();
            String xml = XMLEditor.getNetworkXml(config);
            virt.updateNetwork(networkName, xml, autostart);

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (LibvirtException e) {
            throw new RuntimeException(e);
        }
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        //  String xml = DomainConfigurationEditor.getXml(report.getResourceConfiguration());
        /*
                log.info("Defining new domain");
                log.debug("New virtualization domain xml:\n" + xml);

                try {
                    this.virt.defineDomain(xml);
                    report.setStatus(CreateResourceStatus.SUCCESS);
                } catch (LibvirtException e) {
                    log.error("Exception creating the domain", e);
                    report.setStatus(CreateResourceStatus.FAILURE);
                }*/

        return report;
    }

    public LibVirtConnection getConnection() throws LibvirtException {
        return resourceContext.getParentResourceComponent().getConnection();
    }

    public String getNetworkName() {
        return networkName;
    }
}