package org.rhq.plugins.nagios.error;

/**
 * Class implements an Exception which is thrown if one of the methods of class LQLReplyParser
 * get a parameter that doesnt have the right context to work correct
 *
 * @author Alexander Kiefer
 */
public class InvalidReplyTypeException extends NagiosException
{
	/**
	 * Constructor is private because it should not be used
	 */
	private InvalidReplyTypeException()
	{
        super();
	}

	/**
	 *
	 * @param expectedReplyType - The replyType that would have been correct
	 * @param actualReplyType - The replyType that has been given and which was wrong
	 */
	public InvalidReplyTypeException(String expectedReplyType, String actualReplyType)
	{
		super("REPLY TYPE <" + actualReplyType + "> WHERE REPLY TYPE <" + expectedReplyType + "> WAS EXPECTED");
	}
}
