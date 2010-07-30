package org.rhq.enterprise.server.plugins.alertSnmp;

import org.rhq.core.domain.configuration.Configuration;

public class SnmpInfo {

    final public String host;
    final public String port;
    final public String oid;

    final public String error;

    private SnmpInfo(String host, String port, String oid) {
        this.host = host;
        this.port = port;
        this.oid = oid;

        String error = null;
        if (oid == null) {
            error = "Missing: OID";
        }
        if (host == null) {
            if (error == null) {
                error = "Missing: host";
            } else {
                error += ", host";
            }
        }
        this.error = error;
    }

    public static SnmpInfo load(Configuration configuration) {
        String host = configuration.getSimpleValue("host", null); // required
        String port = configuration.getSimpleValue("port", "162");
        String oid = configuration.getSimpleValue("oid", null); // required

        return new SnmpInfo(host, port, oid);
    }

    public String toString() {
        String hostString = (host == null ? "UnknownHost" : host);
        String oidString = (oid == null ? "UnknownOID" : oid);
        return hostString + ":" + port + " (" + oidString + ")";
    }
}
