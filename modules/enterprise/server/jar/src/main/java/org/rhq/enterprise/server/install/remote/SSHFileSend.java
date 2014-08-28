/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.install.remote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author Greg Hinkle
 */
public class SSHFileSend {

    public static boolean sendFile(Session session, String sourceFilename, String destFilename) throws IOException, JSchException {

        String command = "scp -p -t '" + destFilename + "'";
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            return false;
        }

        //send "C0644 filesize filename" where filename doesn't contain a /
        long filesize = (new File(sourceFilename)).length();
        command = "C0644 " + filesize + " ";
        if (sourceFilename.lastIndexOf('/') > 0) {
            command += sourceFilename.substring(sourceFilename.lastIndexOf('/') + 1);
        } else if (sourceFilename.lastIndexOf('\\') > 0) {
            command += sourceFilename.substring(sourceFilename.lastIndexOf('\\') + 1);
        } else {
            command += sourceFilename;
        }
        command += "\n";

        out.write(command.getBytes());
        out.flush();

        if (checkAck(in) != 0) {
            throw new IOException("Error while trying to write " + destFilename + " , " + getReason(in));
        }

        //send the contents of the source file
        FileInputStream fis = new FileInputStream(sourceFilename);
        byte[] buf = new byte[1024];
        while (true) {
            int len = fis.read(buf, 0, buf.length);

            if (len <= 0) {
                break;
            }

            out.write(buf, 0, len);
        }

        fis.close();

        //send '\0' to end it
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        if (checkAck(in) != 0) {
            throw new IOException("Error while trying to write " + destFilename + " , " + getReason(in));
        }

        out.close();

        channel.disconnect();

        return true;
    }

    public static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        return b;
    }

    public static String getReason(InputStream in) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        do {
            c = in.read();
            sb.append((char) c);
        } while (c != '\n');
        return sb.toString();
    }
}
