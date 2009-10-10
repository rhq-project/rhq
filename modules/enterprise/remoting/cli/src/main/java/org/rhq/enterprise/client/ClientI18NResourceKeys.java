/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.client;

import mazz.i18n.annotation.I18NMessage;
import mazz.i18n.annotation.I18NResourceBundle;

/**
 * @author Greg Hinkle
 */
@I18NResourceBundle(baseName = "client-messages", defaultLocale = "en")
public class ClientI18NResourceKeys {

    @I18NMessage("RHQ Client\\n\\\n"
        + "\\n\\\n"
        + "Usage: rhq-cli [options]\\n\\\n"
        + "\\n\\\n"
        + "options:\\n\\\n"
        + "\\   -s, --host=<host>             The host of the server to connect to\\n\\\n"
        + "\\   -t, --port=<port>             The port of the server to connect to\\n\\\n"
        + "\\   -u, --user=<user>             The user to log in as\\n\\\n"
        + "\\   -p, --password=<password>     The password of the user to log in with\\n\\\n"
        + "\\   -f, --file=<script file>      A file containing commands to be run once connected and then disconnected\\n\\\n"
        + "\\   -o, --output=<output file>    A file to output the result of the commands to\\n\\\n"
        + "\\   -c, --command=<command>       A single command to run\\n\\\n"
        + "\\   -h, --help                    displays this message\\n\\\n")
    public static final String USAGE = "ClientMain.usage";

    @I18NMessage("Bad arguments specified on command line")
    public static final String BAD_ARGS = "ClientMain.badArgs";
}
