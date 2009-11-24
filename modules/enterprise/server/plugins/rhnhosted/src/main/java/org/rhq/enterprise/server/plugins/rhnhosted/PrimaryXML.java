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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageObsoletesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageProvidesEntryType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageProvidesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageRequiresEntryType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageRequiresType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;

/**
 * @author John Matthews
 */
public class PrimaryXML {
    /** Example package element from primary.xml used by yum
     *

    <package type="rpm">
      <name>gnome-user-docs</name>
      <arch>noarch</arch>
      <version ver="2.16.0" rel="2.fc6" epoch="0"/>
      <checksum type="md5" pkgid="YES">4e7d379301407b4c79f211596b802a13</checksum>
      <summary>GNOME User Documentation</summary>
      <description>This package contains end user documentation for the GNOME desktop environment.</description>
      <packager/>
      <url/>
      <time file="1157750823" build="1157750823"/>
      <size package="6549456" archive="11858600" installed=""/>
      <location href="getPackage/gnome-user-docs-2.16.0-2.fc6.noarch.rpm"/>
      <format>
          <rpm:license>FDL</rpm:license>
          <rpm:vendor>Red Hat, Inc.</rpm:vendor>
          <rpm:group>Documentation</rpm:group>
          <rpm:buildhost>altix2.build.redhat.com</rpm:buildhost>
          <rpm:sourcerpm>gnome-user-docs-2.16.0-2.fc6.src.rpm</rpm:sourcerpm>
          <rpm:header-range start="440" end="97656"/>
          <rpm:provides>
              <rpm:entry name="gnome-user-docs" flags="EQ" epoch="0" ver="2.16.0" rel="2.fc6"/>
          </rpm:provides>
          <rpm:requires>
              <rpm:entry name="/bin/sh"/>
              <rpm:entry name="/bin/sh"/>
              <rpm:entry name="rpmlib(CompressedFileNames)" flags="LE" epoch="0" ver="3.0.4" rel="1"/>
              <rpm:entry name="rpmlib(PayloadFilesHavePrefix)" flags="LE" epoch="0" ver="4.0" rel="1"/>
              <rpm:entry name="rpmlib(VersionedDependencies)" flags="LE" epoch="0" ver="3.0.3" rel="1"/>
              <rpm:entry name="scrollkeeper" flags="GE" epoch="0" ver="0.3.11"/>
          </rpm:requires>
          <rpm:conflicts/>
          <rpm:obsoletes>
              <rpm:entry name="gnome-users-guide"/>
          </rpm:obsoletes>
      </format>
    </package>
    
       **/

    /** RPM uses bit flags to determine if a package relationships is "Greater, LessThan, Equals";
     * These values can be found from running the python console and executing
     * "import rpm"  "print rpm.RPMSENSE_LESS", "print rpm.RPMSENSE_GREATER", "print rpm.RPMSENSE_EQUAL"
     */
    final static int RPM_SENSE_LESS = 2;
    final static int RPM_SENSE_GREATER = 4;
    final static int RPM_SENSE_EQUAL = 8;

    static private final Log log = LogFactory.getLog(PrimaryXML.class);

    static public String getFlags(String sense) {
        int tmp = Integer.parseInt(sense);
        int flags = tmp & (RPM_SENSE_LESS | RPM_SENSE_GREATER | RPM_SENSE_EQUAL);
        if (flags == (RPM_SENSE_LESS | RPM_SENSE_EQUAL)) {
            return "LE";
        } else if (flags == RPM_SENSE_LESS) {
            return "LT";
        } else if (flags == (RPM_SENSE_GREATER | RPM_SENSE_EQUAL)) {
            return "GE";
        } else if (flags == (RPM_SENSE_GREATER)) {
            return "GT";
        } else if (flags == (RPM_SENSE_EQUAL)) {
            return "EQ";
        }

        log.debug("Unknown rpm sense value of " + sense + " which parsed to an integer of " + tmp);
        return "";
    }

    static public String getEpoch(String version) {
        int end = version.indexOf("-");
        if (end < 0) {
            return "";
        }
        String tmp = version.substring(0, end);
        int index = tmp.indexOf(":");
        if (index < 0) {
            return "";
        }
        String epoch = tmp.substring(0, index);
        return epoch;
    }

    static public String getVersion(String version) {
        int end = version.indexOf("-");
        if (end < 0) {
            return version; //unsure how to parse, return passed in string
        }
        String tmp = version.substring(0, end);
        int start = tmp.indexOf(":");
        if (start < 0) {
            start = 0;
        } else {
            start = start + 1;
        }
        return tmp.substring(start, end);
    }

    static public String getRelease(String version) {
        int start = version.indexOf("-");
        if (start < 0) {
            return "";
        }
        start = start + 1; // go past the "-" character
        return version.substring(start);
    }

    /**
     * Will create a xml string snippet conforming to the <package> entry in a primary.xml file used by yum
     *
     * @param pkg JAXB Object to transform
     * @return string snippet of xml data
     */
    static public String createPackageXML(RhnPackageType pkg) {
        Element top = new Element("package");
        top.setAttribute("type", "rpm");

        Element name = new Element("name");
        name.setText(pkg.getName());
        top.addContent(name);

        Element arch = new Element("arch");
        arch.setText(pkg.getPackageArch());
        top.addContent(arch);

        Element version = new Element("version");
        version.setText(pkg.getVersion());
        version.setAttribute("ver", pkg.getVersion());
        version.setAttribute("rel", pkg.getRelease());
        String epoch = pkg.getEpoch();
        // Note, if epoch is empty we need to change it to a zero as that is what yum expects.
        if (StringUtils.isBlank(epoch)) {
            epoch = "0";
        }
        version.setAttribute("epoch", epoch);
        top.addContent(version);

        Element checksum = new Element("checksum");
        checksum.setAttribute("type", "md5");
        checksum.setAttribute("pkgid", "YES");
        checksum.setText(pkg.getMd5Sum());
        top.addContent(checksum);

        Element summary = new Element("summary");
        summary.setText(pkg.getRhnPackageSummary());
        top.addContent(summary);

        Element description = new Element("description");
        description.setText(pkg.getRhnPackageDescription());
        top.addContent(description);

        Element packager = new Element("packager");
        //TODO: Not sure if we get 'packager' info from RHN.
        packager.setText("");
        top.addContent(packager);

        Element url = new Element("url");
        //TODO: Not sure what to put for url value, don't think it applies to RHN
        url.setText("");
        top.addContent(url);

        Element time = new Element("time");
        //TODO: Verify below, guessing for file/build times.
        time.setAttribute("file", pkg.getLastModified());
        time.setAttribute("build", pkg.getBuildTime());
        top.addContent(time);

        Element size = new Element("size");
        size.setAttribute("package", pkg.getPackageSize());
        size.setAttribute("archive", pkg.getPayloadSize());
        //TODO: Unsure about installed, does this need to change on server side when package is installed on a client?
        size.setAttribute("installed", "");
        top.addContent(size);

        Element location = new Element("location");
        //This value can not be empty and can not contain a "?".
        //It's value is ignored by the RHQ processing for yum.  
        //RHQ will append a series of request parameters to download the file.
        String rpmName = RHNHelper.constructRpmName(pkg.getName(), pkg.getVersion(), pkg.getRelease(), pkg.getEpoch(),
            pkg.getPackageArch());
        location.setAttribute("href", rpmName);
        top.addContent(location);

        Element format = new Element("format");

        Element rpmLicense = new Element("license", "rpm");
        rpmLicense.setText(pkg.getRhnPackageCopyright());
        format.addContent(rpmLicense);

        Element rpmVendor = new Element("vendor", "rpm");
        rpmVendor.setText(pkg.getRhnPackageVendor());
        format.addContent(rpmVendor);

        Element rpmGroup = new Element("group", "rpm");
        rpmGroup.setText(pkg.getPackageGroup());
        format.addContent(rpmGroup);

        Element rpmBuildHost = new Element("buildhost", "rpm");
        rpmBuildHost.setText(pkg.getBuildHost());
        format.addContent(rpmBuildHost);

        Element rpmSourceRPM = new Element("sourcerpm", "rpm");
        rpmSourceRPM.setText(pkg.getSourceRpm());
        format.addContent(rpmSourceRPM);

        Element rpmHeaderRange = new Element("header-range", "rpm");
        rpmHeaderRange.setAttribute("start", pkg.getRhnPackageHeaderStart());
        rpmHeaderRange.setAttribute("end", pkg.getRhnPackageHeaderEnd());
        format.addContent(rpmHeaderRange);

        Element rpmProvides = new Element("provides", "rpm");
        RhnPackageProvidesType provides_type = pkg.getRhnPackageProvides();
        if (provides_type != null) {
            List<RhnPackageProvidesEntryType> provides = provides_type.getRhnPackageProvidesEntry();
            for (RhnPackageProvidesEntryType provEntry : provides) {
                Element entry = new Element("entry", "rpm");
                entry.setAttribute("name", provEntry.getName());
                entry.setAttribute("flags", getFlags(provEntry.getSense()));
                entry.setAttribute("epoch", getEpoch(provEntry.getVersion()));
                entry.setAttribute("ver", getVersion(provEntry.getVersion()));
                entry.setAttribute("rel", getRelease(provEntry.getVersion()));
                rpmProvides.addContent(entry);
            }
        }
        format.addContent(rpmProvides);

        Element rpmRequires = new Element("requires", "rpm");
        RhnPackageRequiresType requires_type = pkg.getRhnPackageRequires();
        if (requires_type != null) {
            List<RhnPackageRequiresEntryType> requires = requires_type.getRhnPackageRequiresEntry();
            for (RhnPackageRequiresEntryType reqEntry : requires) {
                Element entry = new Element("entry", "rpm");
                entry.setAttribute("name", reqEntry.getName());
                entry.setAttribute("flags", getFlags(reqEntry.getSense()));
                entry.setAttribute("epoch", getEpoch(reqEntry.getVersion()));
                entry.setAttribute("ver", getVersion(reqEntry.getVersion()));
                entry.setAttribute("rel", getRelease(reqEntry.getVersion()));
                rpmRequires.addContent(entry);
            }
        }
        format.addContent(rpmRequires);

        Element rpmConflicts = new Element("conflicts", "rpm");
        rpmConflicts.setText(pkg.getRhnPackageConflicts());
        format.addContent(rpmConflicts);

        Element rpmObsoletes = new Element("obsoletes", "rpm");
        RhnPackageObsoletesType obs_type = pkg.getRhnPackageObsoletes();
        if (obs_type != null) {
            List<Serializable> obsoletes = obs_type.getContent();
            for (Serializable obsEntry : obsoletes) {
                Element entry = new Element("entry", "rpm");
                entry.setAttribute("name", obsEntry.toString());
                rpmObsoletes.addContent(entry);
            }
        }
        format.addContent(rpmObsoletes);

        top.addContent(format);
        XMLOutputter xmlOut = new XMLOutputter();
        return xmlOut.outputString(top);
    }
}
