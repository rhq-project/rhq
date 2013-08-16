/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.bundle.composite;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleGroup;

/**
 * This composite is used to specify which bundle groups a bundle can be assigned to, for a particular user.
 * 
 * @author Jay Shaughnessy
 */
public class BundleGroupAssignmentComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private Subject subject;
    private Bundle bundle;
    private Map<BundleGroup, Boolean> bundleGroupMap;
    private boolean canBeUnassigned = false;

    public BundleGroupAssignmentComposite() {
        // GWT needs this
    }

    public BundleGroupAssignmentComposite(Subject subject, Bundle bundle) {
        // GWT needs this
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public Map<BundleGroup, Boolean> getBundleGroupMap() {
        if (null == bundleGroupMap) {
            bundleGroupMap = new HashMap<BundleGroup, Boolean>();
        }
        return bundleGroupMap;
    }

    public void setBundleGroupMap(Map<BundleGroup, Boolean> bundleGroupMap) {
        this.bundleGroupMap = bundleGroupMap;
    }

    public boolean isCanBeUnassigned() {
        return canBeUnassigned;
    }

    public void setCanBeUnassigned(boolean canBeUnassigned) {
        this.canBeUnassigned = canBeUnassigned;
    }

    @Override
    public String toString() {
        return "BundleGroupAssignmentComposite [subject=" + subject + ", bundle=" + bundle + ", bundleGroups="
            + bundleGroupMap.keySet() + ", canBeUnassigned=" + canBeUnassigned + "]";
    }

}
