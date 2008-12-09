package org.openmim.msn;

public class ErrorInfo
{
  public final String errorCode;
  public final String errorMessage;
  public final boolean killNS;
  public final boolean killSSS;
  public final int mimExceptionLogger;
  public final int mimExceptionEndUserReasonCode;
  
  ErrorInfo(String errorCode, String errorMessage, boolean killNS, boolean killSSS, int mimExceptionLogger, int mimExceptionEndUserReasonCode)
  {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.killNS = killNS;
    this.killSSS = killSSS;
    this.mimExceptionEndUserReasonCode = mimExceptionEndUserReasonCode;
    this.mimExceptionLogger = mimExceptionLogger;
  }
}
