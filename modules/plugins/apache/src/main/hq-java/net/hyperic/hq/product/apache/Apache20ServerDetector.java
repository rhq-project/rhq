package net.hyperic.hq.product.apache;

import org.jboss.on.plugins.apache.util.ApacheBinaryInfo;

import java.io.File;

import java.util.List;

import net.hyperic.hq.product.PluginException;
import net.hyperic.hq.product.RegistryServerDetector;

import net.hyperic.sigar.win32.RegistryKey;
import net.hyperic.util.config.ConfigResponse;

public class Apache20ServerDetector
    extends ApacheServerDetector
    implements RegistryServerDetector {

    /**
     * The List returned from this method will either be null
     * (if no servers were found), or it will contain a single
     * AIServerValue (if a server was found).  Currently the 
     * DotOrgDetector does not support detecting multiple instances 
     * of Apache in a single directory.
     */
    public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) 
        throws PluginException {

        if (path.endsWith(File.separator)) {
            path = path.substring(0, path.length()-1);
        }

        String binary = path + "/bin/Apache.exe";
        return getServerList(path, current.getSubKeyName(),
                             ApacheBinaryInfo.getInfo(binary));
    }
}
