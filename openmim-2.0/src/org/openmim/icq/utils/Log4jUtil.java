package org.openmim.icq.utils;

import org.openmim.stuff.Defines;

public class Log4jUtil
{
  public static void dump(org.apache.log4j.Logger CAT, byte[] byteArray) 
  { 
    dump(CAT, byteArray, 0, byteArray.length); 
  }

  public static void dump(org.apache.log4j.Logger CAT, byte[] byteArray, int ofs, int len)
  { 
    dump(CAT, "", byteArray, ofs, len, ""); 
  }

  public static void dump(org.apache.log4j.Logger CAT, String message, byte[] byteArray, String dumpLinePrefix)
  {
    dump(CAT, message, byteArray, 0, byteArray.length, dumpLinePrefix);
  }
  
  public static void dump(org.apache.log4j.Logger CAT, String message, byte[] byteArray, int ofs, int len, String dumpLinePrefix)
  {
    if (Defines.DEBUG_FULL_DUMPS && CAT.isDebugEnabled())
    {
      java.io.ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream(5 * byteArray.length);
      HexUtil.dump_(bas, byteArray, ofs, len, dumpLinePrefix);
      CAT.debug(message + "\n" + new String(bas.toByteArray()));
    }
  }
}