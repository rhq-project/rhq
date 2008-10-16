package org.jboss.on.plugins.mock.jboss.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.plugins.mock.jboss.scenario.ScenarioPropertyConfigurationError;
import org.rhq.plugins.mock.jboss.scenario.ScenarioProperty;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;

/**
 * Class to hold all the error messages from the scenario.xml file for a resource. This holds the error messages
 * for individual properties and one message for a global configuration error. In the @see ResourceCache there will be a
 * map of these objects, one for each resource. When update is called through the @see JndiResourceComponent, it will
 * look for this class in the ResourceCache for the corresponding resource, then call handleUpdate in this class to add
 * error messages to the @see ConfigurationUpdateReport. If there are no errors in the handler, then nothing is added
 * to the update report and the status is set at SUCCESS.
 *
 * @author: Mark Spritzler
 */
public class ConfigurationHandler
{

   private Map<String, String> propertyErrors = new HashMap<String, String>();
   private String configurationError;

   /**
    * Method will add the errors to the @see ConfigurationUpdateReport
    * @param report the update report to add errors to, or just set to Success
    */
   public void handleUpdate(ConfigurationUpdateReport report)
   {
      Configuration config = report.getConfiguration();
      report.setStatus(ConfigurationUpdateStatus.SUCCESS);
      if (configurationError != null && !configurationError.equals(""))
      {
         //report.setStatus(ConfigurationUpdateStatus.FAILURE);
         report.setErrorMessage(configurationError);
      }

      if (propertyErrors.size() > 0)
      {
         //report.setStatus(ConfigurationUpdateStatus.FAILURE);
         loadConfigurationWithErrors(config);
      }
   }

   /**
    * Sets the top level configuration error, there can only be one.
    * @param configurationError String for the error message.
    */
   public void setConfigurationError(String configurationError)
   {
      this.configurationError = configurationError;
   }

   /**
    * Adds an error for a particular property.
    * @param property property name that has an error
    * @param message The error message for the property.
    */
   public void addPropertyError(String property, String message)
   {
      propertyErrors.put(property, message);
   }

   /**
    * If you have a whole list of @see ScenarioProperty objects then passing them to this method will loop through
    * the properties and add an error message to properties that have error messages defined in the scenario.xml file
    * into the propertyErrors instance variable.
    * @param properties List of @see ScenarioProperty objects
    */
   public void addPropertyErrors(List<ScenarioProperty> properties)
   {
      for (ScenarioProperty property : properties)
      {
         ScenarioPropertyConfigurationError propertyError =
               property.getConfigurationError();

         if (propertyError != null)
         {
            String message = propertyError.getMessage();
            if (message != null && !message.equals(""))
            {
               addPropertyError(property.getName(), message);
            }
         }
      }
   }

   private void loadConfigurationWithErrors(Configuration config)
   {
      Map<String, PropertySimple> propertyMap = config.getSimpleProperties();
      Set<String> keys = propertyMap.keySet();
      for (String key : keys)
      {
         PropertySimple propertySimple = propertyMap.get(key);
         String message = propertyErrors.get(key);
         if (message != null && !message.equals(""))
         {
            propertySimple.setErrorMessage(message);
         }
      }
   }
}
