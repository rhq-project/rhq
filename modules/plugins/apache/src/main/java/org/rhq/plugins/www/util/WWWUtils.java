/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.plugins.www.util;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;

/**
 * @author Ian Springer
 */
public abstract class WWWUtils
{

   /**
    *
    * @param httpURL an http or https URL
    * @return true if connecting to the URL succeeds, or false otherwise
    */
   public static boolean isAvailable(URL httpURL)
   {
      try
      {
         HttpURLConnection connection = (HttpURLConnection)httpURL.openConnection();
         connection.setRequestMethod("HEAD");
         connection.setConnectTimeout(3000);
         connection.connect();
         // TODO: Check response status code?
      }
      catch (IOException e)
      {
         return false;
      }
      return true;
   }

}
