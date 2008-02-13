package net.hyperic.hq.product.apache;

import net.hyperic.hq.product.ServerControlPlugin;
import net.hyperic.hq.product.PluginException;

import net.hyperic.util.config.ConfigResponse;

public class ApacheControlPlugin 
    extends ServerControlPlugin {
    
    static final String DEFAULT_SCRIPT = "bin/apachectl";
    static final String DEFAULT_PIDFILE = "logs/httpd.pid";

    public ApacheControlPlugin() {
        super();
        setPidFile(DEFAULT_PIDFILE);
        setControlProgram(DEFAULT_SCRIPT);
    }

    public boolean useSigar() {
        return true;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);
        validateControlProgram(getTypeInfo().getName());
    }

    // Define control methods

    public void start()
    {
        int res = doCommand("start");

        handleResult(res, STATE_STARTED);
    }

    // XXX: should we handle encrypted keys?
    public void startssl()
    {
        int res = doCommand("startssl");

        handleResult(res, STATE_STARTED);
    }

    public void stop()
    {
        int res = doCommand("stop");

        handleResult(res, STATE_STOPPED);
    }

    public void restart()
    {
        int res = this.doCommand("restart");

        handleResult(res, STATE_STARTED);
    }

    public void graceful()
    {
        int res = doCommand("graceful");
        
        handleResult(res, STATE_STARTED);
    }

    public void configtest()
    {
        // state does not change during configtest
        
        int res = doCommand("configtest");

        setResult(res);
        if (res != RESULT_SUCCESS)
            setErrorStr(this.stdErr.toString());
    }
}
