package net.hyperic.hq.product.apache;

import net.hyperic.hq.rt.RtConstants;
import net.hyperic.hq.product.logparse.SimpleRtPlugin;

public class ApacheRtPlugin extends SimpleRtPlugin{
    
    public double getTimeMultiplier() {
        return 0.001;
    }

    public boolean supportsEndUser() {
        return true;
    }

    public int getSvcType()
    {
        return RtConstants.WEBSERVER;
    }
}
