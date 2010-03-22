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

import java.io.File;

import org.jboss.seam.core.Events;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.util.MessageDigestGenerator;

public class RawConfigUIBean {

    private RawConfiguration originalRawConfiguration;

    private RawConfiguration rawConfiguration;

    private String errorMessage;

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
        String sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(contents);
        rawConfiguration.setContents(contents, sha256);
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
    public String getFileName() {
        File file = new File(rawConfiguration.getPath());
        return file.getName();
    }

    public String getErrorLabel() {
        if (errorMessage == null) {
            return " ";
        }

        return "View Errors";
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String msg) {
        errorMessage = msg;
    }

    /**
     * @return The name of the raw config file excluding the path. If the file has been modified, an asterisk is
     * prepended to the name.
     */
    public String getFileDisplayName() {
        if (isModified()) {
            return "* " + getFileName();
        }

        return getFileName();
    }

    public String getIcon() {
        if (isModified()) {
            return "/images/star_on_24.png";
        }

        return "/images/blank.png";
    }

}
