package org.rhq.enterprise.server.plugins.groovy

class RHQScriptException extends Exception {

  def RHQScriptException() {
    super();
  }

  def RHQScriptException(String message) {
    super(message);
  }

  def RHQScriptException(String message, Throwable cause) {
    super(message, cause);
  }

  def RHQScriptException(Throwable cause) {
    super(cause);
  }


}
