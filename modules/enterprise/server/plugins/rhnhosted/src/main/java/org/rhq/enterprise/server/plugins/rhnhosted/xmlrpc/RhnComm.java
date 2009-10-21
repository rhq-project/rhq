package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;

public class RhnComm {

    protected XmlRpcExecutor dumpHandler;
    protected String serverUrl = "http://satellite.rhn.redhat.com";
    protected String SAT_HANDLER = "/SAT";
    protected String SATDUMP_HANDLER = "/SAT-DUMP";
    protected String XML_DUMP_VERSION = "3.3";

    public RhnComm(String serverUrlIn) {
        dumpHandler = XmlRpcExecutorFactory.getJaxbClient(serverUrl + SATDUMP_HANDLER);
        serverUrl = serverUrlIn;
    }

    public List<RhnProductNameType> getProductNames(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.product_names", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnProductNameType> names = sat.getRhnProductNames().getRhnProductName();
        return names;
    }

    public List<RhnChannelFamilyType> getChannelFamilies(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.channel_families", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelFamilyType> families = sat.getRhnChannelFamilies().getRhnChannelFamily();
        return families;
    }

    public List<RhnChannelType> getChannels(String systemId, List<String> channelLabels) throws IOException,
        XmlRpcException {

        Object[] params = new Object[] { systemId, channelLabels };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.channels", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelType> channels = sat.getRhnChannels().getRhnChannel();
        return channels;
    }

    public List<RhnPackageShortType> getPackageShortInfo(String systemId, List<String> pkgIds) throws IOException,
        XmlRpcException {

        Object[] params = new Object[] { systemId, pkgIds };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.packages_short", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageShortType> pkgs = sat.getRhnPackagesShort().getRhnPackageShort();
        return pkgs;
    }

    public List<RhnPackageType> getPackageMetadata(String systemId, List<String> pkgIds) throws IOException,
        XmlRpcException {

        Object[] params = new Object[] { systemId, pkgIds };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.packages", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageType> pkgs = sat.getRhnPackages().getRhnPackage();
        return pkgs;
    }

    public List<RhnKickstartableTreeType> getKickstartTreeMetadata(String systemId, List<String> ksLabels)
        throws IOException, XmlRpcException {

        Object[] params = new Object[] { systemId, ksLabels };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.kickstartable_trees", params);
        RhnSatelliteType sat = result.getValue();
        return sat.getRhnKickstartableTrees().getRhnKickstartableTree();
    }

}
