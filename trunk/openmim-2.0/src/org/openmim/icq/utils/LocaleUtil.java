package org.openmim.icq.utils;

import java.util.*;

/**
 * Insert the type's description here.
 * Creation date: (10.11.00 15:07:27)
 * @author:
 */
public final class LocaleUtil
{

  /**
   * LocaleUtil constructor comment.
   */
  private LocaleUtil()
  {
  }
  public static String prepareKey(String key)
  {
    if ((key) == null) Lang.ASSERT_NOT_NULL(key, key);
		return key.replace(' ', '_').replace(':', '_').replace('\'', '_');
  }
}