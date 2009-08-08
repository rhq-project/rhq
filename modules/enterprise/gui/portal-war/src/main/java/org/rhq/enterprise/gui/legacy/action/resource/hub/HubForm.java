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
package org.rhq.enterprise.gui.legacy.action.resource.hub;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.jetbrains.annotations.Nullable;

import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A common extension for hub pages
 */
public abstract class HubForm extends BaseValidatorForm {
    /**
     * The view style - either "chart" or "list". The value corresponds to {@link HubView#name()} - either "CHART" or
     * "LIST".
     */
    protected String view;

    /**
     * Resource type filter (null indicates all types within the current category). For compatible groups, this field
     * indicates the group's resource type.
     */
    protected String resourceType;
    protected String plugin;

    protected List<LabelValueBean> functions;
    protected List<LabelValueBean> types;
    protected List<LabelValueBean> plugins;
    protected String keywords;
    protected String[] resources;

    public HubForm() {
        super();
        setDefaults();
    }

    @Nullable
    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public List<LabelValueBean> getFunctions() {
        return this.functions;
    }

    public void setFunctions(List<LabelValueBean> functions) {
        this.functions = functions;
    }

    public void addFunction(LabelValueBean function) {
        if (this.functions == null) {
            this.functions = new ArrayList<LabelValueBean>();
        }

        this.functions.add(function);
    }

    public List<LabelValueBean> getTypes() {
        return this.types;
    }

    public void setTypes(List<LabelValueBean> types) {
        this.types = types;
    }

    public void addType(LabelValueBean type) {
        if (this.types == null) {
            this.types = new ArrayList<LabelValueBean>();
        }

        this.types.add(type);
    }

    public void addTypeFirst(LabelValueBean type) {
        if ((this.types != null) && (type != null)) {
            this.types.add(0, type);
        }
    }

    public List<LabelValueBean> getPlugins() {
        return this.plugins;
    }

    public void setPlugins(List<LabelValueBean> plugins) {
        this.plugins = plugins;
    }

    public void addPlugin(LabelValueBean plugin) {
        if (this.plugins == null) {
            this.plugins = new ArrayList<LabelValueBean>();
        }

        this.plugins.add(plugin);
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        setDefaults();
    }

    protected void setDefaults() {
        functions = new ArrayList<LabelValueBean>();
        types = new ArrayList<LabelValueBean>();
        view = HubView.LIST.name();
        resources = new String[0];
    }

    public String[] getResources() {
        return resources;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.append(" resourceType=").append(resourceType);
        stringBuilder.append(" plugin=").append(plugin);
        stringBuilder.append(" functions=").append(functions);
        stringBuilder.append(" types=").append(types);
        stringBuilder.append(" view=").append(view);
        stringBuilder.append(" resources=");
        if (resources != null) {
            for (int i = 0; i < resources.length; i++) {
                if (i != 0) {
                    stringBuilder.append(",");
                }

                stringBuilder.append(resources[i]);
            }
        }

        return stringBuilder.toString();
    }
}