/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.helpers.rtfilter.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Definition of the rhq-rtfilter-subsystem resource.
 *
 * @author Ian Springer
 */
public class RtFilterSubsystemDefinition extends SimpleResourceDefinition {

    public static final RtFilterSubsystemDefinition INSTANCE = new RtFilterSubsystemDefinition();

    private RtFilterSubsystemDefinition() {
        super(RtFilterExtension.SUBSYSTEM_PATH,
                RtFilterExtension.getResourceDescriptionResolver(null),
                RtFilterSubsystemAdd.INSTANCE,
                RtFilterSubsystemRemove.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

}
