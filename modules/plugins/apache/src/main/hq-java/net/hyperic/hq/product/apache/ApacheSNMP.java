package net.hyperic.hq.product.apache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import net.hyperic.util.config.ConfigResponse;

import net.hyperic.snmp.SNMPClient;
import net.hyperic.snmp.SNMPException;
import net.hyperic.snmp.SNMPSession;
import net.hyperic.snmp.SNMPValue;

public class ApacheSNMP {

    static final String COLUMN_VHOST_NAME = "wwwServiceName";
    static final String COLUMN_VHOST_PORT = "wwwServiceProtocol";
    static final String COLUMN_VHOST_DESC = "wwwServiceDescription";
    static final String COLUMN_VHOST_ADM  = "wwwServiceContact";
    static final String TCP_PROTO_ID      = "1.3.6.1.2.1.6.";
    private static final int TCP_PROTO_ID_LEN = TCP_PROTO_ID.length();
    private static HashMap configCache = null;

    private SNMPClient client = new SNMPClient();

    public class Server {
        String name;
        String port;
        String description;
        String version;
        String admin;

        public String toString() {
            return this.name + ":" + this.port;
        }
    }

    static class ConfigFile {
        long lastModified;
        String port;
        String listen = "127.0.0.1";

        public String toString() {
            return this.listen + ":" + this.port;
        }
    }

    public List getServers(Properties config) throws SNMPException {
        return getServers(new ConfigResponse(config));
    }

    /**
     * Find configured virtual servers using SNMP.
     */
    public List getServers(ConfigResponse config) throws SNMPException {
        SNMPSession session;
        List names, ports, admins;
        List servers = new ArrayList();
        String description = null, version = null;

        try {
            session = client.getSession(config);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP session: " 
                                    + e.getMessage(), e);
        }
        try {
            names = session.getBulk(COLUMN_VHOST_NAME);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_NAME +
                                    ": " + e.getMessage(), e);
        }

        try {
            ports = session.getBulk(COLUMN_VHOST_PORT);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_PORT +
                                    ": " + e.getMessage(), e);
        }
        
        try {
            admins = session.getBulk(COLUMN_VHOST_ADM);
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP column: " 
                                    + COLUMN_VHOST_ADM +
                                    ": " + e.getMessage(), e);
        }
        
        try {
            //just get the first, they are all the same.
            SNMPValue desc = session.getNextValue(COLUMN_VHOST_DESC);
            if (desc != null) {
                description = desc.toString();
                StringTokenizer tok = new StringTokenizer(description);
                final String ap = "Apache/";
                while (tok.hasMoreTokens()) {
                    String component = tok.nextToken();
                    if (component.startsWith(ap)) {
                        version = component.substring(ap.length());
                        break;
                    }
                }
            }
        } catch (SNMPException e) {
            throw new SNMPException("Error getting SNMP value: " 
                                    + COLUMN_VHOST_DESC +
                                    ": " + e.getMessage(), e);
        }

        for (int i=0; i<names.size(); i++) {
            Server server = new Server();

            server.port =
                ports.get(i).toString().substring(TCP_PROTO_ID_LEN);

            server.name = names.get(i).toString();

            server.admin = admins.get(i).toString();

            server.description = description;
            server.version = version;

            servers.add(server);
        }

        return servers;
    }

    static ConfigFile getConfig(String file) throws IOException {
        if (configCache == null) {
            configCache = new HashMap();
        }

        ConfigFile config = (ConfigFile)configCache.get(file);

        long lastModified = new File(file).lastModified();

        if ((config == null) || (lastModified != config.lastModified)) {
            config = new ConfigFile();
            config.lastModified = lastModified;
            parse(file, config);
            configCache.put(file, config);
        }

        return config;
    }

    private static void parse(String file, ConfigFile config)
        throws IOException {

        String line, port=null;
        BufferedReader reader =
            new BufferedReader(new FileReader(file));

        final String portToken = "agentaddress";
        final String listenToken = "com2sec";

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith(portToken)) {
                config.port = line.substring(portToken.length()).trim();
            }
            else if (line.startsWith(listenToken)) {
                StringTokenizer tok = new StringTokenizer(line.trim());
                if (tok.countTokens() == 4) {
                    tok.nextToken(); //com2sec
                    if (!"local".equals(tok.nextToken())) {
                        continue;
                    }
                    config.listen = tok.nextToken();
                    if ("localhost".equals(config.listen)) {
                        config.listen = SNMPClient.DEFAULT_IP;
                    }
                }
            }
        }

        reader.close();
    }

    /**
     * Get the default SNMP configuration properties.
     */
    public static Properties getConfigProperties() {
        return getConfigProperties(null);
    }

    /**
     * Get the default SNMP configuration properties overriding the port.
     */
    public static Properties getConfigProperties(ConfigFile config) {
        Properties props = new Properties();

        if (config != null) {
            props.setProperty(SNMPClient.PROP_PORT, config.port);
            props.setProperty(SNMPClient.PROP_IP, config.listen);
        }

        return props;
    }
}
