package org.rhq.plugins.www.snmp;

import org.snmp4j.mp.SnmpConstants;

/**
 * Implements the SNMPSession interface for SNMPv3 sessions by
 * extending the SNMPSession_v2c implementation. SNMPv3 is
 * only different from v1 or v2c inthe way that a session
 * is initialized.
 */
class SNMPSession_v3 extends SNMPSession_v2c
{

   /**
    * Should only be called by SNMPClient.
    * To get an instance of this class, use SNMPClient.startSession().
    *
    * @see SNMPClient#startSession
    */
   SNMPSession_v3()
   {
      this.version = SnmpConstants.version3;
   }

   /**
    * Initializes the SNMP session using the specified agent connection and authentication info.
    */
   void init(String address,
             int port,
             String user,
             String password,
             int authMethod)
         throws SNMPException
   {
      // TODO
   }

}
