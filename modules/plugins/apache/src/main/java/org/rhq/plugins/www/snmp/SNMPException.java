package org.rhq.plugins.www.snmp;

public class SNMPException extends Exception
{

   public SNMPException(String msg)
   {
      super(msg);
   }

   public SNMPException(String msg, Throwable t)
   {
      super(msg, t);
   }
   
}
