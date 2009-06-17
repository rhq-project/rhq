/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.util.Set;

import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ComponentType;

import org.rhq.plugins.jbossas5.util.Ejb2BeanUtils;

/**
 * A plugin component for managing an EJB 1/2 bean.
 *
 * @author Lukas Krejci
 */
public class Ejb2BeanComponent extends AbstractEjbBeanComponent {
    private static final ComponentType MDB_COMPONENT_TYPE = new ComponentType("EJB", "MDB");

    @Override
    protected ManagedComponent getManagedComponent() {
        if (MDB_COMPONENT_TYPE.equals(getComponentType())) {
            try {
                Set<ManagedComponent> mdbs = getConnection().getManagementView().getComponentsForType(MDB_COMPONENT_TYPE);

                for (ManagedComponent mdb : mdbs) {
                    if (getComponentName().equals(Ejb2BeanUtils.getUniqueBeanIdentificator(mdb))) {
                        return mdb;
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            return super.getManagedComponent();
        }

        return null;
    }
}
