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
package org.rhq.plugins.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Information identifying a Linux distribution (e.g. RHEL 4 Update 4, Ubuntu Edgy, etc.).
 *
 * @author Ian Springer
 */
public class LinuxDistroInfo {
    private static final String DEFAULT_NAME = "UNKNOWN";
    private static final String DEFAULT_VERSION = "UNKNOWN";

    private static final File LSB_RELEASE_FILE = new File("/etc/lsb-release");
    private static final File REDHAT_RELEASE_FILE = new File("/etc/redhat-release");
    private static final File REDHAT_VERSION_FILE = new File("/etc/redhat-version"); // rare
    private static final File DEBIAN_VERSION_FILE = new File("/etc/debian_version");

    private static final String DISTRIB_ID_VARIABLE = "DISTRIB_ID";
    private static final String DISTRIB_RELEASE_VARIABLE = "DISTRIB_RELEASE";
    private static final String DISTRIB_CODENAME_VARIABLE = "DISTRIB_CODENAME";

    private static LinuxDistroInfo instance;

    private final Log log = LogFactory.getLog(LinuxDistroInfo.class);

    private String name;
    private String version;

    /**
     * Returns the Linux distro information for the platform this JVM is running on.
     *
     * @return the Linux distro information for the platform this JVM is running on
     */
    public static LinuxDistroInfo getInstance() {
        if (instance == null) {
            if (!isLinux()) {
                throw new IllegalStateException(
                    "Attempt to determine Linux distro information for a non-Linux machine.");
            }

            instance = new LinuxDistroInfo();
        }

        return instance;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    private LinuxDistroInfo() {
        String codename = null;

        // First check for distro-specific release files (e.g. /etc/redhat-release).
        if (REDHAT_RELEASE_FILE.exists() || REDHAT_VERSION_FILE.exists()) {
            File releaseFile;
            if (REDHAT_RELEASE_FILE.exists()) {
                releaseFile = REDHAT_RELEASE_FILE;
            } else {
                releaseFile = REDHAT_VERSION_FILE;
            }

            String release = readSingleLineReleaseFile(releaseFile);
            if (release != null) {
                int index = release.indexOf("release ");
                if (index != -1) {
                    this.name = release.substring(0, index).trim();
                    this.version = release.substring(index);
                }
            }

            if (this.name == null) {
                this.name = "Red Hat Linux";
            }
        } else if (DEBIAN_VERSION_FILE.exists()) {
            String release = readSingleLineReleaseFile(DEBIAN_VERSION_FILE);
            this.name = "Debian";
            if (release != null) {
                this.name += " " + release;
            }
        }
        // TODO: Add support for other common distributions (SuSE, Slackware, Mandrake, etc.); for a nice reference of the
        //       release files supplied by various distros, see http://linuxmafia.com/faq/Admin/release-files.html.

        // Now that we've parsed any distro-specific release files, check for the semi-standardized LSB (Linux Standards
        // Base) release file that is present in many different distros. If distro-related information is defined in this
        // file, it overrides the information defined in any distro-specific version files. For more info on
        // /etc/lsb-release, see http://manpages.unixforum.co.uk/man-pages/linux/opensuse-10.2/1/lsb_release-man-page.html.
        if (LSB_RELEASE_FILE.exists()) {
            Properties releaseInfo = readMultiVariableReleaseFile(LSB_RELEASE_FILE);
            String lsbName = releaseInfo.getProperty(DISTRIB_ID_VARIABLE);
            if (lsbName != null) {
                this.name = lsbName;
            }

            String lsbVersion = releaseInfo.getProperty(DISTRIB_RELEASE_VARIABLE);
            if (lsbVersion != null) {
                this.version = lsbVersion;
            }

            codename = releaseInfo.getProperty(DISTRIB_CODENAME_VARIABLE);
        }

        if (this.name == null) {
            this.name = DEFAULT_NAME;
        }

        if (this.version == null) {
            this.name = DEFAULT_VERSION;
        }

        // Finally, append the release codename if we were able to ascertain it.
        if (codename != null) {
            this.version += " (" + codename + ")";
        }
    }

    private String readSingleLineReleaseFile(File releaseFile) {
        String release = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(releaseFile));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.equals("")) {
                    release = line;
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Failed reading single-line release file: " + releaseFile, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.debug("Failed closing single-line release file: " + releaseFile, e);
                }
            }
        }

        return release;
    }

    private Properties readMultiVariableReleaseFile(File releaseFile) {
        Properties variables = new Properties();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(releaseFile));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.equals("") && (line.charAt(0) != '#')) {
                    int index = line.indexOf('=');
                    if (index != -1) {
                        String varName = line.substring(0, index);
                        String varValue = line.substring(index + 1);
                        varValue = stripSurroundingQuotes(varValue);
                        variables.setProperty(varName, varValue);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed reading multi-variable release file: " + releaseFile, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.debug("Failed closing multi-variable release file: " + releaseFile, e);
                }
            }
        }

        return variables;
    }

    private String stripSurroundingQuotes(String varValue) {
        if (((varValue.charAt(0) == '"') && (varValue.charAt(varValue.length() - 1) == '"'))
            || ((varValue.charAt(0) == '\'') && (varValue.charAt(varValue.length() - 1) == '\''))) {
            varValue = varValue.substring(1, varValue.length() - 1);
        }

        return varValue;
    }

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase(Locale.US).indexOf("linux") != -1;
    }
}