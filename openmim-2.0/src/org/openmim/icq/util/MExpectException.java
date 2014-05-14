package org.openmim.icq.util;

import org.openmim.messaging_network.MessagingNetworkException;

public class MExpectException extends MessagingNetworkException
{
  private MExpectException(int cat, int endUserReasonCode)
  {
    super(cat, endUserReasonCode);
  }
  public MExpectException(String s, int cat, int endUserReasonCode)
  {
    super(s, cat, endUserReasonCode);
  }
}