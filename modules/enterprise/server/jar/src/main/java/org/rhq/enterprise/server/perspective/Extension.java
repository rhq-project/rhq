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

import org.rhq.enterprise.server.perspective.activator.Activator;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ExtensionType;

/**
 * A GUI extension defined by the Perspective subsystem. Currently there are four types of extensions -
 * menu item, tab, global task, and Resource task.
 *
 * @author Ian Springer
 */
public abstract class Extension {
    protected String perspectiveName;
    protected String name;
    protected String url;
    protected boolean debugMode;
    private List<Activator<?>> activators;

    public Extension(ExtensionType rawExtension, String perspectiveName, String url) {
        this.perspectiveName = perspectiveName;
        this.name = rawExtension.getName();
        this.url = url;
        this.activators = new ArrayList<Activator<?>>();
        this.debugMode = false;
    }

    public String getPerspectiveName() {
        return perspectiveName;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public List<Activator<?>> getActivators() {
        return activators;
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
        return this.getClass().getSimpleName() + "[name=" + this.name + ", url=" + this.url + "]";
    }
}
