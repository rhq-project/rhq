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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import org.rhq.enterprise.gui.legacy.action.resource.ResourceForm;

/**
 * A subclass of <code>ResourceForm</code> representing the <em>RemoveDefinition</em> form.
 */
public class RemoveDefinitionForm extends ResourceForm {
    /**
     * Holds value of alert definitions.
     */
    private Integer[] definitions;
    private Integer ad;
    private Integer active;
    private String setActiveInactive;
    private String aetid;

    public RemoveDefinitionForm() {
    }

    public String toString() {
        if (definitions == null) {
            return "empty";
        } else {
            return definitions.toString();
        }
    }

    /**
     * Getter for alert definitionss
     *
     * @return alert definitions in an array
     */
    public Integer[] getDefinitions() {
        return this.definitions;
    }

    /**
     * Setter for alert definitions
     *
     * @param alert definitions As an Integer array
     */
    public void setDefinitions(Integer[] definitions) {
        this.definitions = definitions;
    }

    public Integer getAd() {
        return this.ad;
    }

    public void setAd(Integer ad) {
        this.ad = ad;
    }

    public Integer getActive() {
        return this.active;
    }

    public void setActive(Integer active) {
        this.active = active;
    }

    public String getSetActiveInactive() {
        return this.setActiveInactive;
    }

    public void setSetActiveInactive(String setActiveInactive) {
        this.setActiveInactive = setActiveInactive;
    }

    public String getAetid() {
        return aetid;
    }

    public void setAetid(String aetid) {
        this.aetid = aetid;
    }
}