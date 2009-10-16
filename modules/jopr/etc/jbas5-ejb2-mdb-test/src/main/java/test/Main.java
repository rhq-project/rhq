package test;

import java.io.File;
import java.util.Properties;
import java.util.Set;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.profileservice.spi.ProfileService;

/**
 * @author Lukas Krejci
 *
 */
public class Main {

    private static String ARCHIVE_NAME = null;
    private static String TEST_JAR_PATH = null; 

    private static final int MESSAGES_SENT = 10;
    private static final String QUEUE_NAME = "queue/A";
    private static final String MDB_NAME = "StrictlyPooledMDB";
    
    public static void main(String[] args) {
        try {
            TEST_JAR_PATH = args[0];
            File f = new File(TEST_JAR_PATH);
            ARCHIVE_NAME = f.getName();
            
            ManagementView mgtView = getManagementView();
            mgtView.load();

            deployTestJar();

            mgtView.load();

            InitialContext ctx = getInitialContext();

            System.out.println("Sending " + MESSAGES_SENT + " messages to " + QUEUE_NAME);
            
            QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("ConnectionFactory");
            Queue queue = (Queue) ctx.lookup(QUEUE_NAME);

            QueueConnection connection = factory.createQueueConnection();
            connection.start();

            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            QueueSender sender = session.createSender(queue);
            
            TextMessage message = session.createTextMessage();

            for (int i = 0; i < MESSAGES_SENT; ++i) {
                message.setText("Message no. " + i);
                sender.send(message);
            }
            
            sender.close();
            session.close();
            connection.close();
            
            Thread.sleep(2000);
            
            Set<ManagedComponent> comps = mgtView.getComponentsForType(new ComponentType("EJB", "MDB"));
            boolean found = false;
            for (ManagedComponent comp : comps) {
                System.out.println(comp.getName());
                if (comp.getName().contains(MDB_NAME)) {
                    found = true;
                    
                    //check the CreateCount property
                    ManagedProperty createCountProp = comp.getProperty("CreateCount");
                    System.out.println("CreateCount = " + createCountProp.getValue());

                    //check the MessageCount property
                    ManagedProperty messageCountProp = comp.getProperty("MessageCount");
                    System.out.println("MessageCount = " + messageCountProp.getValue());
                    break;
                }
            }
            if (!found) {
                System.out.println("Couldn't find the session bean in management view.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                undeploy();
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
    }

    private static InitialContext getInitialContext() throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.PROVIDER_URL, "jnp://localhost:1099");
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        
        InitialContext ctx = new InitialContext(env);
        return ctx;
    }

    private static ProfileService getProfileService() throws Exception {
        InitialContext ctx = getInitialContext();
        ProfileService ps = (ProfileService) ctx.lookup("ProfileService");
        return ps;
    }
    
    private static ManagementView getManagementView() throws Exception {
        ManagementView activeView = getProfileService().getViewManager();
        // Init the VFS to setup the vfs* protocol handlers
        //VFS.init();

        return activeView;
    }
    
    private static DeploymentManager getDeploymentManager() throws Exception {
        DeploymentManager manager = getProfileService().getDeploymentManager();
        
        return manager;
    }
    
    private static void deployTestJar() throws Exception {
        DeploymentManager deploymentManager = getDeploymentManager();
        
        File archiveFile = new File(TEST_JAR_PATH);
        
        DeploymentProgress progress = deploymentManager.distribute(ARCHIVE_NAME, archiveFile.toURI().toURL(), false);
        progress.run();

        DeploymentStatus status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to distribute " + archiveFile.getAbsolutePath() + " with message: "
                + status.getMessage());
        }

        String[] deploymentNames = progress.getDeploymentID().getRepositoryNames();

        progress = deploymentManager.start(deploymentNames);
        progress.run();

        status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to start " + archiveFile.getAbsolutePath() + " with message: "
                + status.getMessage());
        }        
    }
    
    
    public static void undeploy() throws Exception {
        DeploymentManager deploymentManager = getDeploymentManager();

        DeploymentProgress progress = deploymentManager.remove(ARCHIVE_NAME);
        progress.run();

        DeploymentStatus status = progress.getDeploymentStatus();

        if (status.isFailed()) {
            throw new IllegalStateException("Failed to undeploy " + ARCHIVE_NAME + " with message: "
                + status.getMessage());
        }
    }        
}
