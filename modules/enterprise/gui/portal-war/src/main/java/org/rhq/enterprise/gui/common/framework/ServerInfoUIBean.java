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

import org.jboss.seam.Component;
import org.rhq.core.util.maven.MavenArtifactNotFoundException;
import org.rhq.core.util.maven.MavenArtifactProperties;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.faces.component.UIComponent;
import java.util.TimeZone;

/**
 * A JSF managed bean that exposes various general server information - RHQ version, timezone, etc.
 *
 * @author Ian Springer
 */
public class ServerInfoUIBean {
    private static final String UNKNOWN_VERSION = "UNKNOWN";

    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    public boolean isDebugModeEnabled() {
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        return Boolean.valueOf(systemManager.getSystemConfiguration().getProperty(RHQConstants.EnableDebugMode));
    }

    public String getFacesVersion() {
        return getVersion(UIComponent.class);
    }

    public String getSeamVersion() {
        return getVersion(Component.class);
    }

    public String getRichFacesVersion() {
        MavenArtifactProperties richFacesMavenProps;
        try {
            richFacesMavenProps = MavenArtifactProperties.getInstance("org.richfaces.framework", "richfaces-api");
        } catch (MavenArtifactNotFoundException e) {
            // This really should never happen. TODO: Log an error.
            richFacesMavenProps = null;
        }
        String richFacesVersion = (richFacesMavenProps != null) ? richFacesMavenProps.getVersion() : UNKNOWN_VERSION;
        return (richFacesVersion != null) ? richFacesVersion : UNKNOWN_VERSION;
    }

    private static String getVersion(Class clazz) {
        String version = null;
        try {
            return clazz.getPackage().getImplementationVersion();
        }
        catch (Exception e) {
            return UNKNOWN_VERSION;
        }
    }
}