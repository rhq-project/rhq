/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache;

/**
 * 
 *
 * @author Lukas Krejci
 */
public interface PluginLocation {

    public static final String PLATFORM_PLUGIN = "file:target/itest/plugins/rhq-platform-plugin-for-apache-test.jar";
    public static final String AUGEAS_PLUGIN = "file:target/itest/plugins/rhq-augeas-plugin-for-apache-test.jar";
    public static final String APACHE_PLUGIN = "file:target/itest/plugins/rhq-apache-plugin-for-apache-test.jar";

}
