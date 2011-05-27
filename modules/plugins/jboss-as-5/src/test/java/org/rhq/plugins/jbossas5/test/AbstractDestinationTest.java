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
package org.rhq.plugins.jbossas5.test;

import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.operation.EmsOperation;
import org.mc4j.ems.connection.bean.parameter.EmsParameter;

import org.rhq.core.clientapi.agent.inventory.CreateResourceRequest;
import org.rhq.core.clientapi.agent.inventory.CreateResourceResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceFactoryManager;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.plugins.jbossas5.ApplicationServerComponent;
import org.rhq.plugins.jbossas5.test.util.AppServerUtils;

/**
 * @author Noam Malki
 */
public abstract class AbstractDestinationTest extends AbstractResourceTest {
    protected static final Integer NUM_OF_MESSAGES = 10;

    protected abstract String getDestinationName();

    protected abstract String getDestinationJndi();

    private Boolean successReceiveMessage;
    private String receiveErrorMessage;

    public void initDestination() throws Exception {

        class SendMessages extends Thread {
            public void run() {
                try {
                    String destinationName = getDestinationJndi();
                    InitialContext ctx = AppServerUtils.getAppServerInitialContext();

                    QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("ConnectionFactory");
                    Destination destination = (Destination) ctx.lookup(destinationName);

                    Connection connection = factory.createQueueConnection();
                    connection.start();

                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                    MessageConsumer consumer = session.createConsumer(destination);

                    TextMessage message = (TextMessage) consumer.receive();

                    successReceiveMessage = true;

                    System.out.println("Received message:  " + message.getText());
                } catch (Exception e) {
                    successReceiveMessage = false;
                    receiveErrorMessage = e.getMessage();
                }
            }
        }

        System.out.println("Creating Destination - " + getDestinationName());
        ResourceFactoryManager resourceFactoryManager = PluginContainer.getInstance().getResourceFactoryManager();
        CreateResourceRequest request = new CreateResourceRequest(0, AppServerUtils.getASResource().getId(),
            getDestinationName(), getResourceTypeName(), getPluginName(), new Configuration(),
            getTestResourceConfiguration(), null);
        //resourceFactoryManager.createResource(request);
        CreateResourceResponse response = resourceFactoryManager.executeCreateResourceImmediately(request);
        response.getErrorMessage();
        System.out.println("creating response message: " + response.getErrorMessage());

        // Enable Destination Statistics using JMX 
        System.out.println("Enable Destination Statistics..");
        Resource asResource = AppServerUtils.getASResource();
        ApplicationServerComponent asResourceComponenet = (ApplicationServerComponent) PluginContainer.getInstance()
            .getInventoryManager().getResourceComponent(asResource);

        EmsConnection emsConnection = asResourceComponenet.getEmsConnection();
        if (emsConnection == null) {
            throw new RuntimeException("Can not connect to the server");
        }
        EmsBean bean = emsConnection.getBean("jboss.messaging:service=ServerPeer");

        EmsOperation operation = bean.getOperation("enableMessageCounters");
        List<EmsParameter> params = operation.getParameters();
        int count = params.size();
        if (count == 0)
            operation.invoke(new Object[0]);
        else { // overloaded operation
            operation.invoke(new Object[] { 0 }); // return code of 0
        }

        //reset counters
        operation = bean.getOperation("resetAllMessageCounters");
        params = operation.getParameters();
        count = params.size();
        if (count == 0)
            operation.invoke(new Object[0]);
        else { // overloaded operation
            operation.invoke(new Object[] { 0 }); // return code of 0
        }

        operation = bean.getOperation("resetAllMessageCounterHistories");
        params = operation.getParameters();
        count = params.size();
        if (count == 0)
            operation.invoke(new Object[0]);
        else { // overloaded operation
            operation.invoke(new Object[] { 0 }); // return code of 0
        }
        Thread.sleep(5000);
        ///////

        //remove messages from the destination
        PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
        Thread.sleep(4000);

        Set<Resource> resources = getResources();
        Boolean foundDestination = false;
        for (Resource resource : resources) {
            if (resource.getName().equals(getDestinationName())) {
                foundDestination = true;
                OperationFacet operationFacet = ComponentUtil.getComponent(resource.getId(), OperationFacet.class,
                    FacetLockType.WRITE, 3000, true, true);
                String name = "removeAllMessages";
                operationFacet.invokeOperation(name, getTestOperationParameters(name));
            }
        }
        if (!foundDestination)
            throw new Exception("cannot find " + getDestinationName());
        Thread.sleep(4000);

        //receive a message from the destination
        successReceiveMessage = false;
        (new SendMessages()).start();
        Thread.sleep(1000);

        //send messages to the Destination
        sendMessagesToDestination(getDestinationJndi(), NUM_OF_MESSAGES);

        Thread.sleep(1000);
        if (!successReceiveMessage) {
            System.out.println(receiveErrorMessage);
            throw new Exception("Receiving message was unsuccessful. message:" + receiveErrorMessage);
        }

        Thread.sleep(1000);
    }

    @Override
    protected Configuration getTestResourceConfiguration() {
        Configuration resourceConfig = new Configuration();
        Property property = new PropertySimple("name", getDestinationName());
        resourceConfig.put(property);
        property = new PropertySimple("JNDIName", getDestinationJndi());
        resourceConfig.put(property);

        return resourceConfig;
    }

    private void sendMessagesToDestination(String jndiName, int nofMessages) throws Exception {
        System.out.println("Sending " + nofMessages + " messages to " + jndiName);

        InitialContext ctx = AppServerUtils.getAppServerInitialContext();

        ConnectionFactory factory = (ConnectionFactory) ctx.lookup("ConnectionFactory");
        Destination destination = (Destination) ctx.lookup(jndiName);

        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        MessageProducer messageProducer = session.createProducer(destination);

        TextMessage message = session.createTextMessage();

        for (int i = 0; i < nofMessages; ++i) {
            message.setText("Message no. " + i);
            messageProducer.send(message);
        }

        messageProducer.close();
        session.close();
        connection.close();

        System.out.println("Giving some time for the messages to arrive...");
        Thread.sleep(2000);
    }

}
