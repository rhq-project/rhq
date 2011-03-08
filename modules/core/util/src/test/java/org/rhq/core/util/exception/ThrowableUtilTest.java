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
package org.rhq.core.util.exception;

import java.sql.SQLException;
import org.testng.annotations.Test;

/**
 * Tests the utilities for working with throwables.
 *
 * @author John Mazzitelli
 */
@Test
public class ThrowableUtilTest {
    /**
     * Tests getting all messages.
     */
    public void testGetAllMessagesArray() {
        assert ThrowableUtil.getAllMessagesArray(null, false).length == 0;
        assert ThrowableUtil.getAllMessagesArray(new Throwable(), false).length == 1;
        assert ThrowableUtil.getAllMessagesArray(new Throwable(), false)[0] == null;
        assert ThrowableUtil.getAllMessagesArray(new Throwable((String) null), false).length == 1;
        assert ThrowableUtil.getAllMessagesArray(new Throwable((String) null), false)[0] == null;
        assert ThrowableUtil.getAllMessagesArray(new Throwable("boo"), false).length == 1;
        assert ThrowableUtil.getAllMessagesArray(new Throwable("boo"), false)[0].equals("boo");

        assert ThrowableUtil.getAllMessages(null, false).equals(">> exception was null <<");
        assert ThrowableUtil.getAllMessages(new Throwable(), false).equals("null");
        assert ThrowableUtil.getAllMessages(new Throwable((String) null), false).equals("null");
        assert ThrowableUtil.getAllMessages(new Throwable("boo"), false).equals("boo");

        Throwable t = new Throwable("one", new Exception("two", new Error("three")));
        assert ThrowableUtil.getAllMessagesArray(t, false).length == 3;
        assert ThrowableUtil.getAllMessagesArray(t, false)[0].equals("one");
        assert ThrowableUtil.getAllMessagesArray(t, false)[1].equals("two");
        assert ThrowableUtil.getAllMessagesArray(t, false)[2].equals("three");
        assert ThrowableUtil.getAllMessages(t, false).equals("one -> two -> three");

        t = new Throwable("one", new Exception(null, new Error("three")));
        assert ThrowableUtil.getAllMessagesArray(t, false).length == 3;
        assert ThrowableUtil.getAllMessagesArray(t, false)[0].equals("one");
        assert ThrowableUtil.getAllMessagesArray(t, false)[1] == null;
        assert ThrowableUtil.getAllMessagesArray(t, false)[2].equals("three");
        assert ThrowableUtil.getAllMessages(t, false).equals("one -> null -> three");
    }

    public void testSqlException() {
        SQLException e1 = new SQLException("one");
        SQLException e2 = new SQLException("two");
        SQLException e3 = new SQLException("three");
        e1.setNextException(e2);
        e2.setNextException(e3);

        String msg = ThrowableUtil.getAllSqlExceptionMessages(e1, true);
        assert msg
            .equals("java.sql.SQLException:one -> java.sql.SQLException:two(error-code=0,sql-state=null) -> java.sql.SQLException:three(error-code=0,sql-state=null)") : "Msg doesn't match: "
            + msg;

        msg = ThrowableUtil.getAllMessages(new Throwable("sql exception wrapper", e1), true);
        assert msg
            .equals("java.lang.Throwable:sql exception wrapper -> java.sql.SQLException:one[SQLException=one -> two(error-code=0,sql-state=null) -> three(error-code=0,sql-state=null)]") : "Msg doesn't match: "
            + msg;
    }
    
    public void testMaxSize() {
        String len10 = "1010101010";
        String len20 = "20202020202020202020";
        String len30 = "303030303030303030303030303030";
        
        Exception e = new Exception(len10, new Exception(len20, new Exception(len30)));
        
        assert ThrowableUtil.getAllMessages(e, false, 10).equals(len30);
        assert ThrowableUtil.getAllMessages(e, false, 35).equals(" ... " + len30);
        assert ThrowableUtil.getAllMessages(e, false, 45).equals(len10 + " ... " + len30);
        assert ThrowableUtil.getAllMessages(e, false, 65).equals(len10 + " ... " + len30);
        assert ThrowableUtil.getAllMessages(e, false, 67).equals(len10 + " ... " + len30);
        assert ThrowableUtil.getAllMessages(e, false, 68).equals(len10 + " -> " + len20 + " -> "+ len30);
    }
}