 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.configuration.test;

import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Jason Dobies
 */
public class ConfigurationValueSetExample {
    // --------------------------------------------------------------------
    //   Test Cases

    @Test
    public void securityDomainExample() {
        Configuration securityDomain = createSecurityDomainValueSet1();
        ConfigurationTest.prettyPrintConfiguration(securityDomain);
    }

    // --------------------------------------------------------------------
    //   Value Set Examples

    private Configuration createSecurityDomainValueSet1() {
        Configuration configuration = new Configuration();

        // ------------------------------------
        // Value Set Initialization

        // Entered by user in value editor screen, contains general notes on the value set changes
        // similar to a commit message for a VCS.
        configuration.setNotes("- Added new login manager");

        // Automatically set by JON server
        configuration.setVersion(2);

        PropertyList propertyList = new PropertyList("applicationPolicies");

        PropertyMap applicationPolicy1 = new PropertyMap("jmx-console");
        propertyList.add(applicationPolicy1);
        // ------------------------------------
        // Application Policy Properties

        /* The following properties are used to describe the data found in the following
         * example security domain:
         *
         * 1. <application-policy name="jmx-console"> 2.   <authentication> 3.     <login-module
         * code="org.jboss.security.auth.spi.UsersRolesLoginModule" flag="required"> 4.       <module-option
         * name="usersProperties">props/jmx-console-users.properties</module-option> 5.       <module-option
         * name="rolesProperties">props/jmx-console-roles.properties</module-option> 6.     </login-module> 7.
         * </authentication> 8. </application-policy>
         */

        /* Refers to line 1 in the example XML above.
         * Note that the name of the property as exposed by the plugin is not necessarily identical to the underlying
         * resource name for the property. Additionally, the display name of the property may further describe the
         * property.
         */
        applicationPolicy1.put(new PropertySimple("applicationPolicyName", "jmx-console"));

        /* Refers to the login-module defined on lines 3-6. */
        PropertyMap loginModule = new PropertyMap("userRolesLoginModule");
        applicationPolicy1.put(loginModule);

        loginModule.put(new PropertySimple("code", "org.jboss.security.auth.spi.UserRolesLoginModule"));
        loginModule.put(new PropertySimple("flag", "required"));

        PropertyMap loginModuleOptions = new PropertyMap("loginModuleOptions");
        loginModuleOptions.put(new PropertySimple("usersProperties", "props/jmx-console-users.properties"));
        loginModuleOptions.put(new PropertySimple("rolesProperties", "props/jmx-console-roles.properties"));

        return configuration;
    }
}