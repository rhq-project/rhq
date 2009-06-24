package test;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.profileservice.spi.ProfileService;

public class EjbClient
{   
   private static final String JNDI_LOGIN_INITIAL_CONTEXT_FACTORY = "org.jboss.security.jndi.JndiLoginInitialContextFactory";

   private static final String SECURE_PROFILE_SERVICE_JNDI_NAME = "SecureProfileService/remote";
   private static final String SECURE_MANAGEMENT_VIEW_JNDI_NAME = "SecureManagementView/remote";
   private static final String SECURE_DEPLOYMENT_MANAGER_JNDI_NAME = "SecureDeploymentManager/remote";
   
   private static final String PROFILE_SERVICE_PRINCIPAL = "admin";
   private static final String PROFILE_SERVICE_CREDENTIALS = "admin";
   
   public static void main(String[] args)
      throws Exception
   {                 
      Properties env = new Properties();
      env.setProperty(Context.PROVIDER_URL, "jnp://127.0.0.1:1099/");
      env.setProperty(Context.INITIAL_CONTEXT_FACTORY, JNDI_LOGIN_INITIAL_CONTEXT_FACTORY);
      env.setProperty(Context.SECURITY_PRINCIPAL, PROFILE_SERVICE_PRINCIPAL);
      env.setProperty(Context.SECURITY_CREDENTIALS, PROFILE_SERVICE_CREDENTIALS);      
      
      InitialContext initialContext = createInitialContext(env);
      ProfileService profileService = (ProfileService)lookup(initialContext, SECURE_PROFILE_SERVICE_JNDI_NAME);      
      ManagementView managementView = (ManagementView)lookup(initialContext, SECURE_MANAGEMENT_VIEW_JNDI_NAME);     
      DeploymentManager deploymentManager = (DeploymentManager)lookup(initialContext, SECURE_DEPLOYMENT_MANAGER_JNDI_NAME);      
      
      profileService.getDomains();
      profileService.getProfileKeys();            
      managementView.load();
      managementView.getDeploymentNames();  
      deploymentManager.getProfiles();      
      
      DeploymentTemplateInfo template = managementView.getTemplate("NoTxDataSourceTemplate");
      managementView.applyTemplate("MyNoTxDs", template);
      managementView.process();
   }
   
    private static InitialContext createInitialContext(Properties env) throws NamingException
    { 
        System.out.println("Creating JNDI InitialContext with env [" + env + "]...");              
        InitialContext initialContext = new InitialContext(env);
        System.out.println("Created JNDI InitialContext [" + initialContext + "].");
        return initialContext;
    }

    private static Object lookup(InitialContext initialContext, String name) throws NamingException
    {
        System.out.println("Looking up name '" + name + "' from InitialContext...");              
        Object obj = initialContext.lookup(name);
        System.out.println("Found Object: " + obj);
        return obj;
    }   
}
