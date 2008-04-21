/*
 * Taken from original JON code base
 */

package org.rhq.plugins.iis.util;


import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.sigar.win32.MetaBase;

import java.util.HashMap;
import java.util.Map;


public class IISMetaBase {
    private static final String IIS_MKEY = "/LM/W3SVC";
    private static final int MD_SSL_ACCESS_PERM = 6030;
    private static final int MD_ACCESS_SSL = 0x00000008;

    String id;
    String ip;
    String hostname;
    String port;
    String path;
    boolean requireSSL = false;


    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public boolean isRequireSSL() {
        return requireSSL;
    }

    public String toString() {
        String s = "id: " + id + " " + ip + ":" + port;
        if (hostname != null) {
            s += ", Host: " + hostname;
        }
        return s;
    }

    public static Map<String, IISMetaBase> getWebSites() throws Win32Exception {

        String keys[];
        Map<String, IISMetaBase> websites = new HashMap<String, IISMetaBase>();
        MetaBase mb = new MetaBase();

        try {
            mb.OpenSubKey(IIS_MKEY);
            keys = mb.getSubKeyNames();
        } finally {
            mb.close();
        }

        for (String key : keys) {
            int id;
            if (!Character.isDigit(key.charAt(0))) {
                continue;
            }

            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }

            String subkey = IIS_MKEY + "/" + id;
            MetaBase srv = null;
            try {
                srv = new MetaBase();
                srv.OpenSubKey(subkey);

                String[] bindings = null;

                IISMetaBase info = new IISMetaBase();

                //IIS 6.0+Windows 2003 has Administration website
                //that requires SSL by default.
                //Any Web Site can be configured to required ssl.
                try {
                    int flags = srv.getIntValue(MD_SSL_ACCESS_PERM);
                    info.requireSSL = (flags & MD_ACCESS_SSL) != 0;
                    if (info.requireSSL) {
                        bindings =
                                srv.getMultiStringValue(MetaBase.MD_SECURE_BINDINGS);
                    }
                } catch (Win32Exception e) {
                }

                if (bindings == null) {
                    bindings =
                            srv.getMultiStringValue(MetaBase.MD_SERVER_BINDINGS);
                }

                if (bindings.length == 0) {
                    continue;
                }

                String entry = bindings[0];
                int ix = entry.indexOf(":");
                if (ix == -1) {
                    continue;
                }

                info.id = key;
                //binding format:
                //"listen ip:port:host header"
                info.ip = entry.substring(0, ix);

                entry = entry.substring(ix + 1);
                ix = entry.indexOf(":");
                info.port = entry.substring(0, ix);

                //if host header is defined, URLMetric
                //will add Host: header with this value.
                info.hostname = entry.substring(ix + 1);
                if ((info.hostname != null) &&
                        (info.hostname.length() == 0)) {
                    info.hostname = null;
                }

                if ((info.ip == null) ||
                        (info.ip.length() == 0)) {
                    //not bound to a specific ip
                    info.ip = "localhost";
                }

                String name =
                        srv.getStringValue(MetaBase.MD_SERVER_COMMENT);

                websites.put(name, info);

                //XXX this is bogus, else locks the metabase
                //because OpenSubKey does not close the key
                //thats already open.
                srv.close();
                srv = null;
                srv = new MetaBase();
                srv.OpenSubKey(subkey + "/ROOT");
                String docroot = srv.getStringValue(3001);
                info.path = docroot;
            } catch (Win32Exception e) {
            } finally {
                if (srv != null) {
                    srv.close();
                }
            }
        }

        return websites;
    }



    public static void main(String[] args) throws Exception {
        Map websites = IISMetaBase.getWebSites();
        System.out.println(websites);
    }
}
