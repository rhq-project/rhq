package net.hyperic.hq.product.apache;

import java.io.File;

import net.hyperic.hq.product.ConfigFileTrackPlugin;

public class ApacheConfigTrackPlugin
    extends ConfigFileTrackPlugin {

    public String getDefaultConfigFile(String installPath) {
        String conf = getTypeProperty("DEFAULT_CONF");
        return new File(installPath, conf).getAbsolutePath();
    }
}
