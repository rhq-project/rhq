package org.rhq.plugins.www.snmp;

import org.snmp4j.mp.SnmpConstants;

/**
 * Implements the SNMPSession interface for SNMPv2c sessions by
 * extending the SNMPSession_v1 implementation - mostly identical to
 * the v1 session implementation.
 */
class SNMPSession_v2c extends SNMPSession_v1
{

   /**
    * Should only be called by SNMPClient.
    * To get an instance of this class, use SNMPClient.startSession().
    *
    * @see SNMPClient#startSession
    */
   SNMPSession_v2c()
   {
      this.version = SnmpConstants.version2c;
   }

}
