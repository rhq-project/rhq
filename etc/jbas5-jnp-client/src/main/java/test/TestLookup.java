package test;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.profileservice.spi.ProfileService;
import org.jnp.interfaces.NamingContext;

public class TestLookup
{
   private static final String NAMING_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
   private static final String JNDI_LOGIN_INITIAL_CONTEXT_FACTORY = "org.jboss.security.jndi.JndiLoginInitialContextFactory";

   public static void main(String[] args)
      throws Exception
   {
      ProfileService profileService;
      ManagementView managementView;
      DeploymentManager deploymentManager; 

      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, "jnp://127.0.0.1:1099");
      env.setProperty(Context.INITIAL_CONTEXT_FACTORY, NAMING_CONTEXT_FACTORY);
      env.setProperty(NamingContext.JNP_DISABLE_DISCOVERY, "true");
      // Make sure the timeout always happens, even if the JBoss server is hung.
      env.setProperty("jnp.timeout", "60");
      env.setProperty("jnp.sotimeout", "60");
      InitialContext ctx = new InitialContext(env);
      profileService = (ProfileService) ctx.lookup("ProfileService");
      System.err.println("ProfileService: " + profileService);
      System.err.println("ManagementView: " + profileService.getViewManager());
      System.err.println("DeploymentManager: " + profileService.getDeploymentManager());      
   }
}
