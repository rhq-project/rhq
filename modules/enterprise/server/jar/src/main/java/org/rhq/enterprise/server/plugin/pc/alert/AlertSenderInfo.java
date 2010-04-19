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
package org.rhq.enterprise.server.plugin.pc.alert;

import java.net.URL;

import org.rhq.core.domain.plugin.PluginKey;

/**
 * Information about an {@link AlertSender}
 * @author Heiko W. Rupp
 */
public class AlertSenderInfo {

    private String shortName;
    private String description;
    private String pluginName;
    private PluginKey pluginKey;
    private URL uiSnippetUrl;
    private String uiSnippetShortPath;

    public AlertSenderInfo(String shortName, String description, PluginKey key) {
        this.shortName = shortName;
        this.description = description;
        this.pluginKey = key;
        this.pluginName = key.getPluginName();
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPluginName() {
        return pluginName;
    }

    public PluginKey getPluginKey() {
        return pluginKey;
    }

    public URL getUiSnippetUrl() {
        return uiSnippetUrl;
    }

    public void setUiSnippetUrl(URL uiSnippetUrl) {
        this.uiSnippetUrl = uiSnippetUrl;
    }

    public String getUiSnippetShortPath() {
        return uiSnippetShortPath;
    }

    public void setUiSnippetShortPath(String uiSnippetShortPath) {
        this.uiSnippetShortPath = uiSnippetShortPath;
    }
}
