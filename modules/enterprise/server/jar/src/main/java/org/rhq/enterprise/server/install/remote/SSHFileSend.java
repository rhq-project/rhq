/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import com.jcraft.jsch.Session;

/**
 * @author Greg Hinkle
 */
public class SSHFileSend {


    public static boolean sendFile(Session session, String sourceFilename, String destFilename) {
        FileInputStream fis = null;

        try {

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
                return false;
            }

            //send the contents of the source file
            fis = new FileInputStream(sourceFilename);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);

                if (len <= 0) {
                    break;
                }

                out.write(buf, 0, len);
            }

            fis.close();
            fis = null;

            //send '\0' to end it
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            if (checkAck(in) != 0) {
                return false;
            }

            out.close();

            channel.disconnect();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }

        return false;
    }


    public static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }


}
