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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
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
import org.rhq.plugins.virt.LibVirtConnection.HVInfo;

/**
 * Component for managing both virtual hosts and guests though some features are guest only and only
 * defined in the guest metadata.
 *
 * @author Greg Hinkle
 */
public class VirtualizationHostComponent implements ResourceComponent, MeasurementFacet, ConfigurationFacet,
    CreateChildResourceFacet {

    private Log log = LogFactory.getLog(VirtualizationDomainComponent.class);
    private String uri = "";
    private LibVirtConnection virt;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        uri = resourceContext.getPluginConfiguration().getSimpleValue("connectionURI", "");
        virt = new LibVirtConnection(uri);
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            virt.getHVInfo();
            return AvailabilityType.UP;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        HVInfo hi = virt.getHVInfo();
        for (MeasurementScheduleRequest request : metrics) {
            System.out.println(request.getName());
            if (request.getName().equals("cpus")) {
                report.addData(new MeasurementDataTrait(request, "" + hi.nodeInfo.cpus));
            } else if (request.getName().equals("memory")) {
                report.addData(new MeasurementDataTrait(request, "" + hi.nodeInfo.memory));
            } else if (request.getName().equals("memoryUsage")) {
                report.addData(new MeasurementDataNumeric(request, virt.getMemoryPercentage()));
            }
        }
    }

    public Configuration loadResourceConfiguration() throws LibvirtException {
        Configuration config = new Configuration();
        HVInfo info = virt.getHVInfo();
        config.put(new PropertySimple("hypervisorType", info.hvType));
        config.put(new PropertySimple("hostName", info.hostname));
        config.put(new PropertySimple("libvirtVersion", info.libvirtVersion));
        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        /*try {
            String xml = this.virt.getDomainXML(this.domainName);

            Configuration oldConfig = loadResourceConfiguration();
            Configuration newConfig = report.getConfiguration();

            String newXml = DomainConfigurationEditor.updateXML(report.getConfiguration(), xml);

            log.info("Calling libvirt to redefine domain");
            if (!this.virt.defineDomain(newXml)) {
                log.warn("Call to redefine domain did not return a domain pointer");
            }

            // TODO GH: There seems to be some situations where an xml define doesn't change settings so we try a more direct approach here
            // TODO BK: Make this operations on the domain
            if (!oldConfig.getSimple("memory").getLongValue().equals(newConfig.getSimple("memory").getLongValue())) {
                this.virt.setMaxMemory(domainName, newConfig.getSimple("memory").getLongValue());
            }
            if (!oldConfig.getSimple("currentMemory").getLongValue().equals(
                newConfig.getSimple("currentMemory").getLongValue())) {
                this.virt.setMemory(domainName, newConfig.getSimple("currentMemory").getLongValue());
            }
            if (!oldConfig.getSimple("vcpu").getIntegerValue().equals(newConfig.getSimple("vcpu").getIntegerValue())) {
                this.virt.setVcpus(domainName, newConfig.getSimple("vcpu").getIntegerValue());
            }

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (LibvirtException e) {
            throw new RuntimeException(e);
        }*/
    }

    // TODO 
    public CreateResourceReport createResource(CreateResourceReport report) {
        /*String xml = DomainConfigurationEditor.getXml(report.getResourceConfiguration());

        log.info("Defining new domain");
        log.debug("New virtualization domain xml:\n" + xml);

        try {
            this.virt.defineDomain(xml);
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (LibvirtException e) {
            log.error("Exception creating the domain", e);
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        */
        return report;
    }

    public LibVirtConnection getConnection() throws LibvirtException {
        //return this.resourceContext.getParentResourceComponent().getConnection();
        return null;
    }

}