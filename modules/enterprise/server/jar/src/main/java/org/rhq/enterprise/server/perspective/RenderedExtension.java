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

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.RenderedExtensionType;

/**
 * A GUI extension defined by the Perspective subsystem. Currently there are four types of extensions -
 * menu item, tab, global task, and Resource task.
 *
 * @author Ian Springer
 */
public abstract class RenderedExtension extends Extension {

    private String displayName;
    private String iconUrl;

    public RenderedExtension(RenderedExtensionType rawExtension, String perspectiveName, String url) {
        super(rawExtension, perspectiveName, url);
        this.displayName = rawExtension.getDisplayName();
        this.iconUrl = rawExtension.getIconUrl();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[name=" + this.name + ", displayName=" + this.displayName + ", url="
            + this.url + ", iconUrl=" + this.iconUrl + "]";
    }
}
