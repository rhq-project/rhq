package org.rhq.metrics.simulator;/*
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

import java.io.File;

import org.testng.annotations.Test;

import org.rhq.metrics.simulator.plan.SimulationPlan;
import org.rhq.metrics.simulator.plan.SimulationPlanner;

/**
 * @author John Sanda
 */
public class SimulatorTest {

    @Test
    public void runSimulator() throws Exception {
        File jsonFile = new File(getClass().getResource("test-simulator.json").toURI());
        SimulationPlanner planner = new SimulationPlanner();
        SimulationPlan plan = planner.create(jsonFile);
        Simulator simulator = new Simulator();
        simulator.run(plan);
    }

}
