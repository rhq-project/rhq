#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import junit.framework.Assert;

import com.google.gwt.junit.client.GWTTestCase;

public class ApplicationTestCase extends GWTTestCase
{
   private static final String MODULE_NAME = "org.example.Application";

   public String getModuleName() {
      return MODULE_NAME;
   }

   public void testSomething() {
      // Not much to actually test in this sample app
      // Ideally you would test your Controller here (NOT YOUR UI)
      // (Make calls to RPC services, test client side model objects, test client side logic, etc)
      Assert.assertTrue( true );
   }
}