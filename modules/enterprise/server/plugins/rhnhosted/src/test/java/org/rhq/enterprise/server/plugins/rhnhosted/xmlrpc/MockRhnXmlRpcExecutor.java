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

package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamiliesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelsType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFileType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartFilesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackagesShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackagesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNamesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;

/**
 * @author mmccune
 *
 */
public class MockRhnXmlRpcExecutor implements XmlRpcExecutor {

    private URL serverUrl;

    /**
     * Constructor
     * @param client to ignore
     */
    public MockRhnXmlRpcExecutor(XmlRpcClient client) {
        XmlRpcClientConfigImpl config = (XmlRpcClientConfigImpl) client.getClientConfig();
        this.serverUrl = config.getServerURL();
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.XmlRpcExecutor#execute(java.lang.String, java.lang.Object[])
     */
    @Override
    public Object execute(String methodName, Object[] params) throws XmlRpcException {
        //for (Object object : params) {
        //    System.out.println("parm: " + object);
        //}

        // Check auth
        String systemid = (String) params[0];
        // Check URL
        URLConnection conn;
        try {
            conn = serverUrl.openConnection();
            conn.connect();
        } catch (Exception e) {
            System.out.println("serverUrl: " + serverUrl);
            throw new XmlRpcException(0, "Failed to read server's response");
        }
        if (!systemid.contains("<name>system_id</name>")) {
            throw new XmlRpcException(-9, "Invalid System Credentials");
        }
        if (methodName.equals("authentication.check")) {
            if (systemid.contains("<value><string>ID-0000000000</string></value>")) {
                throw new XmlRpcException(-9, "Invalid System Credentials");
            }
            return new Integer(1);
        } else if (methodName.equals("authentication.login")) {
            Map retval = new HashMap();
            retval.put("foo", "bar");
            return retval;
        } else if (methodName.equals("dump.packages_short")) {
            JAXBElement element = getRhnSatelliteType();
            RhnSatelliteType retval = (RhnSatelliteType) element.getValue();
            List<String> pids = (List<String>) params[1];
            List<RhnPackageShortType> rhnPackageShort = new LinkedList<RhnPackageShortType>();
            for (String pid : pids) {
                // PackageInfoShort
                RhnPackageShortType pstype = new RhnPackageShortType();
                pstype.setId("1");
                pstype.setName("foo");
                pstype.setEpoch("0");
                pstype.setVersion("1");
                pstype.setRelease("1");
                pstype.setPackageSize("999");
                pstype.setMd5Sum("ABCDE");
                pstype.setLastModified("Wed Oct 21 16:03:26 PDT 2009");
                rhnPackageShort.add(pstype);

                RhnPackagesShortType pshortypes = new RhnPackagesShortType();
                setPrivateList("rhnPackageShort", pshortypes, rhnPackageShort);
                retval.setRhnPackagesShort(pshortypes);

            }
            return element;
        } else if (methodName.equals("dump.kickstartable_trees")) {
            JAXBElement element = getRhnSatelliteType();
            RhnSatelliteType retval = (RhnSatelliteType) element.getValue();
            RhnKickstartableTreesType parentKSTree = new RhnKickstartableTreesType();
            List<RhnKickstartableTreeType> ksTreeList = parentKSTree.getRhnKickstartableTree();
            List<String> kstids = (List<String>) params[1];
            for (String kid : kstids) {
                RhnKickstartableTreeType kstype = new RhnKickstartableTreeType();
                kstype.setBasePath("basepath");
                kstype.setBootImage("boot");
                kstype.setChannel("channel");
                kstype.setInstallTypeLabel("type");
                kstype.setInstallTypeName("name");
                kstype.setKstreeTypeLabel("label");
                kstype.setKstreeTypeName("type");
                kstype.setLabel("label");
                kstype.setLastModified("lastmod");
                // Need to add ks file
                RhnKickstartFilesType parentFiles = new RhnKickstartFilesType();
                List<RhnKickstartFileType> files = parentFiles.getRhnKickstartFile();
                RhnKickstartFileType f = new RhnKickstartFileType();
                f.setFileSize("192");
                f.setLastModified("tbd");
                f.setMd5Sum("tbd");
                f.setRelativePath("path");
                files.add(f);
                kstype.setRhnKickstartFiles(parentFiles);

                ksTreeList.add(kstype);
            }
            retval.setRhnKickstartableTrees(parentKSTree);
            return element;
        } else if (methodName.equals("dump.packages")) {
            if (systemid.contains("<value><string>ID-0000000000</string></value>")) {
                throw new XmlRpcException(-9, "Invalid System Credentials");
            }
            JAXBElement element = getRhnSatelliteType();
            RhnSatelliteType retval = (RhnSatelliteType) element.getValue();
            List<String> pids = (List<String>) params[1];
            List<RhnPackageType> rhnPackage = new LinkedList<RhnPackageType>();
            for (String pid : pids) {
                RhnPackageType ptype = new RhnPackageType();
                ptype.setId(pid);
                ptype.setLastModified("lastmod");
                ptype.setRhnPackageSummary("summary");
                ptype.setRhnPackageDescription("desc");
                rhnPackage.add(ptype);

                RhnPackagesType ptypes = new RhnPackagesType();
                setPrivateList("rhnPackage", ptypes, rhnPackage);
                retval.setRhnPackages(ptypes);
            }
            return element;

        } else {
            return getRhnSatelliteType();
        }

    }

    private JAXBElement getRhnSatelliteType() {
        RhnSatelliteType sattype = new RhnSatelliteType();

        // ProductNames
        RhnProductNameType type = new RhnProductNameType();
        type.setLabel("some-prod-label");
        type.setName("some product name");
        List<RhnProductNameType> rhnProductName = new LinkedList<RhnProductNameType>();
        rhnProductName.add(type);

        RhnProductNamesType prodNamesType = new RhnProductNamesType();
        setPrivateList("rhnProductName", prodNamesType, rhnProductName);
        sattype.setRhnProductNames(prodNamesType);

        // ChannelFamilies
        List<RhnChannelFamilyType> rhnChannelFamily = new LinkedList<RhnChannelFamilyType>();
        RhnChannelFamilyType ctype = new RhnChannelFamilyType();
        ctype.setLabel("channel-fam-label");
        ctype.setMaxMembers("2112");
        ctype.setChannelLabels("foo,bar,baz");
        ctype.setId("1");
        rhnChannelFamily.add(ctype);

        RhnChannelFamiliesType cfamtypes = new RhnChannelFamiliesType();
        setPrivateList("rhnChannelFamily", cfamtypes, rhnChannelFamily);
        sattype.setRhnChannelFamilies(cfamtypes);

        // Channels
        List<RhnChannelType> rhnChannel = new LinkedList<RhnChannelType>();
        RhnChannelType chantype = new RhnChannelType();
        chantype.setLabel("channel-fam-label");
        chantype.setRhnChannelName("channel name");
        chantype.setRhnChannelSummary("channel summary");
        chantype.setPackages("1 2 3 4");
        chantype.setKickstartableTrees("ks-rhel-i386-server-5 ks-rhel-i386-server-5-u1 ks-rhel-i386-server-5-u2");
        rhnChannel.add(chantype);

        RhnChannelsType chantypes = new RhnChannelsType();
        setPrivateList("rhnChannel", chantypes, rhnChannel);
        sattype.setRhnChannels(chantypes);

        JAXBElement<RhnSatelliteType> element = new JAXBElement<RhnSatelliteType>(new QName(""),
            RhnSatelliteType.class, sattype);
        element.setValue(sattype);
        return element;

    }

    private void setPrivateList(String fieldName, Object oIn, List listIn) {
        try {
            Field privateListField = oIn.getClass().getDeclaredField(fieldName);
            privateListField.setAccessible(true);
            privateListField.set(oIn, listIn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Object execute(String pMethodName, List pParams) throws XmlRpcException {
        return execute(pMethodName, pParams.toArray());
    }
}
