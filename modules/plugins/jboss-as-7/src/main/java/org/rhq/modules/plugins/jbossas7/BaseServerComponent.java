/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Base component for functionality that is common to Standalone Servers and Host Controllers.
 *
 * @author Heiko W. Rupp
 */
public class BaseServerComponent<T extends ResourceComponent<?>> extends BaseComponent<T> implements MeasurementFacet {

    private static final String SEPARATOR = "\n-----------------------\n";
    final Log log = LogFactory.getLog(BaseServerComponent.class);

    /**
     * Restart the server by first executing a 'shutdown' operation via its API. Then call
     * the #startServer method to start it again.
     *
     * @param parameters Parameters to pass to the (recursive) invocation of #invokeOperation
     * @param mode Mode of the server to start (domain or standalone)
     * @return State of execution
     * @throws Exception If anything goes wrong
     */
    protected OperationResult restartServer(Configuration parameters, AS7Mode mode) throws Exception {
        OperationResult tmp = invokeOperation("shutdown", parameters);

        if (tmp.getErrorMessage()!=null) {
            tmp.setErrorMessage("Restart failed while failing to shut down: " + tmp.getErrorMessage());
            return tmp;
        }

        context.getAvailabilityContext().requestAvailabilityCheck();

        return startServer(mode);
    }

    protected boolean waitUntilDown(OperationResult tmp) throws InterruptedException {
        boolean down=false;
        int count=0;
        while (!down) {
            Operation op = new ReadAttribute(new Address(),"release-version");
            Result res = getASConnection().execute(op);
            if (!res.isSuccess()) { // If op succeeds, server is not down
                down=true;
            } else if (count > 20) {
                tmp.setErrorMessage("Was not able to shut down the server");
                return true;
            }
            if (!down) {
                Thread.sleep(1000); // Wait 1s
            }
            count++;
        }
        log.debug("waitUntilDown: Used " + count + " delay round(s) to shut down");
        return false;
    }

    /**
     * Start the server by calling the start script listed in the plugin configuration. If a different
     * config is given, this is passed via --server-config
     * @return State of Execution.
     * @param mode Mode of the server to start (domain or standalone)
     */
    protected OperationResult startServer(AS7Mode mode) {
        OperationResult operationResult = new OperationResult();
        String startScript = pluginConfiguration.getSimpleValue("startScript", mode.getStartScript());
        String homeDir = pluginConfiguration.getSimpleValue("homeDir", "");
        if (homeDir.isEmpty()) {
            operationResult.setErrorMessage("No home directory provided.");
            return operationResult;
        }
        String script = homeDir + File.separator + startScript;

        ProcessExecution processExecution;
        processExecution = ProcessExecutionUtility.createProcessExecution(new File("/bin/sh"));

        String config = pluginConfiguration.getSimpleValue(mode.getConfigPropertyName(), mode.getDefaultXmlFile());
        List<String> arguments = processExecution.getArguments();
        if (arguments==null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

        arguments.add(script);

        if (!config.equals(mode.getDefaultXmlFile())) {
            arguments.add(mode.getConfigArg());
            arguments.add(config);
        }
        if (mode==AS7Mode.DOMAIN) {
            // We also need to check for host-config
            config =  pluginConfiguration.getSimpleValue(AS7Mode.HOST.getConfigPropertyName(),AS7Mode.HOST.getDefaultXmlFile());
            if (!config.equals(AS7Mode.HOST.getDefaultXmlFile())) {
                arguments.add(AS7Mode.HOST.getConfigArg());
                arguments.add(config);

            }
        }
        processExecution.setWorkingDirectory(homeDir);
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(15000L); // 15 seconds // TODO: Should we wait longer than 15 seconds?
        processExecution.setKillOnTimeout(false);

        String javaHomeDir = pluginConfiguration.getSimpleValue("javaHomePath",null);
        if (javaHomeDir!=null) {
            processExecution.getEnvironmentVariables().put("JAVA_HOME", javaHomeDir);
        }

        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = context.getSystemInformation().executeProcess(processExecution);
        logExecutionResults(results);
        if (results.getError()!=null) {
            operationResult.setErrorMessage(results.getError().getMessage());
        } else if (results.getExitCode()!=null) {
            operationResult.setErrorMessage("Start failed with error code " + results.getExitCode() + ":\n" + results.getCapturedOutput());
        } else {

            // Lets try to connect to the server
            boolean up=false;
            int count=0;
            while (!up) {
                Operation op = new ReadAttribute(new Address(),"release-version");
                Result res = getASConnection().execute(op);
                if (res.isSuccess()) { // If op succeeds, server is not down
                    up=true;
                } else if (count > 20) {
                    operationResult.setErrorMessage("Was not able to start the server");
                    return operationResult;
                }
                if (!up) {
                    try {
                        Thread.sleep(1000); // Wait 1s
                    } catch (InterruptedException e) {
                        ; // Ignore
                    }
                }
                count++;
            }

            operationResult.setSimpleResult("Success");
        }



        context.getAvailabilityContext().requestAvailabilityCheck();

        return operationResult;

    }

    private void logExecutionResults(ProcessExecutionResults results) {
        // Always log the output at info level. On Unix we could switch depending on a exitCode being !=0, but ...
        log.info("Exit code from process execution: " + results.getExitCode());
        log.info("Output from process execution: " + SEPARATOR + results.getCapturedOutput() + SEPARATOR);
    }

    /**
     * Do some post processing of the Result - especially the 'shutdown' operation needs a special
     * treatment.
     * @param name Name of the operation
     * @param res Result of the operation vs. AS7
     * @return OperationResult filled in from values of res
     */
    protected OperationResult postProcessResult(String name, Result res) {
        OperationResult operationResult = new OperationResult();
        if (res==null) {
            operationResult.setErrorMessage("No result received from server");
            return operationResult;
        }

        if (name.equals("shutdown") || name.equals("reload")) {
            /*
             * Shutdown needs a special treatment, because after sending the operation, if shutdown succeeds,
             * the server connection is closed and we can't read from it. So if we get connection refused for
             * reading, this is a good sign.
             */
            if (!res.isSuccess()) {
                if (res.getRhqThrowable()!=null && (res.getRhqThrowable() instanceof ConnectException || res.getRhqThrowable().getMessage().equals("Connection refused"))) {
                    operationResult.setSimpleResult("Success");
                    log.debug("Got a ConnectionRefused for operation " + name + " this is considered ok, as the remote server sometimes closes the communications channel before sending a reply");
                }
                if (res.getFailureDescription().contains("Socket closed")) { // See https://issues.jboss.org/browse/AS7-4192
                    operationResult.setSimpleResult("Success");
                    log.debug("Got a 'Socket closed' result from AS for operation " + name );
                }
                else
                    operationResult.setErrorMessage(res.getFailureDescription());
            }
            else {
                operationResult.setSimpleResult("Success");
            }
        }
        else {
            if (res.isSuccess()) {
                if (res.getResult()!=null)
                    operationResult.setSimpleResult(res.getResult().toString());
                else
                    operationResult.setSimpleResult("-None provided by server-");
            }
            else
                operationResult.setErrorMessage(res.getFailureDescription());
        }
        return operationResult;
    }

    protected OperationResult installManagementUser(Configuration parameters, Configuration pluginConfig, AS7Mode mode) {
        String user = parameters.getSimpleValue("user", "");
        String password = parameters.getSimpleValue("password", "");

        OperationResult result = new OperationResult();

        PropertySimple remoteProp = pluginConfig.getSimple("manuallyAdded");
        if (remoteProp!=null && remoteProp.getBooleanValue()!= null && remoteProp.getBooleanValue()) {
            result.setErrorMessage("This is a manually added server. This operation can not be used to install a management user. Use the server's 'bin/add-user.sh'");
            return result;
        }

        if (user.isEmpty() || password.isEmpty()) {
            result.setErrorMessage("User and Password must not be empty");
            return result;
        }

        String baseDirString = pluginConfig.getSimpleValue("baseDir", "");
        if (baseDirString.isEmpty()) {
            result.setErrorMessage("No baseDir found - cannot continue.");
            return result;
        }
        File baseDir = new File(baseDirString);

        String configFile;
        BaseProcessDiscovery processDiscovery;
        String configDir = pluginConfig.getSimpleValue("configDir",null);
        switch (mode) {
            case STANDALONE:
                processDiscovery = new StandaloneASDiscovery();
                configFile = pluginConfig.getSimpleValue("config", null);
                configFile = configDir + File.separator + configFile;
                break;
            case HOST:
                processDiscovery = new HostControllerDiscovery();
                configFile = pluginConfig.getSimpleValue("hostConfig", null);
                configFile = configDir + File.separator + configFile;
                break;
            default:
                throw new IllegalArgumentException("Unsupported mode: " + mode);
        }
        processDiscovery.readStandaloneOrHostXmlFromFile(configFile);

        String realm = pluginConfig.getSimpleValue("realm", "ManagementRealm");
        String propertiesFilePath = processDiscovery.getSecurityPropertyFileFromHostXml(baseDir, mode, realm);

        String encryptedPassword;
        try {
            UsernamePasswordHashUtil hashUtil = new UsernamePasswordHashUtil();
            encryptedPassword = hashUtil.generateHashedHexURP(user, realm, password.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password.", e);
        }

        boolean userAlreadyExisted;
        try {
            PropertiesFileUpdate propsFileUpdate = new PropertiesFileUpdate(propertiesFilePath);
            Properties existingProps = propsFileUpdate.loadExistingProperties();
            userAlreadyExisted = existingProps.containsKey(user);
            propsFileUpdate.update(user, encryptedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update management users properties file [" + propertiesFilePath
                    + "].", e);
        }

        String verb = (userAlreadyExisted) ? "updated" : "added";
        result.setSimpleResult("Management user [" + user + "] " + verb + ".");
        log.info("Management user [" + user + "] " + verb + " for " + context.getResourceType().getName()
                + " server with key [" + context.getResourceKey() + "].");

        context.getAvailabilityContext().requestAvailabilityCheck();

        return result;
    }

    public void getValues(MeasurementReport report, Set metrics) throws Exception {
        Set<MeasurementScheduleRequest> requests = metrics;
        Set<MeasurementScheduleRequest> leftovers = new HashSet<MeasurementScheduleRequest>(requests.size());

        Set<MeasurementScheduleRequest> skmRequests = new HashSet<MeasurementScheduleRequest>(requests.size());
        for (MeasurementScheduleRequest req: requests) {
            if (req.getName().startsWith("_skm:"))
                skmRequests.add(req);
        }

        for (MeasurementScheduleRequest request: requests) {
            String requestName = request.getName();
            if (requestName.equals("startTime")) {
                String path = getPath();
                if (context.getResourceType().getName().contains("Host Controller")) {
                    if (path!=null)
                        path = "host=master," + path ;  // TODO is the local controller always on host=master?? AS7-3678
                    else
                        path = "host=master";
                }
                Address address = new Address(path);
                address.add("core-service","platform-mbean");
                address.add("type","runtime");
                Operation op = new ReadAttribute(address,"start-time");
                Result res = getASConnection().execute(op);

                if (res.isSuccess()) {
                    Long startTime= (Long) res.getResult();
                    MeasurementDataTrait data = new MeasurementDataTrait(request,new Date(startTime).toString());
                    report.addData(data);
                }
            }
            else if (!requestName.startsWith("_skm:")) { // handled below
                leftovers.add(request);
            }
        }

        // Now handle the skm
        if (skmRequests.size()>0) {
            Address address = new Address();
            ReadResource op = new ReadResource(address);
            op.includeRuntime(true);
            ComplexResult res = getASConnection().executeComplex(op);
            if (res.isSuccess()) {
                Map<String,Object> props = res.getResult();

                for (MeasurementScheduleRequest request: skmRequests) {
                    String requestName = request.getName();
                    String realName = requestName.substring(requestName.indexOf(':') + 1);
                    String val=null;
                    if (props.containsKey(realName)) {
                        val = getStringValue( props.get(realName) );
                    }

                    if ("null".equals(val)) {
                        if (realName.equals("product-name"))
                            val = "JBoss AS";
                        else if (realName.equals("product-version"))
                            val = getStringValue(props.get("release-version"));
                        else
                            log.debug("Value for " + realName + " was 'null' and no replacement found");
                    }
                    MeasurementDataTrait data = new MeasurementDataTrait(request,val);
                    report.addData(data);
                }
            }
            else {
                log.debug("getSKMRequests failed: " + res.getFailureDescription());
            }
        }

        super.getValues(report, leftovers);
    }

}
