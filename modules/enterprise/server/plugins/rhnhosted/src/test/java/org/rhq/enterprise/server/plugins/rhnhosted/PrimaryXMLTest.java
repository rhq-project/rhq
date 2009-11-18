/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.rhnhosted;

import java.util.List;
import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageProvidesEntryType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageProvidesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageRequiresType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageRequiresEntryType;
import org.apache.commons.lang.StringUtils;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;

/**
 * @author John Matthews
 */
public class PrimaryXMLTest extends TestCase
{
    protected RhnPackageType getTestRhnPackage() {
        RhnPackageType pkg = new RhnPackageType();
        pkg.setName("gnome-user-docs");
        pkg.setPackageArch("noarch");
        pkg.setVersion("2.16.0");
        pkg.setRelease("2.6fc6");
        pkg.setEpoch("0");
        pkg.setMd5Sum("4e7d379301407b4c79f211596b802a13");
        pkg.setRhnPackageSummary("GNOME User Documentation");
        pkg.setRhnPackageDescription("This package contains end user documentation for the GNOME desktop environment.");
        pkg.setLastModified("1157750823");
        pkg.setBuildTime("1157750823");
        pkg.setPackageSize("6549456");
        pkg.setPayloadSize("11858600");
        pkg.setRhnPackageCopyright("FDL");
        pkg.setRhnPackageVendor("Red Hat, Inc.");
        pkg.setPackageGroup("Documentation");
        pkg.setBuildHost("altix2.build.redhat.com");
        pkg.setSourceRpm("gnome-user-docs-2.16.0-2.fc6.src.rpm");
        pkg.setRhnPackageHeaderStart("440");
        pkg.setRhnPackageHeaderEnd("97656");


        RhnPackageProvidesType provides_type = new RhnPackageProvidesType();
        List<RhnPackageProvidesEntryType> provides = provides_type.getRhnPackageProvidesEntry();
        RhnPackageProvidesEntryType prov = new RhnPackageProvidesEntryType();
        prov.setName("gnome-user-docs");
        prov.setSense("8");
        prov.setVersion("2.16.0-2.fc6");
        provides.add(prov);
        pkg.setRhnPackageProvides(provides_type);

        RhnPackageRequiresType requires_type = new RhnPackageRequiresType();
        List<RhnPackageRequiresEntryType> requires = requires_type.getRhnPackageRequiresEntry();
        RhnPackageRequiresEntryType req = new RhnPackageRequiresEntryType();
        req.setName("rpmlib(CompressedFileNames)");
        req.setSense("10");
        req.setVersion("2:3.0.4-1");
        requires.add(req);
        pkg.setRhnPackageRequires(requires_type);

        pkg.setRhnPackageConflicts("entry1, entry2");
        return pkg;
    }

    public void testGetFlags()
    {
        String value = PrimaryXML.getFlags("8");
        assert StringUtils.equals(value, "EQ");

        value = PrimaryXML.getFlags("2");
        assert StringUtils.equals(value, "LT");

        value = PrimaryXML.getFlags("4");
        assert StringUtils.equals(value, "GT");

        value = PrimaryXML.getFlags("10");
        assert StringUtils.equals(value, "LE");

        value = PrimaryXML.getFlags("12");
        assert StringUtils.equals(value, "GE");

    }

    public void testGetEpoch()
    {
        String value = PrimaryXML.getEpoch("4:98.9-4");
        assert StringUtils.equals(value, "4");

        value = PrimaryXML.getEpoch("98.9-4");
        assert StringUtils.equals(value, "");

        value = PrimaryXML.getEpoch("");
        assert StringUtils.equals(value, "");

        value = PrimaryXML.getEpoch("98.9-45:3.f");
        assert StringUtils.equals(value, "");
    }


    public void testGetVersion()
    {
        String value = PrimaryXML.getVersion("4:98.9-4");
        assert StringUtils.equals(value, "98.9");

        value = PrimaryXML.getVersion("98.9-4");
        assert StringUtils.equals(value, "98.9");

        value = PrimaryXML.getVersion("");
        assert StringUtils.equals(value, "");

    }

    public void testGetRelease()
    {
        String value = PrimaryXML.getRelease("4:98.9-4");
        assert StringUtils.equals(value, "4");

        value = PrimaryXML.getRelease("98.9-4");
        assert StringUtils.equals(value, "4");

        value = PrimaryXML.getRelease("");
        assert StringUtils.equals(value, "");

        value = PrimaryXML.getRelease("98.9");
        assert StringUtils.equals(value, "");
    }
    
    public void testCreatePackageXML() throws Exception
    {
        RhnPackageType pkg = getTestRhnPackage();
        String xml = PrimaryXML.createPackageXML(pkg);
        System.out.println(xml);

        ByteArrayInputStream inStream = new ByteArrayInputStream(xml.getBytes());
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(inStream);
        Element root = doc.getRootElement();
        assert StringUtils.equals(root.getName(), "package");
    }
}
