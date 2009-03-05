/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.client;

import mazz.i18n.annotation.I18NMessage;

/**
 * @author Greg Hinkle
 */
public class ClientI18NResourceKeys {

    @I18NMessage("RHQ Client\\n\\\n"
        + "\\n\\\n"
        + "Usage: {0} [options]\\n\\\n"
        + "\\n\\\n"
        + "options:\\n\\\n"
        + "\\   -s, --host=<host>             The host of the server to connect to\\n\\\n"
        + "\\   -t, --port=<port>             The port of the server to connect to\\n\\\n"
        + "\\   -u, --user=<user>             The user to log in as\\n\\\n"
        + "\\   -p, --password=<password>     The password of the user to log in with\\n\\\n"
        + "\\   -f, --file=<script file>      A file containing commands to be run once connected and then disconnected\\n\\\n"
        + "\\   -o, --output=<output file>    A file to output the result of the commands to\\n\\\n"
        + "\\   -c, --command=<command>       A single command to run\\n\\\n"
        + "\\   -h, --help                    displays this message\\n\\\n"
    )
    static String USAGE = "ClientMain.usage";

    
    public static final Object BAD_ARGS = "ClientMain.badArgs";
}
