/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.platform.win;

import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.Date;
import java.util.HashSet;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;

/**
 * Provides for discovery of Windows Installed Software. Uses the uninstall registry that's
 * used by Add/Remove Programs. Only supports discovery, not installation.
 *
 * @author Greg Hinkle
 */
public class WindowsSoftwareDelegate {

    private final Log log = LogFactory.getLog(WindowsSoftwareDelegate.class);

    public Set<ResourcePackageDetails> discoverInstalledSoftware(PackageType type) {
        Set<ResourcePackageDetails> installedSoftware = new HashSet<ResourcePackageDetails>();
        try {
            String uninstallList = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall";
            RegistryKey key = RegistryKey.LocalMachine;
            RegistryKey uninstall = key.openSubKey(uninstallList);
            String[] packages = uninstall.getSubKeyNames();

            for (String packageName : packages) {

                RegistryKey packageKey = uninstall.openSubKey(packageName);

                String installDateString = getStringValue(packageKey, "InstallDate");
                String displayName = getStringValue(packageKey, "DisplayName");
                String version = getStringValue(packageKey, "DisplayVersion");


                if (displayName != null && installDateString != null && version != null) {

                    ResourcePackageDetails details =
                            new ResourcePackageDetails(
                                    new ContentSourcePackageDetailsKey(
                                            displayName,
                                            version,
                                            type.getName(),
                                            "noarch",
                                            "Windows",
                                            "Platform"
                                    ));
                    details.setFileCreatedDate(getDate(installDateString));
                    details.setFileSize((long) packageKey.getIntValue("EstimatedSize", 0));
                    details.setDeploymentTimeConfiguration(getConfigurations(packageKey));
                    installedSoftware.add(details);
                }
            }
        } catch (Win32Exception e) {
            log.warn("Failed in discovery of installed Windows software.", e);
        }
        return installedSoftware;
    }

    private Configuration getConfigurations(RegistryKey packageKey) {
        Configuration config = new Configuration();
        String[] properties = { "Publisher","Comments", "Contact", "HelpLink", "HelpTelephone", "InstallLocation", "InstallSource" };
        for (String key : properties) {
            config.put(new PropertySimple(key, getStringValue(packageKey,key)));
        }

        config.put(new PropertySimple("EstimatedSize", packageKey.getIntValue("EstimatedSize",0)));

        return config;
    }


    private static Date getDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Date date = null;

        try {
            date = sdf.parse(dateString);
        } catch (ParseException e) {

            DateFormat df = DateFormat.getDateTimeInstance();
            try {
                date = df.parse(dateString);
            } catch (ParseException e1) {
                /* Poorly formatted dates are ignored */
            }
        }

        return date;
    }

    /**
     * String values are returned with their null terminator in the string... clear them out
     */
    private static String getStringValue(RegistryKey key, String name) {
        String value = key.getStringValue(name, null);
        if (value != null) {
            value = value.trim();
        }
        return value;
    }
}
