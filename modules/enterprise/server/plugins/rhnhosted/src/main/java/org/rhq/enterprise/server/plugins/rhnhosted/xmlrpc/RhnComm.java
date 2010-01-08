package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnErratumType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;

public class RhnComm {
    private final Log log = LogFactory.getLog(RhnComm.class);
    protected XmlRpcExecutor satHandler;
    protected XmlRpcExecutor dumpHandler;
    protected String serverUrl;
    protected String SAT_HANDLER = "/SAT";
    protected String SATDUMP_HANDLER = "/SAT-DUMP";

    public RhnComm(String serverUrlIn) {
        serverUrl = serverUrlIn;
        satHandler = XmlRpcExecutorFactory.getJaxbClient(serverUrl + SAT_HANDLER);
        dumpHandler = XmlRpcExecutorFactory.getJaxbClient(serverUrl + SATDUMP_HANDLER);
    }

    public boolean checkSystemId(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };

        log.debug("checkSystemId() systemId used is " + systemId);
        Integer result = (Integer) satHandler.execute("authentication.check", params);
        if (result == 1) {
            return true;
        } else {
            return false;
        }
    }

    public List<RhnProductNameType> getProductNames(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };

        log.debug("getProductNames()");
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.product_names", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnProductNameType> names = sat.getRhnProductNames().getRhnProductName();
        return names;
    }

    public List<RhnChannelFamilyType> getChannelFamilies(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };

        log.debug("getChannelFamilies()");
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.channel_families", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelFamilyType> families = sat.getRhnChannelFamilies().getRhnChannelFamily();
        return families;
    }

    public List<RhnChannelType> getChannels(String systemId, List<String> channelLabels) throws IOException,
        XmlRpcException {

        log.debug("getChannels(" + channelLabels + ")");
        Object[] params = new Object[] { systemId, channelLabels };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.channels", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelType> channels = sat.getRhnChannels().getRhnChannel();
        return channels;
    }

    public List<RhnPackageShortType> getPackageShortInfo(String systemId, List<String> pkgIds) throws IOException,
        XmlRpcException {

        log.debug("getPackageShortInfo() passed in package id list has " + pkgIds.size() + " entries");
        Object[] params = new Object[] { systemId, pkgIds };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.packages_short", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageShortType> pkgs = sat.getRhnPackagesShort().getRhnPackageShort();
        return pkgs;
    }

    public List<RhnPackageType> getPackageMetadata(String systemId, List<String> pkgIds) throws IOException,
        XmlRpcException {

        log.debug("getPackageMetadata() passed in package id list has " + pkgIds.size() + " entries");
        Object[] params = new Object[] { systemId, pkgIds };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.packages", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageType> pkgs = sat.getRhnPackages().getRhnPackage();
        return pkgs;
    }

    public List<RhnKickstartableTreeType> getKickstartTreeMetadata(String systemId, List<String> ksLabels)
        throws IOException, XmlRpcException {

        log.debug("getKickstartTreeMetadata(" + ksLabels + ")");
        Object[] params = new Object[] { systemId, ksLabels };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.kickstartable_trees", params);
        RhnSatelliteType sat = result.getValue();
        return sat.getRhnKickstartableTrees().getRhnKickstartableTree();
    }

    public List<RhnErratumType> getErrataMetadata(String systemId, List<String> erratumIds) throws IOException,
        XmlRpcException {

        log.debug("getErratum(" + erratumIds + ")");
        Object[] params = new Object[] { systemId, erratumIds };
        JAXBElement<RhnSatelliteType> result = (JAXBElement) dumpHandler.execute("dump.errata", params);
        RhnSatelliteType sat = result.getValue();
        return sat.getRhnErrata().getRhnErratum();
    }

}
