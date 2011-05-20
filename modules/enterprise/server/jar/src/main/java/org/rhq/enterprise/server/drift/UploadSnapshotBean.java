package org.rhq.enterprise.server.drift;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceUtil;

import static org.rhq.enterprise.server.util.LookupUtil.getCoreServer;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/DriftSnapshotsQueue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
public class UploadSnapshotBean implements MessageListener {
    private final Log log = LogFactory.getLog(UploadSnapshotBean.class);

    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage msg = (ObjectMessage) message;
            UploadRequest request = (UploadRequest) msg.getObject();

            File snapshotsDir = getSnapshotsDir();
            File destDir = new File(snapshotsDir, Integer.toString(request.getResourceId()));
            destDir.mkdir();

            StreamUtil.copy(remoteStream(request.getMetaDataStream()), new BufferedOutputStream(new FileOutputStream(
                new File(destDir, "metadata.txt"))), false);
            StreamUtil.copy(remoteStream(request.getDataStream()), new BufferedOutputStream(new FileOutputStream(
                new File(destDir, "data.zip"))), false);
        } catch(FileNotFoundException e) {
            log.error(e);
        } catch (JMSException e) {
            log.error(e);
        }
    }

    private File getSnapshotsDir() throws FileNotFoundException {
        File serverHomeDir = getCoreServer().getJBossServerHomeDir();
        File snapshotsDir = new File(serverHomeDir, "deploy/rhq.ear/rhq-downloads/snapshots");
        if (!snapshotsDir.isDirectory()) {
            snapshotsDir.mkdirs();
            if (!snapshotsDir.isDirectory()) {
                throw new FileNotFoundException("Missing snapshots directory at [" + snapshotsDir + "]");
            }
        }
        return snapshotsDir;
    }

    private InputStream remoteStream(InputStream stream) {
        RemoteInputStream remoteStream = (RemoteInputStream) stream;
        ServiceContainer serviceContainer = ServerCommunicationsServiceUtil.getService().getServiceContainer();
        ClientCommandSenderConfiguration config = serviceContainer.getClientConfiguration();
        ClientCommandSender sender = serviceContainer.createClientCommandSender(remoteStream.getServerEndpoint(),
            config);
        sender.startSending();
        remoteStream.setClientCommandSender(sender);

        return stream;
    }
}
