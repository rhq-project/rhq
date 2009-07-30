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

package org.rhq.enterprise.client.commands;

import org.rhq.enterprise.client.Controller;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.NoOpRecorder;
import org.rhq.enterprise.client.Recorder;
import org.rhq.enterprise.client.script.CommandLineParseException;

import java.io.IOException;
import java.io.File;

public class RecordCommand implements ClientCommand {

    private boolean recording;

    public void setController(Controller controller) {
    }

    public String getPromptCommandString() {
        return "record";
    }

    public boolean execute(ClientMain client, String[] args) {
        if (args.length < 2) {
            throw new CommandLineParseException("Parse error occurred");
        }

        File file = new File(args[1]);

        if (recording) {
            stopRecording(client);
        }
        else {
            startRecording(client, file);
        }

        return true;
    }

    private void stopRecording(ClientMain client) {
        try {
            recording = false;
            
           client.getRecorder().stop();
           client.setRecorder(new NoOpRecorder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecording(ClientMain client, File file) {
       try {
           recording = true;

           Recorder recorder = new Recorder();
           recorder.setFile(file);

           client.setRecorder(recorder);
       } catch (IOException e) {
           e.printStackTrace();
       }
    }

    public String getSyntax() {
        return "record <file>";
    }

    public String getHelp() {
        return "Records user input commands to a specified file";
    }

    public String getDetailedHelp() {
        return getHelp();
    }
}
