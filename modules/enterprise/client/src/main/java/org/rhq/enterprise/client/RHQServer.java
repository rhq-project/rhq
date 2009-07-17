package org.rhq.enterprise.client;

public class RHQServer {

    private String host = "localhost";

    private int port = 7080;

    public RHQServer() {
    }

    public RHQServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[host: " + host + ", port: " + port + "]";
    }
}
