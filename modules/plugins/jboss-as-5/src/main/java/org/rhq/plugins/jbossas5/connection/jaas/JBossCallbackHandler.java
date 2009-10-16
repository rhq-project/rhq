/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas5.connection.jaas;

import java.io.IOException;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

/**
 * @author Ian Springer
 */
public class JBossCallbackHandler implements CallbackHandler {
    private String username;
    private char[] password;

    public JBossCallbackHandler(String username, String password)
    {
        this.username = username;
        this.password = password.toCharArray();
    }

    public void handle(Callback[] callbacks) throws
            IOException, UnsupportedCallbackException
    {
        for (Callback callback : callbacks)
        {
            //System.out.println("Handling Callback [" + callback + "]...");
            if (callback instanceof NameCallback)
            {

                NameCallback nameCallback = (NameCallback)callback;
                nameCallback.setName(this.username);
            }
            else if (callback instanceof PasswordCallback)
            {
                PasswordCallback passwordCallback = (PasswordCallback)callback;
                passwordCallback.setPassword(this.password);
            }
            else
            {
                throw new UnsupportedCallbackException(callback, "Unrecognized Callback: " + callback);
            }
        }
    }
}
