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
import org.libvirt.DomainInfo.DomainState;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Component for managing both virtual hosts and guests though some features are guest only and only
 * defined in the guest metadata.
 *
 * @author Greg Hinkle
 */
public class VirtualizationDomainComponent implements ResourceComponent<VirtualizationHostComponent>, MeasurementFacet,
    OperationFacet, ConfigurationFacet, CreateChildResourceFacet, DeleteResourceFacet {

    private Log log = LogFactory.getLog(VirtualizationDomainComponent.class);
    private String domainName;
    private String uri;
    private long cpuNanosLast;
    private long cpuCheckedLast;
    ResourceContext<VirtualizationHostComponent> resourceContext;

    public void start(ResourceContext<VirtualizationHostComponent> resourceContext)
        throws InvalidPluginConfigurationException, Exception {
        uri = resourceContext.getPluginConfiguration().getSimpleValue("connectionURI", "");
        domainName = resourceContext.getResourceKey();
        this.resourceContext = resourceContext;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            LibVirtConnection virt = getConnection();
            DomainState state = virt.getDomainInfo(domainName).domainInfo.state;
            switch (state) {
            case VIR_DOMAIN_RUNNING:
                return AvailabilityType.UP;
            case VIR_DOMAIN_PAUSED:
                return AvailabilityType.UP;
            default:
                return AvailabilityType.DOWN;
            }
        } catch (LibvirtException e) {
            log.error("Exception caught retriveing the domain info for " + domainName);
            throw new RuntimeException(e);
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        LibVirtConnection virt = this.getConnection();
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
        }
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        int result = -1;
        LibVirtConnection virt = getConnection();
        log.info("Executing " + name + " operation on domain " + getDomainName());
        if (name.equals("reboot")) {
            result = virt.domainReboot(this.domainName);
        } else if (name.equals("restore")) {
            result = virt.domainRestore(parameters.getSimpleValue("fromPath", null));
        } else if (name.equals("resume")) {
            result = virt.domainResume(this.domainName);
        } else if (name.equals("save")) {
            result = virt.domainSave(this.domainName, parameters.getSimpleValue("toPath", null));
        } else if (name.equals("shutdown")) {
            result = virt.domainShutdown(this.domainName);
        } else if (name.equals("suspend")) {
            result = virt.domainSuspend(this.domainName);
        } else if (name.equals("start")) {
            result = virt.domainCreate(this.domainName);
        } else if (name.equals("destroy")) {
            result = virt.domainDestroy(this.domainName);
        }

        if (result < 0) {
            throw new Exception("Failed to run " + name + " command. Result was: " + result);
        } else {
            return new OperationResult();
        }
    }

    public Configuration loadResourceConfiguration() throws LibvirtException {
        LibVirtConnection virt = getConnection();

        String xml = virt.getDomainXML(this.domainName);
        return XMLEditor.getDomainConfiguration(xml);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {

            LibVirtConnection virt = getConnection();
            String xml = virt.getDomainXML(this.domainName);

            Configuration oldConfig = loadResourceConfiguration();
            
            if (oldConfig == null) {
                throw new IllegalStateException("Failed to parse the XML specification for domain '" + domainName + "'.");
            }
            
            Configuration newConfig = report.getConfiguration();

            String newXml = XMLEditor.updateDomainXML(report.getConfiguration(), xml);

            log.info("Calling libvirt to redefine domain");
            if (!virt.defineDomain(newXml)) {
                log.warn("Call to redefine domain did not return a domain pointer");
            }

            // TODO GH: There seems to be some situations where an xml define doesn't change settings so we try a more direct approach here
            // TODO BK: Make this operations on the domain
            if (!oldConfig.getSimple("memory").getLongValue().equals(newConfig.getSimple("memory").getLongValue())) {
                virt.setMaxMemory(domainName, newConfig.getSimple("memory").getLongValue());
            }
            if (!oldConfig.getSimple("currentMemory").getLongValue().equals(
                newConfig.getSimple("currentMemory").getLongValue())) {
                virt.setMemory(domainName, newConfig.getSimple("currentMemory").getLongValue());
            }
            if (!oldConfig.getSimple("vcpu").getIntegerValue().equals(newConfig.getSimple("vcpu").getIntegerValue())) {
                virt.setVcpus(domainName, newConfig.getSimple("vcpu").getIntegerValue());
            }

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (LibvirtException e) {
            throw new RuntimeException(e);
        }
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        String xml = XMLEditor.getDomainXml(report.getResourceConfiguration());
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

    public String getDomainName() {
        return this.domainName;
    }

    public void deleteResource() throws Exception {
        LibVirtConnection virt = this.getConnection() ;
        virt.domainDelete(domainName) ;
    }
}