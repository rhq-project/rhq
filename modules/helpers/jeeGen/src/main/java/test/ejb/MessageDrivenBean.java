package test.ejb;

import javax.ejb.MessageDrivenContext;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Message-driven bean - impl.
 */
public class MessageDrivenBean implements javax.ejb.MessageDrivenBean, MessageListener {

    private MessageDrivenContext context;

    // Constructor, which is public and takes no arguments
    public MessageDrivenBean() {
    }

    /**
     * Begin EJB-required methods. The following methods are called
     * by the container, and never called by client code.
     */

    /**
     * ejbCreate method, declared as public (but not final or
     * static), with a return type of void, and with no arguments.
     */
    public void ejbCreate() {
    }

    public void setMessageDrivenContext(MessageDrivenContext context) {
        // As with all enterprise beans, you must set the context in order to be
        // able to use it at another time within the MDB methods
        this.context = context;
    }

    // life cycle Methods

    public void ejbRemove() {
    }

    /**
     * JMS MessageListener-required methods. The following
     * methods are called by the container, and never called by
     * client code.
     */

    // Receives the incoming Message and displays the text.
    public void onMessage(Message message) {
        // MDB does not carry state for an individual client

        try {
            Context namingContext = new InitialContext();
            // 1. Retrieve the QueueConnectionFactory using a
            // resource reference defined in the ejb-jar.xml file.
            QueueConnectionFactory qcf = (QueueConnectionFactory)
                namingContext.lookup("java:comp/env/jms/myQueueConnectionFactory");
            namingContext.close();

            // 2. Create the queue connection
            QueueConnection queueConnection = qcf.createQueueConnection();
            // 3. Create the session over the queue connection.
            QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            // 4. Create the sender to send messages over the session.
            QueueSender queueSender = queueSession.createSender(null);

            // When the onMessage method is called, a message has been sent.
            // You can retrieve attributes of the message using the Message object.
            String txt = ("mdb rcv: " + message.getJMSMessageID());
            System.out.println(txt + " redel="
                + message.getJMSRedelivered() + " cnt="
                + message.getIntProperty("JMSXDeliveryCount"));

            // Create a new message using the createMessage method.
            // To send it back to the originator of the other message,
            // set the String property of "RECIPIENT" to "CLIENT."
            // The client only looks for messages with string property CLIENT.
            // Copy the original message ID into new message's Correlation ID for
            // tracking purposes using the setJMSCorrelationID method. Finally,
            // set the destination for the message using the getJMSReplyTo method
            // on the previously received message. Send the message using the
            // send method on the queue sender.

            // 5. Create a message using the createMessage method
            Message returnMessage = queueSession.createMessage();
            // 6. Set properties of the message.
            returnMessage.setStringProperty("RECIPIENT", "CLIENT");
            returnMessage.setIntProperty("count", message.getIntProperty("JMSXDeliveryCount"));
            returnMessage.setJMSCorrelationID(message.getJMSMessageID());
            // 7. Retrieve the reply destination.
            Destination destination = message.getJMSReplyTo();
            // 8. Send the message using the send method of the sender.
            queueSender.send((Queue) destination, returnMessage);
            System.out.println(txt + " snd: " + returnMessage.getJMSMessageID());
            // close the connection
            queueConnection.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process message [" + message + "].", e);
        }
    }
}
