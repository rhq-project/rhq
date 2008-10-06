 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.platform.content;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;

/**
 * Handles discovering RPMs installed on a Linux system.
 *
 * @author Jason Dobies
 */
public class RpmPackageDiscoveryDelegate {
    // Static  --------------------------------------------

    private static SystemInfo systemInfo;

    /**
     * Holds the full file name of the RPM executable if found. Will contain <code>null</code> if there is no RPM
     * executable on the system.
     */
    private static String rpmExecutable;

    // Public  --------------------------------------------

    public static void checkExecutables() {
        // Make sure rpm is actually on the system
        ProcessExecution processExecution = new ProcessExecution("/usr/bin/which");
        processExecution.setArguments(new String[] { "rpm" });
        processExecution.setCaptureOutput(true);

        ProcessExecutionResults executionResults = systemInfo.executeProcess(processExecution);
        String capturedOutput = executionResults.getCapturedOutput();

        rpmExecutable = (((capturedOutput == null) || "".equals(capturedOutput)) ? null : capturedOutput.trim());
    }

    public static void setSystemInfo(SystemInfo systemInfo) {
        RpmPackageDiscoveryDelegate.systemInfo = systemInfo;
    }

    public static Set<ResourcePackageDetails> discoverPackages(PackageType type) throws IOException {
        Log log = LogFactory.getLog(RpmPackageDiscoveryDelegate.class);

        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        if (rpmExecutable == null) {
            return packages;
        }

        /* Sample output from: rpm -qa
         * xorg-x11-fonts-ethiopic-7.2-3.fc8 bitmap-fonts-0.3-5.1.2.fc7 bluez-utils-cups-3.20-4.fc8
         *
         * In short, it's a list of installed packages.
         */
        ProcessExecution listRpmsProcess = new ProcessExecution(rpmExecutable);
        listRpmsProcess.setArguments(new String[] { "-qa" });
        listRpmsProcess.setCaptureOutput(true);

        ProcessExecutionResults executionResults = systemInfo.executeProcess(listRpmsProcess);
        String capturedOutput = executionResults.getCapturedOutput();

        // Process the resulting output
        if (capturedOutput == null) {
            return packages;
        }

        BufferedReader rpmNameReader = new BufferedReader(new StringReader(capturedOutput));

        String rpmName;
        while ((rpmName = rpmNameReader.readLine()) != null) {
            String name = null;
            String version = null;
            String architectureName = null;

            try {
                // Execute RPM query for each RPM
                ProcessExecution rpmQuery = new ProcessExecution(rpmExecutable);
                rpmQuery
                    .setArguments(new String[] {
                        "-q",
                        "--qf",
                        "%{NAME}\\n%{VERSION}.%{RELEASE}\\n%{ARCH}\\n%{INSTALLTIME}\\n%{FILEMD5S}\\n%{GROUP}\\n%{FILENAMES}\\n%{SIZE}\\n%{LICENSE}\\n%{DESCRIPTION}",
                        rpmName });
                rpmQuery.setCaptureOutput(true);

                ProcessExecutionResults rpmDataResults = systemInfo.executeProcess(rpmQuery);
                String rpmData = rpmDataResults.getCapturedOutput();

                BufferedReader rpmDataReader = new BufferedReader(new StringReader(rpmData));

                name = rpmDataReader.readLine();
                version = rpmDataReader.readLine();
                architectureName = rpmDataReader.readLine();
                String installTime = rpmDataReader.readLine();
                String md5 = rpmDataReader.readLine();
                String category = rpmDataReader.readLine();
                String fileName = rpmDataReader.readLine();
                String sizeString = rpmDataReader.readLine();
                String license = rpmDataReader.readLine();

                StringBuffer description = new StringBuffer();
                String descriptionTemp;
                while ((descriptionTemp = rpmDataReader.readLine()) != null) {
                    description.append(descriptionTemp);
                }

                Long fileSize = null;
                if (sizeString != null) {
                    try {
                        fileSize = Long.parseLong(sizeString);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse file size: " + sizeString);
                    }
                }

                // There may be multiple file names. For now, just ignore the package (I'll find a better way
                // to deal with this going forward). There will be a blank space in the fileName attribute, so
                // just abort if we see that
                if ((fileName == null) || fileName.equals("")) {
                    continue;
                }

                PackageDetailsKey key = new PackageDetailsKey(name, version, "rpm", architectureName);
                ResourcePackageDetails packageDetails = new ResourcePackageDetails(key);
                packageDetails.setClassification(category);
                packageDetails.setDisplayName(name);
                packageDetails.setFileName(fileName);
                packageDetails.setMD5(md5);
                packageDetails.setFileSize(fileSize);
                packageDetails.setLicenseName(license);
                packageDetails.setLongDescription(description.toString());

                packages.add(packageDetails);
            } catch (Exception e) {
                log.error("Error creating resource package. RPM Name: " + rpmName + " Version: " + version
                    + " Architecture: " + architectureName);
            }
        }

        return packages;
    }
}