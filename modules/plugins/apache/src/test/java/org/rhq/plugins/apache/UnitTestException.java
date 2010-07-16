package org.rhq.plugins.apache;

public class UnitTestException extends Exception{

	public UnitTestException(){
		super();
	}

	public UnitTestException(String text){
		super(text);
	}

	public UnitTestException(Exception e){
		super(e);
	}

	public UnitTestException(String message,Throwable cause){
	    super(message,cause);
	}

}
