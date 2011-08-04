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
package org.rhq.enterprise.server.xmlschema;

import org.rhq.core.clientapi.descriptor.DescriptorPackages;

/**
 * Defines constants related to the server-side XML Schemas, including their JAXB generated package names
 * and their .xsd files, as found in the server's classpath.
 * 
 * See also: http://rhq-project.org/display/RHQ/Design-Server+Side+Plugins#Design-ServerSidePlugins-xmlschemas
 *
 * @author John Mazzitelli
 */
public interface XmlSchemas {
    // NOTE: if you add XSD/PKG constants for any new server plugin type,
    // be sure to add them to the static map constant in ServerPluginDescriptorUtil

    // the abstract plugin descriptor schema that all server plugin types extend when defining their own schemas
    public static final String XSD_SERVERPLUGIN = "rhq-serverplugin.xsd";
    public static final String PKG_SERVERPLUGIN = "org.rhq.enterprise.server.xmlschema.generated.serverplugin";

    // the server plugin descriptor for the generic plugin type
    public static final String XSD_SERVERPLUGIN_GENERIC = "rhq-serverplugin-generic.xsd";
    public static final String PKG_SERVERPLUGIN_GENERIC = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.generic";

    // the server plugin descriptor for the content plugin type
    public static final String XSD_SERVERPLUGIN_CONTENT = "rhq-serverplugin-content.xsd";
    public static final String PKG_SERVERPLUGIN_CONTENT = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.content";

    // the server plugin descriptor for the perspective plugin type
    public static final String XSD_SERVERPLUGIN_PERSPECTIVE = "rhq-serverplugin-perspective.xsd";
    public static final String PKG_SERVERPLUGIN_PERSPECTIVE = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective";

    // the server plugin descriptor for the alert plugin type
    public static final String XSD_SERVERPLUGIN_ALERT = "rhq-serverplugin-alert.xsd";
    public static final String PKG_SERVERPLUGIN_ALERT = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert";

    // the server plugin descriptor for the entitlement plugin type
    public static final String XSD_SERVERPLUGIN_ENTITLEMENT = "rhq-serverplugin-entitlement.xsd";
    public static final String PKG_SERVERPLUGIN_ENTITLEMENT = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.entitlement";

    // the server plugin descriptor for the bundle plugin type
    public static final String XSD_SERVERPLUGIN_BUNDLE = "rhq-serverplugin-bundle.xsd";
    public static final String PKG_SERVERPLUGIN_BUNDLE = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle";

    // the server plugin descriptor for the package type plugin type
    public static final String XSD_SERVERPLUGIN_PACKAGETYPE = "rhq-serverplugin-packagetype.xsd";
    public static final String PKG_SERVERPLUGIN_PACKAGETYPE = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.packagetype";

    // the configuration schema that can be reused in any other server-side schema to define normal configuration properties
    public static final String XSD_CONFIGURATION = "rhq-configuration.xsd";
    public static final String PKG_CONFIGURATION = DescriptorPackages.CONFIGURATION;

    // the schema used to define content source metadata which is used by content plugins like the URL or Disk plugins
    public static final String XSD_CONTENTSOURCE_PACKAGEDETAILS = "rhq-contentsource-packagedetails.xsd";
    public static final String PKG_CONTENTSOURCE_PACKAGEDETAILS = "org.rhq.enterprise.server.xmlschema.generated.contentsource.packagedetails";

    // the schema used to define content source metadata which is used by content plugins like the URL or Disk plugins
    public static final String XSD_SERVERPLUGIN_DRIFT = "rhq-serverplugin-drift.xsd";
    public static final String PKG_SERVERPLUGIN_DRIFT = "org.rhq.enterprise.server.xmlschema.generated.serverplugin.drift";
}