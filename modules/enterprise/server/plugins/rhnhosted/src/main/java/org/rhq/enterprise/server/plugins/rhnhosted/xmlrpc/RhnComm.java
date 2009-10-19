package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.FileUtils;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelFamilyType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnChannelType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnKickstartableTreeType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnPackageShortType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSourcePackageType;

public class RhnComm {

    protected String serverUrl = "http://satellite.rhn.redhat.com";
    protected String SAT_HANDLER = "/SAT";
    protected String SATDUMP_HANDLER = "/SAT-DUMP";
    protected String XML_DUMP_VERSION = "3.3";

    public RhnComm() {
    }

    public RhnComm(String serverUrl) {
        setServerURL(serverUrl);
    }

    public void setServerURL(String url) {
        serverUrl = url;
    }

    public String getServerURL() {
        return serverUrl;
    }

    public Map getRequestProperties() {
        Map reqProps = new HashMap();
        reqProps.put("X-RHN-Satellite-XML-Dump-Version", XML_DUMP_VERSION);
        return reqProps;
    }
    
    public boolean checkAuth(String systemId) throws IOException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SAT_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId};
        Integer result = (Integer) client.execute("authentication.check", params);
        if (result.intValue() == 1) {
            return true;
        }
        return false;
    }

    public List<RhnProductNameType> getProductNames(String systemId) throws IOException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SATDUMP_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(false);
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId};
        JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.product_names", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnProductNameType> names = sat.getRhnProductNames().getRhnProductName();
        return names;
    }

    public List<RhnChannelFamilyType> getChannelFamilies(String systemId) throws IOException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SATDUMP_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(false);
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId};
        JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.channel_families", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelFamilyType> families = sat.getRhnChannelFamilies().getRhnChannelFamily();
        return families;
    }
    
    public List<RhnChannelType> getChannels(String systemId, List<String> channelLabels) 
        throws IOException, XmlRpcException {

        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SATDUMP_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(false);
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId, channelLabels};
        JAXBElement<RhnSatelliteType> result =  (JAXBElement) client.execute("dump.channels", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnChannelType> channels = sat.getRhnChannels().getRhnChannel();
        return channels;
    }

    public List<RhnPackageShortType> getPackageShortInfo(String systemId, List<String> pkgIds) 
        throws IOException, XmlRpcException {
        
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SATDUMP_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(false);
        client.setTransportFactory(transportFactory);
        Object[] params = new Object[]{systemId, pkgIds};
        JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages_short", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageShortType> pkgs = sat.getRhnPackagesShort().getRhnPackageShort();
        return pkgs;
    }

    public List<RhnPackageType> getPackageMetadata(String systemId, List<String> pkgIds) 
        throws IOException, XmlRpcException {
        
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SATDUMP_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        RhnJaxbTransportFactory transportFactory = new RhnJaxbTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        transportFactory.setJaxbDomain("org.rhq.enterprise.server.plugins.rhnhosted.xml");
        transportFactory.setDumpMessageToFile(false);
        client.setTransportFactory(transportFactory);
        Object[] params = new Object[]{systemId, pkgIds};
        JAXBElement<RhnSatelliteType> result = (JAXBElement) client.execute("dump.packages", params);
        RhnSatelliteType sat = result.getValue();
        List<RhnPackageType> pkgs = sat.getRhnPackages().getRhnPackage();
        return pkgs;
    }

    /**
     * Expected return header values for: X-Client-Version, X-RHN-Server-Id, X-RHN-Auth
     * X-RHN-Auth-User-Id, X-RHN-Auth-Expire-Offset, X-RHN-Auth-Server-Time
    */
    public Map login(String systemId) throws IOException, XmlRpcException {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(serverUrl + SAT_HANDLER));
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        CustomReqPropTransportFactory transportFactory = new CustomReqPropTransportFactory(client);
        transportFactory.setRequestProperties(getRequestProperties());
        client.setTransportFactory(transportFactory);

        Object[] params = new Object[]{systemId};
        Map result = (Map) client.execute("authentication.login", params);
        return result;
    }

    public boolean getRPM(String systemId, String channelName, String rpmName, String saveFilePath) 
        throws IOException, XmlRpcException {

        String baseUrl = "http://satellite.rhn.redhat.com";
        String extra = "/SAT/$RHN/" + channelName + "/getPackage/" + rpmName;
        URL url = new URL(serverUrl + extra);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        Map props = login(systemId);
        for (Object key: props.keySet()) {
            conn.setRequestProperty((String)key, props.get(key).toString());
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
        }
        finally {
            in.close();
            out.close();
            conn.disconnect();
        }
        return true;
    }


}
