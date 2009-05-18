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
package org.rhq.plugins.jbossas5.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.deployers.spi.management.NameMatcher;
import org.jboss.managed.api.ManagedComponent;

/**
 * @author Ian Springer
 */
public class RegularExpressionNameMatcher implements NameMatcher<ManagedComponent>
{
    /**
     * Matches a managed component's name against a regular expression.
     *
     * @param component a managed component whose name will be matched against the given regular expression
     * @param regex     a Java regular expression as described by {@link Pattern}
     * @return true if the component's name matches the regular expression, or false if it does not
     */
    public boolean matches(ManagedComponent component, String regex)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(component.getName());
        return matcher.matches();
    }
}
