/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.remoting.cli;

import org.rhq.enterprise.client.ClientMain;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;
import org.testng.annotations.Optional;

import java.net.URL;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class ScriptTest {

    @Parameters({"script", "args"})
    @Test
    public void testScript(String script, @Optional("") String scriptArgs) throws Exception {
        List<String> args = createRequiredArgs(script);

        if (scriptArgs.length() != 0) {
            args.addAll(getOptioalArgs(scriptArgs));
        }

        ClientMain.main(args.toArray(new String[] {}));
    }

    private List<String> createRequiredArgs(String script) {
        List<String> args = new ArrayList<String>();
        args.add("exec");
        args.add("-f");
        args.add(script);
        args.add("--args-style=named");
        args.add("rhqServerName=" + System.getProperty("rhq.server.name", "localhost"));

        return args;
    }

    private List<String> getOptioalArgs(String scriptArgs) {
        return Arrays.asList(scriptArgs.split("\\s"));
    }

}
