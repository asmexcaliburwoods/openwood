package org.openmim.msn;

import java.io.*;

import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.icq.util.*;
import org.openmim.icq.utils.*;

public class LoginIdUtil
{
  /** Throws a logged_off exception on error */
  public static void checkValid_Fatal(String loginId)
      throws MExpectException
  {
    checkValid(loginId,
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      MessagingNetworkException.ENDUSER_INVALID_LOGIN_ID_SPECIFIED_LOGGED_OFF);
  }

  /** Throws a still_connected exception on error */
  public static void checkValid_Ignorable(String loginId)
      throws MExpectException
  {
    checkValid(loginId,
      MessagingNetworkException.CATEGORY_STILL_CONNECTED,
      MessagingNetworkException.ENDUSER_INVALID_LOGIN_ID_SPECIFIED_STILL_CONNECTED);
  }

  private static void checkValid(String loginId, int errorLogger, int endUserErrorCode)
      throws MExpectException
  {
    if (loginId == null)
      throw new AssertException("loginId cannot be null");
    try
    {
      if (loginId.getBytes("ASCII").length > 129)
        throw new MExpectException("loginId is too long; length must be <= 129", errorLogger, endUserErrorCode);
    }
    catch (UnsupportedEncodingException ee)
    {
      throw new AssertException(ee.toString());
    }
    char[] ca = loginId.toCharArray();
    int atcount = 0;
    boolean validDomain = false;
    for (int i = 0; i < ca.length; i++)
    {
      char c = ca[i];
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n')
        throw new MExpectException("loginId cannot contain whitespace", errorLogger, endUserErrorCode);
      if (c == '@') ++atcount;
      else
        if (c == '.' && atcount == 1) validDomain = true;
    }
    if (atcount != 1)
      throw new MExpectException("loginId must contain single '@'", errorLogger, endUserErrorCode);
    if (!validDomain)
      throw new MExpectException("loginId must be a valid email address", errorLogger, endUserErrorCode);
  }
  
  public static String normalize(String loginId)
  {
    if (loginId == null)  throw new AssertException("loginId cannot be null");
    return loginId.toLowerCase(java.util.Locale.US);
  }
}