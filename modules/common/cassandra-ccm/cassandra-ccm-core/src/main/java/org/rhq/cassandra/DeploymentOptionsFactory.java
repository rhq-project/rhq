/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra;

/**
 * Creates {@link DeploymentOptions} objects.
 *
 * @author John Sanda
 */
public class DeploymentOptionsFactory {

    /**
     * @return A new {@link DeploymentOptions}. This method checks the platform arch
     * (32 bit vs 64 bit) and the JRE being used to determine if any particular defaults
     * need to be set.
     */
    public DeploymentOptions newDeploymentOptions() {
        // Make sure we have a high enough stack size. See https://bugzilla.redhat.com/show_bug.cgi?id=1008090
        System.setProperty("rhq.cassandra.stack.size", "256k");
        return new DeploymentOptions();
    }

}
