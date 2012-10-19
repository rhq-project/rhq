package org.rhq.modules.plugins.jbossas7;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Authenticator to authenticate against as7
 * @author Heiko W. Rupp
 */
public class AS7Authenticator extends Authenticator {

    private String user;
    private String pass;

    public AS7Authenticator(String user, String pass) {
        this.user = user;
        this.pass = pass;
        if (this.pass==null)
            this.pass=""; // prevent NPE later
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user,pass.toCharArray());
    }
}
