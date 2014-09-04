
/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.wildfly.Patch;
import org.rhq.common.wildfly.PatchBundle;
import org.rhq.common.wildfly.PatchInfo;
import org.rhq.common.wildfly.PatchParser;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.bundle.BundlePurgeRequest;
import org.rhq.core.pluginapi.bundle.BundlePurgeResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;
import org.rhq.modules.plugins.jbossas7.util.PatchDetails;

/**
 * @author Lukas Krejci
 * @since 4.13
 */
public class PatchHandlerComponent implements ResourceComponent<ResourceComponent<?>>, BundleFacet {
    private static final Log LOG = LogFactory.getLog(PatchHandlerComponent.class);

    private static final String PATCH_ROLLBACK_COMMAND = "patch rollback --reset-configuration=false --patch-id=";

    private ResourceContext<ResourceComponent<?>> context;

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public BundleDeployResult deployBundle(BundleDeployRequest request) {
        ServerControl control = onServer(request);

        String errorMessage = sanityCheck(control, request.getReferencedConfiguration(),
            request.getBundleManagerProvider(), request.getResourceDeployment());

        if (errorMessage != null) {
            BundleDeployResult result = new BundleDeployResult();
            result.setErrorMessage(errorMessage);

            return result;
        }

        if (request.isRevert()) {
            return handleRevert(request);
        }

        BundleDeployResult result = new BundleDeployResult();

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        ProcessExecutionResults results;
        BundleManagerProvider bmp = request.getBundleManagerProvider();
        BundleResourceDeployment rd = request.getResourceDeployment();

        StopResult stop = stopIfNeeded(connection, control,
            request.getResourceDeployment().getBundleDeployment().getConfiguration(), bmp, rd);

        boolean startUp = stop.hasStopped;

        if (stop.errorMessage != null) {
            result.setErrorMessage(stop.errorMessage);
            return result;
        }

        try {
            StringBuilder command = new StringBuilder("patch apply --path=").append(
                request.getPackageVersionFiles().values().iterator().next().getAbsolutePath());

            Configuration bundleConfig = request.getResourceDeployment().getBundleDeployment().getConfiguration();
            String override = bundleConfig.getSimpleValue("override");
            String overrideAll = bundleConfig.getSimpleValue("override-all");
            String preserve = bundleConfig.getSimpleValue("preserve");
            String overrideModules = bundleConfig.getSimpleValue("override-modules");

            if (override != null) {
                command.append(" --override=").append(override);
            }

            if (overrideAll != null) {
                command.append(" --override-all=").append(Boolean.valueOf(overrideAll));
            }

            if (preserve != null) {
                command.append(" --preserve=").append(preserve);
            }

            if (overrideModules != null) {
                command.append(" --override-modules=").append(overrideModules);
            }

            results = control.cli().disconnected(true).executeCliCommand(command.toString());

            switch (handleExecutionResults(results, bmp, rd, true)) {
            case EXECUTION_ERROR:
                result
                    .setErrorMessage("Error while trying to execute patch command: " + results.getError().getMessage());
                return result;
            case TIMEOUT:
                result.setErrorMessage("Patch application timed out. Output was: " + results.getCapturedOutput());
                return result;
            case ERROR:
                result.setErrorMessage("Patch application failed with error code " + results.getExitCode() + ".");
                return result;
            }
        } finally {
            if (startUp) {
                errorMessage = startServer(connection, control, bmp, rd);
                if (errorMessage != null) {
                    result.setErrorMessage(errorMessage);
                }
            }
        }

        return result;
    }

    @Override
    public BundlePurgeResult purgeBundle(BundlePurgeRequest request) {
        BundlePurgeResult result = new BundlePurgeResult();

        ServerControl control = ServerControl
            .onServer(request.getReferencedConfiguration(), AS7Mode.valueOf(request.getDestinationTarget().getPath()),
                context.getSystemInformation());

        String errorMessage = sanityCheck(control, request.getReferencedConfiguration(),
            request.getBundleManagerProvider(), request.getLiveResourceDeployment());
        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
            return result;
        }

        PropertySimple patchIdProp = request.getLiveResourceDeployment().getBundleDeployment().getConfiguration()
            .getSimple("patchId");
        PropertySimple allPatchIdsProp = request.getLiveResourceDeployment().getBundleDeployment().getConfiguration()
            .getSimple("allPatchIds");

        String[] pids;
        if (patchIdProp != null && patchIdProp.getStringValue() != null) {
            pids = new String[1];
            pids[0] = patchIdProp.getStringValue();
        } else if (allPatchIdsProp != null && allPatchIdsProp.getStringValue() != null) {
            pids = allPatchIdsProp.getStringValue().split("\\|");
            //we need to rollback in the reverse order to patch definition order in which they have been applied.
            Collections.reverse(Arrays.asList(pids));
        } else {
            result.setErrorMessage("Could not determine what patch to purge from the bundle configuration.");
            return result;
        }

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        errorMessage = rollbackPatches(control, request.getBundleManagerProvider(),
            request.getLiveResourceDeployment(), connection, pids);

        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
        }

        return result;
    }

    private BundleDeployResult handleRevert(BundleDeployRequest request) {
        //right, so we're in revert mode.
        //The deployment we get is the version we should be deploying (i.e. we don't know what the version was
        //that was last deployed).
        //So let's revert everything until the installed patch matches what we are deploying in this request.

        BundleDeployResult result = new BundleDeployResult();

        ProcessExecutionResults results = onServer(request).cli().disconnected(true).executeCliCommand("patch history");
        switch (handleExecutionResults(results, null, null, false)) {
        case EXECUTION_ERROR:
            result.setErrorMessage(
                "Failed to determine the patch history while doing a revert: " + results.getError().getMessage());
            return result;
        case TIMEOUT:
            result.setErrorMessage("Timed out while determining patch history for a revert. Output was: " +
                results.getCapturedOutput());
            return result;
        case ERROR:
            result.setErrorMessage("Failed to determine the patch histor for a revert. Returned error code was: " +
                results.getExitCode() + "\nOutput was: " + results.getCapturedOutput());
            return result;
        }

        List<PatchDetails> installedPatches = PatchDetails.fromHistory(results.getCapturedOutput());

        if (installedPatches.size() < 2) {
            result.setErrorMessage("Could not revert to previous patch. " +
                (installedPatches.size() == 0 ? "There are no patches installed at the moment" :
                    "There's only a single patch installed at the moment."));
            return result;
        }

        File patchZipFile = request.getPackageVersionFiles().values().iterator().next();
        FileInputStream patchZipContents = null;

        String patchIdToRollBackTo = null;
        try {
            patchZipContents = new FileInputStream(patchZipFile);

            PatchInfo patchInfo = PatchParser.parse(patchZipContents, true);
            if (patchInfo.is(Patch.class)) {
                patchIdToRollBackTo = patchInfo.as(Patch.class).getId();
            } else if (patchInfo.is(PatchBundle.class)) {
                //this is the patch bundle so we need to roll back to the last patch in the bundle
                for (PatchBundle.Element e : patchInfo.as(PatchBundle.class)) {
                    patchIdToRollBackTo = e.getPatch().getId();
                }
            }
        } catch (Exception e) {
            result.setErrorMessage("Failed to analyze the patch to revert to. " + e.getMessage());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to analyze the patch to revert to.", e);
            }

            return result;
        } finally {
            StreamUtil.safeClose(patchZipContents);
        }

        List<String> rolledBackPatches = new ArrayList<String>();
        for (PatchDetails patchDetails : installedPatches) {
            if (patchDetails.getId().equals(patchIdToRollBackTo)) {
                break;
            }

            rolledBackPatches.add(patchDetails.getId());
        }

        if (rolledBackPatches.size() == installedPatches.size()) {
            result.setErrorMessage("The patch to revert to is no longer installed.");
            return result;
        }

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        String errorMessage = rollbackPatches(onServer(request), request.getBundleManagerProvider(),
            request.getResourceDeployment(), connection,
            rolledBackPatches.toArray(new String[rolledBackPatches.size()]));

        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
        }

        return result;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) {
        this.context = context;
    }

    @Override
    public void stop() {
    }

    private boolean waitForServerToStart(ASConnection connection) throws InterruptedException {
        boolean up = false;
        while (!up) {
            up = isServerUp(connection);

            if (!up) {
                if (context.getComponentInvocationContext().isInterrupted()) {
                    // Operation canceled or timed out
                    throw new InterruptedException();
                }
                Thread.sleep(SECONDS.toMillis(1));
            }
        }
        return true;
    }

    private ExecutionResult handleExecutionResults(ProcessExecutionResults results, BundleManagerProvider bmp,
        BundleResourceDeployment resourceDeployment, boolean doAudit) {
        ExecutionResult ret = ExecutionResult.OK;

        if (results.getError() != null) {
            ret = ExecutionResult.EXECUTION_ERROR;
        } else if (results.getExitCode() == null) {
            ret = ExecutionResult.TIMEOUT;
        } else if (results.getExitCode() != 0) {
            ret = ExecutionResult.ERROR;
        }

        if (doAudit) {
            audit(bmp, resourceDeployment, "Output", ret == ExecutionResult.OK ? "Standard" : "Error", ret.status(),
                results.getCapturedOutput());
        }

        return ret;
    }

    private void audit(BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment, String action,
        String info, BundleResourceDeploymentHistory.Status status, String message) {

        try {
            bmp.auditDeployment(resourceDeployment, action, info,
                BundleResourceDeploymentHistory.Category.AUDIT_MESSAGE, status, message, null);
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to report audit deployment.", e);
            }
        }
    }

    private enum ExecutionResult {
        EXECUTION_ERROR, TIMEOUT, ERROR, OK;

        public BundleResourceDeploymentHistory.Status status() {
            return this == OK ? BundleResourceDeploymentHistory.Status.SUCCESS :
                BundleResourceDeploymentHistory.Status.FAILURE;
        }
    }

    private String fullErrorMessage(String basicMessage, String[] patchIds, int attemptedPatchIdIndex, String action) {
        String message = basicMessage;

        if (attemptedPatchIdIndex > 0) {
            message += " The following patches were successfully " + action + ": " + StringUtil.collectionToString(
                Arrays.asList(patchIds).subList(0, attemptedPatchIdIndex)) + ".";
        }

        if (attemptedPatchIdIndex < patchIds.length - 1) {
            message += " The following patches were NOT " + action + ": " +
                StringUtil
                    .collectionToString(Arrays.asList(patchIds).subList(attemptedPatchIdIndex + 1, patchIds.length))
                + ".";
        }

        return message;
    }

    private ServerControl onServer(BundleDeployRequest request) {
        return ServerControl.onServer(request.getReferencedConfiguration(),
            AS7Mode.valueOf(request.getDestinationTarget().getPath()),
            context.getSystemInformation());
    }

    private String rollbackPatches(ServerControl control, BundleManagerProvider bmp, BundleResourceDeployment rd,
        ASConnection connection, String... pids) {

        ProcessExecutionResults results;

        Configuration deploymentConfiguration = rd.getBundleDeployment().getConfiguration();

        //if the server is online, let's bring it down for the duration of the rollback.
        StopResult stop = stopIfNeeded(connection, control, deploymentConfiguration, bmp, rd);
        boolean serverWasUp = stop.hasStopped;

        List<String> patchCommands = new ArrayList<String>();
        for (String pid : pids) {
            patchCommands.add(PATCH_ROLLBACK_COMMAND + pid);
        }

        String errorMessage = null;

        ServerControl.Cli cli = control.cli().disconnected(true);

        try {
            int i = 0;
            for (String command : patchCommands) {
                results = cli.executeCliCommand(command);
                switch (handleExecutionResults(results, bmp, rd, true)) {
                case EXECUTION_ERROR:
                    return fullErrorMessage("Error trying to run patch rollback: " +
                        results.getError().getMessage(), pids, i - 1, "rolled back");
                case TIMEOUT:
                    return fullErrorMessage("Patch rollback timed out. Captured output: " +
                        results.getCapturedOutput(), pids, i - 1, "rolled back");
                case ERROR:
                    return fullErrorMessage("Patch rollback failed with error code " + results.getExitCode()
                        + ".", pids, i - 1, "rolled back");
                }

                ++i;
            }
        } finally {
            if (serverWasUp) {
                errorMessage = startServer(connection, control, bmp, rd);
            }
        }

        return errorMessage;
    }

    private boolean isServerUp(ASConnection connection) {
        Operation op = new ReadAttribute(new Address(), "release-version");
        try {
            Result res = connection.execute(op);
            if (res.isSuccess()) { // If op succeeds, server is not down
                return true;
            }
        } catch (Exception e) {
            //do absolutely nothing
            //if an exception is thrown that means the server is still down, so consider this
            //a single failed attempt, equivalent to res.isSuccess == false
        }

        return false;
    }

    private StopResult stopIfNeeded(ASConnection connection, ServerControl control,
        Configuration bundleDeploymentConfiguration, BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment) {

        boolean doRestart = Boolean.valueOf(bundleDeploymentConfiguration.getSimpleValue("restart", "true"));

        if (doRestart && isServerUp(connection)) {
            audit(bmp, resourceDeployment, "Stop", "Stop", null,
                "The server is running. Stopping it before any operation on patches.");

            ProcessExecutionResults results = control.lifecycle().shutdownServer();
            switch (handleExecutionResults(results, bmp, resourceDeployment, true)) {
            case EXECUTION_ERROR:
                return new StopResult(false, "Error trying to shutdown the server: " + results.getError().getMessage());
            case TIMEOUT:
                return new StopResult(false, "Stopping the server timed out. Captured output: " +
                    results.getCapturedOutput());
            case ERROR:
                return new StopResult(false, "Stopping the server failed with error code " + results.getExitCode() +
                    " and output: " + results.getCapturedOutput());
            }

            return new StopResult(true, null);
        }

        return new StopResult(false, null);
    }

    private String startServer(ASConnection connection, ServerControl control, BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment) {
        audit(bmp, resourceDeployment, "Start", "Start", null, "Starting the server back up.");

        ProcessExecutionResults results = control.lifecycle().startServer();

        switch (handleExecutionResults(results, bmp, resourceDeployment, false)) {
        case EXECUTION_ERROR:
            return "Error trying to start the server. " + results.getError().getMessage();
        case ERROR:
            return "Starting the server failed with error code " + results.getExitCode() + " and output: " +
                results.getCapturedOutput();
        // ignore timeout, because starting the server actually would always be detected as doing it, because the start
        // script never stops...
        }

        try {
            waitForServerToStart(connection);
        } catch (InterruptedException e) {
            String message = "Interrupted while waiting for the server to start up after applying the patch";

            Thread.currentThread().interrupt();

            return message;
        }

        return null;
    }

    private String sanityCheck(ServerControl serverControl, Configuration referencedConfiguration,
        BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment) {

        PropertySimple supportsPatching = referencedConfiguration.getSimple("supportsPatching");

        if (supportsPatching == null) {
            return "Target resource doesn't contain the 'Supports Patching' property in its connection settings. Using an old version of the JBossAS7 plugin?";
        }

        if (supportsPatching.getBooleanValue() == null || !supportsPatching.getBooleanValue()) {
            return "The target resource does not support patching.";
        }

        ProcessExecutionResults results = serverControl.cli().disconnected(true).executeCliCommand("help --commands");
        switch (handleExecutionResults(results, bmp, resourceDeployment, false)) {
        case EXECUTION_ERROR:
            return
                "Failed to check availability of patch command using the 'help --commands' command. The error was: " +
                    results.getError().getMessage();
        case ERROR:
            return
                "Failed to check availability of patch command using the 'help --commands' command. The execution failed with an exit code " +
                    results.getExitCode();
        case TIMEOUT:
            return
                "Failed to check availability of patch command using the 'help --commands' command. The execution timed out with the output: " +
                    results.getCapturedOutput();
        case OK:
            if (results.getCapturedOutput() == null || !results.getCapturedOutput().contains(" patch ")) {
                return "The underlying server does not support the patch command. Cannot perform the patch operation.";
            }
            break;
        }

        return null;
    }

    private static class StopResult {
        public final boolean hasStopped;
        public final String errorMessage;

        public StopResult(boolean hasStopped, String errorMessage) {
            this.hasStopped = hasStopped;
            this.errorMessage = errorMessage;
        }
    }
}
