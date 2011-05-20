package org.rhq.enterprise.server.drift;

import java.io.InputStream;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

@Stateless
public class DriftManagerBean implements DriftManagerLocal {

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(mappedName = "queue/DriftSnapshotsQueue")
    private Queue snapshotsQueue;

    @Override
    public void uploadSnapshot(int resourceId, long metadataSize, InputStream metadataStream, long dataSize,
        InputStream dataStream) throws Exception {
        Connection connection = factory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(snapshotsQueue);
        ObjectMessage msg = session.createObjectMessage(new UploadRequest(resourceId, metadataSize, metadataStream,
            dataSize, dataStream));
        producer.send(msg);
        connection.close();
    }

}
