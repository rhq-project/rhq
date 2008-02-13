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
package org.rhq.enterprise.communications.command.client;

import javax.management.MBeanServer;
import org.testng.annotations.Test;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;

/**
 * Tests low level remoting stuff.
 *
 * @author John Mazzitelli
 */
public class RemotingTest implements ServerInvocationHandler {
    @Test(groups = "comm-client")
    public void testShutdownRestart() throws Throwable {
        if (true) {
            System.out.println("!!!!!!!!!!!!! RUN THIS TEST ONLY WHEN JBREM-745 IS FIXED");
            return;
        }

        String locatorString1 = "socket://127.0.0.1:11111/?force_remote=true";

        Connector c1 = new Connector(locatorString1);
        c1.create();
        c1.addInvocationHandler("test", this);
        c1.start();

        try {
            Client client1 = new Client(new InvokerLocator(locatorString1), "test");
            client1.connect();
            String results = (String) client1.invoke("");
            assert "foo".equals(results) : "Results were " + results;

            c1.stop();
            c1.destroy();
            c1.create();

            c1.addInvocationHandler("test", this);
            c1.start();

            results = (String) client1.invoke("");
            assert "foo".equals(results) : "Results were " + results;
        } finally {
            c1.stop();
            c1.destroy();
        }
    }

    @Test(groups = "comm-client")
    public void testShutdownRestartWithJBossRemotingRemoteCommunicator() throws Throwable {
        String locatorString1 = "socket://127.0.0.1:11112/?force_remote=true";

        Connector c1 = new Connector(locatorString1);
        c1.create();
        c1.addInvocationHandler("test", this);
        c1.start();

        try {
            JBossRemotingRemoteCommunicator client1 = new JBossRemotingRemoteCommunicator(locatorString1);
            client1.connect();
            CommandResponse results = client1.send(new IdentifyCommand());
            assert "foo".equals(results.getResults()) : "Results were " + results;

            c1.stop();
            c1.destroy();

            Thread.sleep(5000); // I have no idea if this will matter, but maybe this avoids the address-already-in-use error we periodically get

            c1.create();

            c1.addInvocationHandler("test", this);
            c1.start();

            results = client1.send(new IdentifyCommand());
            assert "foo".equals(results.getResults()) : "Results were " + results;
        } finally {
            c1.stop();
            c1.destroy();
        }
    }

    // ServerInvocationHandler methods
    public Object invoke(InvocationRequest invocation) {
        if (invocation.getParameter() instanceof IdentifyCommand) {
            return new GenericCommandResponse(null, true, "foo", null);
        }

        return "foo";
    }

    public void addListener(InvokerCallbackHandler callbackHandler) {
    }

    public void removeListener(InvokerCallbackHandler callbackHandler) {
    }

    public void setInvoker(ServerInvoker invoker) {
    }

    public void setMBeanServer(MBeanServer server) {
    }
}