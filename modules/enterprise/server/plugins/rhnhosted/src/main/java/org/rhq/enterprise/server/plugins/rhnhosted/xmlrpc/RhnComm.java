package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

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

    protected XmlRpcExecutor satHandler;
    protected XmlRpcExecutor dumpHandler;
    protected String serverUrl = "http://satellite.rhn.redhat.com";
    protected String SAT_HANDLER = "/SAT";
    protected String SATDUMP_HANDLER = "/SAT-DUMP";
    protected String XML_DUMP_VERSION = "3.3";

    public RhnComm(String serverUrlIn) {
        satHandler = XmlRpcExecutorFactory.getClient(serverUrl + SAT_HANDLER);
        dumpHandler = XmlRpcExecutorFactory.getClient(serverUrl + SATDUMP_HANDLER);
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

    /**
     * Expected return header values for: X-Client-Version, X-RHN-Server-Id, X-RHN-Auth
     * X-RHN-Auth-User-Id, X-RHN-Auth-Expire-Offset, X-RHN-Auth-Server-Time
    */
    public Map login(String systemId) throws IOException, XmlRpcException {

        Object[] params = new Object[] { systemId };
        Map result = (Map) satHandler.execute("authentication.login", params);
        return result;
    }

    public boolean checkAuth(String systemId) throws IOException, XmlRpcException {
        Object[] params = new Object[] { systemId };
        Integer result = (Integer) satHandler.execute("authentication.check", params);
        if (result.intValue() == 1) {
            return true;
        }
        return false;
    }

    public boolean getRPM(String systemId, String channelName, String rpmName, String saveFilePath) throws IOException,
        XmlRpcException {

        String baseUrl = "http://satellite.rhn.redhat.com";
        String extra = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key : props.keySet()) {
            conn.setRequestProperty((String) key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();
        OutputStream out = new FileOutputStream(saveFilePath);

        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
            conn.disconnect();
        }
        return true;
    }

    public InputStream getKickstartTreeFile(String systemId, String channelName, String ksTreeLabel, String ksFilePath)
        throws IOException, XmlRpcException {

        String baseUrl = "http://satellite.rhn.redhat.com";
        String extra = "/SAT/$RHN/" + channelName + "/getKickstartFile/" + ksTreeLabel + "/" + ksFilePath;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key : props.keySet()) {
            conn.setRequestProperty((String) key, props.get(key).toString());
        }
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getInputStream();
        return in;
    }
}
