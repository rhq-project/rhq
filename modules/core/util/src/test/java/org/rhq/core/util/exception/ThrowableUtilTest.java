/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
}