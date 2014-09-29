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

package org.rhq.modules.plugins.jbossas7.patching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.modules.plugins.jbossas7.util.PatchDetails;

/**
* @author Lukas Krejci
* @since 4.13
*/
final class BundleMetadata {
    final List<DeploymentMetadata> deployments;
    final MetadataFiles metadataFiles;

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

    private BundleMetadata(List<DeploymentMetadata> deployments, MetadataFiles files) {
        this.deployments = deployments;
        this.metadataFiles = files;
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

            return Result.with(new BundleMetadata(deployments, files.result));
        } catch (IOException e) {
            return Result.error("Failed to read bundle metadata for " + rd + ": " + e.getMessage());
        }
    }

    MetadataFiles getMetadataFiles() {
        return metadataFiles;
    }
}
