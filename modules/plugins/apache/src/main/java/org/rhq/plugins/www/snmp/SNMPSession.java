package org.rhq.plugins.www.snmp;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Generic interface for any version of the SNMP protocol.
 * Use {@link SNMPClient#getSession} to get an instance
 * of a class that implements this interface.
 */
public interface SNMPSession
{

   /**
    * Retrieves the variable with the the specified MIB name or OID.
    *
    * @param mibName the name of the variable to retrieve
    *
    * @return a SNMPValue object representing the value of the
    *         variable.
    *
    * @throws SNMPException if an error occurs communicating
    *                       with the SNMP agent.
    */
   @NotNull
   SNMPValue getSingleValue(String mibName)
         throws SNMPException;

   /**
    * Retrieve the variable that logically next after
    * the specified MIB name or OID.
    *
    * @param mibName the name of the MIB variable at which to start looking
    *
    * @return An SNMPValue object representing the value of the
    *         specified MIB name, or if not found, the next logical MIB
    *         name.
    *
    * @throws SNMPException if an error occurs communicating
    *                       with the SNMP agent.
    */
   @NotNull
   SNMPValue getNextValue(String mibName)
         throws SNMPException;

   /**
    * Retrieves all values from a column of an SNMP table (i.e. returns the subtree of values rooted at the specified
    * MIB name or OID).
    *
    * @param mibName The name of the column of the SNMP table.
    *
    * @return a List of SNMPValue objects representing the values
    *         found in the column.
    *
    * @throws SNMPException if an error occurs communicating
    *                       with the SNMP agent.
    */
   @NotNull
   List<SNMPValue> getColumn(String mibName)
         throws SNMPException;

   /**
    * TODO
    *
    * @param mibName the MIB name of the table
    * @param index
    * @return
    * @throws SNMPException if an error occurs communicating
    *                       with the SNMP agent.
    */
   @NotNull
   Map<String, SNMPValue> getTable(String mibName, int index)
         throws SNMPException;

   /**
    * TODO
    *
    * @param mibName the MIB name of the starting OID
    * @return
    * @throws SNMPException if an error occurs communicating
    *                       with the SNMP agent.
    */
   @NotNull
   List<SNMPValue> getBulk(String mibName)
         throws SNMPException;

   /**
    * Pings the agent associated with this session to see if it is responsive.
    *
    * @return true if the agent responds, or false otherwise
    */
   boolean ping();

   /**
    * Get the timeout value for this session.
    *
    * @return the timeout value for this session
    */
   long getTimeout();

   /**
    * Set the timeout value for this session.
    *
    * @param timeout the timeout value for this session
    */
   void setTimeout(long timeout);

   /**
    * Get the retries value for this session.
    *
    * @return the retries value for this session
    */
   int getRetries();

   /**
    * Set the retries value for this session.
    *
    * @param retries the retries value for this session
    */
   void setRetries(int retries);

}
