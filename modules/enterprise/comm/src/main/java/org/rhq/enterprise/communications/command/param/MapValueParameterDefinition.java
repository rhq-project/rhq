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
package org.rhq.enterprise.communications.command.param;

import java.util.HashMap;

/**
 * An extension to a parameter definition that marks the parameter as holding a map of values.
 *
 * @author <a href="mailto:ccrouch@jboss.com">Charles Crouch</a>
 */
public class MapValueParameterDefinition extends ParameterDefinition {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link MapValueParameterDefinition} object.
     *
     * @param  name
     * @param  renderingInfo
     *
     * @throws IllegalArgumentException
     */
    public MapValueParameterDefinition(String name, ParameterRenderingInformation renderingInfo)
        throws IllegalArgumentException {
        super(name, HashMap.class.getName(), renderingInfo);
    }
}