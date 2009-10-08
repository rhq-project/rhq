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
package org.rhq.enterprise.gui.action.license;

import org.apache.struts.upload.FormFile;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * Created by IntelliJ IDEA. User: ghinkle Date: Aug 15, 2005 Time: 2:25:26 PM To change this template use File |
 * Settings | File Templates.
 */
public class LicenseForm extends BaseValidatorForm {
    FormFile licenseFile;

    String portalUsername;
    String portalPassword;

    public FormFile getLicenseFile() {
        return licenseFile;
    }

    public void setLicenseFile(FormFile licenseFile) {
        this.licenseFile = licenseFile;
    }

    public String getPortalUsername() {
        return portalUsername;
    }

    public void setPortalUsername(String portalUsername) {
        this.portalUsername = portalUsername;
    }

    public String getPortalPassword() {
        return portalPassword;
    }

    public void setPortalPassword(String portalPassword) {
        this.portalPassword = portalPassword;
    }
}