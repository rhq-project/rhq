/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.framework;

import java.util.TimeZone;

import javax.faces.component.UIComponent;

import com.sun.facelets.Facelet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.Component;

import org.rhq.core.util.maven.MavenArtifactNotFoundException;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A JSF managed bean that exposes various general server information - RHQ version, timezone, etc.
 *
 * @author Ian Springer
 */
public class ServerInfoUIBean {
    private static final String UNKNOWN_VERSION = "UNKNOWN";

    private final Log log = LogFactory.getLog(this.getClass());

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public boolean isDebugModeEnabled() {
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        return Boolean.valueOf(systemManager.getSystemConfiguration(LookupUtil.getSubjectManager().getOverlord())
            .getProperty(RHQConstants.EnableDebugMode));
    }

    public boolean isExperimentalFeaturesEnabled() {
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        return Boolean.valueOf(systemManager.getSystemConfiguration(LookupUtil.getSubjectManager().getOverlord())
            .getProperty(RHQConstants.EnableExperimentalFeatures));
    }

    public String getFacesVersion() {
        return getManifestVersion(UIComponent.class);
    }

    public String getFaceletsVersion() {
        return getManifestVersion(Facelet.class);
    }

    public String getSeamVersion() {
        return getManifestVersion(Component.class);
    }

    public String getRichFacesVersion() {
        MavenArtifactProperties richFacesMavenProps;
        try {
            richFacesMavenProps = MavenArtifactProperties.getInstance("org.richfaces.framework", "richfaces-api");
        } catch (MavenArtifactNotFoundException e) {
            log.error("Could not ascertain RichFaces version.", e);
            richFacesMavenProps = null;
        }
        String richFacesVersion = (richFacesMavenProps != null) ? richFacesMavenProps.getVersion() : null;
        return (richFacesVersion != null) ? richFacesVersion : UNKNOWN_VERSION;
    }

    private String getManifestVersion(Class clazz) {
        String version = null;
        try {
            Package pkg = clazz.getPackage();
            if (pkg != null) {
                version = pkg.getImplementationVersion();
                if (version == null) {
                    version = pkg.getSpecificationVersion();
                }
            }
        } catch (Exception e) {
            log.error("Error while looking up manifest version for " + clazz + ".", e);
        }
        return version;
    }
}