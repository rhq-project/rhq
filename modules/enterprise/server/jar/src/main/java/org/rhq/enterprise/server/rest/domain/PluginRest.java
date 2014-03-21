/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;

/**
 * @author Lukas Krejci
 * @since 4.10
 */
@XmlRootElement(name = "plugin")
@ApiClass("Represents an agent plugin installed in RHQ.")
public final class PluginRest {

    private int id;
    private String name;
    private String displayName;
    private String version;
    private boolean enabled;
    private PluginStatusType status;

    private List<Link> links = new ArrayList<Link>();

    public static PluginRest from(Plugin plugin) {
        PluginRest ret = new PluginRest();

        ret.setId(plugin.getId());
        ret.setName(plugin.getName());
        ret.setDisplayName(plugin.getDisplayName());
        ret.setEnabled(plugin.isEnabled());
        ret.setStatus(plugin.getStatus());
        ret.setVersion(plugin.getVersion());

        return ret;
    }

    public static List<PluginRest> list(List<Plugin> plugins) {
        List<PluginRest> ret = new ArrayList<PluginRest>();
        for(Plugin p : plugins) {
            ret.add(PluginRest.from(p));
        }

        return ret;
    }

    public PluginRest() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PluginStatusType getStatus() {
        return status;
    }

    public void setStatus(PluginStatusType status) {
        this.status = status;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
