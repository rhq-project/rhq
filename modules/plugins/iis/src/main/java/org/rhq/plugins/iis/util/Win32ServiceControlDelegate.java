/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.iis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Support for starting and stopping windows services along with their dependencies and dependents.
 *
 * @author Greg Hinkle
 */
public class Win32ServiceControlDelegate {

    private Log log = LogFactory.getLog(Win32ServiceControlDelegate.class);

    private String serviceName;

    private List<String> dependencies;
    private List<String> reverseDependencies;
    private List<String> dependents;
    private List<String> reverseDependents;

    private Service service;
    private long timeout;

    public Win32ServiceControlDelegate(String serviceName, long timeout) throws Win32Exception {
        this(serviceName, null, null, timeout);
    }

    public Win32ServiceControlDelegate(String serviceName, List<String> dependencies, List<String> dependents,
        long timeout) throws Win32Exception {
        log.debug("serviceName = " + serviceName);
        log.debug("serviceDependencies = " + (dependencies == null ? "none" : dependencies));
        log.debug("serviceDependents = " + (dependents == null ? "none" : dependents));
        log.debug("serviceOperationTimeout = " + timeout + " ms");

        this.serviceName = serviceName;
        this.dependencies = dependencies;
        if (dependencies != null) {
            this.reverseDependencies = new ArrayList<String>(dependencies);
            Collections.reverse(this.reverseDependencies);
        }
        this.dependents = dependents;
        if (dependents != null) {
            this.reverseDependents = new ArrayList<String>(dependents);
            Collections.reverse(this.dependents);
        }
        this.timeout = timeout;

        service = new Service(serviceName);
    }

    public AvailabilityType getAvailabilityType() {
        return getAvailability(this.service);
    }

    public void start() throws Win32Exception {
        startServices(dependencies);
        service.start();
        startServices(dependents);
    }

    public void stop() throws Win32Exception {
        stopServices(reverseDependents);
        service.stop();
        stopServices(reverseDependencies);
    }

    public void restart() throws Win32Exception {
        stop();
        start();
    }

    protected void startServices(List<String> services) {
        if (services == null)
            return;

        log.debug("Starting list of services for Service [" + serviceName + "], services [" + services + "]");
        for (String relatedServiceName : services) {
            try {
                Service relatedService = new Service(relatedServiceName);
                int relatedServiceStatus = relatedService.getStatus();
                if (relatedServiceStatus == Service.SERVICE_STOPPED
                    || relatedServiceStatus == Service.SERVICE_STOP_PENDING) {
                    log.debug("Starting Service [" + relatedServiceName + "]");
                    relatedService.start(this.timeout);
                    log.debug("Service started [" + relatedServiceName + "]");
                    relatedService.close();
                }
            } catch (Exception e) {
                log.warn("Unable to start [" + relatedServiceName + "] from the list [" + services
                    + "]. Will continue with the rest. Cause:" + e);
            }
        }
    }

    protected void stopServices(List<String> services) {
        if (services == null)
            return;

        log.debug("Stopping list of services for Service [" + serviceName + "], services [" + services + "]");
        for (String relatedServiceName : services) {
            try {
                Service relatedService = new Service(relatedServiceName);
                int relatedServiceStatus = relatedService.getStatus();
                if (!(relatedServiceStatus == Service.SERVICE_STOPPED || relatedServiceStatus == Service.SERVICE_STOP_PENDING)) {
                    log.debug("Stopping Service [" + relatedServiceName + "]");
                    relatedService.stop(this.timeout);
                    log.debug("Service stopped [" + relatedServiceName + "]");
                    relatedService.close();
                }
            } catch (Exception e) {
                log.warn("Unable to stop [" + relatedServiceName + "] from the list [" + services
                    + "]. Will continue with the rest. Cause:" + e);
            }
        }
    }

    protected AvailabilityType getAvailability(Service service) {
        switch (service.getStatus()) {
        case Service.SERVICE_START_PENDING:
        case Service.SERVICE_STOP_PENDING:
        case Service.SERVICE_RUNNING:
            return AvailabilityType.UP;

        case Service.SERVICE_STOPPED:
        case Service.SERVICE_CONTINUE_PENDING:
        case Service.SERVICE_PAUSE_PENDING:
        case Service.SERVICE_PAUSED:
        default:
            return AvailabilityType.DOWN;

        }
    }

}
