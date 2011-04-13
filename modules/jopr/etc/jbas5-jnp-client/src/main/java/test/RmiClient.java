package test;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.profileservice.spi.ProfileService;

public class RmiClient
{
   private static final String NAMING_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";

   private static final String PROFILE_SERVICE_JNDI_NAME = "ProfileService";
   
   public static void main(String[] args)
      throws Exception
   {          
      String jnpUrl = (args.length == 1) ? args[0] : "jnp://127.0.0.1:1099";
      
      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, jnpUrl);
      env.setProperty(Context.INITIAL_CONTEXT_FACTORY, NAMING_CONTEXT_FACTORY);
      env.setProperty("jnp.disableDiscovery", "true");      
      env.setProperty("jnp.timeout", "120");
      env.setProperty("jnp.sotimeout", "120");
      InitialContext initialContext = new InitialContext(env);
      
      ProfileService profileService = (ProfileService)initialContext.lookup(PROFILE_SERVICE_JNDI_NAME);      
      System.err.println("ProfileService: " + profileService);
      ManagementView managementView = profileService.getViewManager();
      System.err.println("ManagementView: " + managementView);     
      DeploymentManager deploymentManager = profileService.getDeploymentManager();
      System.err.println("DeploymentManager: " + deploymentManager);      
      
      //profileService.getDomains();
      //profileService.getProfileKeys();
      managementView.load();
      //managementView.getDeploymentNames();      
      //deploymentManager.getProfiles();

      ComponentType localTxDataSourceType = new ComponentType("DataSource", "LocalTx");
      ManagedComponent defaultDSComponent = managementView.getComponent("DefaultDS", localTxDataSourceType);      
      ManagedProperty maxPoolSizeProp = defaultDSComponent.getProperty("max-pool-size");
      SimpleValueSupport maxPoolSizeValue = (SimpleValueSupport)maxPoolSizeProp.getValue();
      System.err.println("original value: " + maxPoolSizeValue.getValue());            
      maxPoolSizeValue.setValue(null);
      managementView.updateComponent(defaultDSComponent);
      
      managementView.load();
      defaultDSComponent = managementView.getComponent("DefaultDS", localTxDataSourceType);      
      maxPoolSizeProp = defaultDSComponent.getProperty("max-pool-size");
      maxPoolSizeValue = ((SimpleValueSupport)maxPoolSizeProp.getValue());
      System.err.println("new value: " + maxPoolSizeValue.getValue());      
   }
}

