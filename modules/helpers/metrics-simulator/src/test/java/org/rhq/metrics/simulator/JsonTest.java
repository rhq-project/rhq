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

package org.rhq.metrics.simulator;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.testng.annotations.Test;

/**
 * @author John Sanda
 */
public class JsonTest {

    @Test
    public void mapJson() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("test-simulator.json");
        ObjectMapper mapper = new ObjectMapper();
//        Simulation simulation = mapper.readValue(getClass().getResourceAsStream("test-simulator.json"),
//            Simulation.class);
        Map<String, Object>  simulation = mapper.readValue(inputStream, Map.class);
        assertNotNull(simulation);
    }

}
