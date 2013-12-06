/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * A component for a "Managed Server" Resource.
 *
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class ManagedASComponent extends BaseComponent<HostControllerComponent<?>> {
    private static final String MANAGED_SERVER_TYPE_NAME = "Managed Server";

    private LogFileEventResourceComponentHelper logFileEventDelegate;

    @Override
    public void start(ResourceContext<HostControllerComponent<?>> hostControllerComponentResourceContext)
        throws InvalidPluginConfigurationException, Exception {
        super.start(hostControllerComponentResourceContext);

        logFileEventDelegate = new LogFileEventResourceComponentHelper(context);
        logFileEventDelegate.startLogFileEventPollers();

    }

    @Override
    public void stop() {
        super.stop();
        logFileEventDelegate.stopLogFileEventPollers();
    }

    /**
     * Get the availability of the managed AS server. We can't just check if
     * a connection succeeds, as the check runs against the API/HostController
     * and the managed server may still be down even if the connection succeeds.
     * @return Availability of the managed AS instance.
     */
    @Override
    public AvailabilityType getAvailability() {
        if (context.getResourceType().getName().equals(MANAGED_SERVER_TYPE_NAME)) {
            Address theAddress = new Address();
            String host = pluginConfiguration.getSimpleValue("domainHost", "local");
            theAddress.add("host", host);
            theAddress.add("server-config", myServerName);
            Operation getStatus = new ReadAttribute(theAddress, "status");
            Result result;
            try {
                result = getASConnection().execute(getStatus);
            } catch (Exception e) {
                log.warn(e.getMessage());
                return AvailabilityType.DOWN;
            }
            if (!result.isSuccess())
                return AvailabilityType.DOWN;

            String msg = result.getResult().toString();
            if (msg.contains("STARTED"))
                return AvailabilityType.UP;
            else
                return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> skmRequests = new HashSet<MeasurementScheduleRequest>(requests.size());
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());

        for (MeasurementScheduleRequest request : requests) {
            if (request.getName().startsWith("_skm:")) {
                skmRequests.add(request);
            } else if (request.getName().equals("startTime")) {
                String path = getPath();
                path = path.replace("server-config", "server");
                Address address = new Address(path);
                address.add("core-service", "platform-mbean");
                address.add("type", "runtime");
                Operation op = new ReadAttribute(address, "start-time");
                Result res = getASConnection().execute(op);

                if (res.isSuccess()) {
                    Long startTime = (Long) res.getResult();
                    MeasurementDataTrait data = new MeasurementDataTrait(request, new Date(startTime).toString());
                    report.addData(data);
                }
            } else if (request.getName().equals("multicastAddress")) {
                collectMulticastAddressTrait(report, request);
            } else {
                leftovers.add(request);
            }
        }

        // Now handle the skm (this could go into a common method with BaseServerComponent's impl.
        if (skmRequests.size() > 0) {
            Address address = new Address();
            ReadResource op = new ReadResource(address);
            op.includeRuntime(true);
            ComplexResult res = getASConnection().executeComplex(op);
            if (res.isSuccess()) {
                Map<String, Object> props = res.getResult();

                for (MeasurementScheduleRequest request : skmRequests) {
                    String requestName = request.getName();
                    String realName = requestName.substring(requestName.indexOf(':') + 1);
                    String val = null;
                    if (props.containsKey(realName)) {
                        val = getStringValue(props.get(realName));
                    }

                    if ("null".equals(val)) {
                        if (realName.equals("product-name"))
                            val = "JBoss AS";
                        else if (realName.equals("product-version"))
                            val = getStringValue(props.get("release-version"));
                        else
                            log.debug("Value for " + realName + " was 'null' and no replacement found");
                    }
                    MeasurementDataTrait data = new MeasurementDataTrait(request, val);
                    report.addData(data);
                }
            } else {
                log.debug("getSKMRequests failed: " + res.getFailureDescription());
            }
        }

        if (!leftovers.isEmpty())
            super.getValues(report, leftovers);
    }

    @Override
    protected Address getServerAddress() {
        String serverConfigElement = getAddress().get(1);
        String serverName = serverConfigElement.substring(serverConfigElement.indexOf('=') + 1);
        Address serverAddress = getAddress().getParent();
        serverAddress.add("server", serverName);
        return serverAddress;
    }

    @Override
    protected String getSocketBindingGroup() {
        String socketBindingGroup;
        try {
            socketBindingGroup = readAttribute("socket-binding-group");
        } catch (Exception e) {
            socketBindingGroup = null;
        }
        if (socketBindingGroup == null) {
            String group;
            try {
                group = readAttribute("group");
            } catch (Exception e) {
                group = null;
            }
            if (group != null) {
                Address groupAddress = new Address("server-group", group);
                try {
                    socketBindingGroup = readAttribute(groupAddress, "socket-binding-group");
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return socketBindingGroup;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();

        // We need to deduct the domain host from the path, as it is not encoded in the resource itself.
        String serverPath = path;
        try {
            serverPath = serverPath.substring(0, serverPath.indexOf(","));
            serverPath = serverPath.substring(serverPath.indexOf("=") + 1);
        } catch (RuntimeException e) {
            throw new Exception("Failed to extract hostname from server path [" + serverPath + "].", e);
        }
        configuration.put(new PropertySimple("hostname", serverPath));

        Operation op = new ReadResource(getAddress());
        ComplexResult res = getASConnection().executeComplex(op);
        if (res.isSuccess()) {
            Map<String, Object> map = res.getResult();
            String group = (String) map.get("group");
            configuration.put(new PropertySimple("group", group));

            Map<String, Object> sgMap = getServerGroupMap(group);

            String sbGroup = (String) map.get("socket-binding-group");
            if (sbGroup == null)
                sbGroup = (String) sgMap.get("socket-binding-group");

            configuration.put(new PropertySimple("socket-binding-group", sbGroup));
            Integer offSet = (Integer) map.get("socket-binding-port-offset");
            if (offSet == null)
                offSet = 0;
            configuration.put(new PropertySimple("socket-binding-port-offset", offSet));
        } else {
            throw new RuntimeException("Could not load configuration from remote server");
        }

        return configuration;
    }

    /**
     * Get the resource details of the server group with the given name
     * @param group Name of the server group to query
     * @return Map with the properties of the group. Or an empty map if the group does not exist.
     */
    private Map<String, Object> getServerGroupMap(String group) {
        Operation op = new ReadResource("server-group", group);
        ComplexResult cr = getASConnection().executeComplex(op);
        if (cr.isSuccess()) {
            return cr.getResult();
        }

        return Collections.emptyMap();
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        Operation op = new Operation(name, getAddress());
        op.addAdditionalProperty("blocking", Boolean.valueOf(parameters.getSimpleValue("blocking", "false")));
        Result res = getASConnection().execute(op,
            Integer.parseInt(parameters.getSimpleValue("operationTimeout", "120")));
        OperationResult opRes;
        if (res.isSuccess()) {
            opRes = new OperationResult("successfully invoked [" + name + "]");
        } else {
            opRes = new OperationResult("Operation [" + name + "] failed");
            opRes.setErrorMessage(res.getFailureDescription());
        }
        return opRes;
    }

}
