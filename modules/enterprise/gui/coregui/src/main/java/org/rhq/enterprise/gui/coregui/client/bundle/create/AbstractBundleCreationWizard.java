/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.create;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.gui.coregui.client.bundle.AbstractBundleWizard;

public abstract class AbstractBundleCreationWizard extends AbstractBundleWizard {

    // the things we build up in the wizard
    private BundleType bundleType;
    private String bundleName;
    private String bundleVersionString;
    private String bundleDescription;
    private String recipe;
    private BundleVersion bundleVersion;

    public BundleType getBundleType() {
        return bundleType;
    }

    public void setBundleType(BundleType bundleType) {
        this.bundleType = bundleType;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getBundleVersionString() {
        return bundleVersionString;
    }

    public void setBundleVersionString(String bundleVersionString) {
        this.bundleVersionString = bundleVersionString;
    }

    public String getBundleDescription() {
        return bundleDescription;
    }

    public void setBundleDescription(String desc) {
        this.bundleDescription = desc;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bv) {
        this.bundleVersion = bv;
    }
}
