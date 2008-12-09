package org.openmim.icq2k;

import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.*;
import org.openmim.icq.util.joe.*;

public class IcqUinUtil
{
  //icq2k uin can be 10000...2147483646
  //Integer.MAX_VALUE==2147483647
  public static final int MIN_VALUE = 10000;
  public static final int MAX_VALUE = Integer.MAX_VALUE - 1;
  private static final String MSG1 = " must be an icq uin in the range 10000...2147483646, but it is \"";
  //int dstUin = IcqUinUtil.parseUin(dstLoginId, "dst", MessagingNetworkException.CATEGORY_STILL_CONNECTED);

  public static int parseUin(String value, String valueName, int cat)
  throws MExpectException
  {
    if ((value) == null) Lang.ASSERT_NOT_NULL(value, valueName);

    if (StringUtil.isNullOrEmpty(value)) 
      MLang.EXPECT_NOT_NULL_NOR_EMPTY(
        value, valueName,
        cat,
        MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_BE_EMPTY_STRING);

    if (value.indexOf(' ') != -1 ||
         value.indexOf('\t') != -1 ||
         value.indexOf('\r') != -1 ||
         value.indexOf('\n') != -1)
      MLang.EXPECT_FALSE(
        "login id should never contain whitespace, but it does: "+StringUtil.toPrintableString(value)+".",
        cat,
        MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_CANNOT_CONTAIN_WHITESPACE);

    if (value.startsWith("0"))
      throw new MExpectException(
        valueName + " cannot start with '0', but it does: \"" + value + "\"",
        cat,
        MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_UIN_CANNOT_START_WITH_ZERO);

    int i = -1;
    try
    {
      i = Integer.parseInt(value);
    }
    catch (NumberFormatException ex)
    {
      throw new MExpectException(valueName + MSG1 + value + "\"",
        cat,
        MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_MUST_BE_INTEGER_IN_THE_RANGE);
    }

    if (i < MIN_VALUE || i > MAX_VALUE)
      throw new MExpectException(valueName + MSG1 + value + "\"",
        cat,
        MessagingNetworkException.ENDUSER_INVALID_UIN_SPECIFIED_MUST_BE_INTEGER_IN_THE_RANGE);
    return i;
  }
}