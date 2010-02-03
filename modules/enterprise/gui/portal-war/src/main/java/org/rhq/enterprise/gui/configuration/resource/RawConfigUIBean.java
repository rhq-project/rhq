/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.gui.configuration.resource;

import org.jboss.seam.core.Events;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;

import javax.faces.component.NamingContainer;
import java.io.File;

public class RawConfigUIBean {

    private RawConfiguration originalRawConfiguration;

    private RawConfiguration rawConfiguration;

    public RawConfigUIBean(RawConfiguration rawConfiguration) {
        this.rawConfiguration = rawConfiguration;
        originalRawConfiguration = rawConfiguration.deepCopy(false);
    }

    public void setRawConfiguration(RawConfiguration rawConfiguration) {
        this.rawConfiguration = rawConfiguration;
    }

    public boolean isModified() {
        return !rawConfiguration.getSha256().equals(originalRawConfiguration.getSha256());
    }

    public String getContents() {
        return rawConfiguration.getContents();
    }

    public void setContents(String contents) {
        Configuration configuration = rawConfiguration.getConfiguration();
        configuration.removeRawConfiguration(rawConfiguration);
        rawConfiguration.setContents(contents);
        configuration.addRawConfiguration(rawConfiguration);

        fireRawConfigUpdateEvent();
    }

    public void undoEdit() {
        setContents(originalRawConfiguration.getContents());
    }

    private void fireRawConfigUpdateEvent() {
        Events.instance().raiseEvent("rawConfigUpdate", this);
    }

    /** @return The full path name of the raw config file */
    public String getPath() {
        return rawConfiguration.getPath();
    }

    /** @return The name of the raw config file excluding its path */
    // Note: In the mock-up an asterisk is prepended to the file name in the menu to indicate that it has modifications.
    //       We may want to modify this method to include logic for that as the UI evolves. If so, it might also be
    //       good to change the method name to something a bit more descriptive like getFileNameLabel().
    public String getFileName() {
        File file = new File(rawConfiguration.getPath());
        return file.getName();
    }

    public String getIcon() {
        if (isModified()) {
            return "/images/star_on_24.png";
        }

        return "/images/blank.png";
    }

}
