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
package org.rhq.plugins.jbossas5.test.ejb2;

import org.rhq.core.domain.configuration.Configuration;
import org.testng.annotations.Test;

/**
 * @author Lukas Krejci
 */
@Test(groups = "as5-plugin")
public class Ejb2JarResourceTest extends AbstractEjb2ResourceTest {

    protected String getResourceTypeName() {
        return "EJB2 JAR";
    }

    protected Configuration getTestResourceConfiguration() {
        return new Configuration();
    }
}
