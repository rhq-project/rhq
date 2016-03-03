
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

package org.rhq.modules.plugins.wildfly10.patching;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.rhq.modules.plugins.wildfly10.AS7Mode;
import org.rhq.modules.plugins.wildfly10.ASConnection;
import org.rhq.modules.plugins.wildfly10.ASConnectionParams;
import org.rhq.modules.plugins.wildfly10.ServerControl;
import org.rhq.modules.plugins.wildfly10.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.util.PatchDetails;

/**
 * @author Lukas Krejci
 * @since 4.13
 */
public final class PatchHandlerComponent implements ResourceComponent<ResourceComponent<?>>, BundleFacet {
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

        Result<Void> check = sanityCheck(control, request.getReferencedConfiguration(),
            request.getBundleManagerProvider(), request.getResourceDeployment(),
            !isTakeOver(request.getResourceDeployment().getBundleDeployment().getConfiguration()));

        if (check.failed()) {
            BundleDeployResult result = new BundleDeployResult();
            result.setErrorMessage(check.errorMessage);

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

        Result<Boolean> stop = stopIfNeeded(connection, control,
            request.getResourceDeployment().getBundleDeployment().getConfiguration(), bmp, rd);
        if (stop.failed()) {
            result.setErrorMessage(stop.errorMessage);
            return result;
        }

        boolean startUp = stop.result;

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

            // as a last thing before the deployment, check the patch history
            Result<List<PatchDetails>> historyBeforeDeployment = getPatchHistory(control, "deploy");
            if (historyBeforeDeployment.failed()) {
                result.setErrorMessage(historyBeforeDeployment.errorMessage);
                return result;
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

            String errorMessage = storeState(control, request.getResourceDeployment(),
                request.getReferencedConfiguration(), historyBeforeDeployment.result);

            if (errorMessage != null) {
                result.setErrorMessage(errorMessage);
                return result;
            }
        } finally {
            if (startUp) {
                String errorMessage = startServer(connection, control, bmp, rd);
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

        ServerControl control = ServerControl.onServer(request.getReferencedConfiguration(),
            AS7Mode.valueOf(request.getDestinationTarget().getPath()), context.getSystemInformation());

        Result<Void> check = sanityCheck(control, request.getReferencedConfiguration(),
            request.getBundleManagerProvider(), request.getLiveResourceDeployment(), false);
        if (check.failed()) {
            result.setErrorMessage(check.errorMessage);
            return result;
        }

        Result<BundleMetadata> metadata = BundleMetadata
            .forDeployment(request.getLiveResourceDeployment(), request.getReferencedConfiguration());
        if (metadata.failed()) {
            result.setErrorMessage(metadata.errorMessage);
            return result;
        }

        LinkedHashSet<String> pidsToRollback = new LinkedHashSet<String>();
        for (BundleMetadata.DeploymentMetadata dm : metadata.result.deployments) {
            for (PatchDetails pd : dm.applied) {
                pidsToRollback.add(pd.getId());
            }
        }

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        String errorMessage = rollbackPatches(control, request.getBundleManagerProvider(),
            request.getLiveResourceDeployment(), connection, "purge", new ArrayList<String>(pidsToRollback));

        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
            return result;
        }

        forgetState(request.getLiveResourceDeployment(), request.getReferencedConfiguration());

        return result;
    }

    private BundleDeployResult handleRevert(BundleDeployRequest request) {
        BundleDeployResult result = new BundleDeployResult();

        //this is what was recorded during prior deployments
        Result<BundleMetadata> latestDeploymentMetadata = BundleMetadata
            .forDeployment(request.getResourceDeployment(), request.getReferencedConfiguration());
        if (latestDeploymentMetadata.failed()) {
            result.setErrorMessage(latestDeploymentMetadata.errorMessage);
            return result;
        }

        //this is what we're reverting to.
        Result<String[]> pids = getPids(request.getResourceDeployment(), "revert");
        if (pids.failed()) {
            result.setErrorMessage(pids.errorMessage);
            return result;
        }

        // determine the pids that have been deployed on top of the bundle being reverted to.
        // this is a linked set because it can happen that a single patch gets applied multiple times
        // through but we obviously want to only roll it back once, at the position of the latest application.
        // The circumstance of patch being applied multiple times can happen when:
        // 1) patch is applied through destination A
        // 2) patch revert fails due to an outside change
        // 3) manual intervention in CLI recovers to the original state
        // 4) patch is applied again
        Set<String> pidsToRollback = new LinkedHashSet<String>();
        List<BundleMetadata.DeploymentMetadata> deploymentsToForget = new ArrayList<BundleMetadata.DeploymentMetadata>();

        String stopPid = pids.result[0];
        boolean stopPidFound = false;
        outer: for (BundleMetadata.DeploymentMetadata dm : latestDeploymentMetadata.result.deployments) {
            for (PatchDetails pd : dm.applied) {
                String pid = pd.getId();
                if (pid.equals(stopPid)) {
                    stopPidFound = true;
                    break outer;
                }
                pidsToRollback.add(pid);
            }

            deploymentsToForget.add(dm);
        }

        if (!stopPidFound) {
            result.setErrorMessage("The patch to revert to (" + stopPid +
                ") was not previously deployed by RHQ. This means that this server joined the resource group '" +
                request.getResourceDeployment().getBundleDeployment().getDestination().getGroup().getName() +
                "' after this patch was already deployed. To prevent accidental damage no changes will be made to the server.");

            return result;
        }

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        ServerControl control = onServer(request);

        String errorMessage = rollbackPatches(control, request.getBundleManagerProvider(),
            request.getResourceDeployment(), connection, "revert", new ArrayList<String>(pidsToRollback));

        if (errorMessage != null) {
            result.setErrorMessage(errorMessage);
            return result;
        }

        for (BundleMetadata.DeploymentMetadata dm : deploymentsToForget) {
            dm.forget(request.getResourceDeployment(), request.getReferencedConfiguration());
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

    private String fullErrorMessage(String basicMessage, List<String> patchIds, int attemptedPatchIdIndex, String action) {
        String message = basicMessage;

        if (attemptedPatchIdIndex > 0) {
            message += " The following patches were successfully " + action + ": " + StringUtil.collectionToString(
                patchIds.subList(0, attemptedPatchIdIndex)) + ".";
        }

        if (attemptedPatchIdIndex < patchIds.size() - 1) {
            message += " The following patches were NOT " + action + ": " +
                StringUtil
                    .collectionToString(patchIds.subList(attemptedPatchIdIndex + 1, patchIds.size()))
                + ".";
        }

        return message;
    }

    private ServerControl onServer(BundleDeployRequest request) {
        return ServerControl.onServer(request.getReferencedConfiguration(),
            AS7Mode.valueOf(request.getDestinationTarget().getPath()), context.getSystemInformation());
    }

    private String rollbackPatches(ServerControl control, BundleManagerProvider bmp, BundleResourceDeployment rd,
        ASConnection connection, String operation, List<String> pids) {

        if (pids.isEmpty()) {
            return null;
        }

        ProcessExecutionResults results;
        ServerControl.Cli cli = control.cli().disconnected(true);

        Result<List<PatchDetails>> history = getPatchHistory(control, operation);
        if (history.failed()) {
            return history.errorMessage;
        }

        List<PatchDetails> installedPatches = history.result;

        List<String> pidsToRollback = new ArrayList<String>(pids);
        List<String> noLongerRemovablePids = new ArrayList<String>();
        Set<String> installedPids = new HashSet<String>();
        int lastPidToRollback = 0;
        int installedPidIdx = 0;
        for (; installedPidIdx < installedPatches.size() &&
            lastPidToRollback < pidsToRollback.size(); ++installedPidIdx) {

            PatchDetails installed = installedPatches.get(installedPidIdx);
            String pidToRollback = pidsToRollback.get(lastPidToRollback);

            String installedId = installed.getId();

            if (installedId.equals(pidToRollback)) {
                lastPidToRollback++;
            } else {
                while (!installedId.equals(pidToRollback) && lastPidToRollback < pidsToRollback.size()) {
                    pidToRollback = pidsToRollback.get(lastPidToRollback);
                    noLongerRemovablePids.add(pidsToRollback.remove(lastPidToRollback));
                }
                if (installedId.equals(pidToRollback)) {
                    lastPidToRollback++;
                }
            }

            installedPids.add(installedId);
        }

        for (; installedPidIdx < installedPatches.size(); ++installedPidIdx) {
            installedPids.add(installedPatches.get(installedPidIdx).getId());
        }

        // remove pids that we have not seen installed
        if (lastPidToRollback < pidsToRollback.size()) {
            List<String> uninstalledPids = pidsToRollback.subList(lastPidToRollback, pidsToRollback.size());
            noLongerRemovablePids.addAll(uninstalledPids);
            uninstalledPids.clear();
        }

        boolean inconsistent = false;
        for (String pid : noLongerRemovablePids) {
            if (installedPids.contains(pid)) {
                // the current patch state is inconsistent with what we're expecting, because
                // we see pids that are still installed but are no longer on rollback-able positions
                // even though they should.
                // If they're no longer installed then that's actually OK, we'd be rolling them back anyway.
                inconsistent = true;
                break;
            }
        }

        if (pidsToRollback.isEmpty()) {
            if (noLongerRemovablePids.isEmpty()) {
                audit(bmp, rd, "Rollback", "Nothing To Do", BundleResourceDeploymentHistory.Status.WARN,
                    "None of the patches " + pids + " is installed anymore.");

                return null;
            } else {
                String message =  "The following patches were not rolled back due to other patches having been applied in the meantime: " +
                        noLongerRemovablePids + ". No other patches can be rolled back.";

                if (inconsistent) {
                    return message;
                } else {
                    audit(bmp, rd, "Rollback", "Missing patches", BundleResourceDeploymentHistory.Status.WARN,
                        "The following patches were to be rolled back but they aren't installed anymore: " +
                            noLongerRemovablePids + ".");
                }
            }
        }

        Configuration deploymentConfiguration = rd.getBundleDeployment().getConfiguration();

        //if the server is online, let's bring it down for the duration of the rollback.
        Result<Boolean> stop = stopIfNeeded(connection, control, deploymentConfiguration, bmp, rd);
        if (stop.failed()) {
            return stop.errorMessage;
        }

        boolean serverWasUp = stop.result;

        List<String> patchCommands = new ArrayList<String>();
        for (String pid : pidsToRollback) {
            patchCommands.add(PATCH_ROLLBACK_COMMAND + pid);
        }

        String errorMessage = null;

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
            if (!noLongerRemovablePids.isEmpty()) {
                if (inconsistent) {
                    errorMessage = "The following patches were not rolled back due to other patches having been applied in the meantime: " +
                        noLongerRemovablePids;
                } else {
                    audit(bmp, rd, "Rollback", "Missing patches", BundleResourceDeploymentHistory.Status.WARN,
                        "The following patches were to be rolled back but they aren't installed anymore: " +
                            noLongerRemovablePids + ".");
                }
            }

            if (serverWasUp) {
                String startError = startServer(connection, control, bmp, rd);
                if (startError != null) {
                    if (errorMessage != null) {
                        audit(bmp, rd, "Restart", "Failure", BundleResourceDeploymentHistory.Status.FAILURE,
                            startError);
                    } else {
                        errorMessage = startError;
                    }
                }
            }
        }

        return errorMessage;
    }

    private boolean isServerUp(ASConnection connection) {
        Operation op = new ReadAttribute(new Address(), "release-version");
        try {
            org.rhq.modules.plugins.wildfly10.json.Result res = connection.execute(op);
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

    private Result<Boolean> stopIfNeeded(ASConnection connection, ServerControl control,
        Configuration bundleDeploymentConfiguration, BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment) {

        boolean doRestart = Boolean.valueOf(bundleDeploymentConfiguration.getSimpleValue("restart", "true"));

        if (doRestart && isServerUp(connection)) {
            audit(bmp, resourceDeployment, "Stop", "Stop", null,
                "The server is running. Stopping it before any operation on patches.");

            ProcessExecutionResults results = control.lifecycle().shutdownServer();
            switch (handleExecutionResults(results, bmp, resourceDeployment, true)) {
            case EXECUTION_ERROR:
                return new Result<Boolean>(false, "Error trying to shutdown the server: " + results.getError().getMessage());
            case TIMEOUT:
                return new Result<Boolean>(false, "Stopping the server timed out. Captured output: " +
                    results.getCapturedOutput());
            case ERROR:
                return new Result<Boolean>(false, "Stopping the server failed with error code " + results.getExitCode() +
                    " and output: " + results.getCapturedOutput());
            }

            return new Result<Boolean>(true, null);
        }

        return new Result<Boolean>(false, null);
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

    private Result<Void> sanityCheck(ServerControl serverControl, Configuration referencedConfiguration,
        BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment, boolean uniqueDeploymentRequired) {

        //check if patching is supported

        PropertySimple supportsPatching = referencedConfiguration.getSimple("supportsPatching");

        if (supportsPatching == null) {
            return Result.error("Target resource doesn't contain the 'Supports Patching' property in its connection settings. Using an old version of the JBossAS7 plugin?");
        }

        if (supportsPatching.getBooleanValue() == null || !supportsPatching.getBooleanValue()) {
            return Result.error("The target resource does not support patching.");
        }

        ProcessExecutionResults results = serverControl.cli().disconnected(true).executeCliCommand("help --commands");
        switch (handleExecutionResults(results, bmp, resourceDeployment, false)) {
        case EXECUTION_ERROR:
            return
                Result.error("Failed to check availability of patch command using the 'help --commands' command. The error was: " +
                    results.getError().getMessage());
        case ERROR:
            return
                Result.error("Failed to check availability of patch command using the 'help --commands' command. The execution failed with an exit code " +
                    results.getExitCode());
        case TIMEOUT:
            return
                Result.error("Failed to check availability of patch command using the 'help --commands' command. The execution timed out with the output: " +
                    results.getCapturedOutput());
        case OK:

            if (results.getCapturedOutput() == null || !(results.getCapturedOutput().contains(" patch ") || results.getCapturedOutput().contains("\npatch"))) {
                return Result.error("The underlying server does not support the patch command. Cannot perform the patch operation.");
            }
            break;
        }

        if (!uniqueDeploymentRequired) {
            return Result.with(null);
        }

        Result<MetadataFiles> metadata = MetadataFiles.getActive(referencedConfiguration);
        if (metadata.failed()) {
            return Result.error(metadata.errorMessage);
        }

        if (metadata.result == null) {
            // no active deployment yet, we're cool.
            return Result.with(null);
        }

        int destinationId = resourceDeployment.getBundleDeployment().getDestination().getId();
        int activeDestinationId = metadata.result.getDestinationId();

        if (destinationId != activeDestinationId) {
            try {
                String destName = metadata.result.getDestinationName();
                return Result.error("The destination \"" + destName +
                    "\" already deployed one or more patches to this server. You can deploy to a server using only a single destination.");
            } catch (Exception e) {
                //k, we just failed to read the name of the destination but we still know that the user
                //tries to use a different destination, so let's just return a slightly less user-friendly
                //error message.
                return Result.error("The destination with id " + destinationId +
                    " already handles patch deployments to this server. You can deploy to a server using only a single destination.");
            }
        }

        return Result.with(null);
    }

    /**
     * Store the last deployed patch ID at this point in time. This information is stored for each bundle resource
     * deployment individually.
     *
     * @return the error message or null if everything went fine.
     */
    private String storeState(ServerControl control, BundleResourceDeployment rd, Configuration referencedConfiguration,
        List<PatchDetails> historyBeforeDeployment) {

        Result<List<PatchDetails>> history = getPatchHistory(control, "deployment");
        if (history.failed()) {
            return history.errorMessage;
        }

        Result<MetadataFiles> metadataFiles = MetadataFiles.forDeployment(rd, referencedConfiguration);
        if (metadataFiles.failed()) {
            return metadataFiles.errorMessage;
        }

        Result<Void> saveDestinationName = metadataFiles.result.saveDestinationName(
            rd.getBundleDeployment().getDestination().getName());
        if (saveDestinationName.failed()) {
            return saveDestinationName.errorMessage;
        }

        // if sanity check allowed going forward with deployment and we have multiple destinations, forget about
        // all others...
        Result<Void> saveAsActive = metadataFiles.result.saveAsActive();
        if (saveAsActive.failed()) {
            return saveAsActive.errorMessage;
        }

        BundleMetadata.DeploymentMetadata deploymentMetadata = BundleMetadata.DeploymentMetadata
            .from(historyBeforeDeployment, history.result);

        Result<Void> write = deploymentMetadata.persistAsNewState(rd, referencedConfiguration);
        if (write.failed()) {
            return write.errorMessage;
        }

        return null;
    }

    private String forgetState(BundleResourceDeployment rd, Configuration referencedConfiguration) {
        Result<MetadataFiles> metadata = MetadataFiles.forDeployment(rd, referencedConfiguration);
        if (metadata.failed()) {
            return metadata.errorMessage;
        }

        metadata.result.delete();

        return null;
    }

    private Result<List<PatchDetails>> getPatchHistory(ServerControl control, String operation) {
        Result<String> json = getPatchHistoryJSON(control, operation);
        if (json.errorMessage != null) {
            return new Result<List<PatchDetails>>(null, json.errorMessage);
        }

        List<PatchDetails> installedPatches = PatchDetails.fromHistory(json.result);

        return new Result<List<PatchDetails>>(installedPatches, null);
    }

    private Result<String> getPatchHistoryJSON(ServerControl control, String operation) {
        ProcessExecutionResults results = control.cli().disconnected(true).executeCliCommand("patch history");
        switch (handleExecutionResults(results, null, null, false)) {
        case EXECUTION_ERROR:
            return new Result<String>(null,
                "Failed to determine the patch history while doing a " + operation + ": " +
                    results.getError().getMessage());
        case TIMEOUT:
            return new Result<String>(null,
                "Timed out while determining patch history for a " + operation + ". Output was: " +
                    results.getCapturedOutput());
        case ERROR:
            return new Result<String>(null,
                "Failed to determine the patch history for a " + operation + ". Returned error code was: " +
                    results.getExitCode() + "\nOutput was: " + results.getCapturedOutput());
        }

        return new Result<String>(results.getCapturedOutput(), null);
    }

    private Result<String[]> getPids(BundleResourceDeployment rd, String operation) {
        PropertySimple patchType = rd.getBundleDeployment().getConfiguration().getSimple("patchType");
        boolean isBundle = patchType != null && "patch-bundle".equals(patchType.getStringValue());

        PropertySimple patchIdProp = rd.getBundleDeployment().getConfiguration()
            .getSimple("patchId");
        PropertySimple allPatchIdsProp = rd.getBundleDeployment().getConfiguration()
            .getSimple("allPatchIds");

        String[] pids;
        if (isBundle) {
            if (allPatchIdsProp == null || allPatchIdsProp.getStringValue() == null) {
                return new Result<String[]>(null,
                    "Could not determine the list of patch ids from the bundle configuration while performing " +
                        operation);
            }
            pids = allPatchIdsProp.getStringValue().split("#");
            //we need to return the pids in the same order as patch history - i.e. in the reversed deployment order
            Collections.reverse(Arrays.asList(pids));
        } else {
            if (patchIdProp == null || patchIdProp.getStringValue() == null) {
                return new Result<String[]>(null,
                    "Could not determine the list of patch ids from the bundle configuration while performing " +
                        operation);
            }
            pids = new String[1];
            pids[0] = patchIdProp.getStringValue();
        }

        return new Result<String[]>(pids, null);
    }

    private boolean isTakeOver(Configuration deploymentConfiguration) {
        PropertySimple takeOver = deploymentConfiguration.getSimple("takeOver");
        if (takeOver == null) {
            return false;
        }

        Boolean value = takeOver.getBooleanValue();

        return value != null && value;
    }
}
