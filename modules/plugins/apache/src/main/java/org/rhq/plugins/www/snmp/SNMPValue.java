package org.rhq.plugins.www.snmp;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

public class SNMPValue
{

   private final Log log = LogFactory.getLog(this.getClass());

   public enum Type
   {
      LONG,
      STRING,
      LONG_CONVERTABLE
   }

   private OID oid;
   private Variable var;

   SNMPValue(VariableBinding varBinding)
   {
      this.oid = varBinding.getOid();
      this.var = varBinding.getVariable();
   }

   private String toHex(int val)
   {
      return Integer.toHexString(val & 0xff);
   }

   //from SNMPv2-TC:
   //PhysAddress ::= TEXTUAL-CONVENTION
   //DISPLAY-HINT "1x:"
   //STATUS       current
   //DESCRIPTION
   //        "Represents media- or physical-level addresses."
   //SYNTAX       OCTET STRING
   public String toPhysAddressString()
   {
      byte[] data = ((OctetString)this.var).getValue();

      if (data.length == 0)
      {
         return "0:0:0:0:0:0"; //e.g. loopback
      }

      StringBuilder buffer = new StringBuilder();

      buffer.append(toHex(data[0]));

      for (int i = 1; i < data.length; i++)
      {
         buffer.append(':').append(toHex(data[i]));
      }

      return buffer.toString();
   }

   public String getOID()
   {
      return this.oid.toString();
   }

   public Type getType()
   {
      switch (this.var.getSyntax())
      {
         case SMIConstants.SYNTAX_INTEGER32:
         case SMIConstants.SYNTAX_COUNTER32:
         case SMIConstants.SYNTAX_COUNTER64:
         case SMIConstants.SYNTAX_TIMETICKS:
         case SMIConstants.SYNTAX_GAUGE32:
            return Type.LONG;
         case SMIConstants.SYNTAX_OCTET_STRING:
            //XXX while we are able to convert long
            //does not mean we should. treat as a string
            //for now.
            //return Type.LONG_CONVERTABLE;
         default:
            return Type.STRING;
      }
   }

   /**
    * @return The value of the variable as a long
    *
    * @throws SNMPException If the variable cannot be expressed as a long.
    */
   public long toLong() throws SNMPException
   {
      if (this.var.getSyntax() == SMIConstants.SYNTAX_OCTET_STRING)
      {
         return convertDateTimeOctetStringToLong();
      }
      else
      {
         try
         {
            return this.var.toLong();
         }
         catch (UnsupportedOperationException e)
         {
            String msg =
                  "Cannot convert " +
                        this.var.getSyntaxString() +
                        " to long";
            throw new SNMPException(msg);
         }
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof SNMPValue)
      {
         Variable var = ((SNMPValue)obj).var;
         return (var != null) && var.equals(this.var);
      }
      return false;
   }

   @Override
   public int hashCode()
   {
      return (var != null ? var.hashCode() : 0);
   }

   @Override
   public String toString()
   {
      return this.var.toString();
   }

   // XXX A bit of a hack - if it is an OctetString, treat
   // it like a DateAndTime (from the SNMPv2-TC MIB)
   private long convertDateTimeOctetStringToLong()
         throws SNMPException
   {
      byte[] bytes = ((OctetString)this.var).getValue();

      if (bytes.length != 8)
      {
         throw new SNMPException("OctetString is not in DateAndTime syntax.");
      }

      Calendar cal = Calendar.getInstance();

      int ix = 0;
      int year = (bytes[ix] > 0) ?
            bytes[ix] : (256 + bytes[ix]);

      year <<= 8;
      ix++;
      year += (bytes[ix] > 0) ?
            bytes[ix] : (256 + bytes[ix]);

      ix++;

      int month = bytes[ix++];
      int day = bytes[ix++];
      int hour = bytes[ix++];
      int minutes = bytes[ix++];
      int seconds = bytes[ix++];
      int deciseconds = bytes[ix++];

      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.MONTH, (month - 1));
      cal.set(Calendar.DAY_OF_MONTH, day);
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, minutes);
      cal.set(Calendar.SECOND, seconds);
      cal.set(Calendar.MILLISECOND, (100 * deciseconds));
      cal.set(Calendar.ZONE_OFFSET, 0);
      cal.set(Calendar.DST_OFFSET, 0);

      if (log.isDebugEnabled())
      {
         log.debug("converted to DateAndTime: millis=" +
               cal.getTimeInMillis() + ", date=" +
               cal.getTime());
      }

      return cal.getTimeInMillis();
   }

}
