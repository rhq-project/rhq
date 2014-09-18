
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

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
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.helper.ServerPluginConfiguration;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
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

        Result<Void> check = sanityCheck(control, request.getReferencedConfiguration(),
            request.getBundleManagerProvider(), request.getResourceDeployment());

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
            request.getBundleManagerProvider(), request.getLiveResourceDeployment());
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

        //this is what is currently deployed
        Result<BundleMetadata> liveDeploymentMetadata = BundleMetadata
            .forDeployment(request.getResourceDeployment(), request.getReferencedConfiguration());
        if (liveDeploymentMetadata.failed()) {
            result.setErrorMessage(liveDeploymentMetadata.errorMessage);
            return result;
        }

        //this is what we're reverting to.
        Result<String[]> pids = getPids(request.getResourceDeployment(), "revert");
        if (pids.failed()) {
            result.setErrorMessage(pids.errorMessage);
            return result;
        }

        //determine the pids that have been deployed on top of the bundle being reverted to.
        List<String> pidsToRollback = new ArrayList<String>();
        List<BundleMetadata.DeploymentMetadata> deploymentsToForget = new ArrayList<BundleMetadata.DeploymentMetadata>();

        String stopPid = pids.result[0];
        outer: for (BundleMetadata.DeploymentMetadata dm : liveDeploymentMetadata.result.deployments) {
            for (PatchDetails pd : dm.applied) {
                String pid = pd.getId();
                if (pid.equals(stopPid)) {
                    break outer;
                }
                pidsToRollback.add(pid);
            }

            deploymentsToForget.add(dm);
        }

        ASConnection connection = new ASConnection(
            ASConnectionParams.createFrom(new ServerPluginConfiguration(request.getReferencedConfiguration())));

        ServerControl control = onServer(request);

        String errorMessage = rollbackPatches(control, request.getBundleManagerProvider(),
            request.getResourceDeployment(), connection, "revert", pidsToRollback);

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
            return "Nothing to rollback.";
        }

        ProcessExecutionResults results;
        ServerControl.Cli cli = control.cli().disconnected(true);

        Result<List<PatchDetails>> history = getPatchHistory(control, operation);
        if (history.errorMessage != null) {
            return history.errorMessage;
        }

        List<PatchDetails> installedPatches = history.result;

        if (!pids.get(0).equals(installedPatches.get(0).getId())) {
            return "No patch out of " + pids + " can be rolled back. The latest applied patch on the target resource is '" +
                installedPatches.get(0).getId() + "' but was expecting '" + pids.get(0) +
                "' to be able to perform the " + operation + ".";
        }

        List<String> pidsToRollback = new ArrayList<String>(pids);
        List<String> noLongerRemovablePids = new ArrayList<String>();
        for (int ins = 0, rb = 0; ins < installedPatches.size() && rb < pidsToRollback.size(); ++ins) {
            PatchDetails installed = installedPatches.get(ins);
            String pidToRollback = pidsToRollback.get(rb);

            if (installed.getId().equals(pidToRollback)) {
                rb++;
            } else {
                noLongerRemovablePids.add(pidsToRollback.remove(rb));
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
            if (serverWasUp) {
                errorMessage = startServer(connection, control, bmp, rd);
            }
        }

        if (!noLongerRemovablePids.isEmpty()) {
            audit(bmp, rd, "Rollback", "Kept patches", BundleResourceDeploymentHistory.Status.WARN,
                "The following patches were not rolled back due to other patches having been applied in the meantime: " +
                    noLongerRemovablePids);
        }

        return errorMessage;
    }

    private boolean isServerUp(ASConnection connection) {
        Operation op = new ReadAttribute(new Address(), "release-version");
        try {
            org.rhq.modules.plugins.jbossas7.json.Result res = connection.execute(op);
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
        BundleManagerProvider bmp, BundleResourceDeployment resourceDeployment) {

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

        BundleMetadata.DeploymentMetadata metadata = BundleMetadata.DeploymentMetadata
            .from(historyBeforeDeployment, history.result);

        Result<Void> write = metadata.persistAsNewState(rd, referencedConfiguration);
        if (write.failed()) {
            return write.errorMessage;
        }

        return null;
    }

    private String forgetState(BundleResourceDeployment rd, Configuration referencedConfiguration) {
        File baseDir = MetadataFiles.baseDirFor(rd, referencedConfiguration);

        FileUtil.purge(baseDir, true);

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

    private static class MetadataFiles {
        final File[] files;
        final File baseDir;

        private MetadataFiles(File baseDir, File[] files) {
            this.baseDir = baseDir;
            this.files = files;
        }

        boolean exists() {
            for (File f : files) {
                if (!f.exists()) {
                    return false;
                }
            }

            return files.length > 0;
        }

        static Result<MetadataFiles> forDeployment(BundleResourceDeployment rd, Configuration referencedConfiguration) {
            File destinationDir = baseDirFor(rd, referencedConfiguration);

            if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                return Result.error("Failed to create metadata storage under " + destinationDir.getAbsolutePath());
            }

            File[] files = destinationDir.listFiles();

            if (files == null) {
                return Result.error("Could not list files in the destination metadata directory " + destinationDir);
            }

            // sort the files in reverse order so that the newest, current state, is on 0th index.
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o2.getName().compareTo(o1.getName());
                }
            });

            return Result.with(new MetadataFiles(destinationDir, files));
        }

        static File baseDirFor(BundleResourceDeployment rd, Configuration referencedConfiguration) {
            ServerPluginConfiguration config = new ServerPluginConfiguration(referencedConfiguration);

            File installationDir = new File(config.getHomeDir(), ".installation");

            File rhqBaseDir = new File(installationDir, ".rhq");

            File destinationDir = new File(rhqBaseDir,
                Integer.toString(rd.getBundleDeployment().getDestination().getId()));

            return destinationDir;
        }
    }

    private static class BundleMetadata {
        final List<DeploymentMetadata> deployments;

        static class DeploymentMetadata {
            final List<PatchDetails> applied;
            int deploymentIndex;

            private DeploymentMetadata(List<PatchDetails> applied, int deploymentIndex) {
                this.applied = applied;
                this.deploymentIndex = deploymentIndex;
            }

            static DeploymentMetadata from(List<PatchDetails> beforeDeployment, List<PatchDetails> afterDeployment) {
                ArrayList<PatchDetails> currentDeployment = new ArrayList<PatchDetails>();

                PatchDetails firstHistorical =
                    beforeDeployment.isEmpty() ? null : beforeDeployment.get(0);

                for (Iterator<PatchDetails> it = afterDeployment.iterator(); it.hasNext();) {
                    PatchDetails p = it.next();
                    if (p.equals(firstHistorical)) {
                        break;
                    }

                    currentDeployment.add(p);
                }

                return new DeploymentMetadata(currentDeployment, -1);
            }

            Result<Void> persistAsNewState(BundleResourceDeployment rd, Configuration referencedConfiguration) {
                try {
                    Result<MetadataFiles> files = MetadataFiles.forDeployment(rd, referencedConfiguration);
                    if (files.failed()) {
                        return Result.error(files.errorMessage);
                    }

                    deploymentIndex = files.result.files.length;

                    // 1000000 deployments to a single destination should be fairly safe maximum
                    String fileNameBase = String.format("%06d-", deploymentIndex);

                    String appliedPidsFileName = fileNameBase + "applied";

                    StringReader rdr = new StringReader(applied.toString());
                    PrintWriter wrt = new PrintWriter(new FileOutputStream(new File(files.result.baseDir, appliedPidsFileName)));

                    StreamUtil.copy(rdr, wrt, true);

                    return Result.with(null);
                } catch (IOException e) {
                    return Result.error("Failed to save bundle metadata for " + rd + ": " + e.getMessage());
                }
            }

            Result<Void> forget(BundleResourceDeployment rd, Configuration referencedConfiguration) {
                if (deploymentIndex < 0) {
                    throw new IllegalStateException(
                        "Tried to forget deployment metadata without index set. This should not happen");
                }

                String fileNameBase = String.format("%06d-", deploymentIndex);
                String appliedPidsFileName = fileNameBase + "applied";

                File baseDir = MetadataFiles.baseDirFor(rd, referencedConfiguration);

                File applied = new File(baseDir, appliedPidsFileName);

                if (!applied.delete()) {
                    return Result
                        .error("Failed to delete the deployment metadata file '" + applied.getAbsolutePath() + "'.");
                }

                return Result.with(null);
            }
        }
        /**
         * to be used SOLELY by the {@link #forDeployment(org.rhq.core.domain.bundle.BundleResourceDeployment,
         * org.rhq.core.domain.configuration.Configuration)}
         * method
         */
        private BundleMetadata(List<DeploymentMetadata> deployments) {
            this.deployments = deployments;
        }

        static Result<BundleMetadata> forDeployment(BundleResourceDeployment rd, Configuration referecenedConfiguration) {
            try {
                Result<MetadataFiles> files = MetadataFiles.forDeployment(rd, referecenedConfiguration);
                if (files.failed()) {
                    return Result.error(files.errorMessage);
                }

                if (!files.result.exists()) {
                    return Result.error("The metadata for deployment " + rd + " not found.");
                }

                List<DeploymentMetadata> deployments = new ArrayList<DeploymentMetadata>();

                File[] fs = files.result.files;
                for (int i = 0; i < fs.length; ++i) {
                    String addedJson = StreamUtil.slurp(new InputStreamReader(new FileInputStream(fs[i])));

                    List<PatchDetails> addedPatches = PatchDetails.fromJSONArray(addedJson);

                    // the files returned from MetadataFiles are in the reverse order, so we need
                    // to compute the right index here.
                    deployments.add(new DeploymentMetadata(addedPatches, fs.length - i - 1));
                }

                return Result.with(new BundleMetadata(deployments));
            } catch (IOException e) {
                return Result.error("Failed to read bundle metadata for " + rd + ": " + e.getMessage());
            }
        }
    }

    private static class Result<T> {
        final T result;
        final String errorMessage;

        Result(T result, String errorMessage) {
            this.result = result;
            this.errorMessage = errorMessage;
        }

        static <T> Result<T> with(T result) {
            return new Result<T>(result, null);
        }

        static <T> Result<T> error(String errorMessage) {
            return new Result<T>(null, errorMessage);
        }

        boolean failed() {
            return errorMessage != null;
        }
    }
}
