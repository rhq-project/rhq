package org.rhq.plugins.modcluster;

import java.util.concurrent.TimeUnit;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

/**
 * 
 * @author Stefan Negrea, Maxime Beck
 */

public class ModClusterServiceComponent extends CatalinaServiceComponent {

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.toLowerCase().startsWith("ping")) {
            name = "ping";
        }

        return super.invokeOperation(name, parameters);
    }

    @Override
    protected Object getPropertyValueAsType(PropertySimple propSimple, String typeName) {
        if (typeName.equals(TimeUnit.class.getName())) {
            return TimeUnit.valueOf(propSimple.getStringValue());
        }

        return super.getPropertyValueAsType(propSimple, typeName);
    }
}
