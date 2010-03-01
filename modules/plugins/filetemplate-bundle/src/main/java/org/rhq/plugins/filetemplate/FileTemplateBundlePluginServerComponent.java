/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.filetemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.bundle.BundleDeployDefinition;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class FileTemplateBundlePluginServerComponent implements ResourceComponent, BundleFacet {

    private final Log log = LogFactory.getLog(FileTemplateBundlePluginServerComponent.class);

    private ResourceContext resourceContext;

    public void start(ResourceContext context) {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public BundleDeployResult deployBundle(BundleDeployRequest request) {
        BundleDeployResult result = new BundleDeployResult();
        try {
            BundleDeployDefinition bundleDeployDef = request.getBundleDeployDefinition();
            BundleVersion bundleVersion = bundleDeployDef.getBundleVersion();

            // download all the bundle files to our tmp directory
            File tmpDir = new File(this.resourceContext.getTemporaryDirectory(), "" + bundleVersion.getId());
            BundleManagerProvider bundleManager = request.getBundleManagerProvider();
            List<PackageVersion> packageVersions = bundleManager.getAllBundleVersionPackageVersions(bundleVersion);
            for (PackageVersion packageVersion : packageVersions) {
                File packageFile = new File(tmpDir, packageVersion.getFileName());
                packageFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(packageFile);
                try {
                    long size = bundleManager.getFileContent(packageVersion, fos);
                    if (packageVersion.getFileSize() != null && size != packageVersion.getFileSize().longValue()) {
                        log.warn("Downloaded bundle file [" + packageVersion + "] but its size was [" + size
                            + "] when it was expected to be [" + packageVersion.getFileSize() + "].");
                    }
                } finally {
                    fos.close();
                }

                // verify the content
                if (packageVersion.getMD5() != null) {
                    String realMD5 = MessageDigestGenerator.getDigestString(packageFile);
                    if (!packageVersion.getMD5().equals(realMD5)) {
                        throw new Exception("Package version [" + packageVersion + "] failed MD5 check. expected=["
                            + packageVersion.getMD5() + "], actual=[" + realMD5 + "]");
                    }
                } else if (packageVersion.getSHA256() != null) {
                    FileInputStream is = new FileInputStream(packageFile);
                    try {
                        MessageDigestGenerator gen = new MessageDigestGenerator("SHA256");
                        gen.add(is);
                        String realSHA256 = gen.getDigestString();
                        if (!packageVersion.getSHA256().equals(realSHA256)) {
                            throw new Exception("Package version [" + packageVersion
                                + "] failed SHA256 check. expected=[" + packageVersion.getSHA256() + "], actual=["
                                + realSHA256 + "]");
                        }
                    } finally {
                        is.close();
                    }

                } else {
                    log.debug("Package version [" + packageVersion + "] has no MD5/SHA256 hash - not verifying it");
                }
            }

            // TODO all bundle files are downloaded - now process the bundle

        } catch (Throwable t) {
            log.error("Failed to deploy bundle [" + request + "]", t);
            result.setErrorMessage(t);
        }
        return result;
    }
}
