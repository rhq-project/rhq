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

import java.util.regex.Pattern;
import org.rhq.core.system.SystemInfo;

/**
 * Handles discovery of DEB artifacts and revisions on a Linux system.
 *
 * @author Jason Dobies
 */
public class DebPackageDiscoveryDelegate {
    // Constants  --------------------------------------------

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    // Static  --------------------------------------------

    private static SystemInfo systemInfo;

    private static boolean dpkgExists;
    private static boolean aptCacheExists;

    // Public  --------------------------------------------

    /*
     * public static void checkExecutables() {   File dpkgExec = new File("/usr/bin/dpkg");   dpkgExists =
     * dpkgExec.exists();
     *
     * File aptCacheExec = new File("/usr/bin/apt-cache");   aptCacheExists = aptCacheExec.exists(); }
     *
     * public static Set<ArtifactDetails> discoverPackages(ArtifactType type)   throws IOException {
     * Set<ArtifactDetails> artifacts = new HashSet<ArtifactDetails>();
     *
     * // Make sure dpkg is actually on the system   if (!dpkgExists)      return artifacts;
     *
     * ProcessExecution processExecution = new ProcessExecution("/usr/bin/dpkg");   processExecution.setArguments(new
     * String[]{"-l"});   processExecution.setCaptureOutput(true);
     *
     * ProcessExecutionResults executionResults = systemInfo.executeProcess(processExecution);   String capturedOutput =
     * executionResults.getCapturedOutput();
     *
     * if (capturedOutput == null)   {      return artifacts;   }
     *
     */
    /* Output Format:
     *        Desired=Unknown/Install/Remove/Purge/Hold     |
     * Status=Not/Installed/Config-files/Unpacked/Failed-config/Half-installed     |/
     * Err?=(none)/Hold/Reinst-required/X=both-problems (Status,Err: uppercase=bad)     ||/ Name
     * Version                    Description
     * +++-==========================-==========================-====================================================================
     *     ii  acpi                       0.09-1                     displays information on ACPI devices     ii
     * acpi-support               0.95                       a collection of useful events for acpi     ii  acpid
     *               1.0.4-5ubuntu6             Utilities for using ACPI power management     rc  apache2-common
     *     2.0.55-4ubuntu4            next generation, scalable, extendable web server
     */
    /*
     *
     * BufferedReader lineReader = new BufferedReader(new StringReader(capturedOutput));
     *
     * // Pop off the first 5 lines of table headers   for (int ii = 0; ii < 5; ii++)   {      lineReader.readLine();
     * }
     *
     * String line;   while ((line = lineReader.readLine()) != null)   {      String[] columns =
     * WHITESPACE_PATTERN.split(line);
     *
     *    String artifactName = columns[1];
     *
     *    String description = "";      for (int ii = 3; ii < columns.length; ii++)         description = description +
     * columns[ii] + " ";
     *
     *    ArtifactDetails artifact = new ArtifactDetails(type, artifactName, artifactName);
     * artifact.setDescription(description);
     *
     *    artifacts.add(artifact);   }
     *
     * return artifacts; }
     *
     * public static ArtifactRevisionDetails discoverRevision(ArtifactDetails artifact, ArtifactRevisionDetails
     * lastRevision)   throws IOException {   // Make sure apt-cache is actually on the system   if (!aptCacheExists)
     *   return null;
     *
     * ProcessExecution processExecution = new ProcessExecution("/usr/bin/apt-cache");
     * processExecution.setArguments(new String[]{"show", artifact.getArtifactKey()});
     * processExecution.setCaptureOutput(true);
     *
     * ProcessExecutionResults executionResults = systemInfo.executeProcess(processExecution);   String capturedOutput =
     * executionResults.getCapturedOutput();
     *
     * if (capturedOutput == null)   {      return null;   }
     *
     */
    /* Output Format:
     *        Package: php4-common     Status: install ok installed     Priority: optional     Section: web
     * Installed-Size: 392     Maintainer: Debian PHP Maintainers <pkg-php-maint@lists.alioth.debian.org>
     * Architecture: i386     Source: php4     Version: 4:4.4.2-1.1     Depends: sed (>= 4.1.1-1)     Conffiles:
     * /etc/cron.d/php4 e54ece92e079603b47bbf398e669f9f8     Description: Common files for packages built from the php4
     * source      This package contains the documentation and example files relevant to all      the other packages
     * built from the php4 source.      .      PHP4 is an HTML-embedded scripting language. Much of its syntax is
     * borrowed      from C, Java and Perl with a couple of unique PHP-specific features thrown      in. The goal of the
     * language is to allow web developers to write dynamically      generated pages quickly.      .     Homepage:
     * http://www.php.net/                 Package: aterm     Priority: optional     Section: universe/x11
     * Installed-Size: 228     Maintainer: Ubuntu MOTU Developers <ubuntu-motu@lists.ubuntu.com>
     * Original-Maintainer: G�ran Weinholt <weinholt@debian.org>     Architecture: i386     Version: 1.0.0-4
     * Provides: x-terminal-emulator     Depends: libafterimage0 (>= 2.2.2), libc6 (>= 2.5-0ubuntu1), libfreetype6 (>=
     * 2.2), libice6 (>= 1:1.0.0), libjpeg62, libpng12-0 (>= 1.2.13-4), libsm6, libtiff4, libungif4g (>= 4.1.4),
     * libx11-6, libxext6, zlib1g (>= 1:1.2.1)     Conflicts: suidmanager (<< 0.50)     Filename:
     * pool/universe/a/aterm/aterm_1.0.0-4_i386.deb     Size: 83220     MD5sum: a78aeb3aafe549f91167ed427045c46b
     * SHA1: d56766194b730614d6f143fca1446abe205343c8     SHA256:
     * 14c1dcce265b5f4f393682a7968c0657ed1b5c92888767362279e04e1a3325fe     Description: Afterstep XVT - a VT102
     * emulator for the X window system       Aterm is a colour vt102 terminal emulator, based on rxvt 2.4.8 with
     * some additions of fast transparency, intended as an xterm replacement       for users who do not require features
     * such as Tektronix 4014       emulation and toolkit-style configurability. As a result, aterm uses       much less
     * swap space -- a significant advantage on a machine serving       many X sessions. It was created with AfterStep
     * Window Manager users       in mind, but is not tied to any libraries, and can be used anywhere.     Bugs:
     * mailto:ubuntu-users@lists.ubuntu.com     Origin: Ubuntu
     *
     */
    /*
     *
     * ArtifactRevisionDetails details = new ArtifactRevisionDetails(artifact.getArtifactKey(),
     * "application/octet-stream");   Configuration configuration = new Configuration();
     * details.setConfigurationContent(configuration);
     *
     * BufferedReader lineReader = new BufferedReader(new StringReader(capturedOutput));
     *
     * String line;   String[] pieces;
     *
     * while ((line = lineReader.readLine()) != null)   {      pieces = WHITESPACE_PATTERN.split(line);
     *
     *    if (pieces.length == 0)         continue;
     *
     *    // Version      if (pieces[0].equals("Version:"))      {         String version = pieces[1];
     *
     *       // ***** Version check to determine if this is a new revision *****         if (lastRevision != null &&
     * lastRevision.getRevisionIdentifier() != null && lastRevision.getRevisionIdentifier().equals(version))         {
     *          return null;         }
     *
     *       configuration.put(new PropertySimple("Version", version));         details.setRevisionIdentifier(version);
     *     }
     *
     *    // Filename      else if (pieces[0].equals("Filename:"))         configuration.put(new
     * PropertySimple("Filename", pieces[1]));
     *
     *       // MD5      else if (pieces[0].equals("MD5sum:"))         details.setMd5(pieces[1]);
     *
     *       // Size      else if (pieces[0].equals("Size:"))         details.setContentSize(new Long(pieces[1]));
     *
     *       // Architecture      else if (pieces[0].equals("Architecture:"))         configuration.put(new
     * PropertySimple("Architecture", pieces[1]));   }
     *
     * return details; }
     *
     * public static void setSystemInfo(SystemInfo systemInfo) {   DebPackageDiscoveryDelegate.systemInfo = systemInfo; }
     *
     * // Private  --------------------------------------------
     *
     * private String executeWhich(String findMe) {   ProcessExecution processExecution = new
     * ProcessExecution("/usr/bin/which");   processExecution.setArguments(new String[]{findMe});
     * processExecution.setCaptureOutput(true);
     *
     * ProcessExecutionResults executionResults = systemInfo.executeProcess(processExecution);   String capturedOutput =
     * executionResults.getCapturedOutput();
     *
     * String executable = ("".equals(capturedOutput) ? null : capturedOutput);   return executable; }
     */
}