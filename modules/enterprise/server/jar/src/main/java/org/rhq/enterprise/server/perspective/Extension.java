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
package org.rhq.enterprise.server.perspective;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.server.perspective.activator.Activator;
import org.rhq.enterprise.server.perspective.activator.GlobalPermissionActivator;
import org.rhq.enterprise.server.perspective.activator.LicenseFeature;
import org.rhq.enterprise.server.perspective.activator.LicenseFeatureActivator;
import org.rhq.enterprise.server.perspective.activator.SuperuserActivator;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.CommonActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.DebugModeActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ExtensionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalPermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.LicenseFeatureActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.SuperuserActivatorType;

/**
 * A GUI extension defined by the Perspective subsystem. Currently there are four types of extensions -
 * menu item, tab, global task, and Resource task.
 *
 * @author Ian Springer
 */
@SuppressWarnings("unchecked")
public abstract class Extension {
    private String perspectiveName;
    private String name;
    private String displayName;
    private String url;
    private String iconUrl;
    private boolean debugMode;
    private List<Activator> activators;

    public Extension(ExtensionType rawExtension, String perspectiveName, String url) {
        this.perspectiveName = perspectiveName;
        this.name = rawExtension.getName();
        this.displayName = rawExtension.getDisplayName();
        this.url = url;
        this.iconUrl = rawExtension.getIconUrl();
        this.activators = new ArrayList<Activator>();
    }

    public String getPerspectiveName() {
        return perspectiveName;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public List<Activator> getActivators() {
        return activators;
    }

    protected void initCommonActivators(CommonActivatorsType rawActivators) {
        if (rawActivators == null) {
            return;
        }

        DebugModeActivatorType rawDebugModeActivator = rawActivators.getDebugMode();
        if (rawDebugModeActivator != null) {
            this.debugMode = true;
        }

        List<LicenseFeatureActivatorType> rawLicenseFeatures = rawActivators.getLicenseFeature();
        for (LicenseFeatureActivatorType rawLicenseFeature : rawLicenseFeatures) {
            String rawName = rawLicenseFeature.getName().value();
            LicenseFeature licenseFeature = LicenseFeature.valueOf(rawName.toUpperCase(Locale.US));
            LicenseFeatureActivator licenseFeatureActivator = new LicenseFeatureActivator(licenseFeature);
            this.activators.add(licenseFeatureActivator);
        }

        SuperuserActivatorType rawSuperuserActivator = rawActivators.getSuperuser();
        if (rawSuperuserActivator != null) {
            SuperuserActivator superuserActivator = new SuperuserActivator();
            this.activators.add(superuserActivator);
        }

        List<GlobalPermissionActivatorType> rawGlobalPermissionActivators = rawActivators.getGlobalPermission();
        for (GlobalPermissionActivatorType rawGlobalPermissionActivator : rawGlobalPermissionActivators) {
            String rawName = rawGlobalPermissionActivator.getName().value();
            Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
            GlobalPermissionActivator globalPermissionActivator = new GlobalPermissionActivator(permission);
            this.activators.add(globalPermissionActivator);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Extension extension = (Extension) o;

        if (!name.equals(extension.name))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[name=" + this.name + ", displayName=" + this.displayName + ", url="
            + this.url + ", iconUrl=" + this.iconUrl + "]";
    }
}
