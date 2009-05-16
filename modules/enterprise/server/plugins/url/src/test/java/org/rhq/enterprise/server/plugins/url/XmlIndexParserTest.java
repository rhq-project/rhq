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
package org.rhq.enterprise.server.plugins.url;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

@Test
public class XmlIndexParserTest {
    public void testParse() throws Exception {
        final String CONTENT_INDEX_XML = "target/test-classes/content-index.xml";
        XmlIndexParser parser = new XmlIndexParser();
        File indexFile = new File(CONTENT_INDEX_XML);
        Map<String, RemotePackageInfo> results;
        results = parser.jaxbParse(new FileInputStream(indexFile), indexFile.toURI().toURL(), "http://root/url/");
        assert results != null;
        assert results.size() == 2 : "Wrong size=" + results.size();

        System.out.println("TEST RESULTS:\n" + results);

        RemotePackageInfo details = results.get("Mazzlocation");
        assert details != null : "missing details";
        assert details instanceof FullRemotePackageInfo : "Bad class=" + details.getClass();
        FullRemotePackageInfo fullDetails = (FullRemotePackageInfo) details;
        assert "Mazzlocation".equals(fullDetails.getLocation()) : fullDetails.getLocation();
        assert "Mazzmd5".equals(fullDetails.getMD5()) : fullDetails.getMD5();
        assert new URL("http://root/url/Mazzlocation").equals(fullDetails.getUrl()) : fullDetails.getUrl();
        assert "Mazzarchitecture-name".equals(fullDetails.getSupportedPackageType().architectureName) : fullDetails
            .getSupportedPackageType().architectureName;
        assert "Mazzpackage-type-name".equals(fullDetails.getSupportedPackageType().packageTypeName) : fullDetails
            .getSupportedPackageType().packageTypeName;
        assert "Mazzresource-type-name".equals(fullDetails.getSupportedPackageType().resourceTypeName) : fullDetails
            .getSupportedPackageType().resourceTypeName;
        assert "Mazzresource-type-plugin".equals(fullDetails.getSupportedPackageType().resourceTypePluginName) : fullDetails
            .getSupportedPackageType().resourceTypePluginName;
        ContentSourcePackageDetails cspd = fullDetails.getContentSourcePackageDetails();
        assert "Mazzarchitecture-name".equals(cspd.getArchitectureName()) : cspd.getArchitectureName();
        assert "Mazzclassification".equals(cspd.getClassification()) : cspd.getClassification();
        assert "Mazzdisplay-name".equals(cspd.getDisplayName()) : cspd.getDisplayName();
        assert "Mazzdisplay-version".equals(cspd.getDisplayVersion()) : cspd.getDisplayVersion();
        assert "456".equals(cspd.getFileCreatedDate().toString()) : cspd.getFileCreatedDate();
        assert "Mazzfile-name".equals(cspd.getFileName()) : cspd.getFileName();
        assert "123".equals(cspd.getFileSize().toString()) : cspd.getFileSize();
        assert "Mazzlicense-name".equals(cspd.getLicenseName()) : cspd.getLicenseName();
        assert "Mazzlicense-version".equals(cspd.getLicenseVersion()) : cspd.getLicenseVersion();
        assert "Mazzlocation".equals(cspd.getLocation()) : cspd.getLocation();
        assert "Mazzlong-description".equals(cspd.getLongDescription()) : cspd.getLongDescription();
        assert "Mazzmd5".equals(cspd.getMD5()) : cspd.getMD5();
        assert "Mazzmetadata".equals(new String(cspd.getMetadata())) : new String(cspd.getMetadata());
        assert "Mazzname".equals(cspd.getName()) : cspd.getName();
        assert "Mazzpackage-type-name".equals(cspd.getPackageTypeName()) : cspd.getPackageTypeName();
        assert "Mazzsha256".equals(cspd.getSHA256()) : cspd.getSHA256();
        assert "Mazzshort-description".equals(cspd.getShortDescription()) : cspd.getShortDescription();
        assert "Mazzversion".equals(cspd.getVersion()) : cspd.getVersion();
        assert "Mazzresource-version1".equals(cspd.getResourceVersions().toArray()[0]) : cspd.getResourceVersions();
        assert "Mazzresource-version2".equals(cspd.getResourceVersions().toArray()[1]) : cspd.getResourceVersions();

        Configuration extra = cspd.getExtraProperties();
        PropertySimple firstsimple = extra.getSimple("firstsimple");
        PropertySimple secondsimple = extra.getSimple("secondsimple");
        PropertyList firstlist = extra.getList("firstlist");
        PropertyList anotherlist = extra.getList("anotherlist");
        PropertyMap firstmap = extra.getMap("firstmap");
        PropertyList list_o_maps = extra.getList("list-o-maps");
        assert firstsimple != null;
        assert secondsimple != null;
        assert firstlist != null;
        assert anotherlist != null;
        assert firstmap != null;
        assert list_o_maps != null;

        assert "First Simple".equals(firstsimple.getStringValue()) : firstsimple.getStringValue();
        assert "Second Simple".equals(secondsimple.getStringValue()) : secondsimple.getStringValue();

        List<Property> list = firstlist.getList();
        assert 3 == list.size() : list;
        assert "First List #1".equals(((PropertySimple) list.get(0)).getStringValue()) : list;
        assert "First List #2".equals(((PropertySimple) list.get(1)).getStringValue()) : list;
        assert "First List #3".equals(((PropertySimple) list.get(2)).getStringValue()) : list;

        list = anotherlist.getList();
        assert 3 == list.size() : list;
        assert "Another List #1".equals(((PropertySimple) list.get(0)).getStringValue()) : list;
        assert "Another List #2".equals(((PropertySimple) list.get(1)).getStringValue()) : list;
        assert "Another List #3".equals(((PropertySimple) list.get(2)).getStringValue()) : list;

        Map<String, Property> map = firstmap.getMap();
        assert 3 == map.size() : map;
        assert "First Map #1".equals(((PropertySimple) map.get("firstmap1")).getStringValue()) : map;
        assert "First Map #2".equals(((PropertySimple) map.get("firstmap2")).getStringValue()) : map;
        assert "First Map #3".equals(((PropertySimple) map.get("firstmap3")).getStringValue()) : map;

        list = list_o_maps.getList();
        assert 2 == list.size();
        PropertyMap propmap1 = (PropertyMap) list.get(0);
        PropertyMap propmap2 = (PropertyMap) list.get(1);
        assert "map".equals(propmap1.getName());
        assert "map".equals(propmap2.getName());
        Map<String, Property> map1 = propmap1.getMap();
        Map<String, Property> map2 = propmap2.getMap();
        assert 2 == map1.size() : map1;
        assert 2 == map2.size() : map2;
        assert "List-o-Map #1 value 1".equals(((PropertySimple) map1.get("map1value1")).getStringValue()) : map1;
        assert "List-o-Map #1 value 2".equals(((PropertySimple) map1.get("map1value2")).getStringValue()) : map1;
        assert "List-o-Map #2 value 1".equals(((PropertySimple) map2.get("map2value1")).getStringValue()) : map2;
        assert "List-o-Map #2 value 2".equals(((PropertySimple) map2.get("map2value2")).getStringValue()) : map2;

        //// second package

        details = results.get("WOTGORILLAlocation");
        assert details != null : "missing details";
        assert details instanceof FullRemotePackageInfo : "Bad class=" + details.getClass();
        fullDetails = (FullRemotePackageInfo) details;
        assert "WOTGORILLAlocation".equals(fullDetails.getLocation()) : fullDetails.getLocation();
        assert null == fullDetails.getMD5() : fullDetails.getMD5();
        assert new URL("http://root/url/WOTGORILLAlocation").equals(fullDetails.getUrl()) : fullDetails.getUrl();
        assert "WOTGORILLAarchitecture-name".equals(fullDetails.getSupportedPackageType().architectureName) : fullDetails
            .getSupportedPackageType().architectureName;
        assert "WOTGORILLApackage-type-name".equals(fullDetails.getSupportedPackageType().packageTypeName) : fullDetails
            .getSupportedPackageType().packageTypeName;
        assert "WOTGORILLAresource-type-name".equals(fullDetails.getSupportedPackageType().resourceTypeName) : fullDetails
            .getSupportedPackageType().resourceTypeName;
        assert "WOTGORILLAresource-type-plugin".equals(fullDetails.getSupportedPackageType().resourceTypePluginName) : fullDetails
            .getSupportedPackageType().resourceTypePluginName;
        cspd = fullDetails.getContentSourcePackageDetails();
        assert "WOTGORILLAarchitecture-name".equals(cspd.getArchitectureName()) : cspd.getArchitectureName();
        assert null == cspd.getClassification() : cspd.getClassification();
        assert null == cspd.getDisplayName() : cspd.getDisplayName();
        assert null == cspd.getDisplayVersion() : cspd.getDisplayVersion();
        assert null == cspd.getFileCreatedDate() : cspd.getFileCreatedDate();
        assert null == cspd.getFileName() : cspd.getFileName();
        assert null == cspd.getFileSize() : cspd.getFileSize();
        assert null == cspd.getLicenseName() : cspd.getLicenseName();
        assert null == cspd.getLicenseVersion() : cspd.getLicenseVersion();
        assert "WOTGORILLAlocation".equals(cspd.getLocation()) : cspd.getLocation();
        assert null == cspd.getLongDescription() : cspd.getLongDescription();
        assert null == cspd.getMD5() : cspd.getMD5();
        assert null == cspd.getMetadata() : cspd.getMetadata();
        assert "WOTGORILLAname".equals(cspd.getName()) : cspd.getName();
        assert "WOTGORILLApackage-type-name".equals(cspd.getPackageTypeName()) : cspd.getPackageTypeName();
        assert null == cspd.getSHA256() : cspd.getSHA256();
        assert null == cspd.getShortDescription() : cspd.getShortDescription();
        assert "WOTGORILLAversion".equals(cspd.getVersion()) : cspd.getVersion();
        assert 0 == cspd.getResourceVersions().size() : cspd.getResourceVersions();
    }
}
