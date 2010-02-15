/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import java.io.Serializable;

import org.rhq.enterprise.server.perspective.activator.ActivatorHelper;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PageLinkType;

/**
 * An item in the RHQ GUI's menu.
 */
public class PageLink extends Extension implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String pageName;

    public PageLink(PageLinkType rawPageLink, String perspectiveName, String pageName, String url) {
        super(rawPageLink, perspectiveName, url);

        this.pageName = pageName;
        this.debugMode = ActivatorHelper.initGlobalActivators(rawPageLink.getActivators(), getActivators());
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

}
