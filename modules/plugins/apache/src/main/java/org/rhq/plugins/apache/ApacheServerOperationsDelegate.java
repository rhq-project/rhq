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
package org.rhq.plugins.apache;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.apache.util.OsProcessUtility;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;

/**
 * Executes operations on an Apache server ({@link ApacheServerComponent} delegates to this class).
 *
 * @author Ian Springer
 */
public class ApacheServerOperationsDelegate implements OperationFacet {
    private static final String EXIT_CODE_RESULT_PROP = "exitCode";
    private static final String OUTPUT_RESULT_PROP = "output";

    /**
     * Server component against which the operations are being performed.
     */
    private ApacheServerComponent serverComponent;

    /**
     * Passed in from the resource context for making process calls.
     */
    private SystemInfo systemInfo;

    /**
     * The plugin configuration of the server component.
     */
    private Configuration serverPluginConfiguration;
    
    // Constructors  --------------------------------------------

    public ApacheServerOperationsDelegate(ApacheServerComponent serverComponent, Configuration serverPluginConfiguration, SystemInfo systemInfo) {
        this.serverComponent = serverComponent;
        this.systemInfo = systemInfo;
        this.serverPluginConfiguration = serverPluginConfiguration;
    }

    public void startOperationFacet(OperationContext context) {
    }

    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        if ("install_mod_jk".equals(name)) {
            return ModJKComponent.installModJk(serverComponent, params);
        }

        // Continue with generic operations
        Operation operation = getOperation(name);
        File controlScriptPath = this.serverComponent.getControlScriptPath();
        validateScriptFile(controlScriptPath);
        ProcessExecution processExecution = ProcessExecutionUtility.createProcessExecution(controlScriptPath);
        processExecution.setWaitForCompletion(1000 * 30); // 30 seconds - should be plenty
        processExecution.setCaptureOutput(true); // essential, since we want to include the output in the result
        
        addDefaultArguments(processExecution);
        
        //we always add some arguments to the control script thus forcing the passthrough mode.
        //therefore no matter if we use httpd, Apache.exe or apachectl, the -k argument will always
        //be used to specify the operation to invoke.
        if (operation != Operation.CONFIG_TEST) {
            processExecution.getArguments().add("-k");
        }

        switch (operation) {
        case START: {
            processExecution.getArguments().add("start");
            break;
        }

        case STOP: {
            processExecution.getArguments().add("stop");
            break;
        }

        case RESTART: {
            abortIfOsIsWindows(name);
            processExecution.getArguments().add("restart");
            break;
        }

        case START_SSL: {
            abortIfOsIsWindows(name);
            processExecution.getArguments().add("startssl");
            break;
        }

        case GRACEFUL_RESTART: {
            processExecution.getArguments().add((osIsWindows()) ? "restart" : "graceful");
            break;
        }

        case CONFIG_TEST: {
            // abortIfOsIsWindows(name);
            processExecution.getArguments().add("-t");
            break;
        }
        
        case DETECT_SNMP_INDEXES: {
            return detectSnmpIndexes();
        }
        }

        ProcessExecutionResults processExecutionResults = this.systemInfo.executeProcess(processExecution);
        Integer exitCode = processExecutionResults.getExitCode();

        // Do some more aggressive result code checking, as otherwise errors are not reported as such
        // in the GUI -- see RHQ-627
        // We might want to investigate this agin later.
        if (processExecutionResults.getError() != null || (exitCode != null && exitCode != 0)) {
            String msg = "Operation " + operation + " failed. Exit code: [" + exitCode + "]\n, Output : ["
                + processExecutionResults.getCapturedOutput() + "]\n" + "Error: [" + processExecutionResults.getError()
                + "]";
            throw new Exception(msg);
        }

        return createOperationResult(processExecutionResults);
    }

    private void addDefaultArguments(ProcessExecution processExecution) throws Exception {
        List<String> args = processExecution.getArguments();
        
        //these plugin config properties are required and readonly, so they should never be null
        args.add("-d");
        args.add(serverPluginConfiguration.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, null));
        args.add("-f");
        args.add(serverPluginConfiguration.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_HTTPD_CONF, null));
    }

    private OperationResult createOperationResult(ProcessExecutionResults processExecutionResults) {
        OperationResult operationResult = new OperationResult();

        Integer exitCode = processExecutionResults.getExitCode();
        String output = processExecutionResults.getCapturedOutput(); // NOTE: this is stdout + stderr

        Configuration complexResults = operationResult.getComplexResults();
        complexResults.put(new PropertySimple(EXIT_CODE_RESULT_PROP, exitCode));
        complexResults.put(new PropertySimple(OUTPUT_RESULT_PROP, output));

        return operationResult;
    }

    private boolean osIsWindows() {
        return this.systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS;
    }

    private static void validateScriptFile(File scriptFile) {
        if (!scriptFile.exists()) {
            throw new IllegalStateException("Script (" + scriptFile + ") specified via '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH
                + "' connection property does not exist.");
        }

        if (scriptFile.isDirectory()) {
            throw new IllegalStateException("Script (" + scriptFile + ") specified via '"
                + ApacheServerComponent.PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH
                + "' connection property is a directory, not a file.");
        }
    }

    private static Operation getOperation(String name) {
        Operation operation;
        try {
            operation = Operation.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid operation name: " + name);
        }

        return operation;
    }

    private void abortIfOsIsWindows(String name) {
        if (osIsWindows()) {
            throw new IllegalArgumentException("The " + name + " operation is not supported on Windows.");
        }
    }

    private OperationResult detectSnmpIndexes() throws Exception {
        PropertyList vhostList = new PropertyList("snmpIndexesPerResourceKey");
        
        ApacheDirectiveTree tree = serverComponent.loadParser();
        tree = RuntimeApacheConfiguration.extract(tree, serverComponent.getCurrentProcessInfo(), serverComponent.getCurrentBinaryInfo(), serverComponent.getModuleNames());
        ApacheVirtualHostServiceDiscoveryComponent.SnmpWwwServiceIndexes snmpDiscoveries = ApacheVirtualHostServiceDiscoveryComponent.getSnmpDiscoveries(serverComponent);
        
        String mainVhostRK = ApacheVirtualHostServiceDiscoveryComponent.createMainServerResourceKey(serverComponent, tree, snmpDiscoveries);
        
        List<ApacheDirective> virtualHosts = tree.search("/<VirtualHost");
        
        //the vhosts detected can actually have conflicting resource keys. So before we build the resulting configuration object
        //let's pass the results through this map where we can check for those conflicts more easily.
        LinkedHashMap<String, Integer> mapping = new LinkedHashMap<String, Integer>();
        mapping.put(mainVhostRK, 1);
        
        int idx = 0;
        for(ApacheDirective vhost : virtualHosts) {
            List<String> hosts = vhost.getValues();

            List<ApacheDirective> serverNames = vhost.getChildByName("ServerName");
            String serverName = null;
            if (serverNames.size() > 0) {
                serverName = serverNames.get(0).getValuesAsString();
            }

            String resourceKey = ApacheVirtualHostServiceDiscoveryComponent.createResourceKey(serverName, hosts, tree, serverComponent, snmpDiscoveries);
            
            int snmpIdx = virtualHosts.size() - idx + 1;
                      
            if (!mapping.containsKey(resourceKey)) {
                mapping.put(resourceKey, snmpIdx);
            }
            
            ++idx;
        }
        
        for (Map.Entry<String, Integer> entry : mapping.entrySet()) {
            PropertyMap vhostRow = new PropertyMap("snmpIndexForResourceKey");
            vhostList.add(vhostRow);
            
            vhostRow.put(new PropertySimple("resourceKey", entry.getKey()));
            vhostRow.put(new PropertySimple("snmpWwwServiceIndex", entry.getValue()));
        }
        
        OperationResult ret = new OperationResult();
        ret.getComplexResults().put(vhostList);
        
        return ret;
    }
    
    /**
     * Enumeration of supported operations for an Apache server.
     */
    private enum Operation {
        START, STOP, RESTART, START_SSL, GRACEFUL_RESTART, CONFIG_TEST, DETECT_SNMP_INDEXES
    }
}