/*
  * RHQ Management Platform
  * Copyright (C) 2005-2014 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */


package org.rhq.core.domain.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CannedGroupExpression implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String plugin;
    private String name;
    private List<String> expression;
    private boolean createByDefault;
    private boolean recursive;
    private int recalcInMinutes;
    private String description;
    
    public CannedGroupExpression() {

    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getExpression() {
        if (expression == null) {
            expression = new ArrayList<String>();
        }
        return expression;
    }

    public void setExpression(List<String> expression) {
        this.expression = expression;
    }

    public boolean isCreateByDefault() {
        return createByDefault;
    }

    public void setCreateByDefault(boolean createByDefault) {
        this.createByDefault = createByDefault;
    }

    public int getRecalcInMinutes() {
        return recalcInMinutes;
    }

    public void setRecalcInMinutes(int recalcInMinutes) {
        this.recalcInMinutes = recalcInMinutes;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getGroupDefinitionReferenceKey() {
        return getPlugin()+":"+getId();
    }

    @Override
    public String toString() {
        return new StringBuilder("CannedGroupExpression [").append("id=" + getId()).append(", name=" + getName())
            .append(", expr=" + Arrays.toString(getExpression().toArray())).append("]").toString();
    }

}
