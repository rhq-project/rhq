package org.rhq.plugins.www.snmp;

public class MIBLookupException extends SNMPException
{

   public MIBLookupException(String msg)
   {
      super(msg);
   }

   public MIBLookupException(String msg, Throwable t)
   {
      super(msg, t);
   }
   
}
