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

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageObsoletesEntryType;
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

    final static String RHNHOSTED_URI = "http://rhq-project.org/rhnhosted";

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
        return "";
    }

    /**
     * Will return the 'epoch'.  If the epoch is empty, then a "0" is returned
     * to comply with yum's expectations for package metadata
     * @param version  string containing epoch:version-release
     * @return
     */
    static public String getEpoch(String version) {
        int end = version.indexOf("-");
        if (end < 0) {
            return "0";
        }
        String tmp = version.substring(0, end);
        int index = tmp.indexOf(":");
        if (index < 0) {
            return "0";
        }
        String epoch = tmp.substring(0, index);
        if (StringUtils.isBlank(epoch)) {
            return "0";
        }
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
        Namespace packageNS = Namespace.getNamespace(RHNHOSTED_URI);

        Element top = new Element("package", packageNS);
        top.setAttribute("type", "rpm");

        Element name = new Element("name", packageNS);
        name.setText(pkg.getName());
        top.addContent(name);

        Element arch = new Element("arch", packageNS);
        arch.setText(pkg.getPackageArch());
        top.addContent(arch);

        Element version = new Element("version", packageNS);
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

        Element checksum = new Element("checksum", packageNS);
        checksum.setAttribute("type", "md5");
        checksum.setAttribute("pkgid", "YES");
        checksum.setText(pkg.getMd5Sum());
        top.addContent(checksum);

        Element summary = new Element("summary", packageNS);
        summary.setText(pkg.getRhnPackageSummary());
        top.addContent(summary);

        Element description = new Element("description", packageNS);
        description.setText(pkg.getRhnPackageDescription());
        top.addContent(description);

        Element packager = new Element("packager", packageNS);
        //TODO: Not sure if we get 'packager' info from RHN.
        packager.setText("");
        top.addContent(packager);

        Element url = new Element("url", packageNS);
        //TODO: Not sure what to put for url value, don't think it applies to RHN
        url.setText("");
        top.addContent(url);

        Element time = new Element("time", packageNS);
        //TODO: Verify below, guessing for file/build times.
        time.setAttribute("file", pkg.getLastModified());
        time.setAttribute("build", pkg.getBuildTime());
        top.addContent(time);

        Element size = new Element("size", packageNS);
        size.setAttribute("package", pkg.getPackageSize());
        size.setAttribute("archive", pkg.getPayloadSize());
        //TODO: Unsure about installed, does this need to change on server side when package is installed on a client?
        size.setAttribute("installed", "");
        top.addContent(size);

        Element location = new Element("location", packageNS);
        //This value can not be empty and can not contain a "?".
        //It's value is ignored by the RHQ processing for yum.  
        //RHQ will append a series of request parameters to download the file.
        String rpmName = RHNHelper.constructRpmName(pkg.getName(), pkg.getVersion(), pkg.getRelease(), pkg.getEpoch(),
            pkg.getPackageArch());
        location.setAttribute("href", rpmName);
        top.addContent(location);

        Element format = new Element("format", packageNS);

        Namespace rpmNS = Namespace.getNamespace("rpm", "http://rhq-project.org/rhnhosted");
        Element rpmLicense = new Element("license", rpmNS);
        rpmLicense.setText(pkg.getRhnPackageCopyright());
        format.addContent(rpmLicense);

        Element rpmVendor = new Element("vendor", rpmNS);
        rpmVendor.setText(pkg.getRhnPackageVendor());
        format.addContent(rpmVendor);

        Element rpmGroup = new Element("group", rpmNS);
        rpmGroup.setText(pkg.getPackageGroup());
        format.addContent(rpmGroup);

        Element rpmBuildHost = new Element("buildhost", rpmNS);
        rpmBuildHost.setText(pkg.getBuildHost());
        format.addContent(rpmBuildHost);

        Element rpmSourceRPM = new Element("sourcerpm", rpmNS);
        rpmSourceRPM.setText(pkg.getSourceRpm());
        format.addContent(rpmSourceRPM);

        Element rpmHeaderRange = new Element("header-range", rpmNS);
        rpmHeaderRange.setAttribute("start", pkg.getRhnPackageHeaderStart());
        rpmHeaderRange.setAttribute("end", pkg.getRhnPackageHeaderEnd());
        format.addContent(rpmHeaderRange);

        Element rpmProvides = new Element("provides", rpmNS);
        RhnPackageProvidesType provides_type = pkg.getRhnPackageProvides();
        if (provides_type != null) {
            List<RhnPackageProvidesEntryType> provides = provides_type.getRhnPackageProvidesEntry();
            for (RhnPackageProvidesEntryType provEntry : provides) {
                Element entry = new Element("entry", rpmNS);
                entry.setAttribute("name", provEntry.getName());
                String flags = getFlags(provEntry.getSense());
                if (!StringUtils.isBlank(flags)) {
                    entry.setAttribute("flags", flags);
                    String provEpoch = getEpoch(provEntry.getVersion());
                    entry.setAttribute("epoch", provEpoch);
                    String provVer = getVersion(provEntry.getVersion());
                    entry.setAttribute("ver", provVer);
                    String provRel = getRelease(provEntry.getVersion());
                    entry.setAttribute("rel", getRelease(provEntry.getVersion()));
                }
                rpmProvides.addContent(entry);
            }
        }
        format.addContent(rpmProvides);

        Element rpmRequires = new Element("requires", rpmNS);
        RhnPackageRequiresType requires_type = pkg.getRhnPackageRequires();
        if (requires_type != null) {
            List<RhnPackageRequiresEntryType> requires = requires_type.getRhnPackageRequiresEntry();
            for (RhnPackageRequiresEntryType reqEntry : requires) {
                Element entry = new Element("entry", rpmNS);
                entry.setAttribute("name", reqEntry.getName());
                String flags = getFlags(reqEntry.getSense());
                if (!StringUtils.isBlank(flags)) {
                    entry.setAttribute("flags", flags);
                    String reqEpoch = getEpoch(reqEntry.getVersion());
                    entry.setAttribute("epoch", reqEpoch);
                    String reqVer = getVersion(reqEntry.getVersion());
                    entry.setAttribute("ver", reqVer);
                    String reqRel = getRelease(reqEntry.getVersion());
                    entry.setAttribute("rel", getRelease(reqEntry.getVersion()));
                }
                rpmRequires.addContent(entry);
            }
        }
        format.addContent(rpmRequires);

        Element rpmConflicts = new Element("conflicts", rpmNS);
        rpmConflicts.setText(pkg.getRhnPackageConflicts());
        format.addContent(rpmConflicts);

        Element rpmObsoletes = new Element("obsoletes", rpmNS);
        RhnPackageObsoletesType obs_type = pkg.getRhnPackageObsoletes();
        if (obs_type != null) {
            List<Serializable> obsoletes = obs_type.getContent();
            for (Serializable s : obsoletes) {
                RhnPackageObsoletesEntryType obsEntry = null;
                if (s instanceof String) {
                    String obsString = (String) s;
                    if (StringUtils.isBlank(obsString)) {
                        continue;
                    }
                    //log.debug("Adding Obsoletes info <String Class> value = " + obsString);
                    Element entry = new Element("entry", rpmNS);
                    entry.setAttribute("name", obsString);
                    rpmObsoletes.addContent(entry);
                    // skip rest of obs processing for this entry
                    continue;
                }
                //
                // Below obs entry processing is for JAXBElement types only
                //
                if (s instanceof JAXBElement) {
                    JAXBElement je = (JAXBElement) s;
                    //log.debug("Processing obsolete info for JAXBElement of type : " + je.getDeclaredType());
                    obsEntry = (RhnPackageObsoletesEntryType) je.getValue();
                } else if (s instanceof RhnPackageObsoletesEntryType) {
                    obsEntry = (RhnPackageObsoletesEntryType) s;
                } else {
                    log.info("Processing obsoletes info:  unable to determine what class obsoletes entry is: "
                        + "getClass() =  " + s.getClass() + ", toString() = " + s.toString() + ", hashCode = "
                        + s.hashCode());
                    continue;
                }
                Element entry = new Element("entry", rpmNS);
                entry.setAttribute("name", obsEntry.getName());
                String obsVer = obsEntry.getVersion();
                if (!StringUtils.isBlank(obsVer)) {
                    entry.setAttribute("version", obsVer);
                }
                String obsFlags = getFlags(obsEntry.getSense());
                if (!StringUtils.isBlank(obsFlags)) {
                    entry.setAttribute("flags", obsFlags);
                }
                rpmObsoletes.addContent(entry);
            }
        }
        format.addContent(rpmObsoletes);

        top.addContent(format);
        XMLOutputter xmlOut = new XMLOutputter();
        return xmlOut.outputString(top);
    }
}
