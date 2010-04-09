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
package org.rhq.core.domain.bundle;

/**
 * Bundle Deployment Actions that can be audited.  The same action can be used multiple times in a single
 * deployment audit although the status may change   Typically with different status' to  along with the expected status messages of each.
 * 
 * @author Jay Shaughnessy
 */
public enum BundleDeploymentAction {

    DEPLOYMENT, // The actual deployment of the bundle: IN_PROGRESS | FAILURE | SUCCESS 
    DEPLOYMENT_REQUESTED, // request processed: FAILURE | SUCCESS  
    DEPLOYMENT_SCHEDULED, // request schedules: FAILURE | SUCCESS
    DEPLOYMENT_STEP, // A supplemental message at any point in the process: NO_CHANGE    
    FILE_DOWNLOAD, // The file download preceding actualy deployment: IN_PROGRESS | FAILURE | SUCCESS 
    POLICY_CHECK_FAIL
}
