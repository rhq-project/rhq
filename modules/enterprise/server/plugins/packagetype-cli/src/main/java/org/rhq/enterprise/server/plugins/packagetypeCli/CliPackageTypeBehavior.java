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

package org.rhq.enterprise.server.plugins.packagetypeCli;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionFormatDescription;
import org.rhq.core.domain.content.ValidatablePackageDetailsKey;
import org.rhq.core.domain.util.OSGiVersion;
import org.rhq.core.domain.util.OSGiVersionComparator;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.content.AbstractPackageTypeBehavior;
import org.rhq.enterprise.server.plugin.pc.content.PackageDetailsValidationException;

/**
 * This plugin defines the version formatting of the server-side CLI package type.
 * 
 * In order for the CLI packages to exist per user, the format of the version has to include
 * the username.
 * 
 * The format is therefore following:
 * 
 * {username}:{osgi.version.string}
 *
 * @author Lukas Krejci
 */
public class CliPackageTypeBehavior extends AbstractPackageTypeBehavior<ServerPluginComponent> {

    private static final Log LOG = LogFactory.getLog(CliPackageTypeBehavior.class);
    
    private static final String FULL_VERSION_REGEX = "^([^:]+:)?\\d+(\\.\\d+(\\.\\d+(\\..*)?)?)?$";
    private static final String OSGI_EXTRACT_REGEX = "^([^:]+:)?(\\d+(\\.\\d+(\\.\\d+(\\..*)?)?)?)$";
    private static final int OSGI_EXTRACT_VERSION_GROUP = 2;
    
    private static final String PACKAGETYPE_NAME = "__SERVER_SIDE_CLI_SCRIPT";

    private static final Comparator<PackageVersion> VERSION_COMPARATOR = new Comparator<PackageVersion>() {
        private final OSGiVersionComparator OSGI_COMPARATOR = new OSGiVersionComparator();
        public int compare(PackageVersion o1, PackageVersion o2) {
            String v1 = o1.getVersion();
            String v2 = o2.getVersion();
            
            if (v1 != null && v2 != null) {
                String[] v1Parts = getVersionParts(v1);
                String[] v2Parts = getVersionParts(v2);
                
                if (v1Parts[1] != null && v2Parts[1] != null) {
                    try {
                        return OSGI_COMPARATOR.compare(v1Parts[1], v2Parts[1]);
                    } catch (IllegalArgumentException e) {
                        //well.. they don't look like OSGi versions
                    }
                }
            }
            
            //as a fallback, compare by ID. But we're enforcing the format in the validate
            //method so this shouldn't ever happen.
            
            LOG.warn("Using a fallback version comparison on package versions " + o1 + " and " + o2 + ". This should not happen.");
            
            return Integer.valueOf(o1.getId()).compareTo(o2.getId());
        }           
    };
    
    public Comparator<PackageVersion> getPackageVersionComparator(String packageTypeName) {
        if (PACKAGETYPE_NAME.equals(packageTypeName)) {
            return VERSION_COMPARATOR; 
        }
        return null;
    }
    
    public void validateDetails(ValidatablePackageDetailsKey key, Subject origin)
        throws PackageDetailsValidationException {
        
        key.setVersion(reformatVersion(key.getVersion(), origin.getName()));
    }
    
    public PackageVersionFormatDescription getPackageVersionFormat(String packageTypeName) {
        if (PACKAGETYPE_NAME.equals(packageTypeName)) {
            return new PackageVersionFormatDescription(FULL_VERSION_REGEX, OSGI_EXTRACT_REGEX, OSGI_EXTRACT_VERSION_GROUP, null);
        }
        return null;
    }
    
    private static String reformatVersion(String version, String userName) throws PackageDetailsValidationException {
        String[] parts = getVersionParts(version);
        
        if (!OSGiVersion.isValid(parts[1])) {
            //ok the user supplied something that isn't an OSGi version.
            //We can't accept that.
            throw new PackageDetailsValidationException("The version part of '" + version + "' isn't an OSGi version string.");
        }
        
        return userName + ":" + parts[1];
    }
    
    private static String[] getVersionParts(String version) {
        String[] ret = new String[2];
        
        int colonIdx = version.indexOf(':');
        if (colonIdx >= 0 && colonIdx < version.length() - 1) {
            ret[0] = version.substring(0, colonIdx);
            ret[1] = version.substring(colonIdx + 1);
        } else {
            ret[1] = version;
        }
        
        return ret;
    }    
}
