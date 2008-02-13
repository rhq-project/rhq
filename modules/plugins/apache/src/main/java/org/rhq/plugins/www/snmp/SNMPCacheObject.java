package org.rhq.plugins.www.snmp;

class SNMPCacheObject
{

   int expire = SNMPSessionCache.EXPIRE_DEFAULT;
   long timestamp = 0;
   Object value = null;
}
