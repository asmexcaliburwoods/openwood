package org.openmim.icq.util.joe;

import java.util.*;
import java.io.*;

public final class HexUtil
{
  public final static char[] HEX_DIGITS_CHARS = "0123456789abcdef".toCharArray();
  public final static byte[] HEX_DIGITS_BYTES = "0123456789abcdef".getBytes();

  public static final void dump(OutputStream os, byte[] b, String linePrefix)
  {
    dump(os, b, 0, b.length, linePrefix);
  }
  
  public static final void dump(OutputStream os, byte[] b)
  {
    dump(os, b, 0, b.length, null);
  }
  
  public static final void dump(byte[] b, int o, int l, String p)
  {
    synchronized(System.err)
    {
      dump(System.err, b, o, l, p);
    }
  }
  
  public static final void dump(byte[] b)
  {
    dump(b, 0, b.length, null);
  }
    
  public static final void dump(OutputStream out, byte[] b, int o, int l, String linePrefix)
  {
    try
    {
      if (b == null) Lang.ASSERT_NOT_NULL(b, "b");
      final StringBuffer sb = new StringBuffer(6*l+100)
        .append((linePrefix == null ? "" : linePrefix+", ")+
	"length: "+l+" ("+toHexString0x(l)+")\n\""+new String(b, o, l)+"\",\n      ");
      for (int p = 0; p < 16; ++p)
        sb.append('0').append(HEX[p & 15]).append(' ');
      sb.append("\n");
      for (int i = 0; i < l; i += 16)
      {
        sb.append(HEX[(i >> 12)&15])
          .append(HEX[(i >>  8)&15])
          .append(HEX[(i >>  4)&15])
          .append(HEX[(i      )&15]).append(": ");

        for (int p = 0; p < 16; ++p)
        {
          if (i+p < l)
          {
            byte bb = b[o+i+p];
            sb.append(HEX[(bb >> 4)&15]).append(HEX[bb & 15]).append(' ');
          }
          else
            sb.append("   ");
        }
        sb.append(" \"");
        int m = Math.min(l - i, 16);
        for (int p = 0; p < m; ++p)
        {
          byte bb = b[o+i+p];
          if (bb > 31)
            sb.append((char) bb);
          else
            sb.append('.');
        }
        sb.append("\"\n");
      }
      out.write(sb.toString().getBytes("ASCII"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  private static final char[] HEX = "0123456789ABCDEF".toCharArray();
  
  public static final String byte2hex(byte i)
  {
    return new StringBuffer(2)
      .append(HEX[(i >>  4)&15])
      .append(HEX[(i      )&15])
      .toString();
  }
  
  public static final String word2hex(int i)
  {
    return new StringBuffer(4)
      .append(HEX[(i >> 12)&15])
      .append(HEX[(i >>  8)&15])
      .append(HEX[(i >>  4)&15])
      .append(HEX[(i      )&15])
      .toString();
  }
    
public static final void dump_(byte[] data, String linePrefix)
{
  dump_(data, linePrefix, data.length);
}

public static final void dump_(byte[] data, String linePrefix, int lenToPrint)
{
  dump(data, 0, lenToPrint, linePrefix);
}

public static final void dump_(OutputStream os, byte[] data, String linePrefix)
{
  dump_(os, data, linePrefix, data.length);
}

public static final void dump_(OutputStream os, byte[] data, String linePrefix, int lenToPrint)
{
  dump_(os, data, 0, lenToPrint, linePrefix);
}

public static final void dump_(OutputStream os, byte[] data, int ofs, int len, String linePrefix)
{
  dump(os, data, ofs, len, linePrefix);
}

private static String pad_(String str, int resultingStringLength)
{
  StringBuffer buf = new StringBuffer();
  while (buf.length() < resultingStringLength - str.length())
  buf.append("0");
  return buf.append(str).toString().toLowerCase();
}

public static String toHexString(int word)
{
  return pad_(Integer.toHexString(word & 0xffff), 4);
}

public static String toHexString(long n, long mask, int resultingStringLength)
{
  return pad_(Long.toHexString(n & mask), resultingStringLength);
}

public static String toHexString0x(int word)
{
  return "0x"+pad_(Integer.toHexString(word & 0xffff), 4);
}

public static String toHexString0x(long n, long mask, int resultingDigitStringLengthWithout0x)
{
  return "0x" + toHexString(n, mask, resultingDigitStringLengthWithout0x);
}

}