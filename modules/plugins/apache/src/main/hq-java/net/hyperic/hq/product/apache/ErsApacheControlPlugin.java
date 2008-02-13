package net.hyperic.hq.product.apache;

public class ErsApacheControlPlugin 
    extends ApacheControlPlugin {

    private static final String DEFAULT_SCRIPT = "bin/apache_startup.sh";
    private static final String DEFAULT_PIDFILE = "logs/httpsd.pid";

    public ErsApacheControlPlugin() {
        super();
        setPidFile(DEFAULT_PIDFILE);
        setControlProgram(DEFAULT_SCRIPT);
    }
}
