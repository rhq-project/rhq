/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.etc.perftestDataGen;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import au.com.bytecode.opencsv.CSVWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DbUtil;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class DataGen {

    private final Log log = LogFactory.getLog(DataGen.class);
    private static final String TARGET = "target/";
    private static final String DOTCSV = ".csv";

    private static final String[][] props = {//
        {"agents","RHQ_Agent","id,name,address,port,agenttoken,remote_endpoint"},
        {"plugins","RHQ_Plugin","id,name,display_name,version,amps_version"},
        {"resourceTypes","RHQ_resource_type","id,name,category,plugin"}, // TODO parent / child types?
        {"resources","RHQ_resource","id,uuid,resource_key,name,resource_type_id,parent_resource_id"} // TODO child resources?
        };

    public static void main(String[] args) {

        DataGen dg = new DataGen();

       dg.run(args);
    }

    private void run(String[] args) {

        if (args.length<3) {
            System.err.println("Usage: DataGen jdbcurl user pass");
            return;
        }


        String url = args[0];
        String user = args[1];
        String pass = args[2];

        try {
            Connection conn = DbUtil.getConnection(url,user,pass);

            for (String[] prop : props) {
                exportTable(conn, prop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportTable(Connection conn, String[] prop) throws Exception {

        String fileName = prop[0];
        String tableName = prop[1];
        String columns = prop[2];

        File agents = new File(TARGET + fileName + DOTCSV);
        System.out.println("Writing file: " + agents.getAbsolutePath());
        CSVWriter writer = new CSVWriter(new FileWriter(agents));
        Statement stm = conn.createStatement();
        String query = "SELECT " + columns + " FROM " + tableName;
        System.out.println("  using query: [" + query + "]");
        System.out.flush();
        ResultSet rs = stm.executeQuery(query);
        writer.writeAll(rs,true);
        rs.close();
        writer.close();
        stm.close();

    }
}
