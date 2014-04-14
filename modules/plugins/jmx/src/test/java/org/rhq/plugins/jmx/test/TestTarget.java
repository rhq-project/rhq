/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.plugins.jmx.test;

/**
 * Implementation of our MBean used for testing
 * @author Heiko W. Rupp
 */
public class TestTarget implements TestTargetMBean {

    private int aVariable = 1;

    @Override
    public int getValue() {
        return aVariable;
    }

    @Override
    public void setValue(int val) {
        aVariable = val;
    }

    @Override
    public void doSomething() {
        aVariable++;
    }

    @Override
    public String echo(String in) {
        return in;
    }

    @Override
    public String hello() {
        return "Hello World";
    }

    @Override
    public String concat(String first, String second) {
        return first + second;
    }
}
