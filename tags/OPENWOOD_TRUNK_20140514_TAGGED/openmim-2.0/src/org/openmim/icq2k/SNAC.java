package org.openmim.icq2k;

import java.io.*;

import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.joe.*;

/**
Represents a SNAC header (SNAC family, subtype,
request id, and flags) and subsequent SNAC data for
a packet that can be transmitted over the FLAP channel 2.

@see #send(Aim_conn_t).
*/
public final class SNAC
{
  private final byte flapChannel;
  public final int family;
  public final int subtype;
  public final long requestId;
  public final byte flag1;
  public final byte flag2;
  private final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

public SNAC(byte flapChannel, int family, int subtype) throws IOException
{
  this.flapChannel = flapChannel;
  this.family = family;
  this.subtype = subtype;
  this.flag1 = (byte) 0;
  this.flag2 = (byte) 0;
  this.requestId = 0;
  addWord(family);
  addWord(subtype);
  addByte(flag1);
  addByte(flag2);
  addDWord(requestId);
}
/**
 * SNAC constructor comment.
 */
public SNAC(int family, int subtype) throws IOException
{
  this(family, subtype, 0, 0);
}
/**
 * SNAC constructor comment.
 */
public SNAC(int family, int subtype, int flag1, int flag2) throws IOException
{
  this(family, subtype, 0, 0, 0);
}
/**
 * SNAC constructor comment.
 */
public SNAC(int family, int subtype, int flag1, int flag2, long requestId) throws IOException
{
  this.flapChannel = (byte) 2;
  this.family = family;
  this.subtype = subtype;
  this.flag1 = (byte) (flag1 & 0xff);
  this.flag2 = (byte) (flag2 & 0xff);
  this.requestId = requestId;
  addWord(family);
  addWord(subtype);
  addByte(flag1);
  addByte(flag2);
  addDWord(requestId);
}
/**
 * SNAC constructor comment.
 */
public void addByte(int byt) throws IOException
{
  byteArray.write((byte) ((byt) & 0xff));
}
/**
 * SNAC constructor comment.
 */
public void addByteArray(byte[] b) throws IOException
{
  if (b.length > 0)
    byteArray.write(b);
}
/**
 big endian
*/
public void addDWord(long x) throws IOException
{
  addWord(((int) ((x >> 16) & 0xffff)));
  addWord(((int) ((x) & 0xffff)));
}

/**
 little endian
*/
public void addDWordLE(long x) throws IOException
{
  addWordLE(((int) ((x) & 0xffff)));
  addWordLE(((int) ((x >> 16) & 0xffff)));
}

/**
 big endian
*/
public void addWord(int x) throws IOException
{
  byteArray.write((byte) ((x >> 8) & 0xff));
  byteArray.write((byte) ((x) & 0xff));
}

/**
 little endian
*/
public void addWordLE(int x) throws IOException
{
  byteArray.write((byte) ((x) & 0xff));
  byteArray.write((byte) ((x >> 8) & 0xff));
}

public void addIcqUin(long uin) throws IOException
{
  addDWordLE(uin);
}

/**
 * SNAC constructor comment.
 */
public void addStringPrependedWithByteLength(String s) throws IOException
{
  byte[] ba = Session.string2byteArray(s);
  addByte(ba.length);
  addByteArray(ba);
}
/**
 * SNAC constructor comment.
 */
public void addStringRaw(String s) throws IOException
{
  byte[] ba = Session.string2byteArray(s);
  addByteArray(ba);
}
/**
 * SNAC constructor comment.
 */
public void addTlv(int type, byte[] value) throws IOException
{
  addWord(type);
  addWord(value.length);
  addByteArray(value);
}
/**
 * SNAC constructor comment.
 */
public void addTlv(int type, String value) throws IOException
{
  addTlv(type, Session.string2byteArray(value));
}
/**
 * SNAC constructor comment.
 */
public void addTlvByte(int type, int byt) throws IOException
{
  addWord(type);
  addWord(1);
  addWord(byt);
}
/**
 * SNAC constructor comment.
 */
public void addTlvDWord(int type, long dword) throws IOException
{
  addWord(type);
  addWord(4);
  addDWord(dword);
}
/**
 * SNAC constructor comment.
 */
public void addTlvWord(int type, int word) throws IOException
{
  addWord(type);
  addWord(2);
  addWord(word);
}

/**
  LNTS is a WORD preceded NTS: the word is little-endian and indicates
  the length of the NTS string (null char included).
  NTS is a Null Terminated String.
*/
public void addLNTS(byte[] msgBytes_noTrailingZero) throws IOException
{
  if ((msgBytes_noTrailingZero) == null) Lang.ASSERT_NOT_NULL(msgBytes_noTrailingZero, "msgBytes_noTrailingZero");
  addWordLE(msgBytes_noTrailingZero.length + 1);
  addByteArray(msgBytes_noTrailingZero);
  addByte(0);
}

/**
  LNTS is a WORD preceded NTS: the word is little-endian and indicates
  the length of the NTS string (null char included).
  NTS is a Null Terminated String.
*/
public void addLNTS(String msg) throws IOException
{
  if ((msg) == null) Lang.ASSERT_NOT_NULL(msg, "msg");
  addLNTS(Session.string2byteArray(msg));
}


  /** Creates a channel 2 FLAP packet with this SNAC packet inside,
      and sends it asynchronously using a given connection conn.
  */
  public final void send(Aim_conn_t conn, AsyncOperation op) throws MessagingNetworkException, IOException
  {
    conn.send(getFlapBody(), 2, op);
  }

  /** Creates a channel 2 FLAP packet with this SNAC packet inside.
      FLAP seq number is taken from the conn.
  */
  public final byte[] getFlapBody() throws MessagingNetworkException, IOException
  {
    return byteArray.toByteArray();
  }
}
