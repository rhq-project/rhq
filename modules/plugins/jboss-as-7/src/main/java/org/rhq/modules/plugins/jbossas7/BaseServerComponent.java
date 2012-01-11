/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.sasl.util.UsernamePasswordHashUtil;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Base component for functionality that is common to Standalone AS and HostControllers
 * @author Heiko W. Rupp
 */
public class BaseServerComponent extends BaseComponent {

    private static final String SEPARATOR = "\n-----------------------\n";
    final Log log = LogFactory.getLog(BaseServerComponent.class);

    /**
     * Restart the server by first executing a 'shutdown' operation via its API. Then call
     * the #startServer method to start it again.
     *
     * @param parameters Parameters to pass to the (recursive) invocation of #invokeOperation
     * @param mode
     * @return State of execution
     * @throws Exception If anything goes wrong
     */
    protected OperationResult restartServer(Configuration parameters, AS7Mode mode) throws Exception {
        OperationResult tmp = invokeOperation("shutdown",parameters);

        if (tmp.getErrorMessage()!=null) {
            tmp.setErrorMessage("Restart failed while failing to shut down: " + tmp.getErrorMessage());
            return tmp;
        }
        Thread.sleep(500); // Wait 0.5s -- this is plenty
        return startServer(mode);
    }

    /**
     * Start the server by calling the start script listed in the plugin configuration. If a different
     * config is given, this is passed via --server-config
     * @return State of Execution.
     * @param mode
     */
    protected OperationResult startServer(AS7Mode mode) {
        OperationResult operationResult = new OperationResult();
        String startScript = pluginConfiguration.getSimpleValue("startScript", mode.getStartScript());
        String baseDir = pluginConfiguration.getSimpleValue("baseDir","");
        if (baseDir.isEmpty()) {
            operationResult.setErrorMessage("No base directory provided");
            return operationResult;
        }
        String script = baseDir + File.separator + startScript;

        ProcessExecution processExecution;
        processExecution = ProcessExecutionUtility.createProcessExecution(new File(script));

        String config = pluginConfiguration.getSimpleValue(mode.getConfigPropertyName(), mode.getDefaultXmlFile());
        List<String> arguments = processExecution.getArguments();
        if (arguments==null) {
            arguments = new ArrayList<String>();
            processExecution.setArguments(arguments);
        }

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
        processExecution.setWorkingDirectory(baseDir);
        processExecution.setCaptureOutput(true);
        processExecution.setWaitForCompletion(15000L); // 15 seconds // TODO: Should we wait longer than 15 seconds?
        processExecution.setKillOnTimeout(false);


        if (log.isDebugEnabled()) {
            log.debug("About to execute the following process: [" + processExecution + "]");
        }
        ProcessExecutionResults results = context.getSystemInformation().executeProcess(processExecution);
        logExecutionResults(results);
        if (results.getError()!=null) {
            operationResult.setErrorMessage(results.getError().getMessage());
        } else {
            operationResult.setSimpleResult("Success");
        }

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
             * Shutdown needs a special treatment, because after sending the operation, if shutdown suceeds,
             * the server connection is closed and we can't read from it. So if we get connection refused for
             * reading, this is a good sign.
             */
            if (!res.isSuccess()) {
                if (res.getThrowable()!=null && (res.getThrowable() instanceof ConnectException || res.getThrowable().getMessage().equals("Connection refused"))) {
                    operationResult.setSimpleResult("Success");
                    log.debug("Got a ConnectionRefused for operation " + name + " this is considered ok, as the remote server sometimes closes the communications channel before sending a reply");
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
        String user = parameters.getSimpleValue("user","");
        String password = parameters.getSimpleValue("password","");

        OperationResult result = new OperationResult();
        if (user.isEmpty() || password.isEmpty()) {
            result.setErrorMessage("User and Password must not be empty");
            return result;
        }

        String baseDir = pluginConfig.getSimpleValue("baseDir","");
        if (baseDir.isEmpty()) {
            result.setErrorMessage("No baseDir found, can not continue");
            return result;
        }
        String standaloneXmlFile = pluginConfig.getSimpleValue("config",mode.getDefaultXmlFile());

        String standaloneXml = baseDir + File.separator + mode.getBaseDir() + File.separator + "configuration" + File.separator + standaloneXmlFile;

        AbstractBaseDiscovery abd = new AbstractBaseDiscovery();
        abd.readStandaloneOrHostXmlFromFile(standaloneXml);

        String realm = pluginConfig.getSimpleValue("realm","ManagementRealm");
        String propertiesFilePath = abd.getSecurityPropertyFileFromHostXml(baseDir,mode, realm);


        Properties p = new Properties();
        try {
            UsernamePasswordHashUtil util = new UsernamePasswordHashUtil();
            String value = util.generateHashedHexURP(user, realm, password.toCharArray());


            FileInputStream fis = new FileInputStream(propertiesFilePath);
            p.load(fis);
            fis.close();
            p.setProperty(user,value);
            FileOutputStream fos = new FileOutputStream(propertiesFilePath);
            p.store(fos,null);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            result.setErrorMessage(e.getMessage());
        } catch (NoSuchAlgorithmException nsae) {
            log.error(nsae.getMessage());
            result.setErrorMessage(nsae.getMessage());
        }
        result.setSimpleResult("User/Password set or updated");

        return result;
    }
}
