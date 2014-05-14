package org.openmim.icq.util;

import org.openmim.messaging_network.MessagingNetwork;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.icq.utils.*;

public class MLang
{

  public static void EXPECT(boolean conditionExpected, String errorMessage, int cat, int endUserReasonCode)
  throws MExpectException
  {
    if (!conditionExpected)
      throw new MExpectException(errorMessage, cat, endUserReasonCode);
  }
  public static void EXPECT_EQUAL(long longValue1, long longValue2, String value1Name, String value2Name, int cat, int endUserReasonCode)
  throws MExpectException
  {
    if (longValue1 != longValue2)
      EXPECT_FALSE("The \"" + value1Name + "\" and \"" + value2Name + "\" must be equal, but they are not: "+value1Name+"=" + HexUtil.toHexString0x(longValue1, -1L, 16)+" ("+longValue1+"), "+value2Name+"="+HexUtil.toHexString0x(longValue2, -1L, 16)+" ("+longValue2+").", cat, endUserReasonCode);
  }

  public static void EXPECT_FALSE(String locationName, int cat, int endUserReasonCode)
  throws MExpectException
  {
    EXPECT(false, "This point should never be reached:\r\n" + StringUtil.toPrintableString(locationName), cat, endUserReasonCode);
  }

  public static void EXPECT_IS_MIM_STATUS(int statusValue, String valueName) throws MExpectException
  {
    switch(statusValue)
    {
      case MessagingNetwork.STATUS_ONLINE:
      case MessagingNetwork.STATUS_OFFLINE:
      case MessagingNetwork.STATUS_BUSY:
        break;

      default:
        EXPECT_FALSE("invalid status value: " + statusValue + ", valueName: " + valueName, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, MessagingNetworkException.ENDUSER_MIM_BUG);
        break;
    }
  }
  public static void EXPECT_NON_NEGATIVE(long longValue, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (!(longValue >= 0L)) EXPECT_FALSE(("The value named") + " \"" + valueName + "\" " + ("should be zero or greater than zero, but it is") + " " + longValue + ".", cat, endUserReasonCode);
  }
  public static void EXPECT_NOT_NULL(Object objectValue, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (objectValue == null) EXPECT_FALSE("Non-null object " + StringUtil.toPrintableString(valueName) + " expected, but it is null.", cat, endUserReasonCode);
  }
  public static void EXPECT_NOT_NULL_NOR_EMPTY(String stringValue, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (StringUtil.isNullOrEmpty(stringValue)) EXPECT_FALSE("The string named " + StringUtil.toPrintableString(valueName) + " should be neither null nor empty, but it is " + StringUtil.toPrintableString(stringValue), cat, endUserReasonCode);
  }
  public static void EXPECT_NOT_NULL_NOR_TRIMMED_EMPTY(String stringValue, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (StringUtil.isNullOrEmpty(stringValue)) EXPECT_FALSE("The string named " + StringUtil.toPrintableString(valueName) + " should be neither null, empty, nor contain whitespace only, but it is " + StringUtil.toPrintableString(stringValue), cat, endUserReasonCode);
  }
  public static void EXPECT_POSITIVE(long longValue, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (!(longValue > 0L)) EXPECT_FALSE("The \"" + valueName + "\" should be greater than zero, but it is " + longValue + ".", cat, endUserReasonCode);
  }
  public static int parseInt(String value, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (StringUtil.isNullOrEmpty(value))
      EXPECT_NOT_NULL_NOR_EMPTY(value, valueName, cat, endUserReasonCode);
    int i = -1;
    try
    {
      i = Integer.parseInt(value);
    }
    catch (NumberFormatException ex)
    {
      throw new MExpectException(valueName+" must be an integer number, but it is '" + value + "'", cat, endUserReasonCode);
    }
    return i;
  }
  public static int parseInt_NonNegative(String value, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { int i = parseInt(value, valueName, cat, endUserReasonCode);
    if (i < 0) EXPECT_NON_NEGATIVE(i, valueName, cat, endUserReasonCode);
    return i;
  }
  public static long parseLong(String value, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { if (StringUtil.isNullOrEmpty(value))
      EXPECT_NOT_NULL_NOR_EMPTY(value, valueName, cat, endUserReasonCode);
    long i = -1;
    try
    {
      i = Long.parseLong(value);
    }
    catch (NumberFormatException ex)
    {
      throw new MExpectException(valueName+" must be a long number, but it is '" + value + "'", cat, endUserReasonCode);
    }
    return i;
  }
  public static long parseLong_NonNegative(String value, String valueName, int cat, int endUserReasonCode)
  throws MExpectException
  { long i = parseLong(value, valueName, cat, endUserReasonCode);
    if (i < 0) EXPECT_NON_NEGATIVE(i, valueName, cat, endUserReasonCode);
    return i;
  }
}
