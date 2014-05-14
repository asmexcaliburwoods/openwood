package org.openmim.msn;

public class AccountPropertiesUtil
{
  static boolean isKnownPhoneProperty(String propName)
  {
    return
      "PHH".equals(propName) ||
      "PHW".equals(propName) ||
      "PHM".equals(propName);
  }
}
