package org.openmim.icq2k;

import java.io.*;
import java.net.*;
import java.util.*;

import org.openmim.icq.utils.*;
import org.openmim.icq.util.*;
import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkAdapter;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.messaging_network.MessagingNetworkListener;
import org.openmim.stuff.AsyncOperation;
import org.openmim.stuff.AsyncOperationQueue;
import org.openmim.stuff.Defines;
import org.openmim.stuff.StatusUtilMim;
import org.openmim.stuff.UserDetails;
import org.openmim.stuff.UserDetailsImpl;
import org.openmim.stuff.UserSearchResults;
import org.openmim.wrapper.*;

/**
  Represents a ICQ2KMessagingNetwork session.
  <p>
  <b>Lifecycle.</b>
  <ul>
  <li><b>Creation.</b>
    Session is created while login().
  <li><b>Destruction.</b> Session is destroyed in these cases:
    <ul>
    <li>while logout(),
    <li>while setClientStatus(STATUS_OFFLINE),
    <li>when the IO error occured in the user's TCP/IP connection, or
    <li>when the user is disconnected by the server (socket closed).
    </ul>
  </ul>
  <p>
  Historically, the class contains 95% of the
  plugin functionality.
  <p>
  @see ICQ2KMessagingNetwork
*/

public class Session
implements  SNACFamilies,
            AIMConstants,
            ICQ2KConstants,
            org.openmim.wrapper.MessagingNetworkSession
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Session.class.getName());

  public static String getVersionString()
  {
    String rev = "$Revision: 1.117 $";
    rev = rev.substring("$Revision: ".length(), rev.length() - 2);

    String cvsTag = "$Name:  $";
    cvsTag = cvsTag.substring("$Name: ".length(), cvsTag.length() - 2);

    rev += ", cvs tag: '"+cvsTag+"'";

    return rev;
  }

  private static final boolean FETCH_OFFLINE_MESSAGES = true;
  private static final boolean REQUEST_RATEINFO = false;
  private MessagingNetworkException lastError = null;

  /** Fetched from server at the end of login() */
  private boolean authorizationRequired;

  private boolean shuttingDown = false;
  private final Object shuttingDownLock = new Object();

  /**
    Session's login id (icq number).
    */
  private final String loginId;

  /**
    ICQ number.
    */
  private final int uin;

  /**
    Session's contact list items and their current status.
  */
  private Hashtable contactListUinInt2cli;

  /**
    Should never return null for contact list entries.
    Returning non-null means that the ContactListItem is in the contact list
    (possibly, with the offline status).
    Returning null means that the ContactListItem is not in the contact list.
  */
  public MessagingNetworkContactListItem getContactListItem(String dstLoginId)
  {
    return getContactListItem(new Integer(dstLoginId));
  }

  private ContactListItem getContactListItem(Integer dstLoginId)
  {
    return (ContactListItem) contactListUinInt2cli.get(dstLoginId);
  }

  private int getContactListItemStatus(Integer dstLoginId)
  {
    ContactListItem cli = getContactListItem(dstLoginId);
    if (cli == null) return StatusUtil.OSCAR_STATUS_OFFLINE;
    else return cli.getStatusOscar();
  }

  public Enumeration getContactListItems()
  {
    return contactListUinInt2cli.elements();
  }

  /**
    Session's current status.
  */
  private int status_Oscar = StatusUtil.OSCAR_STATUS_OFFLINE;

  /**
    Connection to BOS (Basic OSCAR service) server.
    Is null when connection closed.
    */
  private Aim_conn_t bosconn;
  private OutgoingMessageQueue messageQueue;
  /**
    Connection to authorization/login server.
    Is null when the auth connection is closed.
    */
  private Aim_conn_t authconn;
  public boolean isConnUp(boolean isBosConn) { synchronized (logoutLock) { if (!isBosConn) return authconn != null; else return bosconn != null; } }

  /**
    Lock object that is used to synchronize login and logout operations.
    */
  private final Object logoutLock = new Object();

  /**
    Next snac request id.
    */
  private long snac_nextid = 1; //C unsigned long

  /**
    Prefix for debugging packet dumps.
    */
  public static final String DBG_DUMP_PREFIX = "icq: ";

  /**
    Creates new instance.
    <p>
    Properties config file is loaded in the static initializer.
  */
  public Session(PluginContext ctx, String loginId)
  throws MessagingNetworkException
  {
    this.ctx = ctx;
    asyncOpQueue = new AsyncOperationQueue(ICQ2KMessagingNetwork.REQPARAM_SESSION_ASYNCOP_QUEUE_SIZE);
    uin = IcqUinUtil.parseUin(loginId, "loginId", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED);
    this.loginId = loginId;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("new Session");
  }

  final PluginContext ctx;

  /*
  public Session()
  {
    uin = 0;
    this.loginId = null;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("new ANONYMOUS Session, uin: 0");

  }
  */

  /**
    Adds.  Throws MessagingNetworkException if not connected.
  */
  public void addToContactList(
    final AsyncOperations.OpAddContactListItem op,
    String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("addToContactList()");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_ADD_TO_CONTACT_LIST_WHILE_OFFLINE);
      //icq2k uin can be 10000...2147483646
      //Integer.MAX_VALUE==2147483647
      //2147483646
      //11111111111
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if (this.uin == dstUin)
        throw new MessagingNetworkException(
          "cannot add yourself to a contact list",
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_CANNOT_ADD_YOURSELF_TO_CONTACT_LIST);
      Integer dstUin_ = new Integer(dstUin);
      if (getContactListItem(dstUin_) != null)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(dstLoginId + " already in a contact list, ignored");
      }
      else
      {
        contactListUinInt2cli.put(dstUin_, makeContactListItem(dstLoginId));
        send_addSingleUserToContactList(op, dstLoginId, ctx);
      }
      op.success();
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  private ContactListItem makeContactListItem(String dstLoginId)
  {
    return new ContactListItem(this, dstLoginId);
  }

  /**
    Creates a new connection to the specified
    host of specified type.

    type: Type of connection to create
    destination: Host to connect to (in "host:port" or "host" syntax)
  */
  private Aim_conn_t aim_newconn(final int type, final String destination, FlapConsumer ac, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    int port = AIMConstants.CFG_AIM_LOGIN_PORT_DEFAULT;
    String host = null;
    if (StringUtil.isNullOrTrimmedEmpty(destination)) Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(destination, "destination");

    int colon = destination.lastIndexOf(':');
    host = destination;
    if (colon > -1)
    {
      try
      {
        port = Integer.parseInt(destination.substring(colon + 1));
        host = destination.substring(0, colon);
      }
      catch (NumberFormatException ex)
      {
        throw new MessagingNetworkException(
          "invalid port value, must be integer: \"" + destination + "\"",
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, //it is unknown where is this port value specified
          MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      }
    }
    return aim_newconn(type, InetAddress.getByName(host), port, ac, ctx);
  }
  /**
    Creates a new connection to the specified host of specified type.

    type: Type of connection to create
    destination: Host to connect to (in "host:port" or "host" syntax)
  */
  private Aim_conn_t aim_newconn(int type, InetAddress host, int port, FlapConsumer ac, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    return ctx.getICQ2KMessagingNetwork().getResourceManager().createTCPConnection(this, host, port, ac, ctx);
  }

  private int aim_putsnac(byte[] buf, int family, int subtype, int flags, long snacid)
  {
    int offset = 0; //0
    offset += aimutil_put16(buf, offset, family & 0xffff); //2
    offset += aimutil_put16(buf, offset, subtype & 0xffff); //4
    offset += aimutil_put16(buf, offset, flags & 0xffff); //6
    offset += aimutil_put32(buf, offset, snacid); //10
    return offset; //10
  }

  /**
    Writes a TLV with a two-byte integer value portion.
    buf: Destination buffer
    t: TLV type
    v: Value
  */
  private int aim_puttlv_16(byte[] buf, int offset, final int t, final int v)
  {
    int delta = 0;
    delta += aimutil_put16(buf, offset + delta, t);
    delta += aimutil_put16(buf, offset + delta, 2);
    delta += aimutil_put16(buf, offset + delta, v);
    return delta;
  }
  /**
    Writes a TLV with a two-byte integer value portion.
    buf: Destination buffer
    t: TLV type
    v: Value
  */
  private int aim_puttlv_32(byte[] buf, int offset, final int t, final long v)
  {
    int delta = 0;
    delta += aimutil_put16(buf, offset + delta, t);
    delta += aimutil_put16(buf, offset + delta, 2);
    delta += aimutil_put32(buf, offset + delta, v);
    return delta;
  }
  /**
    aim_puttlv_str - Write a string TLV.
    buf: Destination buffer
    tlv_type: TLV type
    l: Length of string
    s: String to write

    Writes a TLV with a string value portion.  (Only the first @l
    bytes of the passed string will be written, which should not
    include the terminating null.)
  */
  private int aim_puttlv_str_(byte buf[], final int offset, int tlv_type, byte[] stringAsByteArray)
  {
    int delta = 0;
    delta += aimutil_put16(buf, offset + delta, tlv_type);
    delta += aimutil_put16(buf, offset + delta, stringAsByteArray.length);
    System.arraycopy(stringAsByteArray, 0, buf, offset + delta, stringAsByteArray.length);
    delta += stringAsByteArray.length;
    return delta;
  }
  /**
    Reads and parses a series of TLV patterns from a data buffer.
    data: Input buffer
  */
  private Aim_tlvlist_t aim_readtlvchain(byte[] data, int ofs, int len)
  throws MessagingNetworkException
  {
    return new Aim_tlvlist_t(this, data, ofs, len);
  }

  /** big endian */
  public static int aimutil_get16(byte[] buf, int offset)
  {
    int val;
    val = (buf[offset] << 8) & 0xff00;
    val |= (buf[++offset]) & 0xff;
    return val;
  }

  /** little endian */
  public static int aimutil_get16_le(byte[] buf, int offset)
  {
    int val;
    val = (buf[offset])         & 0x00ff;
    val |= (buf[++offset] << 8) & 0xff00;
    return val;
  }

  /** big endian */
  public static long aimutil_get32(byte[] buf, int offset)
  {
    long val;
    val = (buf[  offset] << 24) & 0xff000000;
    val|= (buf[++offset] << 16) & 0x00ff0000;
    val|= (buf[++offset] <<  8) & 0x0000ff00;
    val|= (buf[++offset]      ) & 0x000000ff;
    return val;
  }

  /** little endian */
  int aimutil_getIcqUin(byte[] buf, int offset)
  {
    return (int) aimutil_get32_le(buf, offset);
  }

  /** little endian */
  long aimutil_get32_le(byte[] buf, int offset)
  {
    //be 5c 94 01  //uin  //"BE 5C 94 01" aka 0x01945CBE == 26500286
    long val;
    val = (buf[  offset]      ) & 0x000000ff;
    val|= (buf[++offset] <<  8) & 0x0000ff00;
    val|= (buf[++offset] << 16) & 0x00ff0000;
    val|= (buf[++offset] << 24) & 0xff000000;
    return val;
  }


  public static int aimutil_get8(byte[] buf, int offset)
  {
    return ((int)(buf[offset])) & 0xff;
  }

  public byte[] aimutil_getByteArray(byte[] buf, int offset)
  throws MessagingNetworkException
  {
    return aimutil_getByteArray(buf, offset+1, aimutil_get8(buf, offset));
  }

  public byte[] aimutil_getByteArray(byte[] buf, int offset, int length)
  throws MessagingNetworkException
  {
    byte[] b = AllocUtil.createByteArray(this, length);
    System.arraycopy(buf, offset, b, 0, b.length);
    return b;
  }
  public String aimutil_getString(byte[] buf, int offset) throws MessagingNetworkException
  {
    byte[] b = aimutil_getByteArray(buf, offset);
    return byteArray2string(b);
  }
  //i += aimutil_put16(newrx.data, i, 0x01);
  private int aimutil_put16(byte[] buf, int offset, int a)
  {
    buf[offset] = (byte) ((a >> 8) & 0xff);
    buf[++offset] = (byte) (a & 0xff);
    return 2;
  }
  //i += aimutil_put16(newrx.data, i, 0x01);
  private int aimutil_put32(byte[] buf, int offset, long a)
  {
    buf[offset] = (byte) ((a >> 24) & 0xff);
    buf[++offset] = (byte) ((a >> 16) & 0xff);
    buf[++offset] = (byte) ((a >> 8) & 0xff);
    buf[++offset] = (byte) (a & 0xff);
    return 4;
  }
  //i += aimutil_put8(newrx.data, i, 0x01);
  private int aimutil_put8(byte[] buf, int offset, int a)
  {
    buf[offset] = (byte) (a & 0xff);
    return 1;
  }
  //i += aimutil_putstr(newrx.data, i, "0", 1);
  private int aimutil_putstr(byte[] data, int offset, String s)
  {
    if ((s) == null) Lang.ASSERT_NOT_NULL(s, "s");
    return aimutil_putstr(data, offset, s, s.length());
  }
  //i += aimutil_putstr(newrx.data, i, "0", 1);
  private int aimutil_putstr(byte[] data, int offset, String s, int length)
  {
    if ((s) == null) Lang.ASSERT_NOT_NULL(s, "s");
    byte[] src = string2byteArray(s);
    System.arraycopy(src, 0, data, offset, length);
    return length;
  }

  public int aimutil_putByteArray(byte[] data, int offset, byte[] ba)
  {
    return aimutil_putByteArray(data, offset, ba, 0, ba.length);
  }

  public int aimutil_putByteArray(byte[] data, int offset, byte[] ba, int baOfs, int baLen)
  {
    System.arraycopy(ba, baOfs, data, offset, baLen);
    return baLen;
  }

  private void ASSERT_LOGGED_IN(int endUserOperationErrorCode) throws MessagingNetworkException
  {
    MLang.EXPECT(
      status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE,
      "Please login first.  Status cannot be offline to perform this operation.",
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      endUserOperationErrorCode);
  }

  /*
    Converts a byte array into string.
  */
  public static String byteArray2string(byte[] ba)
  {
    return byteArray2string(ba, 0, ba.length);
  }

  private static String fileEncoding = System.getProperty("file.encoding");

  /*
    Converts portion of a byte array into string.
  */
  public static String byteArray2string(byte[] ba, int ofs, int len)
  {
    if (len == 0) return "";

    //changed by Antich to test encoding problems - TEMPORARY SOLUTION!!
    try
    {
      //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("Constructing message using encoding " + fileEncoding+", msgBytes.length="+len);
      return new String(ba, ofs, len, fileEncoding);
    }
    catch (Exception e)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("WARNING: constructing message using default enc, cannot use file.encoding = "+StringUtil.quote(fileEncoding), e);
      return new String(ba, ofs, len);
    }
  }

  /*
    Converts byte array into string using some rules.
    Also, replaces 0xFE string delimiters to CRLFs.
  */
  public static String byteArray2stringConvertXfes(byte[] ba)
  {
    StringBuffer sb = new StringBuffer(ba.length + 10);
    int chunkStart = 0;
    int chunkLen = 0;
    for (int i = 0; i < ba.length; i++)
    {
      byte b = ba[i];
      if (b != (byte) 0xFE)
      {
        chunkLen++;
        continue;
      }
      else
      {
        sb.append(byteArray2string(ba, chunkStart, chunkLen));
        i++;
        for (; i < ba.length; i++)
        {
          if (ba[i] != 0xFE)
          {
            i--;
            break;
          }
        }
        chunkStart = i + 1;
        chunkLen = 0;
        if (i >= ba.length)
          break;
        sb.append("\r\n");
      }
    }
    if (chunkLen > 0)
      sb.append(byteArray2string(ba, chunkStart, chunkLen));
    return sb.toString();
  }

  /*
  Closes a connection if it is non-null, just returns otherwise.
  Never throws Exceptions.
  */
  private static void closeConnectionIfNotNull(Aim_conn_t conn)
  {
    if (conn != null)
      conn.closeSocket();
  }

  public final void handleException(Throwable tr, PluginContext ctx)
  throws MessagingNetworkException
  {
    handleExceptionR(tr, "unk", ctx, false);
  }

  public final void handleExceptionR(Throwable tr, PluginContext ctx, boolean reconnecting)
  throws MessagingNetworkException
  {
    handleExceptionR(tr, "unk", ctx, reconnecting);
  }

  public final void handleExceptionR(Throwable tr, String processName, PluginContext ctx, boolean reconnecting)
  throws MessagingNetworkException
  {
    handleException(tr, processName, ctx, true, reconnecting);
  }

  public final void handleException(Throwable tr, String processName, PluginContext ctx)
  throws MessagingNetworkException
  {
    handleException(tr, processName, ctx, true, false);
  }

  public final void eatException(Throwable tr, PluginContext ctx)
  {
    eatException(tr, "unk", ctx);
  }

  public final void eatException(Throwable tr)
  {
    eatException(tr, "unk", ctx);
  }

  public final void eatException(Throwable tr, String processName, PluginContext ctx)
  {
    try
    {
      handleException(tr, processName, ctx, false, false);
    }
    catch (Exception ex)
    {
      if (CAT.isEnabledFor(org.apache.log4j.Level.ERROR))
      {
        CAT.error("unexpected ex ignored", ex);
        CAT.error("ex ignored", new AssertException("this point should never be reached"));
      }
    }
  }

  public final void handleException(
    Throwable tr, String processName, PluginContext ctx, 
    boolean convertAndRethrow, boolean reconnecting)
  throws MessagingNetworkException
  {
    MessagingNetworkException ex = getLastError();
    if (ex == null)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("le0", tr);

      if (tr instanceof MessagingNetworkException)
      {
        ex = (MessagingNetworkException) tr;
      }
      else
      if (tr instanceof UnknownHostException)
        ex = new MessagingNetworkException(
          "DNS error resolving "+tr.getMessage(),
          MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR,
          MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR);
      else
      if (tr instanceof InterruptedIOException)
        ex = new MessagingNetworkException(
          "mim admin restarts the mim server.",
          MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
          MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
      else
      if (tr instanceof InterruptedException)
        ex = new MessagingNetworkException(
          "mim admin restarts the mim server.",
          MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
          MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
      else
      if (tr instanceof IOException)
        ex = new MessagingNetworkException(
          "I/O error: "+tr.getMessage(),
          MessagingNetworkException.CATEGORY_LOGGED_OFF_DUE_TO_NETWORK_ERROR,
          MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_NETWORK_ERROR);
      else
      if (tr instanceof AssertException)
        ex = new MessagingNetworkException(
          "bug found: "+tr.getMessage(),
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          MessagingNetworkException.ENDUSER_MIM_BUG);
      else
      if (tr instanceof ArrayIndexOutOfBoundsException)
        ex = new MessagingNetworkException(
          tr.toString(),
          MessagingNetworkException.CATEGORY_STILL_CONNECTED,
          MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
      else
        ex = new MessagingNetworkException(
          "unknown error: "+tr.getMessage(),
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          MessagingNetworkException.ENDUSER_MIM_BUG_UNKNOWN_ERROR);

      if (ex.getLogger() != MessagingNetworkException.CATEGORY_STILL_CONNECTED)
      {
        setLastError(ex);
        try
        {
          MessagingNetworkException ex1 = convertErrorAfterShutdown(ex, ctx, reconnecting);
          if (ex != ex1)
          {
            ex = ex1;
            if (ex1.getLogger() != ex1.CATEGORY_STILL_CONNECTED) 
              replaceLastError(ex1);
            else
              replaceLastError(null);
          }
        }
        catch (Exception excc)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex ignored", excc);
        }        
        shutdown(ctx, ex.getLogger(), ex.getMessage(), ex.getEndUserReasonCode());
      }
      
      Stats.report(ex.getEndUserReasonCode());
    }

    if (convertAndRethrow)
    {
      throw new MessagingNetworkException(ex.getMessage(), ex.getLogger(), ex.getEndUserReasonCode());
    }
  }

  protected MessagingNetworkException convertErrorAfterShutdown(final MessagingNetworkException ex, PluginContext ctx, boolean reconnecting)
  throws Exception
  {
    return ex;
  }
  
  private final Object lastErrorLock = new Object();

  private final void replaceLastError(MessagingNetworkException mex)
  {
    synchronized (lastErrorLock)
    {
      lastError = mex;
    }
  }

  MessagingNetworkException getLastError()
  {
    synchronized (lastErrorLock)
    {
      return lastError;
    }
  }

  void setLastError(MessagingNetworkException newEx)
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = newEx;
    }
  }

  void setLastError(String exceptionMessage, int reasonLogger, int endUserReasonCode)
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = new MessagingNetworkException(exceptionMessage, reasonLogger, endUserReasonCode);
    }
  }

  void throwLastErrorOrCreateThrowLastError(String exceptionMessage, int reasonLogger, int endUserReasonCode)
  throws MessagingNetworkException
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null)
        lastError = new MessagingNetworkException(exceptionMessage, reasonLogger, endUserReasonCode);
      throw cloneLastError();
    }
  }

  public MessagingNetworkException cloneLastError()
  {
    synchronized (lastErrorLock)
    {
      if (lastError == null) Lang.ASSERT_NOT_NULL(lastError, "lastError");
      return new MessagingNetworkException(lastError.getMessage(), lastError.getLogger(), lastError.getEndUserReasonCode());
    }
  }

  void throwLastError()
  throws MessagingNetworkException
  {
    throw cloneLastError();
  }

  /*
   * Converts text into ASCII7 HTML.
   *
   * Also converts any non-7bit chars to UNICODE HTML entities of the form "&#2026;".
   */
  private static byte[] encodeAsAscii7Html(final String text)
  {
    return string2byteArray(text);
    //try
    //{
      //if (text.length() == 0)
        //return AllocUtil.createByteArray(this, ] {};
      //java.io.ByteArrayOutputStream bas = new java.io.ByteArrayOutputStream(text.length());
      ////bas.write(IM_HTML_PREFIX);
      //char[] chars = text.toCharArray();
      //for (int i = 0; i < chars.length; i++)
      //{
        //final int unicodeChar = chars[i];
        //if ((unicodeChar & 0xff80) == 0)
        //{
          //bas.write((byte) unicodeChar);
          //continue;
        //}
        //bas.write(IM_HTML_UNICODE_ENTITY_PREFIX); //&#
        //bas.write(HexUtil.HEX_DIGITS_BYTES[ (unicodeChar) & 15]);
        //bas.write(HexUtil.HEX_DIGITS_BYTES[ (unicodeChar << 8) & 15]);
        //bas.write(HexUtil.HEX_DIGITS_BYTES[ (unicodeChar << 16) & 15]);
        //bas.write(HexUtil.HEX_DIGITS_BYTES[ (unicodeChar << 24) & 15]);
        //bas.write((byte) ';');
      //}
      ////bas.write(IM_HTML_POSTFIX);
      //return bas.toByteArray();
    //}
    //catch (java.io.IOException ex)
    //{
    //if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(ex.getMessage(), ex);
      //return text.getBytes();
    //}
  }

  private void fireSystemNotice(String errorMessage, PluginContext context)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireSystemNotice: " + errorMessage);
    try
    {
      context.getICQ2KMessagingNetwork().fireMessageReceived("0", loginId, errorMessage);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing system notice to messaging network listeners", ex);
    }
  }

  private void fireAuthRequestReceived(String from, String reason, PluginContext context)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming authrequest: "+reason);
    try
    {
      context.getICQ2KMessagingNetwork().fireAuthRequestReceived(from, loginId, reason);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing AuthRequestReceived to messaging network listeners", ex);
    }
  }

  private void fireAuthReplyReceived(String from, boolean grant, PluginContext context)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming auth reply; granted: "+grant);
    try
    {
      context.getICQ2KMessagingNetwork().fireAuthReplyReceived(from, loginId, grant);
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("error while firing AuthReplyReceived to messaging network listeners", ex);
    }
  }

  private static boolean isSenderValid(String uin, String infoToBeIgnored)
  {
    try
    {
      IcqUinUtil.parseUin(uin, "sender uin", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      return true;
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("invalid sender uin, "+infoToBeIgnored+" ignored", ex);
      return false;
    }
  }

  private void fireMessageReceived(String senderLoginId, String msg, PluginContext ctx)
  {
    if (isSenderValid(senderLoginId, "incoming message="+StringUtil.toPrintableString(msg)))
      ctx.getICQ2KMessagingNetwork().fireMessageReceived(senderLoginId, loginId, msg);
  }

  protected static void checkInterrupted() throws InterruptedException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedException();
  }

  private Aim_conn_t getAuthConnNotNull() throws MessagingNetworkException, InterruptedException, IOException
  {
    checkInterrupted();

    Aim_conn_t conn;
    synchronized (logoutLock)
    {
      conn = authconn;
    }
    if (conn == null)
      throw new IOException("connection closed");
    return conn;
  }
  private Aim_conn_t getBosConnNotNull() throws MessagingNetworkException, IOException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedIOException();

    Aim_conn_t conn;
    synchronized (logoutLock)
    {
      conn = bosconn;
    }
    if (conn == null)
      throw new IOException("connection closed");
    return conn;
  }
  public int getContactStatus_Oscar(String dstLoginId, PluginContext ctx) throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("getContactStatus_Oscar");
    try
    {
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      return getContactListItemStatus(dstUin_);
    }
    catch (Exception ex)
    {
      handleException(ex, "getContactStatus", ctx);
      return StatusUtil.OSCAR_STATUS_OFFLINE;
    }
  }
  public final java.lang.String getLoginId() {
    return loginId;
  }
  public int getStatus_Oscar()
  {
    return status_Oscar;
  }
  private byte[] getStringAsByteArr(byte[] buf, int offset) throws MessagingNetworkException
  {
    int len = aimutil_get16(buf, offset);
    MLang.EXPECT(
      len < 32*1024, "len must be less than 32*1024, but it is "+len,
      MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    byte[] str = AllocUtil.createByteArray(this, len);
    System.arraycopy(buf, offset+2, str, 0, len);
    return buf;
  }

  private void handleAuthorizationError_alwaysThrowEx(
        byte[] flapDataField,
        int ofs,
        int len,
        int flapChannel,
        PluginContext ctx)
  throws MessagingNetworkException
  {
    if (flapChannel != 4)

      throw new MessagingNetworkException(
        "auth response on unknown channel=" + flapChannel,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);

    int errCode = -1;
    String errUrl = null;
    String screenName = null;
    Aim_tlvlist_t tlvlist = new Aim_tlvlist_t(this, flapDataField, ofs, len);
    screenName = tlvlist.getNthTlvOfTypeAsString(1, 1);
    if (tlvlist.getNthTlvOfType(1, 8) != null)
      errCode = tlvlist.getNthTlvOfTypeAs16Bit(1, 8);
    errUrl = tlvlist.getNthTlvOfTypeAsString(1, 4);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rx_handleAuthorizationError: channel 4: shutting down, errUrl=\"" + errUrl + "\", errCode=" + errCode);
    String reason = "Unknown authorization error";
    int endUserReasonCode;
    switch (errCode)
    {
      case 0x0001 :
        reason = "Invalid or not registered ICQ number.";
        endUserReasonCode = MessagingNetworkException.ENDUSER_CANNOT_LOGIN_INVALID_OR_NOT_REGISTERED_UIN;
        break;
      case 0x0005 :
        reason = "Invalid password or ICQ number.";
        endUserReasonCode = MessagingNetworkException.ENDUSER_CANNOT_LOGIN_INVALID_PASSWORD_OR_UIN;
        break;
      case 0x0018 :
        reason = "ICQ server reports: connect rate exceeded.  You are reconnecting too often.  Try to connect again 10 or 20 minutes later.";
        endUserReasonCode = MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_10_OR_20_MINUTES_LATER;
        break;
      case 0x001D :
        reason = "ICQ server reports: connect rate exceeded.  Try to connect again 1 or 2 minutes later.";
        endUserReasonCode = MessagingNetworkException.ENDUSER_CANNOT_LOGIN_CONNECT_RATE_EXCEEDED_TRY_1_OR_2_MINUTES_LATER;
        break;
      case 0x0014:
        Stats.STAT_UNKAUTH14_ERRORS.inc();
      default :
        reason += ": channel=4, errCode=" + HexUtil.toHexString0x(errCode);
        endUserReasonCode = MessagingNetworkException.ENDUSER_CANNOT_LOGIN_MESSAGING_SERVER_REPORTS_UNKNOWN_ERROR;
        if (errUrl != null)
          reason += ", " + errUrl;
        break;
    }
    throw new MessagingNetworkException(
      "Cannot login: " + reason,
      MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
      endUserReasonCode);
  }

  //bb02
  public void handleRx(final Aim_conn_t conn, final int flapChannel, final byte[] flapBody, final int ofs, final int len)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("session.handleRx");
    
    if (conn.isBosConn())
    {
      if (flapBody == null) Lang.ASSERT_NOT_NULL(flapBody, "flapBody");
      if (Defines.DEBUG && CAT.isDebugEnabled())
        CAT.debug("handleRx: input: " +
          (len >= 4 ? //
            "snac f/s=" + HexUtil.toHexString0x(aimutil_get16(flapBody, ofs+0)) + "/" + 
                          HexUtil.toHexString0x(aimutil_get16(flapBody, ofs+2)) + ", "
            : ""
          ) + "flapdata len=" + HexUtil.toHexString0x(len) + " ("+len+"), flapchan: " + flapChannel);

      if (flapChannel == 4)
      {
        rx_handle_negchan_middle(flapChannel, flapBody, ofs, len, ctx);
        return;
      }
      if (flapChannel == 2)
      {
        if (len < 10) MLang.EXPECT(len >= 10, "len >= 10",
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
              MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
        int family = aimutil_get16(flapBody, ofs+0);
        int subtype = aimutil_get16(flapBody, ofs+2);
        int flag1 = aimutil_get8(flapBody, ofs+4);
        int flag2 = aimutil_get8(flapBody, ofs+5);
        long snacid = aimutil_get32(flapBody, ofs+6);
        final SNAC snac = new SNAC(family, subtype, flag1, flag2, snacid);
        if (rxdispatch_BIG_SWITCH(flapBody, ofs, len, snac))
          return;
      }
    }

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("handleRx: parsingflapp.");
    conn.parseFlapPackets(flapChannel, flapBody, ofs, len);
  }

  private boolean isShuttingDown()
  {
    synchronized (shuttingDownLock)
    {
      return shuttingDown;
    }
  }

  public String registerLoginId(final String password, PluginContext ctx)
  throws MessagingNetworkException
  {
    /*
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("registerLoginId() start");
      synchronized (shuttingDownLock)
      {
        shuttingDown = false;
      }
      synchronized (lastErrorLock)
      {
        lastError = null;
      }

      MLang.EXPECT_NOT_NULL_NOR_EMPTY(
        password, "password",
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY);
      MLang.EXPECT(
        password.length() < 256, "password.length() must be < 256, but it is "+password.length(),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        MessagingNetworkException.ENDUSER_PASSWORD_IS_TOO_LONG);

      synchronized (logoutLock)
      {
        if ((password) == null) Lang.ASSERT_NOT_NULL(password, "password");
        snac_nextid = 1;
      }

      DataConsumer loginAC = new AbstractConsumer2()
      {
        public byte[] getBuffer();
        public void consume();
      }

      Aim_ conn_t conn = aim_ newconn(AIMConstants.AIM_CONN_TYPE_AUTH, ICQ2KMessagingNetwork.getLoginServerInetAddress(), ICQ2KMessagingNetwork.getLoginServerPort(), loginAC, ctx);
      {
        Aim_ conn_t oldConn;
        synchronized (logoutLock)
        {
          oldConn = authconn;
          authconn = conn;
        }
        if (oldConn != null)
        {
          oldConn.closeSocket();
        }
      }
      byte[] flap_header = new byte[6];
      getAuthConnNotNull().read(flap_header);
      FLAPHeader flap = new FLAPHeader(flap_header);

      byte[] flap_data_field = AllocUtil.createByteArray(this, flap.data_field_length);
      getAuthConnNotNull().read(flap_data_field);

      MLang.EXPECT(
        flap_data_field.length >= 4, "hello length must be >= 4, but it is " + flap_data_field.length,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      long hello = aimutil_get32(flap_data_field, 0);
      MLang.EXPECT_EQUAL(
        hello, 1, "flap version", "00 00 00 01",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("server hello received, sending our hello");
      byte[] flapHeader = new FLAPHeader(1, conn.getNextOutputStreamSeqnum(), 4).byteArray;
      ByteArrayOutputStream bas  = new ByteArrayOutputStream(6+4);
      bas.write(flapHeader);
      bas.write(new byte[] {0, 0, 0, 1});
      bas.flush();

      conn.write(bas.toByteArray());
      bas.close();
      conn.flush();
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("our hello sent, sending setPassword 17/04 packet");

      SNAC p = new SNAC((byte) 4, 0x17,04);
      p.addByteArray(new byte[] {(byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x3B, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x38, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x0, (byte) 0x0, (byte) 0x03, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0xB4, (byte) 0x25, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0xB4, (byte) 0x25, (byte) 0x00, (byte) 0x00, (byte) 0x0, (byte) 0x0, (byte) 0x0} );
      p.addLNTS(password.getBytes());
      p.addByteArray(new byte[] {(byte) 0x03, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0xB4, (byte) 0x25, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCF, (byte) 0x0} );
      p.send(conn);

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setPassword sent, waiting for the 17/05 new uin");

      long stopTime = System.currentTimeMillis() + 10 * 60 * 1000; //10 minutes

      for (;;)
      {
        checkInterrupted();
        shutdownAt(stopTime, "wait for new uin", ctx);

        flap_header = new byte[6];
        conn.read(flap_header);
        int len = flap_header.length;
        flap = new FLAPHeader(flap_header);
        flap_data_field = AllocUtil.createByteArray(this, flap.data_field_length);
        conn.read(flap_data_field);
        len = flap_data_field.length;
        if (len < 4)
          continue;
        int family = aimutil_get16(flap_data_field, 0);
        int subtype = aimutil_get16(flap_data_field, 2);
        if (family != 0x17 && subtype != 5)
          continue;
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("new uin packet received, parsing");
        int newUin = aimutil_getIcqUin(flap_data_field, 0x5c);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("registerLoginId() finished (success): new uin="+newUin);
        return String.valueOf(newUin);
      }
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("registerLoginId() finished (failed)");
      try
      {
        getAuthConnNotNull().closeSocket();
      }
      catch (Exception exx)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("ex while closing socket", exx);
      }
      handleException(ex, "registerLoginId", ctx);
      throw new AssertException("never reached");
    }
    */
    throw new AssertException("not implemented");
  }

  private boolean noIncomingOfflineMessages;

  protected void resetShuttingDown()
  {
    synchronized (shuttingDownLock)
    {
      shuttingDown = false;
    }
    synchronized (lastErrorLock)
    {
      lastError = null;
    }
    resumeAsyncOps();
  }

  private AsyncOperations.OpLogin opLogin;
  private byte[] cookie;
  private FlapConsumer bosFc;
  private String bos_hostport;

  private boolean loggingIn = false;
  
  protected final boolean isLoggingIn()
  {
    synchronized (logoutLock)
    {
      return loggingIn;
    }
  }
  
  final void setLoggingIn(boolean l)
  {
    synchronized (logoutLock)
    {
      loggingIn = l;
    }
  }
  
  protected boolean crAttempted = false;
  
  public void login_Oscar(
       AsyncOperations.OpLogin op,
       final String password,
       String[] contactList_,
       final int status_Oscar,
       boolean calledByReconnector,
       final PluginContext ctx)
  throws MessagingNetworkException
  {
    final String ERRMSG_PREFIX = "Error logging in: ";
    try
    {
      //if we're already logged in, simulate succesful login.
      if (opLogin!=null || this.status_Oscar!=StatusUtil.OSCAR_STATUS_OFFLINE)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) 
          CAT.debug(
            "WARNING: ALREADY LOGGED IN OR LOGGING IN, opLogin: "+opLogin+
            ", status_Oscar: "+StatusUtil.translateStatusOscarToString(this.status_Oscar));
        op.success();
        return;
      }
        
      opLogin = op;
      
      resetShuttingDown();
      if (password == null || password.length() == 0)
        MLang.EXPECT_NOT_NULL_NOR_EMPTY(
          password, "password",
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          MessagingNetworkException.ENDUSER_MIM_BUG_PASSWORD_CANNOT_BE_NULL_NOR_EMPTY);
      if (password.length() >= 256)
        MLang.EXPECT(
          password.length() < 256, "password.length() must be < 256, but it is "+password.length(),
          MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
          MessagingNetworkException.ENDUSER_PASSWORD_IS_TOO_LONG);

      if (!(  status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE  )) throw new AssertException("StatusUtil.OSCAR_STATUS_OFFLINE should never happen as a login_Oscar() argument.");

      if (contactList_ == null)
      {
        contactList_ = new String[] {};
      }
      final String[] contactList = contactList_;

      if (contactListUinInt2cli == null)
      {
        contactListUinInt2cli = new Hashtable(contactList.length);
        for (int i = 0; i < contactList.length; i++)
        {
          String dstLoginId = contactList[i];
          if (dstLoginId == null)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("exception ignored, cl entry ignored", new Exception("contactList["+i+"] is null"));
            continue;
          }
          int dstUin = -1;
          try
          {
            dstUin = IcqUinUtil.parseUin(dstLoginId, "contactList[i]", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
          }
          catch (Exception ex11)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("invalid contact list entry uin ignored, exception ignored", ex11);
            continue;
          }
          if (this.uin == dstUin)
            throw new MessagingNetworkException(
              "cannot login with your own uin in a contact list",
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
              MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST);
          Integer dstUin_ = new Integer(dstUin);
          if (contactListUinInt2cli.put(dstUin_, makeContactListItem(dstLoginId)) != null)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("" + dstLoginId + " already in a contact list, cl entry ignored");
            continue;
          }
        }
      }

      synchronized (logoutLock)
      {
        if ((loginId) == null) Lang.ASSERT_NOT_NULL(loginId, "loginId");
        if ((password) == null) Lang.ASSERT_NOT_NULL(password, "password");
        snac_nextid = 1;
        loggingIn = true;
      }

      setReconnectData(status_Oscar, contactListUinInt2cli);

      FlapConsumer authServerFC = new FlapConsumer()
      {
        private int state = 10;

        public boolean isBosConn()
        {
          return false;
        }

        public void parse(final int flapChannel, final byte[] flapBody, int ofs, int len) throws Exception
        {
            final int datalen = len;

            switch (state)
            {
              case 10:
              {
                if (datalen < 4)
                  MLang.EXPECT(
                    datalen >= 4, "hello length must be >= 4, but it is " + datalen,
                    MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
                    MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
                long hello = aimutil_get32(flapBody, ofs);
                ofs+=4; len-=4;
                if (hello != 1)
                  MLang.EXPECT_EQUAL(
                    hello, 1, "flap version", "00 00 00 01",
                    MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
                    MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("server hello received, sending login request");
                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("entered loginSequence_10sendLogin()"); //, password="+StringUtil.toPrintableString(password));
                if (StringUtil.isNullOrEmpty(password)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(password, "icq password");
                if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
                final byte[] password_b = string2byteArray(password);
                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("login, stage 10: password length = " + password_b.length);
                int xorindex = 0;
                for (int i = 0; i < password_b.length; i++)
                {
                  password_b[i] ^= xor_table[xorindex++];
                  if (xorindex >= xor_table.length)
                    xorindex = 0;
                }
                final byte[] sn = string2byteArray(loginId);

                {
                  byte[] pak = new byte[0x7e + password_b.length - 1 + sn.length - 8];
                  int ofs1 = 0;
                  ofs1 += aimutil_put32(pak, ofs1, 0x00000001);
                  ofs1 += aim_puttlv_str_(pak, ofs1, 1, sn);
                  ofs1 += aim_puttlv_str_(pak, ofs1, 2, password_b);
                  ofs1 += aim_puttlv_str_(pak, ofs1, 3, string2byteArray("ICQ Inc. - Product of ICQ (TM).2000b.4.63.1.3279.85"));
                  final byte[] trail = {//
                    (byte) 0x00, (byte) 0x16,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x01, (byte) 0x0A,//
                    //
                    (byte) 0x00, (byte) 0x17,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x00, (byte) 0x04,//
                    //
                    (byte) 0x00, (byte) 0x18,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x00, (byte) 0x3F,//
                    //
                    (byte) 0x00, (byte) 0x19,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x00, (byte) 0x01,//
                    //
                    (byte) 0x00, (byte) 0x1A,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x0C, (byte) 0xCF,//
                    //
                    (byte) 0x00, (byte) 0x14,//
                    (byte) 0x00, (byte) 0x04,//
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x55,//
                    //
                    (byte) 0x00, (byte) 0x0F,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x65, (byte) 0x6E,// "en"
                    //
                    (byte) 0x00, (byte) 0x0E,//
                    (byte) 0x00, (byte) 0x02,//
                    (byte) 0x75, (byte) 0x73//  "us"
                    };
                  System.arraycopy(trail, 0, pak, ofs1, trail.length);

                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("login/10: sending uin/password packet");

                  state = 20;

                  getAuthConnNotNull().send(pak, 1, opLogin);
                }
                break;
              }

              case 20:
              {
                //flap channel
                if (flapChannel != 4)
                {
                  handleAuthorizationError_alwaysThrowEx(flapBody, ofs, len, flapChannel, ctx);
                  //unreachable code
                  return;
                }

                Aim_tlvlist_t resp = new Aim_tlvlist_t(Session.this, flapBody, ofs, len);
                bos_hostport = resp.getNthTlvOfTypeAsString(1, 5);
                cookie = resp.getNthTlvOfTypeAsByteArray(1, 6);

                if (cookie == null || bos_hostport == null)
                {
                  cookie = null;
                  bos_hostport = null;
                  handleAuthorizationError_alwaysThrowEx(flapBody, ofs, len, flapChannel, ctx);
                }
                
                state = 30;

                opLogin.runNow(
                  new MessagingTask("login/20", opLogin)
                  {
                    public void run() throws Exception
                    {
                      if (Defines.DEBUG && CAT.isDebugEnabled())
                        CAT.debug("aim_send_login(): cookie received, closing auth connection...");
                      {
                        Aim_conn_t oldconn;
                        synchronized (logoutLock)
                        {
                          oldconn = authconn;
                          authconn = null;
                        }
                        closeConnectionIfNotNull(oldconn);
                      }

                      checkShuttingdown();

                      if (Defines.DEBUG && CAT.isDebugEnabled())
                        CAT.debug("aim_send_login(): auth connection closed, connecting to bos...");

                      {
                        Aim_conn_t conn = aim_newconn(AIMConstants.AIM_CONN_TYPE_BOS, bos_hostport, bosFc, ctx);
                        bos_hostport = null;

                        synchronized (shuttingDownLock)
                        {
                          messageQueue = new OutgoingMessageQueue(conn, Session.this, ctx);
                        }

                        Aim_conn_t oldconn;
                        synchronized (logoutLock)
                        {
                          oldconn = bosconn;
                          bosconn = conn;
                        }
                        closeConnectionIfNotNull(oldconn);
                        if (isShuttingDown()) conn.closeSocket();
                      }

                    }
                  }
                );
                break;
              } //end of case
            } //end of switch(state)
            ofs += datalen; len -= datalen;
        } //end of parse()
      }; //end of authServerFC

      bosFc = new FlapConsumer()
      {
        private int state = 25;

        public boolean isBosConn()
        {
          return true;
        }

        public void parse(final int flapChannel, final byte[] flapBody, final int ofs, final int len) throws Exception
        {
            final int datalen = len;

            switch (state)
            {
              case 25:
              {
                checkShuttingdown();

                byte[] xxx = new byte[8];
                int offset = 0;
                offset += aimutil_put16(xxx, offset, 0x0000);
                offset += aimutil_put16(xxx, offset, 0x0001);
                offset += aimutil_put16(xxx, offset, 0x0006);
                offset += aimutil_put16(xxx, offset, cookie.length);

                state = 30;

                ByteArrayOutputStream ba = new ByteArrayOutputStream(8+cookie.length);
                ba.write(xxx);
                ba.write(cookie);
                cookie = null;
                getBosConnNotNull().send(ba.toByteArray(), 1, opLogin);

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("login/30 waiting for serverready");
                break;
              }

              case 30:
              {
                //if (Thread.currentThread().isInterrupted())
                //  throw new InterruptedException();
                //shutdownAt(stopTime, "waitForServerReady", ctx);
                //byte[] flap_header = new byte[6];
                //getBosConnNotNull().read(flap_header);
                //int len = flap_header.length;
                //FLAPHeader flap = new FLAPHeader(flap_header);

                //byte[] flap_data_field = AllocUtil.createByteArray(this, flap.data_field_length);
                //getBosConnNotNull().read(flap_data_field);
                //len = flap_data_field.length;
                if (flapChannel == 4)
                {
                  rx_handleConnectionError(flapBody, ofs, len, ctx);
                  break; //continue waiting //not executed if the error is "logged on from another pc".
                }
                if (flapChannel != 2 && len < 4)
                  break; //continue waiting
                int family = aimutil_get16(flapBody, ofs);
                int subtype = aimutil_get16(flapBody, ofs+2);
                if ((family != 0 && subtype == 1) || (family == 9 && subtype == 9))
                {
                  rx_handleGenericServiceError(flapBody, ofs, len, ctx);
                  break;
                }
                if (family != AIM_CB_FAM_GEN /*1*/|| subtype != AIM_CB_GEN_SERVERREADY /*3*/)
                  break;

                state = 40;

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("exited waitForServerReady, entered login/40 sendContactList()");

                if (status_Oscar == StatusUtil.OSCAR_STATUS_ONLINE)
                  setStatus_Oscar_Internal(opLogin, status_Oscar, false, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
                else
                  setStatus_Oscar_Internal(opLogin, status_Oscar, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);

                synchronized (logoutLock)
                {
                  crAttempted = false;
                }
                
                SNAC p = null;

                if (!FETCH_OFFLINE_MESSAGES)
                {
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fetch offline messages: disabled.");
                }
                else
                {
                  noIncomingOfflineMessages = true;
                }

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 1/0x17, generic service controls/unknown");
                p = new SNAC(0x0001, 0x0017);
                p.addByteArray(new byte[] {
                  00, 01, 00, 03,
                  00, 02, 00, 01,
                  00, 03, 00, 01,
                  00, 0x15, 00, 01,

                  00, 04, 00, 01,
                  00, 06, 00, 01,
                  00, 0x09, 00, 01,
                  00, 0x0A, 00, 01
                });
                //this packet was exactly like in &rq (incl. snac reqids).
                p.send(getBosConnNotNull(), opLogin);

                /*
                ignored: //NADO WAITAT' HERE FOR 1/0x18

                server sends                   // got it, ack to 1,17

                SERVER seq:61B5 size:48 service:01/18 ref:00008580 flags:0000

                00 01 00 03 00 02 00 01 00 03 00 01 00 04 00 01 00 06 00 01
                00 08 00 01 00 09 00 01 00 0A 00 01 00 0B 00 01 00 0C 00 01
                00 13 00 02 00 15 00 01
                */

                if (!REQUEST_RATEINFO)
                {
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                      ("request rate info: disabled.");
                }
                /*
                else
                {
                  rateInfoResponseReceived = false;

                  getBosConnNotNull().addHandler(new Rx HandlerImpl()
                  {
                    public boolean triggered(Session sess, final int flapChannel, final byte[] flapBody, final int ofs, final int len, final SNAC snac)
                    throws IOException, MessagingNetworkException, InterruptedException
                    {
                      getBosConnNotNull().removeHandler(getThis());
                      synchronized (rateResponseWaitLock)
                      {
                        rateInfoResponseReceived = true;
                        rateResponseWaitLock.notify();
                      }
                      return true;
                    }
                  }, 1, 7); //rate info response

                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("login/40 sendContactList(): sending 1/6, rate info request");
                  p = new SNAC(0x0001, 0x0006);
                  p.send(getBosConnNotNull());
                  //this packet was exactly like in &rq (incl. snac reqids).

                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("login/40 sendContactList(): waiting for rate info response...");
                  long stopTime = System.currentTimeMillis() + 1000 * ICQ2KMessagingNetwork.REQPARAM_SERVER_RESPONSE_TIMEOUT1_SECONDS;
                  for (;;)
                  {
                    synchronized (rateResponseWaitLock)
                    {
                      if (rateInfoResponseReceived) break;
                    }
                    shutdownAt(stopTime, "rate info response wait", ctx);
                    t ick(ctx);
                    Thread.currentThread().sleep(10);
                  }

                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("got rate info response, sending 1/8 rate info ack");
                  p = new SNAC(1, 8);
                  p.addByteArray(new byte[] {00, 01, 00, 02, 00, 03, 00, 04, 00, 05});
                  p.send(getBosConnNotNull());
                  //this packet was exactly like in &rq (incl. snac reqids).
                }
                */

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 1/0xe, generic service controls/request my info");
                p = new SNAC(0x0001, 0x000e);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 2/2, location/Request rights information");
                p = new SNAC(0x0002, 0x0002);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 3/2, buddylist/Request rights information");
                p = new SNAC(0x0003, 0x0002);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 4/4, messaging/Request parameter information");
                p = new SNAC(0x0004, 0x0004);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 9/2, bos-specific/Request BOS Rights");
                p = new SNAC(0x0009, 0x0002);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                /*
                ignored:
                //NADO WAIT HERE FOR 1/0xf

                server sends                   //  response to 1,0E

                SERVER seq:61B7 size:92 service:01/0F ref:00000000 flags:0000
                 45454734         _   %                                         ��             ;��m    ;��n
                08 34 35 34 35 34 37 33 34 00 00 00 06 00 01 00 02 00 80 00
                0C 00 25 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                00 0A 00 04 C2 95 E0 03 00 0F 00 04 00 00 00 00 00 02 00 04
                3B A1 D9 6D 00 03 00 04 3B A1 D9 6E


                //NADO WAIT HERE FOR 2/3

                SERVER seq:61B8 size:18 service:02/03 ref:00000000 flags:0000

                00 01 00 02 04 00 00 02 00 02 00 10 00 03 00 02 00 0A


                //NADO WAIT HERE FOR 3/3

                SERVER seq:61B9 size:18 service:03/03 ref:00000000 flags:0000
                     X     �
                00 01 00 02 02 58 00 02 00 02 02 EE 00 03 00 02 02 00


                //NADO WAIT HERE FOR 4/5

                SERVER seq:61BA size:16 service:04/05 ref:00000000 flags:0000
                         � �   �
                00 02 00 00 00 03 02 00 03 E7 03 E7 00 00 03 E8


                //NADO WAIT HERE FOR 0x9/3

                SERVER seq:61BB size:12 service:09/03 ref:00000000 flags:0000
                     �     �
                00 02 00 02 00 A0 00 01 00 02 00 A0
                */

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 4/2, Messaging/Add ICBM parameter");
                p = new SNAC(0x0004, 0x0002);
                p.addByteArray(new byte[] {
                  00, 00,
                  00, 00,
                  00, 03,
                  0x1F, 0x40,
                  03, (byte) 0xE7,
                  03, (byte) 0xE7,
                  00, 00,
                  00, 00
                });
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 2/4, Location Services/Set user information");
                p = new SNAC(0x0002, 0x0004);
                p.addDWord(0x00050020);//TLV(5)
                p.addDWord(0x09461349);//capability1 //direct-message capability?
                p.addDWord(0x4C7F11D1);//capability1 //licq doesn't send the capability1
                p.addDWord(0x82224445);//capability1 //icq2k sends both cap1 and cap2
                p.addDWord(0x53540000);//capability1

                p.addDWord(0x09461344);//capability2
                p.addDWord(0x4C7F11D1);//capability2
                p.addDWord(0x82224445);//capability2
                p.addDWord(0x53540000);//capability2
                p.send(getBosConnNotNull(), opLogin);
                p = null;
                //this packet was exactly like in &rq (incl. snac reqids).

                //send the buddy list and profile (required, even if empty)
                //aim_bos_setbuddylist(sess, command->getBosConnNotNull(), buddies);
                //
                //Add buddy [Source: User_]
                //Adds a number of buddies to your buddy list, causing AIM to send
                //us on/off events for the given users. Len/buddy combinations can be
                //repeated as many times as you have buddies to add.
                //
                //SNAC Information:
                //
                //Family 0x0003
                //SubType 0x0004
                //Flags {0x00, 0x00}
                //
                //--------------------------------------------------------------------------------
                //
                //RAW / Buddy name length (byte)
                //RAW / Buddy name
                java.io.ByteArrayOutputStream ba = new java.io.ByteArrayOutputStream();
                for (int i = 0; i < contactList.length; i++)
                {
                  String s15 = String.valueOf(IcqUinUtil.parseUin(contactList[i], "contactList[i]", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED));
                  byte[] buddyName = string2byteArray(s15);
                  ba.write((byte) buddyName.length);
                  ba.write(buddyName);
                }
                ba.flush();
                byte[] baa = ba.toByteArray();
                ba.close();

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 3/4, send contact list");
                p = new SNAC(0x0003, 0x0004);
                p.addByteArray(baa);
                p.send(getBosConnNotNull(), opLogin);
                ba = null;
                baa = null;

                //this packet was exactly like in &rq (incl. snac reqids).

                boolean webAware = true;

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 1/0x1e setInitialStatus");
                p = new SNAC(0x0001, 0x001e);

                // TLV(6) status-code
                p.addDWord(0x00060004);
                p.addWord(0x1000 | (webAware ? 1 : 0) ); //joxy: i used 0x0003 earlier
                p.addWord(status_Oscar);          //joxy: i used 0x0000 earlier

                // TLV(8) unknown, usually 0000
                p.addDWord(0x00080002);
                p.addWord(0);

                // TLV(C) direct-connection-info
                p.addByteArray(new byte[] {
                  (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x25, (byte) 0x7F, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x07, (byte) 0x58, (byte) 0x58, (byte) 0x58, (byte) 0x58, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                });

                /*
                  joxy: for TLV(C), i used earlier:
                  (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x25,
                  (byte) 0xC0, (byte) 0xA8, (byte) 0x04, (byte) 0x6B,
                  (byte) 0x00, (byte) 0x00, (byte) 0x73, (byte) 0x74,
                  (byte) 0x04, (byte) 0x00, (byte) 0x07, (byte) 0x1D,
                  (byte) 0xAE, (byte) 0xF0, (byte) 0x9E, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x50, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x3B,
                  (byte) 0x17, (byte) 0x6B, (byte) 0x57, (byte) 0x3B,
                  (byte) 0x18, (byte) 0xAC, (byte) 0xE9, (byte) 0x3B,
                  (byte) 0x17, (byte) 0x6B, (byte) 0x4E, (byte) 0x00,
                  (byte) 0x00
                */
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 1/0x11, generic service controls/unknown");
                p = new SNAC(0x0001, 0x0011);
                p.addDWord(0);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 9/7, BOS-specific/Add deny list entries");
                p = new SNAC(0x0009, 0x0007);
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login/40 sendContactList(): sending 1/2, client ready");
                p = new SNAC(0x0001, 0x0002);
                p.addDWord(0x00010003);  p.addDWord(0x0110028A);
                p.addDWord(0x00020001);  p.addDWord(0x0101028A);
                p.addDWord(0x00030001);  p.addDWord(0x0110028A);
                p.addDWord(0x00150001);  p.addDWord(0x0110028A);
                p.addDWord(0x00040001);  p.addDWord(0x0110028A);
                p.addDWord(0x00060001);  p.addDWord(0x0110028A);
                p.addDWord(0x00090001);  p.addDWord(0x0110028A);
                p.addDWord(0x000A0001);  p.addDWord(0x0110028A);
                /*
                p.addByteArray(new byte[] {//
                  (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x06, (byte) 0x86,
                  (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01,
                  (byte) 0x00, (byte) 0x0b, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01
                };
                */
                p.send(getBosConnNotNull(), opLogin);
                //this packet was exactly like in &rq (incl. snac reqids).

                //2A 02 0A B8 00 18 //clt snds
                //00 15 00 02 //sf 5, unknown/unknown
                //00 00
                //00 01 00 02 //snac reqid
                //00 01 00 0A
                //  08
                //    00 BE 5C 94 01 3C 00 02 00
                //  ..............._\".<...

                //2a 02 5da1 0018  0015 0002 0000 00010002  0001 000a 0800 1b374d07 3c 00 02 00
                if (FETCH_OFFLINE_MESSAGES)
                {
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                      ("sending: get offline messages");
                  p = new SNAC(0x0015, 0x0002);
                  //"BE 5C 94 01"  aka  0x01 94 5C BE == 26500286
                  p.addByteArray(new byte[] {//
                    0x00, 0x01, 0x00, 0x0a, 0x08, 0x00, //
                    (byte) (uin & 0xff), //
                    (byte) ((uin >> 8)& 0xff), //
                    (byte) ((uin >> 16)& 0xff), //
                    (byte) ((uin >> 24)& 0xff), //
                    (byte) 0x3C, (byte) 0x00, //
                    0, // <-- joxy: i used 2 in this byte earlier
                    (byte) 0x00});
                  p.send(getBosConnNotNull(), opLogin);
                  p = null;
                  //this packet was exactly like in &rq (incl. snac reqids).
                }

                /*
                  //2a 02 5da2 0033  0015 0002 0000 00020002  0001 0025 2300 1b374d07 d0 07 03 00 98 08 17 00 3c 6b 65 79 3e 44 61 74 61 46 69 6c 65 73 49 50 3c 2f 6b 65 79 3e 00
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("login/40 sendContactList(): sending 0x15/2, reqid 2/2");
                  p = new SNAC(0x0015, 0x0002, 0, 0, 0x00020002);
                  //"BE 5C 94 01"  aka  0x01 94 5C BE == 26500286
                  p.addByteArray(new byte[] {//
                    0x00, 0x01, 0x00,       0x25, 0x23, 0x00, //
                    (byte) (uin & 0xff), //
                    (byte) ((uin >> 8)& 0xff), //
                    (byte) ((uin >> 16)& 0xff), //
                    (byte) ((uin >> 24)& 0xff), //
                    (byte) 0xd0, (byte) 0x07, (byte) 0x03, (byte) 0x00, (byte) 0x98, (byte) 0x08, (byte) 0x17, (byte) 0x00, (byte) 0x3c, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x44, (byte) 0x61, (byte) 0x74, (byte) 0x61, (byte) 0x46, (byte) 0x69, (byte) 0x6c, (byte) 0x65, (byte) 0x73, (byte) 0x49, (byte) 0x50, (byte) 0x3c, (byte) 0x2f, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x00});
                  p.send(getBosConnNotNull());
                  p = null;

                  //2a 02 5da3 0031  0015 0002 0000 00030002  0001 0023 2100 1b374d07 d0 07 04 00 98 08 15 00 3c 6b 65 79 3e 42 61 6e 6e 65 72 73 49 50 3c 2f 6b 65 79 3e 00
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("login/40 sendContactList(): sending 0x15/2, reqid 3/2");
                  p = new SNAC(0x0015, 0x0002, 0, 0, 0x00030002);
                  //"BE 5C 94 01"  aka  0x01 94 5C BE == 26500286
                  p.addByteArray(new byte[] {//
                    0x00, 0x01, 0x00,       0x23, 0x21, 0x00, //
                    (byte) (uin & 0xff), //
                    (byte) ((uin >> 8)& 0xff), //
                    (byte) ((uin >> 16)& 0xff), //
                    (byte) ((uin >> 24)& 0xff), //
                    (byte) 0xd0, (byte) 0x07, (byte) 0x04, (byte) 0x00, (byte) 0x98, (byte) 0x08, (byte) 0x15, (byte) 0x00, (byte) 0x3c, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x42, (byte) 0x61, (byte) 0x6e, (byte) 0x6e, (byte) 0x65, (byte) 0x72, (byte) 0x73, (byte) 0x49, (byte) 0x50, (byte) 0x3c, (byte) 0x2f, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x00});
                  p.send(getBosConnNotNull());
                  p = null;

                  //2a 02 5da4 0032  0015 0002 0000 00040002  0001 0024 2200 1b374d07 d0 07 05 00 98 08 16 00 3c 6b 65 79 3e 43 68 61 6e 6e 65 6c 73 49 50 3c 2f 6b 65 79 3e 00
                  if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                 ("login/40 sendContactList(): sending 0x15/2, reqid 4/2");
                  p = new SNAC(0x0015, 0x0002, 0, 0, 0x00040002);
                  //"BE 5C 94 01"  aka  0x01 94 5C BE == 26500286
                  p.addByteArray(new byte[] {//
                    0x00, 0x01, 0x00,       0x24, 0x22, 0x00, //
                    (byte) (uin & 0xff), //
                    (byte) ((uin >> 8)& 0xff), //
                    (byte) ((uin >> 16)& 0xff), //
                    (byte) ((uin >> 24)& 0xff), //
                    (byte) 0xd0, (byte) 0x07, (byte) 0x05, (byte) 0x00, (byte) 0x98, (byte) 0x08, (byte) 0x16, (byte) 0x00, (byte) 0x3c, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x43, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x6e, (byte) 0x65, (byte) 0x6c, (byte) 0x73, (byte) 0x49, (byte) 0x50, (byte) 0x3c, (byte) 0x2f, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x3e, (byte) 0x00});
                  p.send(getBosConnNotNull());
                  p = null;
                */
                //"offl msg ack" start
                ////2a 02 5da5 0018  0015 0002 0000 00050002  0001 000a 0800 1b374d07 3e 00 06 00
                //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                //login/40 sendContactList(): sending 0x15/2, reqid 5/2");
                //p = new SNAC(0x0015, 0x0002, 0, 0, 0x00050002);
                ////"BE 5C 94 01"  aka  0x01 94 5C BE == 26500286
                //p.addByteArray(new byte[] {//
                //0x00, 0x01, 0x00,     0x0a, 0x08, 0x00, //
                //(byte) (uin & 0xff), //
                //(byte) ((uin >> 8)& 0xff), //
                //(byte) ((uin >> 16)& 0xff), //
                //(byte) ((uin >> 24)& 0xff), //
                //(byte) 0x3e, (byte) 0x00, (byte) 0x06, (byte) 0x00});
                //p.send(getBosConnNotNull());
                //p = null;
                //"offl msg ack" end

                ////AIM/Oscar Protocol Specification:
                ////Section 3: BOS Signon: Set Initial ICBM Parameter
                ////--------------------------------------------------------------------------------
                ////Set ICBM Paramter[Source: User_]
                ////Not sure what this one does, but it can't hurt to send it.
                ////SNAC Information:
                ////Family 0x0004
                ////SubType 0x0002
                ////Flags {0x00, 0x00}
                ////--------------------------------------------------------------------------------
                ////RAW SNAC Header
                ////RAW 0x0000
                ////RAW 0x00000003
                ////RAW 0x1f40
                ////RAW 0x03e7
                ////RAW 0x03e7
                ////RAW 0x0000
                ////RAW 0x0000
                //
                //SNAC p = new SNAC(4, 2);
                //p.addWord(0);
                //p.addDWord(3);
                //p.addWord(0x1f40);
                //p.addWord(0x03e7);
                //p.addWord(0x03e7);
                //p.addWord(0);
                //p.addWord(0);
                //p.send(getBosConnNotNull());

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("exited login/40 sendContactList()");

                //loginSequence_50waitForWwwAolComPacket(ctx);
                getBosConnNotNull().setRateControlOn(true);

                if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                    ("login(): fetching my authorizationRequired flag from server...");

                //long startGetUserDetailsInternal(String srcLoginId, String dstLoginId) adds an interna operation;
                //events from this operationId are never delivered to a external listeners
                //they are delivered to internal listeners only.
                //addInternalMessagingNetworkListener() adds an internal listener.
                //removeInternalMessagingNetworkListener() removes an internal listener.
                MessagingNetworkListener l =
                  new MessagingNetworkAdapter()
                  {
                    public final void getUserDetailsFailed(byte networkId, long operationId,
                      String originalSrcLoginId, String originalDstLoginId,
                      MessagingNetworkException ex)
                    {
                      if (loginId.equals(originalSrcLoginId) && loginId.equals(originalDstLoginId))
                        authReq(false, ex);
                    }

                    public final void getUserDetailsSuccess(byte networkId, long operationId,
                      String originalSrcLoginId, String originalDstLoginId,
                      UserDetails userDetails)
                    {
                      if (loginId.equals(originalSrcLoginId) && loginId.equals(originalDstLoginId))
                      {
                        boolean ar = false;
                        try
                        {
                          ar = userDetails.isAuthorizationRequired();
                        }
                        catch (MessagingNetworkException exar)
                        {
                          ar = false;
                        }
                        authReq(ar, null);
                      }
                    }

                    private final void authReq(boolean authorizationRequired, MessagingNetworkException mex)
                    {
                      Session.this.authorizationRequired = authorizationRequired;

                      if (mex == null
                      || mex.getLogger() == MessagingNetworkException.CATEGORY_STILL_CONNECTED)
                      {
                        if (Defines.DEBUG && CAT.isDebugEnabled())
                          CAT.debug("login(): fetching my authorizationRequired flag done.");

                        setupSessionTasks();

                        opLogin.success();
                        opLogin = null;
                      }
                    }
                  }
                ;

//                try
//                {
//                  ctx.getGal().addForcedAop();
//                  AsyncOperationImpl aop = null;
//                  try
//                  {
//                    Session.this.enqueue(aop = new AsyncOperations.
//                      OpGetUserDetails(loginId, Session.this, false, ctx), l);
//                  }
//                  catch (MessagingNetworkException mex)
//                  {
//                    ctx.getPlug().handleEarlyException(aop, mex);
//                    throw mex;
//                  }
//                }
//                catch (MessagingNetworkException mex1)
//                {
//                  l.getUserDetailsFailed((byte) -1, -1L, loginId, loginId, mex1);
//                }
                l.getUserDetailsFailed((byte) -1, -1L, loginId, loginId, new MessagingNetworkException(MessagingNetworkException.CATEGORY_STILL_CONNECTED,MessagingNetworkException.ENDUSER_NO_ERROR));
                break;
              }

              case 40: //mainloop state case; must deliver events to Rx handlers etc etc etc
              {
                return;//Lang.TODO("mainloop");//###
              }
            } //end of switch(state)
        } //of run()
      }; //end of bosFc

      Aim_conn_t authconn_ = aim_newconn(
          AIMConstants.AIM_CONN_TYPE_AUTH,
          ICQ2KMessagingNetwork.getLoginServerInetAddress(),
          ICQ2KMessagingNetwork.getLoginServerPort(),
          authServerFC,
          ctx);

      {
        Aim_conn_t oldConn;
        synchronized (logoutLock)
        {
          oldConn = authconn;
          authconn = authconn_;
        }
        if (oldConn != null)
        {
          oldConn.closeSocket();
        }
      }
      if (isShuttingDown()) authconn_.closeSocket();
    }
    catch (Exception ex)
    {
      opLogin.fail(ex);
    }
    finally
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("startLogin() finished");
    }
  }

  private void setupSessionTasks()
  {
    if (ctx.getPlug().isKeepAlivesUsed())
    {
      scheduleKeepalives();
    }
  }

  private void scheduleKeepalives()
  {
    runAt(
      System.currentTimeMillis() + ctx.getPlug().getKeepAlivesIntervalMillis(),

      new MessagingTask("keepalives", Session.this)
      {
        public void run() throws Exception
        {
          sendKeepAlive();
          scheduleKeepalives();
        }
      }
    );
  }

  private final HashSet tasks = new HashSet();

  public final void runNow(PluginContext ctx, MessagingTask task)
  {
    runNow(task);
  }
  
  public final void runNow(MessagingTask task)
  {
    synchronized (tasks)
    {
      tasks.add(task);
    }
    ctx.getScheduler().runNow(task);
  }

  public final void runAt(PluginContext ctx, long time, MessagingTask task)
  {
    runAt(time, task);
  }
  
  public final void runAt(long time, MessagingTask task)
  {
    synchronized (tasks)
    {
      tasks.add(task);
    }
    ctx.getScheduler().runAt(time, task);
  }

  public final void cancel(PluginContext ctx, MessagingTask task)
  {
    cancel(task);
  }
  
  public final void cancel(MessagingTask task)
  {
    synchronized (tasks)
    {
      tasks.remove(task);
    }
    ctx.getScheduler().cancel(task);
  }

  public final void cancelAll(PluginContext ctx)
  {
    synchronized (tasks)
    {
      Iterator it = tasks.iterator();
      while (it.hasNext())
      {
        ctx.getScheduler().cancel((MessagingTask) it.next());
      }
      tasks.clear();
    }
  }

  private final static byte[] xor_table = {//
    (byte) 0xF3, (byte) 0x26, (byte) 0x81, (byte) 0xC4, //
    (byte) 0x39, (byte) 0x86, (byte) 0xDB, (byte) 0x92, //
    (byte) 0x71, (byte) 0xA3, (byte) 0xB9, (byte) 0xE6, //
    (byte) 0x53, (byte) 0x7A, (byte) 0x95, (byte) 0x7C  //
  };

  void checkShuttingdown() throws MessagingNetworkException, InterruptedException
  {
    checkInterrupted();

    synchronized (shuttingDownLock)
    {
      if (shuttingDown)
        throwLastError();
    }
  }

  //private final Object rateResponseWaitLock = new Object();
  //private boolean rateInfoResponseReceived;

  private void sendDeleteOfflineMessages(
    final AsyncOperations.OpLogin opLogin,
    PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
        ("sending: delete offline messages");
    SNAC ack = new SNAC(0x15, 2);
    ack.addByteArray(new byte[] {00, 01, 00, 0x0A, 8, 00});
    ack.addDWordLE(this.uin);
    ack.addDWord(0x3e000100); // <-- joxy: here, i used 0x3e000600 earlier
    ack.send(getBosConnNotNull(), opLogin);
    //this packet was exactly like in &rq (incl. snac reqids).
  }

  /*
  private boolean wwwAolComPacketReceived;

  private void loginSequence_50waitForWwwAolComPacket(PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
        ("entered loginSequence_50waitForWwwAolComPacket");
    this.wwwAolComPacketReceived = false;
    long stopTime = System.currentTimeMillis() + ctx.getICQ2KMessagingNetwork().getServerResponseTimeoutMillis();

    getBosConnNotNull().addHandler(new Rx HandlerImpl()
      {
        public boolean triggered(Session sess, final int flapChannel, final byte[] flapBody, final int ofs, final int len, final SNAC snac)
        throws IOException, MessagingNetworkException, InterruptedException
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                ("loginSequence_50waitForWwwAolComPacket(): wwwAolComPacket received, exiting login");
          Session.this.wwwAolComPacketReceived = true;
          getBosConnNotNull().removeHandler(getThis());
          return true;
        }
      }, 1, 0x13);

    while (!wwwAolComPacketReceived)
    {
      shutdownAt(stopTime, "http://www.aol.com packet wait", ctx);
      t ick(ctx);
      Thread.currentThread().sleep(10);
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
        ("exited loginSequence_50waitForWwwAolComPacket");
  }
  */

  void logout(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
    //    ("logout: "+reasonMessage);
    //ASSERT_LOGGED_IN();
    shutdown(ctx, reasonLogger, reasonMessage, endUserReasonCode);
  }

  /*
    Parses the incoming message (ICBM) packet.
  */
  private void parseMessage(final byte[] flapBody, final int ofs, final int len, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled())
      CAT.debug("parseMessage() start");
    final int datalen = len - 10;
    int ofs1 = 10;
    if (datalen >= 20) MLang.EXPECT(datalen >= 20, "datalen must be >= 20", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    byte[] msgCookie = aimutil_getByteArray(flapBody, ofs+ofs1, 8);
    ofs1 += 8; //0x12 (18)
    final int msgFormat = aimutil_get16(flapBody, ofs+ofs1);
    ofs1 += 2;
    if (Defines.DEBUG && CAT.isDebugEnabled())
      CAT.debug("parseMessage(): msgFormat=" + LogUtil.toString_Hex0xAndDec(msgFormat) +
      ", datalen=" + LogUtil.toString_Hex0xAndDec(datalen));
    if (msgFormat != 1 && msgFormat != 2 && msgFormat != 4)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled())
        CAT.debug("parseMessage(): msg format is unknown: msgFmt=" + msgFormat + ", incoming message ignored");
      return;
    }
    String senderLoginId = aimutil_getString(flapBody, ofs+20);
    ofs1 += 1 + aimutil_get8(flapBody, ofs+20) + 2; //+2: skip WORD warning level
    final int headerTlvCount = aimutil_get16(flapBody, ofs+ofs1);
    ofs1 += 2;
    Aim_tlvlist_t tlvlist = aim_readtlvchain(flapBody, ofs+ofs1, len-ofs1);
    int type = (msgFormat == 1 ? 2 : 5);
    byte[] msgBlock = tlvlist.skipNtlvsGetTlvOfTypeAsByteArray(headerTlvCount, type);
    //skipNtlvsGetTlvOfTypeAsByteArray(int n, int type)
    if (msgBlock == null)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
          ("parseMessage(): headerCount="+headerTlvCount+", type=" + type + ", msgBlock=" + msgBlock);
    }
    else
    {
      if (Defines.DEBUG && CAT.isDebugEnabled())
        CAT.debug("parseMessage(): headerCount="+headerTlvCount+", type=" + type + ", msgBlock.len=" +
          LogUtil.toString_Hex0xAndDec(msgBlock.length));
      switch (msgFormat)
      {
        case 2 : //msg format == 2
        {
          // subformat 0:
          //
          //      00: 00 00
          //      02: C0 F2 1C 00  1E 1D 00 00 //msg cookie (2nd time)
          //      0a: 09 46 13 49  4C 7F 11 D1
          //      12: 82 22 44 45  53 54 00 00
          //      1a: 00 0A 00 02  00 01 00 0F
          //      22: 00 00 27  11
          //          00 5B //some length
          //      28:
          //    28+00:  1B 00 07 00  00 00 00 00
          //            00 00 00 00  00 00 00 00
          //    28+10:  00 00 00 00  00 00 03 00
          //            00 00 00 F7  FF 0E 00 F7
          //    28+20:  FF 00 00 00  00 00 00 00
          //            00 00 00 00
          //    28+2C:  00 01 //msgkind: 0001 is textmsg, 0004 is url msg, 000C is auth msg, 0013 is contacts msg
          //            00 00
          //    (28+30:)
          //  (58:)
          //  --
          //      58: 00
          //      59: 01 //msgKind
          //      5a: 00 //msgflags
          //      5b: 1E 00 //lnts
          //              00: 32 30 30 30  62 20 62 65
          //          74 61 20 76  2E 34 2E 35
          //              10: 36 0D 0A 62  75 69 6C 64
          //          20 33 32 36  34 "2000b beta v.4.56\r\nbuild 3264" //msgtext
          //          (1d:)
          //      00 80 80 00 00 FF FF FF 00
          //--
          //
          // subformat 1:
          //  00 01   //msg format 2, subformat 1 ?
          //  f7 1b 4a 00  2a 74 00 00 //mcookie
          //  09 46 13 49  4c 7f 11 d1 //const
          //  82 22 44 45  53 54 00 00 //const
          //  00 0b 00 02
          //  00 01 //msgkind?

          if (msgBlock.length < 0x5b)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled())
              CAT.debug("parseMessage(): msgblock length < 0x5b, incoming message ignored.");
            return;
          }
          int msgSubFormat = aimutil_get16(msgBlock, 0);
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
              ("parseMessage(): msgSubFormat=" + LogUtil.toString_Hex0xAndDec(msgSubFormat));
          if (msgSubFormat != 0)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled())
              CAT.debug("parseMessage(): msg subformat != 0, incoming message ignored.");
            return;
          }
          //byte enc = (byte) aimutil_get8(msgBlock, 0x58);

          /*
          server sends                      // incoming message
          00 04 00 07  00 00  95 e1 2b d9
          67 4d 3b 09 63 6b 00 00 //??B
          00 02
          08 3x 3x 3x 3x 3x 3x 3x 3x
          00 00
          00 05
          00 01 00 02  00 50
          00 06 00 04  20 03 00 05
          00 0f 00 04  00 02 28 3f
          00 02 00 04  3b b0 90 2b
          00 03 00 04  3b b0 90 2b
          00 05 00 43
            00 00 //??A
            67 4d 3b 09 63 6b 00 00 //same as ??B
            09 46 13 44 4c 7f 11 d1 82 22 44 45 53 54 00 00 //capability1

            00 0a 00 02  00 01 //??C
            00 0f 00 00
            27 11 00 1b  7d 26 36 04  d1 ad 1e 8e
                         44 7a
                         00 00 04 22  54 00 00
                         44 7a
                         00 00 07 00  01 00 00 00
          */
          int msgKind = aimutil_get8(msgBlock, 0x55);
          int msgFlags = aimutil_get8(msgBlock, 0x56);
          if (Defines.DEBUG && CAT.isDebugEnabled()) 
            CAT.debug("parseMessage(): msgKind=" + LogUtil.toString_Hex0xAndDec(msgKind)+
                      ", msgFlags=" + LogUtil.toString_Hex0xAndDec(msgFlags));

          if (handleMsgReceived_LNTS(senderLoginId, msgBlock, 0x5b, msgKind, msgFlags, ctx))
          {
            //send msg ack
            //--
            ////Msgack start:

            //00 04 00 0B
            //00 00
            //00 00 00 0B //snac req id = 0xb
            //C0 F2 1C 00  1E 1D 00 00 //msg cookie of acked msg
            //00 02  //channel id of acked msg
            //08 33 31 32 34 38 30 35 39 //sender of acked msg //31248059
            //        00 03
            //
            //        1B 00 07 00  00 00 00 00
            //        00 00 00 00  00 00 00 00
            //        00 00 00 00  00 00 03 00
            //        00 00 00 F7  FF 0E 00 F7
            //        FF 00 00 00  00 00 00 00
            //        00 00 00 00  00 01
            //  --
            //        00 00 00 00  00 01 00 00
            //        00 00 00 00  FF FF FF FF
            ////msgack end.
            //--
            ////Acked msg msgBlock, offset 28:
            //    00: 1B 00 07 00  00 00 00 00
            //        00 00 00 00  00 00 00 00
            //    10: 00 00 00 00  00 00 03 00
            //        00 00 00 F7  FF 0E 00 F7
            //    20: FF 00 00 00  00 00 00 00
            //        00 00 00 00  00 01 00 00
            //      (30:)
            //  (58:)
            SNAC msgAck = new SNAC(4, 0xb, 0, 0, 0xb);
            msgAck.addByteArray(msgCookie);
            msgAck.addWord(msgFormat);
            msgAck.addStringPrependedWithByteLength(senderLoginId);
            msgAck.addWord(3);
            byte[] block28 = aimutil_getByteArray(msgBlock, 0x28, 0x30-2);
            msgAck.addByteArray(block28);
            block28 = null;
            msgAck.addByteArray(new byte[] {00, 00, 00, 00, 00, 01, 00, 00, 00, 00, 00, 00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                ("parseMessage(): sending msgAck");
            msgAck.send(getBosConnNotNull(), null);
          }
          break;
        }
        case 1 : //msg format == 1
        {
          //   05 01
          //   00 01 //!!!
          //      01
          //   01 01
          //   00 08
          //      00 00 00 00
          //      68 75 68 3f         //"huh?"
          //--
          //
          //   05 01
          //   00 04 //!!!
          //      01 01 01 02
          //   01 01
          //   00 1c
          //      00 00 //enc: 0000 is "ASCII", 0002 is Unicode bib endian "UnicodeBig"
          //      00 00 //ignored 0000 when ASCII, FFFF when UnicodeBig
          //      74 79 70   65 20 6d 73 - 67 20 61 6e   64 20 70 72   "type ms g and pr e"
          //      65 73 73 20   65 6e 74 65 - 72   "ess ente r"
          //--
          int msgOfs = 2;
          msgOfs += 2 + aimutil_get16(msgBlock, msgOfs);
          msgOfs += 2;

          //final block length
          msgOfs += 2;

          //flag1 = aimutil_get16(msgBlock);
          msgOfs += 2;
          //flag2 = aimutil_get16(msgBlock);
          msgOfs += 2;
          MLang.EXPECT(
            msgOfs < msgBlock.length, "Incoming protocol violation: msgOfs > msgBlock.length", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
          byte enc = (byte) aimutil_get8(msgBlock, msgOfs - 3);
          byte[] msgBytes = aimutil_getByteArray(msgBlock, msgOfs, msgBlock.length - msgOfs);
          String msg = getMsgText(msgBytes, enc);
          fireMessageReceived(senderLoginId, msg, ctx);
          break;
        }
        case 4 : //msg format == 4
        {
          //incoming auth request
          //
          //00 06 00 04 / 00 03 00 00 //? //sometimes 00 13
          //[...]
          //00 05 00 2F
          //(msgBlock starts here:)
          //  39 97 B5 03 //sender uin
          //  0C //sometimes "06"  //01 is textmsg, 04 is url msg, 0C & 06 is authreq msg, 13 is contacts msg
          //  00 27
          //     00
          //     6D 75 6B FE  50 6F 6C 69   6E 61 FE 42  65 6C 6F 73
          //     74 6F 74 73  6B 79 FE 6D   75 7A 6B 40  6D 61 69 6C
          //     2E 72 75 FE  31 FE
          //  00
          //--
          //
          //incoming URL message
          //
          //00 06 00 04 / 00 03 00 13
          //[...]
          //00 05 00 3c /
          //(msgBlock starts here:)
          //.  be 5c 94 01  //sender uin  //"BE 5C 94 01" aka 0x01945CBE == 26500286
          //.  04  //msg kind: 0x01 is textmsg, 04 is url msg, 0C & 06 is authreq msg, 13 is contacts msg
          //.  00 34  //msg text length
          //.    00
          //.    69 6f 70 fe "iop."
          //.    66 69 6c    "fil e"
          //.    65 3a 2f 2f   2f 53 3a 2f  73 6f 66 74   77 61 72 65   "e:///S:/ software /"
          //.    2f 6a 61 76   61 2f 6a 64  6b 31 2e 32   2f 64 6f 63   "/java/jd k1.2/doc s"
          //.    73 2f 69 6e   64 65 78 2e  68 74 6d 6c   "s/index. html"
          //.  00
          //--
          //
          //also incoming contacts messages
          //
          if (msgBlock.length < 7)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
                  ("parseMessage(): msgblock length < 7, incoming message ignored.");
            return;
          }

          //msg kind: 0x01 is textmsg, 04 is url msg, 0C & 06 is authreq msg, 13 is contacts msg
          int msgKind = aimutil_get8(msgBlock, 4);
          int msgFlags = aimutil_get8(msgBlock, 5);
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug
              ("parseMessage(): msgKind=" + LogUtil.toString_Hex0xAndDec(msgKind));
          if (Defines.DEBUG && CAT.isDebugEnabled())
            Log4jUtil.dump(CAT, "", msgBlock, "icq msgblock: ");
          handleMsgReceived_LNTS(senderLoginId, msgBlock, 6, msgKind, msgFlags, ctx);
          break;
        }
      }
    }
  }

  private void fireYouAreAddedToContactList(String senderLoginId, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    fireMessageReceived(senderLoginId, "You are added to a contact list.", ctx);
  }

  private static String getMsgText(byte[] data, byte encoding)
  {
    if (encoding == 0x02)
    {
      try
      {
        return new String(data, "UnicodeBig");
      }
      catch (Exception ex)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("cannot parse UnicodeBig encoding, reverted to default enc", ex);
      }
    }
    //encoding == 0x00 is "sender-machine-locale-encoding";
    return byteArray2string(data);
  }

  private String[] splitXfes(byte[] ba)
  {
    List chunks = new LinkedList();
    int chunkStart = 0;
    int chunkLen = 0;
    int i;
    for (i = 0; i < ba.length; i++)
    {
      byte b = ba[i];
      if (b != (byte) 0xFE)
      {
        chunkLen++;
        continue;
      }
      else
      {
        String s = byteArray2string(ba, chunkStart, chunkLen);
        chunks.add(s);
        chunkStart = i + 1;
        chunkLen = 0;
      }
    }
    if (chunkLen == 0) chunkStart = 0;
    String s = byteArray2string(ba, chunkStart, chunkLen);
    chunks.add(s);
    return (String[]) chunks.toArray(EMPTY_STRING_ARRAY);
  }

  private void parseIncomingContacts(String senderLoginId, byte[] ba, PluginContext ctx)
  throws MessagingNetworkException
  {
    List uins = new ArrayList(30);
    List nicks = new ArrayList(30);
    int chunkStart = 0;
    int chunkLen = 0;
    int chunkCount = 0;
    String s = null;
    for (int i = 0; i < ba.length; i++)
    {
      byte b = ba[i];
      if (b != (byte) 0xFE)
      {
        chunkLen++;
        continue;
      }
      else
      {
        s = byteArray2string(ba, chunkStart, chunkLen);
        chunkCount++;
        if (chunkCount <= 1) //ignore
        {
          //ignore number of contacts
        }
        else // chunkCount > 1
        {
          if ((chunkCount & 1) == 0) //uin (2, 4, ...)
            uins.add(s);
          else // if ((chunkCount & 1) == 1) //nick (3, 5, ...)
            nicks.add(s);
        }

        i++;
        for (; i < ba.length; i++)
        {
          if (ba[i] != (byte) 0xFE)
          {
            i--;
            break;
          }
        }
        chunkStart = i + 1;
        chunkLen = 0;
        if (i >= ba.length)
          break;
      }
    }
    if ((uins.size()) != (nicks.size())) MLang.EXPECT_EQUAL(uins.size(), nicks.size(), "incoming contacts: uins.size()", "incoming contacts: nicks.size()", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    String[] uinssa  = (String[]) uins.toArray(EMPTY_STRING_ARRAY);
    String[] nickssa = (String[]) nicks.toArray(EMPTY_STRING_ARRAY);

    if (isSenderValid(senderLoginId, "incoming contacts (count="+uins.size()+")"))
      ctx.getICQ2KMessagingNetwork().fireContactsReceived(senderLoginId, loginId, uinssa, nickssa);
  }

  private static String[] EMPTY_STRING_ARRAY = new String[] {};

  /*
    Parses the incoming offline message packet.
  */
  private void parseOfflineMessage(final byte[] t1b, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    /*
    10: UIN     sender uin
    14: WORD    year (LE)
    16: BYTE    month (1..1jan)
    17: BYTE    day
    18: BYTE    hour (GMT time)
    19: BYTE    minutes
    20: BYTE    msg-subtype
    21: BYTE    msg-flags
    22: LNTS    msg
    xx: WORD    0000, present only in single messages
    */
    if (t1b.length >= 24) MLang.EXPECT(t1b.length >= 24, "t1b.length >= 24", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    int ofs = 10;
    int sender = aimutil_getIcqUin(t1b, ofs);
    ofs+= 4;
    //int year = aimutil_get16_le(t1b, ofs);
    ofs+= 2;
    //int month = aimutil_get8(t1b, ofs);
    ofs++;
    //int dayOfMonth = aimutil_get8(t1b, ofs);
    ofs++;
    //int gmt0Hour = aimutil_get8(t1b, ofs);
    ofs++;
    //int minutes = aimutil_get8(t1b, ofs);
    ofs++;
    int msgSubtype = aimutil_get8(t1b, ofs++);
    int msgFlags = aimutil_get8(t1b, ofs++);
    handleMsgReceived_LNTS(sender, t1b, ofs, msgSubtype, msgFlags, ctx);
  }

  /**
    LNTS is a WORD preceded NTS: the word is little-endian and indicates
    the length of the NTS string (null char included).
  */
  private byte[] getLNTS(byte[] buf, int ofs)
  throws MessagingNetworkException, IOException
  {
    if (buf.length >= 3) MLang.EXPECT(buf.length >= 3, "buf.length >= 3", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    int ntslen = aimutil_get16_le(buf, ofs);
    if (buf.length >= 2 + ntslen) MLang.EXPECT(buf.length >= 2 + ntslen, "buf.length >= 2 + ntslen, ntslen="+ntslen, MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
    return aimutil_getByteArray(buf, ofs+2, ntslen-1);
  }

  private boolean handleMsgReceived_LNTS(int senderUin, byte[] buf, int ofs, int msgKind, int msgFlags, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    return handleMsgReceived_LNTS(String.valueOf(senderUin), buf, ofs, msgKind, msgFlags, ctx);
  }

  /** Returns true if it is necessary to send msgAck */

  private boolean handleMsgReceived_LNTS(String senderUin, byte[] buf, int ofs, int msgKind, int msgFlags, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    if (msgKind == MSGKIND_WWPAGER)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming wwpager msg ignored");
      return false;
    }

    byte[] str_b = null;
    String msg;
    switch (msgKind)
    {
      case 7: //authorization denied
        fireAuthReplyReceived(senderUin, false, ctx);
        return false;
      case 8: //you are authorized
        fireAuthReplyReceived(senderUin, true, ctx);
        return false;
      case MSGKIND_ADDED_TO_CONTACT_LIST: //0C
        fireYouAreAddedToContactList(senderUin, ctx);
        return false;
      case 0xe: //emailexpress
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming emailExpress message ignored");
        return false;
      case 1: //plaintext
        str_b = getLNTS(buf, ofs);
        if (str_b.length == 0)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming plaintext msg length: 0, msg ignored");
          return false;
        }
        msg = byteArray2string(str_b);
        fireMessageReceived(senderUin, msg, ctx);
        return true;
      case 4: //url
        str_b = getLNTS(buf, ofs);
        if (str_b.length == 0)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming url msg length: 0, msg ignored");
          return false;
        }
        msg = byteArray2stringConvertXfes(str_b);
        fireMessageReceived(senderUin, msg, ctx);
        return true;
      case 6: //authreq
        str_b = getLNTS(buf, ofs);
        //"nick FE first FE last FE email FE unk-char FE msg"
        //0        1        2        3        4          5
        msg = null;
        String[] xfes = splitXfes(str_b);
        if (xfes.length != 6)
        {
          if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("Server violates the protocol; authreq reason reset to null.", new Exception("xfes length must be == 6, but it is "+xfes.length));
        }
        else
        {
          String nick = xfes[0];
          String fn = xfes[1];
          String ln = xfes[2];
          String email = xfes[3];
          String reason = xfes[5];
          msg = (nick+" "+fn+" "+ln+" "+email).trim();
          if (msg.length()==0)
          {
            if (reason.length()!=0) msg = reason;
          }
          else
          {
            if (reason.length()==0) reason = "No reason given";
            msg = reason + " (" + msg + ")";
          }
        }
        fireAuthRequestReceived(senderUin, msg, ctx);
        return false;
      case 0x13: //contacts
        str_b = getLNTS(buf, ofs);
        if (str_b.length == 0)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("incoming contacts msg length: 0, msg ignored");
          return false;
        }
        parseIncomingContacts(senderUin, str_b, ctx);
        return true;
      default:
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("handleMsgRecv_LNTS: unknown msgKind="+LogUtil.toString_Hex0xAndDec(msgKind)+", ignored");
        return false;
    }

    //offline auth request
    //
    //00 15 00 03   //offline msg? (flags=0 1)
    //00 01  //flags
    //00 01 00 02  //reqid
    //00 01 00 4a
    //  48 00   //length
    //  7f 0c 55 07 //plugin's own uin
    //  41 00 02 00
    //  be 5c 94 01 //sender's uin
    //  d1 07 07 03   ".U.A... .\...... ."
    //  13 38
    //  06 00
    //  32
    //    00
    //    6a 6f 78 79 fe 45   75 67 65 6e   "jo xy.Eugen e"
    //        65 2f 4a 6f   65 fe 46 69 - 6c 69 70 70   6f 76 fe 6a   "e/Joe.Fi lippov.j o"
    //        6f 65 40 69   64 69 73 79 - 73 2e 69 61   65 2e 6e 73   "oe@idisy s.iae.ns k"
    //        6b 2e 73 75   fe 31 fe
    //  00   "k.su.1.."
    //--
    //
    //offline auth req
    //
    //[plugin's uin: 122994538]
    //00 15 00 03
    //00 01
    //00 01 00 02
    //00 01 00 50
    //.  4e 00
    //.  6a bf 54 07
    //.  41 00 02 00
    //.  1b 37 4d 07
    //.  d1 07 07 03
    //.  14 1a
    //.  06 00   //authreq msg
    //.  38
    //.    00
    //.    6a 6f - 78 79 74 65   73 74 20 31   "....8.jo xytest 1 2"
    //         32 fe 6a 6f   78 79 74 65 - 73 74 20 31   32 fe fe 6a   "2.joxyte st 12..j o"
    //         6f 78 79 74   65 73 74 31 - 32 40 6e 6f   77 68 65 72   "oxytest1 2@nowher e"
    //         65 2e 63 6f   6d fe 30 fe - 61 73 75 73   21 00   "e.com.0. asus!."
    //--
    //
    //2A 02 10 B5 00 2E   *..� //srv snds
    //00 15 00 03 //offline msg? (flags=0 1)
    //00 01 //flags
    //00 01 00 02 //reqid
    //00 01 00 20
    //
    //00:  1E 00
    //02:    BE 5C 94 01  //my (receiver) uin
    //06:      41 00 02 00
    //0a:      8E 95 B5 02  //sender uin?
    //0e:    D1 07 07 03
    //12:    08 19
    //14:      01 00 //normal msg
    //16:    06
    //   00
    //18:      61 67 6F 72 61   agora
    //1d:    00 00 00
    //20:
    /*
    final byte[] data = rxframe.data;
    final int datalen = data.length;
    //
    Aim_tlvlist_t tlvlist = aim_readtlvchain(data, 10, datalen - 10);
    //boolean flag_AIM_IMFLAGS_ACK = tlvlist.getNthTlvOfType(1, 3) != null;
    //boolean flag_AIM_IMFLAGS_AWAY = tlvlist.getNthTlvOfType(1, 4) != null;
    byte[] msgBlock = tlvlist.getNthTlvOfTypeAsByteArray(1, 1);
    if (msgBlock != null)
    {
    int msgOfs;
    msgOfs = 0x14;
    int t1 = aimutil_get8(msgBlock, msgOfs++);
    int t0 = aimutil_get8(msgBlock, msgOfs++);
    final int msgType = (t0 << 8) | t1;
    //
    msgOfs = 0x0a; //offset of sender uin
    //0x01 94 5C BE == 26500286
    //  h0 h1 h2 h3
    if (msgOfs + 4 <= msgBlock.length) MLang.EXPECT(msgOfs + 4 <= msgBlock.length, "msgOfs + 4 must be <= msgBlock.length (A)", MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR);
    int h3 = aimutil_get8(msgBlock, msgOfs++);
    int h2 = aimutil_get8(msgBlock, msgOfs++);
    int h1 = aimutil_get8(msgBlock, msgOfs++);
    int h0 = aimutil_get8(msgBlock, msgOfs++);
    final long senderUin = (((((h0 << 8) | h1) << 8) | h2) << 8) | h3;
    //msgType == 0x01 for plain text msg
    if (msgType == 0x06)
    {
      //auth msg
      sendAuthResponsePositive(String.valueOf(senderUin), ctx);
      return;
    }
    else
    {
      msgOfs += 7;
      if (msgOfs + 3 <= msgBlock.length) MLang.EXPECT(msgOfs + 3 <= msgBlock.length, "msgOfs + 3 must be <= msgBlock.length (B)", MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR, MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      int msgLen = aimutil_get16(msgBlock, msgOfs) - 1;
      msgOfs += 3;
      if ((msgLen) <= 0) MLang.EXPECT_POSITIVE(msgLen, "msgLen", MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR, MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      if (msgOfs + msgLen <= msgBlock.length) MLang.EXPECT(msgOfs + msgLen <= msgBlock.length, "msgOfs + msgLen must be <= msgBlock.length (C)", MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR, MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
      byte[] msgBytes = aimutil_getByteArray(msgBlock, msgOfs, msgLen);
      String msg = byteArray2string(msgBytes);
      fireMessageReceived(String.valueOf(senderUin), msg, ctx);
    }
    }
    else
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseOfflineMessage(): msgBlock TLV (type 1, position 1) does not exist, message ignored.");
    */
  }

  public void removeFromContactList(
    final AsyncOperations.OpRemoveContactListItem op,
    String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("removeFromContactList()");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_REMOVE_FROM_CONTACT_LIST_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      if (contactListUinInt2cli.remove(dstUin_) == null)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(dstLoginId + " is not in a contact list, ignored");
      }
      else
        send_removeSingleUserFromContactList(dstLoginId);
      op.success();
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  private void rx_handle_negchan_middle(
      final int flapChannel, final byte[] flapBody, final int ofs, final int len,
      PluginContext context)
  throws MessagingNetworkException
  {
    rx_handleConnectionError(flapBody, ofs, len, context);
  }

  private void rx_handleConnectionError(byte[] command, int ofs, int len, PluginContext context)
  throws MessagingNetworkException
  {
    String msg = "";
    int code = -1;
    Aim_tlvlist_t tlvlist = new Aim_tlvlist_t(this, command, ofs, len);
    if (tlvlist.getNthTlvOfType(1, 0x0009) != null)
      code = tlvlist.getNthTlvOfTypeAs16Bit(1, 0x0009);
    if (tlvlist.getNthTlvOfType(1, 0x000b) != null)
      msg = tlvlist.getNthTlvOfTypeAsString(1, 0x000b);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rx_handleConnectionError: message=\"" + msg + "\", errCode=" + code);
    if (code == 0x0001)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rx_handleConnectionError: errCode=0x0001, used on another computer.");
      throw new MessagingNetworkException(
        "Logged off: your ICQ number is probably used on another computer.",
        MessagingNetworkException.CATEGORY_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER);
    }
  }

  private void rx_handleGenericServiceError(byte[] flapDataField, int ofs, int len, PluginContext ctx)
  {
    //Generic Errors (SNAC family/subtype */1 and 9/9):
    //Family 0x0001: Generic Service Controls
    //Family 0x0002: Location/User_ Information Services
    //Family 0x0003: Buddy List Service
    //Family 0x0004: Messaging Service
    //Family 0x0005: Advertisements Service
    //Family 0x0007: Administrative/Account/Userinfo change Service
    //Family 0x0008: Display Popup Service
    //Family 0x0009: BOS-specific
    //Family 0x0009 subtype 0x0009: Server BOS error
    //Family 0x000A: User_ Lookup/Search Service
    //Family 0x000B: Statistics/Reports Service
    //Family 0x000C: Translate Service
    //Family 0x000D: Chatrooms Navigation
    //Family 0x000E: Chatrooms Service
    //
    if (!(  len >= 4  ))
      throw new AssertException("data length must be >= 4, but it is " + len);
    int snacFamily = aimutil_get16(flapDataField, ofs);
    int snacSubtype = aimutil_get16(flapDataField, ofs+2);
    int errCode = -1;
    if (len >= 12)
    {
      errCode = aimutil_get16(flapDataField, ofs+10);
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rx_handleGenericServiceError: f/s=" +
        HexUtil.toHexString0x(snacFamily) + "/" +
        HexUtil.toHexString0x(snacSubtype)+", errCode="+
        HexUtil.toHexString0x(errCode));
    String service = "Unknown service ("+HexUtil.toHexString0x(snacFamily)+")";
    switch (snacFamily)
    {
      case 0x0001 :
        service = "Generic Service Controls";
        break;
      case 0x0002 :
        service = "Location/UserInfo Services";
        break;
      case 0x0003 :
        service = "Buddy List Service";
        break;
      case 0x0004 :
        service = "Messaging Service";
        break;
      case 0x0005 :
        service = "Advert Service";
        break;
      case 0x0007 :
        service = "Admin Service";
        break;
      case 0x0008 :
        service = "Display Popup Service";
        break;
      case 0x0009 :
        if (snacSubtype != 0x0009)
          service = "BOS-specific";
        else
          service = "BOS 009";
        break;
      case 0x000A :
        service = "User_ Search Service";
        break;
      case 0x000B :
        service = "Stats/Reports Service";
        break;
      case 0x000C :
        service = "Translate Service";
        break;
      case 0x000D :
        service = "ChatNav Service";
        break;
      case 0x000E :
        service = "ChatRooms Service";
        break;
    }

    String errmsg = "Error: generic ICQ " + service + " error.";

    if (snacFamily == 3) //Buddy List Service
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("//ignored: "+errmsg);
    else
    {
      if (snacFamily == 4 && errCode == 4) //generic messaging error, after "send_notify_added_to_cotact_list".
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("//ignored: "+errmsg);
      else
      {
        if (snacFamily == 0x15) //wpfull search errors
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("//ignored: "+errmsg+" (wpfull search errors?)");
        else
          fireSystemNotice(errmsg, ctx);
      }
    }
  }
  /**
  Returns true if and only if the rxframe was handled.
  */
  private boolean rxdispatch_BIG_SWITCH(
    final byte[] flapBody, final int ofs11, final int len11,
    final SNAC snac)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    //final Aim_conn_t conn
    final int family = snac.family;
    final int subtype = snac.subtype;
    final int flag0 = 0xff & (int) snac.flag1;
    //CAT.debug("@@@0: "+HexUtil.toHexString0x(flag0));
    final int flag1 = 0xff & (int) snac.flag2;
    //CAT.debug("@@@1: "+HexUtil.toHexString0x(flag1));
    final int flags = (flag0 << 8) | flag1;
    //CAT.debug("snacFlags: "+HexUtil.toHexString0x(flags));
    final long requestId = snac.requestId;
    final byte[] snacData = AllocUtil.createByteArray(this, len11 - 10);
    System.arraycopy(flapBody, ofs11+10, snacData, 0, snacData.length);
    SNAC p = null;

    boolean handled = false;

    switch (family)
    {
      //case 0x0009 :
      //switch (subtype)
      //{
      //case 0x0003 : //OPTIONAL
      ////bos.c::rights
      //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rxdispatch_switch: BOS_RIGHTS");
      //////faimtest_bosrights
      ////ret = userfunc(sess, rx, maxpermits, maxdenies);
      ////dvprintf("faimtest: BOS rights: Max permit = %d / Max deny = %d\n", maxpermits, maxdenies);
      //aim_bos_clientready(conn);
      //return true;
      //break;
      //case AIM_CB_FAM_ACK:
      //switch (subtype)
      //{
      //case AIM_CB_ACK_ACK: //"NULL" is in libfaim.
      ////NULL; return true;
      //break;
      //}
      //break;

      case 1 : //AIM_CB_FAM_GEN
        switch (subtype)
        {
          case 0x0018 :
            //faimtest_hostversions; return true;
            break;
          case AIM_CB_GEN_SERVERREADY : //REQUIRED
            break;
            //see loginSequence_20waitForServerReady()
          case 0x0007 :
            break;
          case AIM_CB_GEN_REDIRECT :
            //faimtest_handleredirect; return true;
            break;
          case AIM_CB_GEN_MOTD :
            //faimtest_parse_motd; return true;
            break;
          case AIM_CB_GEN_RATECHANGE :
            //faimtest_parse_ratechange; return true;
            break;
          case AIM_CB_GEN_EVIL :
            //faimtest_parse_evilnotify; return true;
            break;
          case 0x1f: //memrequest
          {
            if (Defines.DEBUG && CAT.isDebugEnabled())
              CAT.debug("login/40 sendContactList(): memrequest 1/0x1f received, sending memreq reply 1/0x20");
            SNAC p1 = new SNAC(1, 0x20, 0, 0, snac_nextid++);
            p1.addByteArray(new byte[] {(byte) 0x00, (byte) 0x10, (byte) 0xd4, (byte) 0x1d, (byte) 0x8c, (byte) 0xd9, (byte) 0x8f, (byte) 0x00, (byte) 0xb2, (byte) 0x04, (byte) 0xe9, (byte) 0x80, (byte) 0x09, (byte) 0x98, (byte) 0xec, (byte) 0xf8, (byte) 0x42, (byte) 0x7e});
            p1.send(getBosConnNotNull(), opLogin);
            handled = true;
            break;
          }
        }
        break;
      case AIM_CB_FAM_STS :
        switch (subtype)
        {
          case AIM_CB_STS_SETREPORTINTERVAL :
            //faimtest_reportinterval; return true;
            break;
        }
        break;
      case AIM_CB_FAM_BUD : //0x0003
        switch (subtype)
        {
          case AIM_CB_BUD_RIGHTSINFO :
            //faimtest_parse_buddyrights; return true;
            break;
          case AIM_CB_BUD_ONCOMING : //0x0003/0x000b
          {
            //faimtest_parse_oncoming; return true;
            //5.1 Oncoming Buddies
            //The "oncoming buddy" command can occur at three different times during the lifecycle of an AIM session. The first, is at the end of the login process, just after the AIM message server is notified of the contents of your buddy list (Phase 3D, Command HI). The second is if/when one of the buddies in that list comes online who wasnt' before, and the third occurs at a regular interval while the connection is otherwise idle. This third case is used for updating your buddy list to make sure you didn't miss anything before. The command syntax for all three cases is exactly the same:

            //SNAC header: Word 0x0003
            //SNAC header: Word 0x000b
            //SNAC header: Word 0x0000
            //SNAC header: DWord gibberish //reqid
            //
            //BYTE oncoming uin length
            //BYTES oncoming uin (not null terminated)
            //WORD warning level //unsigned int containing current warning level of oncoming uin
            //WORD user class (0x0004 for Free, 0x0003 for AOL)
            //tlvlist
            //  TLV(  1) WORD class2 (0x0010 for Free, 0x0004 for AOL. icq2k: usually 0x0050)
            //  TLV(  2) DWORD C unsigned 32-bit long: member-since date //seconds from start of 1970
            //  TLV(  3) DWORD C unsigned 32-bit long: online-since date //seconds from start of 1970
            //  TLV(  4) WORD 0x0000 //aim: only present for of the "Free" or "Trial" classes
            //  TLV(  6)
            //           WORD unknown, sometimes 0x0003
            //           WORD status
            //  TLV(0xA) ip address
            //  TLV(0xC) direct-connection-info
            //  TLV(0xD) capability-info
            //  TLV(0xF) unknown

            //Classes: Every AIM Screen Name is associated with a class. AOL members (who are really just using the AOLIM?AIM Bridge) are in the "AOL" class. Members who are using the AIM-only service are under the "Free" class. And, "Free" members who have had thier account less than thirty days or so, are in the "Trial" class.
            //For those who don't know what "UNIX time_t format" is, it's the format used to represent times as unsigned 32-bit longs in UNIX and some DOS-based libc's. It's simply the number of seconds elapsed from the 01 January 1970 00:00:00 UTC. (This is often referred to as "the UNIX epoch".) Both of the times in this command (at positions 27 and 35) are stored in this format (and yes, these will fail because of the y2.048k bug).
            //Note, that there's also an "Idle for" field in this command somewhere. It may very well be the last word of the command (since I don't think you can get the idle time of an AOL member anyway). Since I've found no good way to "be idle", I can't really figure out exactly where it is.

            String screenName = aimutil_getString(snacData, 0);
            int ofs = 1 + screenName.length() + 4;
            int len = snacData.length - ofs;
            try
            {
              MLang.EXPECT_NON_NEGATIVE(len, "len", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
            }
            catch (Exception ex)
            {
              if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("protocol violation ignored", ex);
              handled = false;
              break;
            }
            Aim_tlvlist_t tl = new Aim_tlvlist_t(Session.this, snacData, ofs, len);

            int status = 0x0000ffff & (int) tl.getNthTlvOfTypeAs32Bit(1, 6);
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rxdispatch_switch(): '" + screenName + "' changed status to " + StatusUtil.translateStatusOscarToString(status));
            if (loginId.equals(screenName))
              setStatus_Oscar_Internal(null, status, false, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
            else
              setContactStatus_Oscar(screenName, status, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null);
            handled = true;
            break;
          }
          case AIM_CB_BUD_OFFGOING : //0x0003/0x000c
          {
            String screenName = aimutil_getString(snacData, 0);
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("rxdispatch_switch(): '" + screenName + "' changed status to " + StatusUtil.translateStatusOscarToString(StatusUtil.OSCAR_STATUS_OFFLINE));
            if (loginId.equals(screenName))
              throw new MessagingNetworkException(
                "icq server reported you as offline",
                MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
                MessagingNetworkException.ENDUSER_LOGGED_OFF_ICQ_SERVER_REPORTED_YOU_AS_OFFLINE);
            else
              setContactStatus_Oscar(screenName, StatusUtil.OSCAR_STATUS_OFFLINE, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null);
            handled = true;
            break;
          }
        }
        break;
      case AIM_CB_FAM_MSG : //0x0004
        switch (subtype)
        {
          case AIM_CB_MSG_INCOMING : //0x0004/0x0007
            parseMessage(flapBody, ofs11, len11, ctx); //faimtest_parse_incoming_im
            handled = true;
            break;
          case 0x0001: //AIM_CB_MSG_ERROR
            handled = parseMsgError(flapBody);
            break;
          case AIM_CB_MSG_MISSEDCALL :
            break;
          case 0x000C: //AIM_CB_MSG_ACK
            handled = parseHostAck(flapBody, ctx);

            break;
        }
        break;
      case 0x0015 :
        switch (subtype)
        {
          case 3 :
          {
            if (FETCH_OFFLINE_MESSAGES)
            {
              //2A 02 10 B8 00 19 //srv snds
              //00 15 00 03 //offline msgs done?
              //00 00
              //00 01 00 02 //reqid
              //
              //snacbody:
              //
              //00 01 00 0B
              //  09 00 //subheader length, useless
              //    //subheader start
              //    BE 5C 94 01 //my uin, useless
              //    42 00 //message-type
              //    02 00 //word-cookie
              //    //subheader end
              //  00 //const
              Aim_tlvlist_t tlvlist = new Aim_tlvlist_t(Session.this, snacData);
              byte[] t1b = tlvlist.getNthTlvOfTypeAsByteArray(1, 1);

              if (t1b == null) MLang.EXPECT_NOT_NULL(t1b, "t1b", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
              if (t1b.length <= 2+4+2+2) MLang.EXPECT_FALSE("t1b.length >= 10", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);

              int messageType = aimutil_get16(t1b, 2+4);
              //int wordCookie = aimutil_get16(t1b, 2+6);

              switch (messageType)
              {
                case 0x4100:
                {
                  if (Defines.DEBUG && CAT.isDebugEnabled())
                    CAT.debug("received: offline message");
                  noIncomingOfflineMessages = false;
                  parseOfflineMessage(t1b, ctx);
                  handled = true;
                  break; //of switch msgType
                }
                case 0x4200:
                {
                  if (Defines.DEBUG && CAT.isDebugEnabled())
                    CAT.debug("received: offline messages done, "+
                      (noIncomingOfflineMessages ?
                        "no offline msgs received, skipping" :
                        "sending"
                      )+" ack/delete"
                    );
                  if (!noIncomingOfflineMessages)
                  {
                    sendDeleteOfflineMessages(opLogin, ctx);
                  }
                  handled = true;
                  break; //of switch msgType
                }
              } //of switch msgType

              if (handled) break; //skip parse15_03(...) call
            } //of if (FETCH_OFFLINE_MESSAGES)

            handled = parse15_03(flapBody, flags, requestId, ctx);
            break;
          }
        }
        break;

        //case AIM_CB_FAM_LOC:
        //switch (subtype)
        //{
        //case AIM_CB_LOC_ERROR:
        ////faimtest_parse_locerr; return true;
        //break;
        //}
        //break;
        //aim_conn_addhandler(sess, bosconn, 0x000a, 0x0001, faimtest_parse_searcherror, 0);
        //aim_conn_addhandler(sess, bosconn, 0x000a, 0x0003, faimtest_parse_searchreply, 0);
        //aim_conn_addhandler(sess, bosconn, AIM_CB_FAM_LOC, AIM_CB_LOC_USERINFO, faimtest_parse_userinfo, 0);
        //aim_conn_addhandler(sess, bosconn, AIM_CB_FAM_LOC, AIM_CB_LOC_RIGHTSINFO, faimtest_locrights, 0);
        //aim_conn_addhandler(sess, bosconn, 0x0004, 0x0005, faimtest_icbmparaminfo, 0);

        /////aim_conn_addhandler(sess, bosconn, AIM_CB_FAM_SPECIAL, AIM_CB_SPECIAL_CONNERR, faimtest_parse_connerr, 0);
        /////aim_conn_addhandler(sess, bosconn, 0x0001, 0x001f, faimtest_memrequest, 0);
        /////aim_conn_addhandler(sess, bosconn, 0xffff, 0xffff, faimtest_parse_unknown, 0);
        /////case AIM_CB_FAM_SPECIAL / AIM_CB_SPECIAL_CONNCOMPLETE: faimtest_conncomplete
    }
    if (!handled)
    {
      if (subtype == 1 || (family == 9 && subtype == 9))
      {
        rx_handleGenericServiceError(flapBody, ofs11, len11, ctx);
        handled = true;
      }
    }
    return handled;
  }


  private void send_addSingleUserToContactList(
    final AsyncOperations.OpAddContactListItem op,
    String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    dstLoginId = String.valueOf(IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED));
    SNAC p = new SNAC(3, 4, 0, 0, snac_nextid++);
    p.addStringPrependedWithByteLength(dstLoginId);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("send_addSingleUserToContactList(): send: add \"" + dstLoginId + "\" to a contact list.");
    p.send(getBosConnNotNull(), op);

    sendNotifyIAddedYou(dstLoginId, ctx);
  }

  private void sendNotifyIAddedYou(String dstLoginId, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    /*
    CLIENT  SNAC 4/6  SNACID 0000 0036  FLAGS 0

    � _ N,     2222223   ;_\"   3 joxy�Evgenii/Joe�Filippov�joe@idisys.iae.nsk.su�1�
    B5 01 9A 1A 4E 2C 00 00
    00 04 //msgfmt
    07 32 32 32 32 32 32 33 //recipient
    00 05 / 00 3B  //TLV(5)
      BE 5C 94 01  //sender
      0C //msg kind: MSGKIND_ADDED_TO_CONTACT_LIST
      00 //msg flags
      //LNTS starts
      33 00
      6A 6F 78 79 //sender nick
      FE
      45 76 67 65  6E 69 69 2F 4A 6F 65 //sender firstname
      FE
      46 69 6C 69 70 70 6F 76 //sender lastname
      FE
      6A 6F 65 40 69 64 69 73 79 73 2E 69 61 65 2E 6E 73 6B 2E 73 75 //sender email
      FE
      31 // 31 if auth not required by the sender, 30 if auth required by the sender.
      FE
      00
      00 06 00 00 //request some ack
      //LNTS ends
    */
    ByteArrayOutputStream bas = new ByteArrayOutputStream(128);
    int i = 0;
    //bas.write(sender nick);
    bas.write((byte) 0xfe);
    //bas.write(sender firstname);
    bas.write((byte) 0xfe);
    //  46 69 6C 69 70 70 6F 76 //sender lastname
    bas.write((byte) 0xfe);
    //  6A 6F 65 40 69 64 69 73 79 73 2E 69 61 65 2E 6E 73 6B 2E 73 75 //sender email
    bas.write((byte) 0xfe);
    bas.write((byte) 0x31); // 31 if auth not required by the sender, 30 if auth required by the sender.
    bas.write((byte) 0xfe);

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("send_addSingleUserToContactList(): sendYouReAdded: \"" + dstLoginId + "\" is added to a contact list.");

    sendGenericMessage(null, getBosConnNotNull(), dstLoginId, bas.toByteArray(),
      MSGKIND_ADDED_TO_CONTACT_LIST,
      DEFAULT_MSG_FMT,
      false,
      false, ctx);
  }

  private void send_removeSingleUserFromContactList(String dstLoginId)
  throws MessagingNetworkException, IOException
  {
    dstLoginId = String.valueOf(IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED));
    byte[] buddyName = string2byteArray(dstLoginId);
    SNAC p = new SNAC(3, 5);
    p.addByte(buddyName.length);
    p.addByteArray(buddyName);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("send_removeSingleUserFromContactList(): send: remove \"" + dstLoginId + "\" from a contact list.");
    p.send(getBosConnNotNull(), null);
  }

  private void send_setClientStatus(
      final AsyncOperations.OpSetStatus op,
      int newStatus_Oscar)
  throws MessagingNetworkException, IOException
  {
    //set me invisible packet:
    //snac 0009/0005 flags 0 0 snacid
    //2A 02 05 2A 00 0A
    //00 09 00 05 00 00 00 00 00 05
    //
    //set my status packet:
    //snac 0001/001e flags 0 0 snacid
    //2A 02 14 C5 00 12
    //00 01 00 1E 00 00 00 00 00 1E
    //00 06 00 04 00 03
    //xx xx  //status (word)
    //
    switch (newStatus_Oscar)
    {
      case StatusUtil.OSCAR_STATUS_AWAY :
      case StatusUtil.OSCAR_STATUS_DND :
      case StatusUtil.OSCAR_STATUS_FREE_FOR_CHAT :
      case StatusUtil.OSCAR_STATUS_NA :
      case StatusUtil.OSCAR_STATUS_OCCUPIED :
      case StatusUtil.OSCAR_STATUS_ONLINE :
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("sending client status: "+newStatus_Oscar);
        SNAC p = new SNAC(1, 0x001e, 0, 0, snac_nextid++);
        p.addByteArray(new byte[] {0, 6, 0, 4, 0, 3});
        p.addWord(newStatus_Oscar);
        p.send(getBosConnNotNull(), op);
        break;
      case StatusUtil.OSCAR_STATUS_OFFLINE :
        Lang.ASSERT_FALSE("OSCAR_STATUS_OFFLINE: should never happen here");
        break;
      default :
        Lang.ASSERT_FALSE("unknown status value: " + newStatus_Oscar + ", ignored");
        break;
    }
  }
  private void sendAuthResponsePositive(String authRequestSenderLoginId, final PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("sendAuthResponsePositive(): \""+authRequestSenderLoginId+"\" requested authorization, sending auth response.");
    //2A 02 4F  AA 00 2E
    //00 04 00 06
    //00 00
    //00 0B 00 06 //(or 0 3 0 6) (or 0 1 0 6)
    //65 42 F7 05  50 45 00 00 //msgcook (arbitrary)
    //00 04  //msgchannel
    //08 36 32 32 33 32 33 37 37 //sender uin
    //00 05 00 09 //const
    //  7F 0C 55 07 //my uin
    //  08 00 01 00  00 //const
    //00 06 00 00 //const
    SNAC p = new SNAC(4, 6, 0, 0, 0x000b0006);
    byte[] cookie = fillMsgCookie(new byte[8]);
    p.addByteArray(cookie);
    p.addWord(4); //msg channel
    p.addStringPrependedWithByteLength(authRequestSenderLoginId);
    p.addByteArray(new byte[] {(byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x09, //
    (byte) (uin & 0xff), //
    (byte) ((uin >> 8) & 0xff), //
    (byte) ((uin >> 16) & 0xff), //
    (byte) ((uin >> 24) & 0xff), //
    (byte) 0x08, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, 0, 6, 0, 0});
    p.send(getBosConnNotNull(), null);
    //0x01 94 5C BE == 26500286
  }
  /**
   * No-op: WinAIM 4.x sends these every minute to keep
   * the connection alive.
   */
  private void sendKeepAlive()
  throws MessagingNetworkException, IOException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("sending keepAlive");
    getBosConnNotNull().send(new byte[] {}, 5, null);
  }

  public void sendMessage(
    final AsyncOperations.OpSendMessage op,
    final String dstLoginId, final String text, final PluginContext ctx)
  throws MessagingNetworkException
  {
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("sendMessage() start, dst="+dstLoginId+",\r\ntext="+StringUtil.toPrintableString(text));
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_TEXT_MESSAGE_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if (text == null) Lang.ASSERT_NOT_NULL(text, "text");
      if (text.length() == 0) MLang.EXPECT_NOT_NULL_NOR_EMPTY(text, "text", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, MessagingNetworkException.ENDUSER_MIM_BUG_MESSAGE_TEXT_CANNOT_BE_EMPTY);
      if (Defines.ENABLE_FAKE_PLUGIN)
      {
        ((org.openmim.icq2k.fake.FakeConnection)getBosConnNotNull()).onSendTextMsg();
      }
      final byte[] msg_bytes = encodeAsAscii7Html(text);
      sendGenericMessage(op, getBosConnNotNull(), dstLoginId, msg_bytes, MSGKIND_PLAIN_TEXT, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  /**
    Using the connection of srcLoginId uin,
    retrieves the authorizationRequired property of dstLoginId uin
    from the icq server.
  */
  public void isAuthorizationRequired(
    final AsyncOperations.OpIsAuthorizationRequired op,
    String dstLoginId, PluginContext ctx)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("isAuthorizationRequired() start, dst="+dstLoginId);
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      isAuthorizationRequired0(op, dstLoginId, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  /**
    Using the connection of srcLoginId uin,
    sends an auth request to dstLoginId uin with reason reason,
    to ask if dstLoginId allows to add himself
    to srcLoginId's contact list.
    Reason can be null.
  */
  public void authorizationRequest(
    final AsyncOperations.OpSendAuthorizationRequest op,
    String dstLoginId, String text, PluginContext ctx)
  {
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      authorizationRequest0(op, dstLoginId, text, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  /**
    Using the connection of srcLoginId uin,
    sends a reply to preceding auth request of dstLoginId uin,
    to state that srcLoginId grants or denies
    dstLoginId's request to add srcLoginId to dstLoginId's contact list.
  */
  public void authorizationResponse(
    final AsyncOperations.OpSendAuthorizationResponse op,
    String dstLoginId, boolean grant, PluginContext ctx)
  {
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_OPERATION_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      authorizationResponse0(op, dstLoginId, grant, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  public void sendContacts(
    final AsyncOperations.OpSendContacts op,
    String dstLoginId, String[] nicks, String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException
  {
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_PERFORM_SEND_CONTACTS_WHILE_OFFLINE);
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      if ((nicks) == null) Lang.ASSERT_NOT_NULL(nicks, "nicks");
      if ((loginIds) == null) Lang.ASSERT_NOT_NULL(loginIds, "loginIds");
      sendContacts0(op, getBosConnNotNull(), dstLoginId, nicks, loginIds, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  private byte nextDecId = (byte) 0xd9;
  private int nextMsgId = 0x1;
  private final Object operationResponseLock = new Object();
  private byte[] currentMsgCookie = null;
  private long   operationRequestId = -1;

  /**
    Parses the server ack. (User_ ack is 4/0xb).
    Called by this.'boolean rxdispatch_BIG_SWITCH' method.
    Returns true if and only if handled.
  */
  private boolean parseHostAck(final byte[] snacData, PluginContext ctx)
  {
    if ((snacData) == null) Lang.ASSERT_NOT_NULL(snacData, "snacData");
    if ((snacData[0]) != (0)) Lang.ASSERT_EQUAL(snacData[0], 0,   "snacData[0]", "0");
    if ((snacData[1]) != (4)) Lang.ASSERT_EQUAL(snacData[1], 4,   "snacData[1]", "4");
    if ((snacData[2]) != (0)) Lang.ASSERT_EQUAL(snacData[2], 0,   "snacData[2]", "0");
    if ((snacData[3]) != (0xC)) Lang.ASSERT_EQUAL(snacData[3], 0xC, "snacData[3]", "0xC");

    byte[] sentMsgKuka = null;
    long sentMsgId = -1;
    synchronized (operationResponseLock)
    {
      sentMsgKuka = this.currentMsgCookie;
      sentMsgId = this.operationRequestId;
    }
    if (sentMsgKuka == null || sentMsgId == -1)
    {
      String recipientUinAcked = null;
      try
      {
        recipientUinAcked = aimutil_getString(snacData, 10+8+2);
      }
      catch (Exception ex)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN)) CAT.warn("LATE msgack (to generic msg sent to (null) recipient) received, ignored", ex);
        return false;
      }
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("LATE msgack (to generic msg sent to "+recipientUinAcked+") received, SKIPPING send system notice");
      /*
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("LATE msgack (to generic msg sent to "+recipientUinAcked+") received, sending system notice");
      if ((recipientUinAcked) == null) Lang.ASSERT_NOT_NULL(recipientUinAcked, "recipientUinAcked");
      fireSystemNotice("icq server reports: message/contacts are delivered to "+recipientUinAcked+".", ctx);
      */
      return true;
    }

    boolean eq = true;
    long tmpId = sentMsgId;
    for (int i = 0; i < 4; i++)
    {
      if ( snacData[9 - i] != (byte)(tmpId & 0xFF) )
      {
        eq = false;
        break;
      }
      tmpId >>= 8;
    }
    if (eq)
    {
      for (int i = 0; i < 8; i++)
      {
        if (snacData[ 10 + i ] != sentMsgKuka[i])
        {
          eq = false;
          break;
        }
      }
    }

    if (!eq)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unknown msg ack");
      return false; //unknown ack;
    }

    //my ack!
    synchronized (asyncOps)
    {
      for (int aop = 0; aop < currentOps.size(); ++aop)
      {
        Object op = currentOps.get(aop);
        if (op != null && op instanceof AsyncOperations.OpSendGenericMessage)
        {
          AsyncOperations.OpSendGenericMessage om = (AsyncOperations.OpSendGenericMessage) op;
          om.ackReceived();
        }
      }
    }
    synchronized (operationResponseLock)
    {
      //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("msg ack received");
      operationResponseLock.notify(); //for obsolete sync methods
    }
    return true;
  }

  /**
    Called by this.'boolean rxdispatch_BIG_SWITCH' method.
    Returns true if and only if handled.
  */
  private boolean parseMsgError(final byte[] snacData)
  {
    //00 04 00 01   00 00   00 01 00 06  00 0e

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseMsgError");
    if ((snacData) == null) Lang.ASSERT_NOT_NULL(snacData, "snacData");

    final long sentMsgId = this.operationRequestId;
    if (sentMsgId == -1)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("msg error received in no context, generic msgerror handler invoked");
      return false;
    }

    boolean eq = true;
    long tmpId = sentMsgId;
    for (int i = 0; i < 4; i++)
    {
      if ( snacData[9 - i] != (byte)(tmpId & 0xFF) )
      {
        eq = false;
        break;
      }
      tmpId >>= 8;
    }

    if (!eq)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unknown msg error, generic msgerror handler invoked");
      return false;
    }

    int errcode = -1;

    if (snacData.length >= 12)
    {
      errcode = aimutil_get16(snacData, 10);
    }

    //last msg gave error
    synchronized (asyncOps)
    {
      for (int aop = 0; aop < currentOps.size(); ++aop)
      {
        Object op = currentOps.get(aop);
        if (op != null && op instanceof AsyncOperations.OpSendGenericMessage)
        {
          AsyncOperations.OpSendGenericMessage om = (AsyncOperations.OpSendGenericMessage) op;
          om.msgErrorReceived(errcode);
        }
      }
    }
    synchronized (operationResponseLock)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("last msg returned error, errcode: " + (errcode == -1 ? "-1" : HexUtil.toHexString0x(errcode)));
      operationResponseLock.notify(); //for obsolete sync methods
    }
    return true;
  }

  //for obsolete sync methods
  void unlockOperationWait()
  {
    synchronized (operationResponseLock)
    {
      operationResponseLock.notifyAll();
    }
  }

  private static byte[] fillMsgCookie(byte[] cookie)
  {
    new Random().nextBytes(cookie);
    cookie[6] = cookie[7] = (byte) 0;
    return cookie;
  }

  //for obsolete sync methods
  private void waitForOperationResponse(int timeoutMillis, String errMsg)
  throws MessagingNetworkException, InterruptedException
  {
    if (!Defines.ENABLE_FAKE_PLUGIN)
    {
      checkShuttingdown();

      try {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("opResponseLock.wait, src="+loginId);
        operationResponseLock.wait(timeoutMillis);
      } catch (InterruptedException exx) {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("opResponseLock.wait interrupted");
        throw exx;
      }
    }

    checkShuttingdown();
  }

  /**
    Sends a SNAC header over FLAP channel 2.
  */
  /*
  private void sendSnac(Aim_conn_t conn, int family, int subtype, int flags1, int flags2, int snac_data_field_length)
  throws IOException, MessagingNetworkException
  {
    sendSnac(conn, family, subtype, flags1, flags2, 0, snac_data_field_length);
  }
  */
  /**
  Sends a SNAC header over FLAP channel 2.
  */

  /*
  private void sendSnac(Aim_conn_t conn, int family, int subtype, int flags1, int flags2, long requestId, int snac_data_field_length)
  throws IOException, MessagingNetworkException
  {
    ByteArrayOutputStream b = new
      (new FLAPHeader(2, conn.getNextOutputStreamSeqnum(), 10 + snac_data_field_length).byteArray);
    byte[] snacHeader = new byte[10];
    int offset = 0;
    offset += aimutil_put16(snacHeader, offset, family);
    offset += aimutil_put16(snacHeader, offset, subtype);
    offset += aimutil_put8(snacHeader, offset, flags1);
    offset += aimutil_put8(snacHeader, offset, flags2);
    offset += aimutil_put32(snacHeader, offset, requestId);
    conn.send(snacHeader);
  }
  */

  /** Status of contact list entries */
  private void setContactStatus_Oscar(String dstLoginId, int newStatus_Oscar, PluginContext ctx,
    int reasonLogger, String reasonMessage)
  throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setContactStatus_Oscar");
    try
    {
      if (!(  !this.loginId.equals(dstLoginId)  )) throw new AssertException("this.loginId.equals(dstLoginId) must be false here");
      int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);
      Integer dstUin_ = new Integer(dstUin);
      ContactListItem cli = getContactListItem(dstUin_);
      if (cli == null)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("Session.setContactStatus_Oscar(): '" + dstLoginId + "' is not on contact list, statusOscar change ignored");
        return;
      }
      int oldStatus_Oscar = cli.getStatusOscar();
      cli.setStatusOscar(newStatus_Oscar); //marks it as NOT obsolete
      if (oldStatus_Oscar == newStatus_Oscar)
        return;
      //ignore any status events if this Session is already logged out
      if (this.status_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE)
      {
        int oldStatus_Mim = StatusUtil.translateStatusOscarToMim_cl_entry(oldStatus_Oscar);
        int newStatus_Mim = StatusUtil.translateStatusOscarToMim_cl_entry(newStatus_Oscar);
        if (oldStatus_Mim != newStatus_Mim)
        {
          fireContactListEntryStatusChangeMim_Uncond(dstLoginId, newStatus_Mim, ctx, reasonLogger, reasonMessage);
        }
        else
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatus request ignored by icq2k plugin: attempted to set the same status");
      }
      else
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatus request ignored by icq2k plugin: we are offline, hence silent");
    }
    catch (Exception ex)
    {
      handleException(ex, "setContactStatus", ctx);
    }
  }

  protected void fireContactListEntryStatusChangeMim_Uncond(String dstLoginId, int newStatus_Mim, PluginContext ctx,
    int reasonLogger, String reasonMessage)
  throws MessagingNetworkException
  {
    ctx.getICQ2KMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, dstLoginId, newStatus_Mim,
      reasonLogger, reasonMessage, MessagingNetworkException.ENDUSER_NO_ERROR);
  }

  protected void fireSessionStatusChangeMim_Uncond(int newStatus_Mim, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    ctx.getICQ2KMessagingNetwork().fireStatusChanged_Mim_Uncond(this.loginId, this.loginId, newStatus_Mim,
      reasonLogger, reasonMessage, endUserReasonCode);
  }

  /**
    To be called from plugin user classes via ICQ2KMessagingNetwork;
    should never be called from other plugin classes.
    <p>
    Only works if already logged in, otherwise throws an AssertException.
    <p>
    Does not allow setting an OFFLINE status, since
    ICQ2KMessagingNetwork.logout() should be called instead.
  */
  public void setStatus_Oscar_External(
    final AsyncOperations.OpSetStatusNonOffline op,
    int newStatus, PluginContext ctx)
  throws MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatus_Oscar_External to " + 
      StatusUtil.translateStatusOscarToString(newStatus) + //
      " (current status: " + StatusUtil.translateStatusOscarToString(this.status_Oscar) + ")");
    try
    {
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_MIM_BUG_LOGIN_FIRST_CANNOT_PERFORM_SET_STATUS_NONOFFLINE_WHILE_OFFLINE);
      if (!(  newStatus != StatusUtil.OSCAR_STATUS_OFFLINE  )) throw new AssertException("OSCAR_STATUS_OFFLINE should never happen here; use Session.logout() instead.");

      if (Defines.ENABLE_FAKE_PLUGIN)
      {
        ((org.openmim.icq2k.fake.FakeConnection)getBosConnNotNull()).onSendStatusChange();
      }

      //packetsend is asyncronous(session), so we don't bother
      setStatus_Oscar_Internal(op, newStatus, true, ctx, MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, null, MessagingNetworkException.ENDUSER_NO_ERROR);
      op.success();
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  private void setStatus_Oscar_Internal(
    final AsyncOperations.OpSetStatus op,
    final int newStatus_Oscar, boolean sendToICQServer, PluginContext ctx,
    int reasonLogger, String reasonMessage, int endUserReasonCode)
  throws MessagingNetworkException
  {
    try
    {
      if (sendToICQServer)
        StatusUtil.EXPECT_IS_OSCAR_STATUS(newStatus_Oscar);
      int oldStatus_Mim;
      int newStatus_Mim;
      synchronized (logoutLock)
      {
        final int oldStatus_Oscar = this.status_Oscar;
        if (oldStatus_Oscar == newStatus_Oscar)
          return;
        this.status_Oscar = newStatus_Oscar;

        oldStatus_Mim = StatusUtil.translateStatusOscarToMim_self(oldStatus_Oscar);
        newStatus_Mim = StatusUtil.translateStatusOscarToMim_self(newStatus_Oscar);
        if (oldStatus_Mim != newStatus_Mim)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled())
            CAT.debug(getLoginId()+": internal status changed to "+StatusUtilMim.translateStatusMimToString(newStatus_Mim)+
              " (from "+StatusUtilMim.translateStatusMimToString(oldStatus_Mim)+")");
          if (oldStatus_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE)
            setReconnectData(oldStatus_Oscar, contactListUinInt2cli);
          if (newStatus_Oscar == StatusUtil.OSCAR_STATUS_OFFLINE)
          {
            //contactListUinInt2cli = null;
            synchronized (lastErrorLock)
            {
              if (lastError == null)
              {
                setLastError(new MessagingNetworkException(reasonMessage, reasonLogger, endUserReasonCode));
              }
            }
          }
          if (op != null) op.muteFailureEvent();
          fireSessionStatusChangeMim_Uncond(newStatus_Mim, ctx, reasonLogger, reasonMessage, endUserReasonCode);
        }
        else
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("setStatus request ignored by icq2k plugin: attempted to set the same status");
      }
      if (newStatus_Oscar != StatusUtil.OSCAR_STATUS_OFFLINE)
      {
        if (sendToICQServer)
        {
          send_setClientStatus((AsyncOperations.OpSetStatus) op, newStatus_Oscar);
        }
      }
    }
    catch (Exception ex)
    {
      handleException(ex, ctx);
    }
  }

  /**
    Shuts the session and everything down and throws no exceptions.
  */
  void shutdown(PluginContext ctx, int reasonLogger, String reasonMessage, int endUserReasonCode)
  {
    synchronized (shuttingDownLock)
    {
      if (shuttingDown)
        return;
      if (Defines.DEBUG_DUMPSTACKS_EVERYWHERE) if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("shutting down", new Exception("dumpstack"));
      setLastError(
        (reasonMessage == null ? "no reason message" : reasonMessage),
        reasonLogger,
        endUserReasonCode);
      shuttingDown = true;
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("shutdown("+reasonMessage+")");
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("closing all connections");
      Aim_conn_t conn1;
      Aim_conn_t conn2;
      synchronized (logoutLock)
      {
        logoutLock.notifyAll();
        conn1 = authconn;
        authconn = null;
        conn2 = bosconn;
        bosconn = null;
      }

      closeConnectionIfNotNull(conn1);
      closeConnectionIfNotNull(conn2);

      setStatus_Oscar_Internal(null, StatusUtil.OSCAR_STATUS_OFFLINE, false, ctx, reasonLogger, reasonMessage, endUserReasonCode);
      
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("closing async operations");
      failAsyncOps(lastError);
      unlockOperationWait();

      OutgoingMessageQueue q = null;
      synchronized (shuttingDownLock)
      {
        q = messageQueue;
        messageQueue = null;
      }
      if (q != null) q.stop(cloneLastError());

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("closing remaining tasks");
      cancelAll(ctx);

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("closing all done.");
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) 
        CAT.debug("ex in shutdown(), ignored", ex);
    }
  }
  
  private void shutdownAt(long stopTime, String operationDetails, PluginContext ctx)
  throws MessagingNetworkException, InterruptedException
  {
    checkShuttingdown();

    if (System.currentTimeMillis() >= stopTime)
    {
      throw new MessagingNetworkException(
        "icq server operation timed out: " + operationDetails,
        MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
        MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT);
    }
  }

  void sleep(String activityName, long millis)
  throws MessagingNetworkException, InterruptedException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(activityName + ": sleeping " + ((millis / 100) / (float) 10) + " sec.");
    synchronized (logoutLock)
    {
      logoutLock.wait(millis);
    }
    checkShuttingdown();
  }

  public static byte[] string2byteArray(String s)
  {
    try
    {
      return s.getBytes(fileEncoding);
    }
    catch (java.io.UnsupportedEncodingException ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error(ex.getMessage(), ex);
    }
    return s.getBytes();
  }

  private final Object scheduledSendStatusLock = new Object();

  public Object getFireStatusLock()
  {
    return scheduledSendStatusLock;
  }

  public void fetchUserDetails(
    final AsyncOperations.OpGetUserDetails op,
    String dstLoginId, boolean externalCall, PluginContext ctx)
  throws MessagingNetworkException
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fetchUserDetails() start, dst "+dstLoginId);
      ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_GET_USER_DETAILS_WHILE_OFFLINE);

      fetchUserDetails0(op, dstLoginId, ctx);
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  public UserSearchResults searchUsers(
    final AsyncOperations.OpSearch op,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern,
    PluginContext ctx) throws Exception
  {
    if (emailSearchPattern == null) emailSearchPattern = "";
    if (nickSearchPattern == null) nickSearchPattern = "";
    if (firstNameSearchPattern == null) firstNameSearchPattern = "";
    if (lastNameSearchPattern == null) lastNameSearchPattern = "";

    emailSearchPattern=emailSearchPattern.trim();
    nickSearchPattern=nickSearchPattern.trim();
    firstNameSearchPattern=firstNameSearchPattern.trim();
    lastNameSearchPattern=lastNameSearchPattern.trim();

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("searchUsers() start for "+
          "email '" + emailSearchPattern+
          "' nick '" + nickSearchPattern+
          "' fname '" + firstNameSearchPattern+
          "' lname '" + lastNameSearchPattern+"'");
    ASSERT_LOGGED_IN(MessagingNetworkException.ENDUSER_LOGIN_FIRST_CANNOT_SEARCH_USERS_WHILE_OFFLINE);

    return searchUsers0(
      op,
      emailSearchPattern,
      nickSearchPattern,
      firstNameSearchPattern,
      lastNameSearchPattern, ctx);
  }

  private final int RESPONSE_TYPE_UNDEFINED = -1;
  private final int RESPONSE_TYPE_USER_DETAILS = 1;
  private final int RESPONSE_TYPE_WHITEPAGES_FULLINFO = 2;

  private UserSearchResults searchUsers0(
    final AsyncOperations.OpSearch op,
    String emailSearchPattern,
    String nickSearchPattern,
    String firstNameSearchPattern,
    String lastNameSearchPattern,
    PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    final long id = snac_nextid++ & 0xFFFF;
    SNAC p = new SNAC(0x0015, 0x0002, 0, 0, id);

    final int C1 =
      emailSearchPattern.length()+
      nickSearchPattern.length()+
      firstNameSearchPattern.length()+
      lastNameSearchPattern.length() - (9+8+11);

    p.addWord(1);
    p.addWord(0x61+C1);
    p.addWordLE(0x5F+C1);
    p.addIcqUin(uin);
    p.addWord(0xD007);
    p.addWord(0);
    p.addWord(0x3305);
    p.addLNTS(firstNameSearchPattern);
    p.addLNTS(lastNameSearchPattern);
    p.addLNTS(nickSearchPattern);
    p.addLNTS(emailSearchPattern);
    p.addByteArray(new byte[]
      {
        00, 00, 00, 00,
        00, 00, 01, 00, 00, 01, 00, 00, 00, 00, 01, 00, 00, 01, 00, 00, 01, 00, 00, 00,
        00, 00, 01, 00, 00, 00, 00, 01, 00, 00, 00, 00, 01, 00, 00, 00, 00, 01, 00, 00,
        00
      });
    /*
    00 01  00 61  5F 00
    xx xx xx xx
    D0 07
    00 00
    33 05 //wp-full-request (works with and without wildcards)
    0A 00  xx xx xx xx   xx xx xx xx   xx 00 //fn (w or w/o wildcards)
    09 00  xx xx xx xx   xx xx xx xx   00 //ln (w or w/o wildcards)
    01 00  00 //nick (w or w/o wildcards)
    0C 00  xx xx xx xx   xx xx xx xx   xx xx xx 00 //email (w or w/o wildcards)
    00 00  //WORD    (LE) minimum age, 0 if disabled
    00 00  //WORD    (LE) maximum age, 0 if disabled
    00 00 01 00 00 01 00 00 00 00 01 00 00 01 00 00 01 00 00 00 //etc
    00 00 01 00 00 00 00 01 00 00 00 00 01 00 00 00 00 01 00 00 //etc
    00 //BYTE    only-online-users, (0=off, 1=on)
    */
    try
    {
      synchronized (operationResponseLock)
      {
        this.responseType = RESPONSE_TYPE_WHITEPAGES_FULLINFO;
        this.operationRequestId = id;
        this.userSearchResults = null;
        this.userDetailsUnknownFormat = false;
        this.errorFetchinguserDetails = false;
        this.noUsersFound = false;
      }

      p.send(getBosConnNotNull(), op); //packet sent.

      synchronized (operationResponseLock)
      {
        waitForOperationResponse(ICQ2KMessagingNetwork.REQPARAM_SERVER_RESPONSE_TIMEOUT1_SECONDS * 1000,
          "Error while searchUsers("+
            "email '" + emailSearchPattern+
            "' nick '" + nickSearchPattern+
            "' fname '" + firstNameSearchPattern+
            "' lname '" + lastNameSearchPattern+"')");

        UserSearchResults result = this.userSearchResults;
        if (result != null || this.noUsersFound)
        {
          return result;
        }
        else
        {
          if (this.userDetailsUnknownFormat)
            throw new MessagingNetworkException(
              "error while user search: cannot parse packet.",
              MessagingNetworkException.CATEGORY_STILL_CONNECTED,
              MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
          else
          if (this.errorFetchinguserDetails)
            throw new MessagingNetworkException(
              "icq server reports error while user search.",
              MessagingNetworkException.CATEGORY_STILL_CONNECTED,
              MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REFUSES_TO_RETURN_USER_SEARCH_RESULTS);
          else
            throw new MessagingNetworkException(
              "error while user search: response timeout expired.",
              MessagingNetworkException.CATEGORY_STILL_CONNECTED,
              MessagingNetworkException.ENDUSER_MESSAGING_OPERATION_TIMED_OUT_NOT_LOGGED_OFF);
        }
      }
    }
    finally
    {
      synchronized (operationResponseLock)
      {
        this.responseType = RESPONSE_TYPE_UNDEFINED;
        this.operationRequestId = -1;
        this.userSearchResults = null;
        this.userDetailsUnknownFormat = false;
        this.errorFetchinguserDetails = false;
        this.noUsersFound = false;
      }
    }
  }

  private int responseType = RESPONSE_TYPE_UNDEFINED;

  private long waitingForUserDetailsFor = -1;
  private UserDetailsImpl userDetails = null;
  private UserSearchResultsImpl userSearchResults = null;
  private boolean userDetailsUnknownFormat = false;
  private boolean errorFetchinguserDetails = false;
  private boolean noUsersFound = false;

  private void fireErrorFetchingUserDetails(final byte[] data, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fireErrorFetchingUserDetails() start");
    synchronized (asyncOps)
    {
      for (int aop = 0; aop < currentOps.size(); ++aop)
      {
        Object op = currentOps.get(aop);
        if (op != null && op instanceof AsyncOperations.OpGetUserDetails)
        {
          AsyncOperations.OpGetUserDetails om = (AsyncOperations.OpGetUserDetails) op;
          om.errorFetchingReceived();
        }
      }
    }
    synchronized (operationResponseLock)
    {
      errorFetchinguserDetails = true;
      operationResponseLock.notify(); //for obsolete sync methods
    }
  }

  private boolean parse15_03(final byte[] data, final int flags, long requestId, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    switch (responseType)
    {
      case RESPONSE_TYPE_USER_DETAILS:
        long waitingForReqId;
        synchronized (operationResponseLock)
        {
          waitingForReqId = this.operationRequestId;
        }
        if (waitingForReqId == requestId)
        {
          return parseUserDetails(data, flags, ctx); //###op
        }
        else return false;
      case RESPONSE_TYPE_WHITEPAGES_FULLINFO:
        return parseWhitepagesFullInfo(data, flags, ctx);
      case RESPONSE_TYPE_UNDEFINED:
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parse15_03(): RESPONSE_TYPE_UNDEFINED; response ignored.");
        return false;
      default:
        Lang.ASSERT_FALSE("invalid responseType: "+responseType);
        return false;
    }
  }

  private boolean parseWhitepagesFullInfo(final byte[] data, int flags, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseWhitepagesFullInfo() start");

    try
    {
      if ((aimutil_get16(data, 10)) != (1)) MLang.EXPECT_EQUAL(aimutil_get16(data, 10), 1, "tlv type", "1", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
      int ofs = 0x10;
      final long requesterUin = aimutil_getIcqUin(data, ofs);
      ofs+= 4;
      if ((this.uin) != (requesterUin)) MLang.EXPECT_EQUAL(this.uin, requesterUin, "this.uin", "requesterUin", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);

      //WORD  type
      if(aimutil_get16(data, ofs) != 0xda07) return false;
      ofs+= 4;

      //WORD  subtype
      int subtype = aimutil_get16(data, ofs);
      ofs+= 2;

      //BYTE  result
      int result = aimutil_get8(data, ofs);
      ofs++;

      boolean endOfSearch = false;

      switch (subtype)
      {
        case 0xAE01: //last wpfull search result; marks end of results
          endOfSearch = true;

        case 0xA401: //wpfull search result
          switch (result)
          {
            case 0x32:
            case 0x14:
            case 0x1E:
            // error: empty result or nonexistent user
            //report no users found
            synchronized (operationResponseLock)
            {
              this.userSearchResults = null;
              this.noUsersFound = true;
              operationResponseLock.notify();
            }
            return true;
          }
          //flags 0001 & 0xAE0132 means NO USERS FOUND //wpfull
          //flags 0000 & 0x9A010A means USERS FOUND //wpshort
          //flags 0000 & 0x9A0114 means SERVER ERROR //wpshort
          //analyse s res
          //wp-info
          /*
            * wp-info
              WORD    size //can be ignored
              UIN     uin of search result item
              LNTS    nick
              LNTS    first
              LNTS    last
              LNTS    email

              BYTE    auth (0=required, 1=always)
              BYTE    status (00 offline, 01 online, 02 not webaware)
              BYTE    unknown, usually 0
              BYTE    gender
              BYTE    age
          */

          byte[] ba;

          /*
            39 00
            2B A9 A7 01
            06 00 41 64 6F 6C 67 00
            0A 00 41 6C 65 78 61 6E 64 65 72 00
            09 00 44 6F 6C 67 75 6E 69 6E 00
            11 00 61 64 6F 6C 67 40 66 72 6F 6D 72 75 2E 63 6F 6D 00

            00 02 00 00 00
            00 00
          */

          ofs+=2; //skip size

          int srUin = aimutil_getIcqUin(data, ofs);
          ofs+=4;

          ba = getLNTS(data, ofs);
          String nick = byteArray2string(ba);
          ofs+= ba.length + 3;

          ba = getLNTS(data, ofs);
          String firstName = byteArray2string(ba);
          ofs+= ba.length + 3;

          ba = getLNTS(data, ofs);
          String lastName = byteArray2string(ba);
          ofs+= ba.length + 3;

          ba = getLNTS(data, ofs);
          String email = byteArray2string(ba);
          ofs+= ba.length + 3;

          long quantityOfResults = -1;

          if (endOfSearch)
          {
            ofs = data.length - 4;
            //DWORD   lasting results (LE) //ignored
            quantityOfResults = aimutil_get32_le(data, ofs);
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("QOR: "+quantityOfResults);
          }
          //parsed

          synchronized (operationResponseLock)
          {
            if (this.userSearchResults == null)
              this.userSearchResults = new UserSearchResultsImpl();

            this.userSearchResults.addSearchResult(
              ""+srUin,
              StringUtil.mkNull(nick.trim()),
              StringUtil.mkNull( (firstName.trim() + " " + lastName.trim()).trim() ),
              StringUtil.mkNull(email.trim())
            );

            if (endOfSearch)
            {
              if (quantityOfResults > this.userSearchResults.getSearchResults().size())
                this.userSearchResults.setTruncated(true);

              //report end of search
              operationResponseLock.notify();
            }
          }
          return true;
        default:
          return false;
      }
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parse_wpfull unknown format", ex);
      /**
      synchronized (asyncOps)
      {
        for (int aop = 0; aop < currentOps.size(); ++aop)
        {
          Object op = currentOps.get(aop);
          if (op != null && op instanceof OpGetUserDetails)
          {
            OpGetUserDetails om = (OpGetUserDetails) op;
            om.errorFetchingReceived();
          }
        }
      }
      */
      synchronized (operationResponseLock)
      {
        this.userDetailsUnknownFormat = true;
        operationResponseLock.notify(); //for obsolete sync methods
      }
    }
    return false;
  }

  private boolean parseUserDetails(final byte[] data, final int flags, PluginContext ctx)
  throws MessagingNetworkException, IOException
  {
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseUserDetails() start");
    //System.err.println("pud");
    //HexUtil.dump(data);

    switch (flags)
    {
      case 0x0001 :
        try
        {
          /*
            [0x00:] 00 15 00 03 00 01
                    00 1b 00 02 //snac id
            [0x0a:] 00 01 00 6f //tlv
            [0x0e:] 6d 00 //len
            [0x10:] 8e 95 b5 02 //uin of requester
            [0x14:] da 07  //const
            [0x16:] 34 00  //variable?
            [0x18:] c8 00 //variable, can be 0xc800 or 0xdc00
            [0x1a:] 0a  //const
            09
            00
            41 61 6e 6e 6b 6b 68 68 "Aannkkhh" //nick
            00
            0c
            00
            45 76 67 65  6e 69 69 2f  4a 6f 65 "Evgenii/Joe"
          */

          synchronized (operationResponseLock)
          {
            if (this.userDetails == null)
            {
              //final long snacId = aimutil_get32(data, 6);
              if ((aimutil_get16(data, 10)) != (1)) MLang.EXPECT_EQUAL(aimutil_get16(data, 10), 1, "tlv type", "1", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
              int ofs = 0x10;
              final long requesterUin = aimutil_getIcqUin(data, ofs);
              ofs+= 4;
              if ((this.uin) != (requesterUin)) MLang.EXPECT_EQUAL(this.uin, requesterUin, "this.uin", "requesterUin", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
              if ((aimutil_get16(data, ofs)) != (0xda07)) MLang.EXPECT_EQUAL(aimutil_get16(data, ofs), 0xda07, "field1", "0xda07", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_PROTOCOL_ERROR_NOT_LOGGED_OFF);
              ofs+= 4; //skip variable field2

              //WORD  subtype
              int userInfoReplyType = (aimutil_get16(data, ofs) << 8) & 0xffff00;
              ofs+= 2;

              //BYTE  result
              userInfoReplyType |= aimutil_get8(data, ofs); //0xa
              ofs++;

              if (userInfoReplyType == 0xc8000a)
              {
                byte[] ba;

                ba = getLNTS(data, ofs);
                String nick = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String firstName = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String lastName = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String email = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String homeCity = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String homeState = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String homePhone = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String homeFax = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String homeStreet = byteArray2string(ba);
                ofs+= ba.length + 3;

                ba = getLNTS(data, ofs);
                String cellPhone = byteArray2string(ba);
                ofs+= ba.length + 3;

                boolean isCellPhoneSMSEnabled;
                if (cellPhone.endsWith(" SMS"))
                {
                  isCellPhoneSMSEnabled = true;
                  cellPhone = cellPhone.substring(0, cellPhone.length() - 4);
                }
                else
                  isCellPhoneSMSEnabled = false;

                ba = getLNTS(data, ofs);
                String homeZipcode = byteArray2string(ba);
                ofs+= ba.length + 3;
                ofs+= 2; //skip WORD country (LE)
                ofs++; //skip BYTE gmt
                //BYTE auth required (00 yes, 01 no)

                boolean authorizationRequired = (data[ofs] == 0);

                this.userDetails = new UserDetailsImpl(
                  StringUtil.mkNull(nick.trim()),
                  StringUtil.mkNull( (firstName.trim() + " " + lastName.trim()).trim() ),
                  StringUtil.mkNull(email.trim()),
                  StringUtil.mkNull(homeCity.trim()),
                  StringUtil.mkNull(homeState.trim()),
                  StringUtil.mkNull(homePhone.trim()),
                  StringUtil.mkNull(homeFax.trim()),
                  StringUtil.mkNull(homeStreet.trim()),
                  StringUtil.mkNull(cellPhone.trim()),
                  isCellPhoneSMSEnabled,
                  StringUtil.mkNull(homeZipcode.trim()),
                  authorizationRequired
                  );

                synchronized (asyncOps)
                {
                  for (int aop = 0; aop < currentOps.size(); ++aop)
                  {
                    //CAT.debug("aop: "+aop);
                    Object op = currentOps.get(aop);
                    //CAT.debug(">>>> "+op);
                    if (op != null && op instanceof AsyncOperations.OpGetUserDetails)
                    {
                      AsyncOperations.OpGetUserDetails om = (AsyncOperations.OpGetUserDetails) op;
                      om.userDetailsReceived(userDetails);
                    }
                  }
                }

                operationResponseLock.notify(); //for obsolete sync methods
                return true;
              }
              else if (userInfoReplyType == 0xfa0014)
              {
                fireErrorFetchingUserDetails(data, ctx);
                return true;
              }
            }
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseUserDetails: extra info ignored");
          }
        }
        catch (Exception ex)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("userDetails response unknown format", ex);
          synchronized (asyncOps)
          {
            for (int aop = 0; aop < currentOps.size(); ++aop)
            {
              Object op = currentOps.get(aop);
              if (op != null && op instanceof AsyncOperations.OpGetUserDetails)
              {
                AsyncOperations.OpGetUserDetails om = (AsyncOperations.OpGetUserDetails) op;
                om.userDetailsUnknownFormatReceived();
              }
            }
          }
          synchronized (operationResponseLock)
          {
            operationResponseLock.notify(); //for obsolete sync methods
          }
        }
        return false;
      //case 0x0000 : //occurs!
      default :  //handle any flag just for safety
        CAT.debug("ud snacFlags: "+HexUtil.toHexString0x(flags));
        fireErrorFetchingUserDetails(data, ctx);
        return true;
    }
  }

  private void fetchUserDetails0(
    final AsyncOperations.OpGetUserDetails op,
    String dstLoginId,
    PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    /*
      request to fetch from our online mimicq user
        requested uin = 43476327  (0x02 97 65 67)
        requester uin = 26500286  (0x01 94 5C BE)

      00 15 / 00 02 //snac family/subtype
      00 00 //snac flags
      00 0D 00 02 //snac request id

      00 01 / 00 10 //tlv
         [00:] 0E 00 //length?
         [02:] BE 5C 94 01 //requester uin = 26500286  (0x01 94 5C BE)
         [06:] D0 07 34 00 B2 04 //?
         [0C:] 67 65 97 02 //requested uin = 43476327  (0x02 97 65 67)
    */

    final long id = ((snac_nextid++ & 0xFFFF) << 16) | 2;
    SNAC p = new SNAC(0x0015, 0x0002, 0, 0, id);
    p.addWord(0x0001);
    p.addWord(0x0010);
    p.addWord(0x0e00);
    p.addIcqUin(uin); //requester
    p.addWord(0xd007);
    p.addWord(0x3400);
    p.addWord(0xb204);

    int dstUin = IcqUinUtil.parseUin(dstLoginId, "dstLoginId", MessagingNetworkException.CATEGORY_STILL_CONNECTED);

    p.addIcqUin(dstUin); //requested

    synchronized (operationResponseLock)
    {
      this.responseType = RESPONSE_TYPE_USER_DETAILS;
      this.operationRequestId = id;
      this.waitingForUserDetailsFor = dstUin;
      this.userDetails = null;
      this.userDetailsUnknownFormat = false;
      this.errorFetchinguserDetails = false;
    }

    p.send(getBosConnNotNull(), op); //packet scheduled

    if (Defines.ENABLE_FAKE_PLUGIN)
    {
      this.userDetails = new UserDetailsImpl(
            "nick",
            "realName",
            "email@null.com",
            "homeCity",
            null,
            "(123)1225",
            "(123)1225",
            "homeStreet",
            "(123)1225",
            true,
            null,
            false);
      synchronized (asyncOps)
      {
        for (int aop = 0; aop < currentOps.size(); ++aop)
        {
          Object op1 = currentOps.get(aop);
          if (op1 != null && op1 instanceof AsyncOperations.OpGetUserDetails)
          {
            AsyncOperations.OpGetUserDetails om1 = (AsyncOperations.OpGetUserDetails) op1;
            om1.userDetailsReceived(userDetails);
          }
        }
      }

      operationResponseLock.notify(); //for obsolete sync methods
    }
  }

  void getUserDetailsComplete()
  {
    synchronized (operationResponseLock)
    {
      this.responseType = RESPONSE_TYPE_UNDEFINED;
      this.waitingForUserDetailsFor = -1;
      this.userDetails = null;
      this.userDetailsUnknownFormat = false;
      this.operationRequestId = -1;
      this.errorFetchinguserDetails = false;
    }
  }

  /** Overridden by ICQ2KReconnecting */
  protected void setReconnectData(int prevStatus_Oscar, Hashtable prevUinInt2cli)
  {
  }

  protected void clearReconnectState()
  {
    contactListUinInt2cli = null;
  }

  private void sendContacts0(
    final AsyncOperations.OpSendContacts op,
    final Aim_conn_t conn, final String dstLoginId, final String[] nicks, final String[] loginIds, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if ((nicks.length) != (loginIds.length)) Lang.ASSERT_EQUAL(nicks.length, loginIds.length, "nicks.length", "loginIds.length");
    //4�18573674�antich�62608614�art�30832422�Babak�111326219�benn gun�
    ByteArrayOutputStream bas = new ByteArrayOutputStream(128);
    int i = 0;
    bas.write(Integer.toString(nicks.length).getBytes());
    bas.write((byte) 0xfe);
    while (i < nicks.length)
    {
      bas.write(string2byteArray(loginIds[i]));
      bas.write((byte) 0xfe);
      bas.write(string2byteArray(nicks[i]));
      bas.write((byte) 0xfe);
      i++;
    }
    sendGenericMessage(op, conn, dstLoginId, bas.toByteArray(), MSGKIND_CONTACTS, ctx);
  }

  private void sendGenericMessage(
    final AsyncOperations.OpSendGenericMessage op,
    final Aim_conn_t conn, final String dstLoginId, final byte[] msg_bytes, final int msgKind, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    sendGenericMessage(op, conn, dstLoginId, msg_bytes, msgKind, DEFAULT_MSG_FMT, false, false, /*true, true, */ctx);
  }

  private void sendGenericMessage001(
    final AsyncOperations.OpSendGenericMessage op,
    final Aim_conn_t conn, final String dstLoginId, final byte[] msg_bytes, final int msgKind, boolean sendAckRequestAndWait, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    sendGenericMessage(op, conn, dstLoginId, msg_bytes, msgKind, DEFAULT_MSG_FMT, sendAckRequestAndWait, sendAckRequestAndWait, ctx);
  }

  private void sendGenericMessage(
    final AsyncOperations.OpSendGenericMessage op,
    final Aim_conn_t conn, final String dstLoginId, final byte[] msg_bytes, final int msgKind, final int msgfmt, boolean sendAckRequest, boolean waitForAck, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    sendGenericMessage002(op, conn, dstLoginId, msg_bytes, msgKind, msgfmt, sendAckRequest, sendAckRequest, waitForAck, ctx);
  }

  private void sendGenericMessage002(
    final AsyncOperations.OpSendGenericMessage op,
    final Aim_conn_t conn, final String dstLoginId,
    final byte[] msg_bytes, final int msgKind, final int msgfmt,
    boolean sendServerAckRequest, boolean sendClientAckRequest, boolean waitForAck, PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    if (Defines.ENABLE_FAKE_PLUGIN)
    {
      waitForAck = false;
    }

    if (msg_bytes == null) Lang.ASSERT_NOT_NULL(msg_bytes, "msg_bytes");
    if (msg_bytes.length > AIMConstants.MAXMSGLEN) MLang.EXPECT(msg_bytes.length <= AIMConstants.MAXMSGLEN, "message is too long: length==" + msg_bytes.length + ", which exceeds a maximum of " + AIMConstants.MAXMSGLEN, MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_TEXT_MESSAGE_IS_TOO_LONG);
    long msg_id = 6 | ((((long) 0xffff) & this.nextMsgId++) << 16);
    SNAC p = new SNAC(0x0004, 0x0006, 0, 0, msg_id);

    //msgfmt 2:
    // 0004 0006  0000 0010 0006   33 46 3e 04 af 09 00 00   00 02   08 34 35 34 35 34 37 33 34 00 05   00dc   00 00    33 46 3e 04 af 09 00 00    09 46 13 49 4c 7f 11 d1 82 22 44 45 53 54 00 00 00 0a 00 02 00 01 00 0f 00 00 27 11   00b4   1b 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 00 00 00 00 fa ff 0e 00 fa ff 00 00 00 00 00 00 00 00 00 00 00   0013     00 00 00 01   007f                           00           38 fe 31 38 35 37 33 36 37 34 fe 61 6e 74 69 63 68 fe 36 32 36 30 38 36 31 34 fe 61 72 74 fe 33 30 38 33 32 34 32 32 fe 42 61 62 61 6b fe 31 31 31 33 32 36 32 31 39 fe 62 65 6e 6e 20 67 75 6e fe 39 34 38 37 38 38 34 30 fe 64 72 6e 61 6d 65 fe 32 32 39 30 32 35 35 32 fe 65 6c fe 37 37 30 34 37 34 37 31 fe 66 6d 69 6b 65 fe 36 33 32 36 31 36 33 35 fe 47 72 65 65 6e 6d 61 6e fe 00 00 03 00 00    -- eo l
    // f/s------  ---- ---------   kuka-------------------   CONST   recipient----------------- CONST   len1   CONST    kuka-------------------    CONST------------------------------------------------------------------------------   len2   CONST--------------------------------------------------------------------------- decid CONST decid CONST---------------------------   msgkind  CONST------   len3==(msg_bytes.length+1)     ENCODING     msg_bytes

    //msgfmt 4:

    byte[] cookie = new byte[8];
    fillMsgCookie(cookie);
    p.addByteArray(cookie);

    p.addWord(msgfmt); //msg channel id aka msgfmt
    p.addStringPrependedWithByteLength(dstLoginId); //recipient

    switch (msgfmt)
    {
      case 2:
        p.addWord(5);
        p.addWord(msg_bytes.length - 0x59 + 0xB7); //len1
        p.addWord(0);
        p.addByteArray(cookie);
        p.addByteArray(new byte[] {(byte) 0x09, (byte) 0x46, (byte) 0x13, (byte) 0x49, (byte) 0x4c, (byte) 0x7f, (byte) 0x11, (byte) 0xd1, (byte) 0x82, (byte) 0x22, (byte) 0x44, (byte) 0x45, (byte) 0x53, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0f, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x11});
        p.addWord(msg_bytes.length - 0x59 + 0x8F); //len2
        p.addByteArray(new byte[] {(byte) 0x1b, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        p.addWord(0xfdff); //decid
        p.addWord(0x0e00); //const
        p.addWord(0xfdff); //decid
        p.addByteArray(new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        p.addWord(msgKind);
        p.addDWord(1); //const
        p.addWord(msg_bytes.length + 1); //len3
        p.addByte(0); //encoding
        p.addByteArray(msg_bytes);       //copied from libfaim
        p.addByte(0); //const
        break;
      case 4:
        //TLV(5)
        p.addWord(5);
        p.addWord(4+1+1+2+msg_bytes.length+1); //len1
        p.addIcqUin(this.uin);  //  UIN    my uin
        p.addByte(msgKind);  //BYTE   msg-subtype
        p.addByte(0);  //BYTE   msg-flags
        p.addLNTS(msg_bytes);  //LNTS   msg
        break;
      default:
        Lang.ASSERT_FALSE("invalid msgfmt: "+msgfmt);
    }

    if (sendServerAckRequest)
    {
      //request server ack for offline recipient
      p.addDWord(0x00060000);
      //if (op != null) op.setRequestServerAck(sendServerAckRequest);
    }

    if (sendClientAckRequest)
    {
      //request server ack for online recipient
      p.addDWord(0x00030000);
      //if (op != null) op.setRequestClientAck(sendClientAckRequest);
    }

    //packet created.

    synchronized (operationResponseLock)
    {
      this.operationRequestId = msg_id;
      this.currentMsgCookie = cookie;
    }

    if (op != null) op.setWaitForAck(waitForAck);

    getMessageQueueNotNull().sendMessage(p.getFlapBody(), op); //packet scheduled
  }

  public void authorizationRequest0(
    AsyncOperations.OpSendGenericMessage op,
    String dstLoginId, String text, PluginContext ctx)
    throws MessagingNetworkException, IOException, InterruptedException
  {
    //nick FE first FE last FE email FE unk-char FE msg
    //msg-type: 06  user-msg  authorization request
    if (text == null) text = "";
    byte[] b = string2byteArray(text);
    if (b.length > 7000)
      throw new MessagingNetworkException("message length must be <= 7000, but it is "+b.length+" bytes long",
        MessagingNetworkException.CATEGORY_STILL_CONNECTED,
        MessagingNetworkException.ENDUSER_TEXT_MESSAGE_IS_TOO_LONG);
    ByteArrayOutputStream bas = new ByteArrayOutputStream(b.length+6);
    bas.write(new byte[] {(byte) 0xfe, (byte) 0xfe, (byte) 0xfe, (byte) 0xfe});
    bas.write((byte) (this.authorizationRequired ? 31 : 30));
    bas.write((byte) 0xfe);
    bas.write(b);
    //au1
    /*
      Sending SRVACK=false, CLTACK=false to DST.OFFLINE=true  causes NEVER-DELIVERED + ???msg error
      Sending SRVACK=false, CLTACK=false to DST.OFFLINE=false causes DELIVERED + no ack

      Sending SRVACK=true,  CLTACK=false to DST.OFFLINE=true  causes DELIVERED + an incoming ack
      Sending SRVACK=true,  CLTACK=false to DST.OFFLINE=false causes NEVER-DELIVERED

      Sending SRVACK=false, CLTACK=true  to DST.OFFLINE=true  causes ???
      Sending SRVACK=false, CLTACK=true  to DST.OFFLINE=false causes ???msg-error

      Sending SRVACK=true,  CLTACK=true  to DST.OFFLINE=true  causes ???
      Sending SRVACK=true,  CLTACK=true  to DST.OFFLINE=false rarely causes delivery timeout
    */

    /*
    boolean on = isOnline(dstLoginId, ctx);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(dstLoginId+" online: "+on);
    */

    sendGenericMessage002(
      op,
      getBosConnNotNull(),
      dstLoginId,
      bas.toByteArray(),
      MSGKIND_AUTH_REQUEST,
      DEFAULT_MSG_FMT,
      true,  //boolean sendServerAckRequest
      false, //boolean sendClientAckRequest
      false, //boolean waitForAck
      ctx);
  }

  public boolean isOnline(String dst, PluginContext ctx)
  throws MessagingNetworkException
  {
    return (getContactStatus_Oscar(dst, ctx) != StatusUtil.OSCAR_STATUS_OFFLINE);
  }

  private final static int DEFAULT_MSG_FMT = 4;

  public void authorizationResponse0(
    AsyncOperations.OpSendGenericMessage op,
    String dstLoginId, boolean grant, PluginContext ctx)
      throws MessagingNetworkException, IOException, InterruptedException
  {
    boolean on = isOnline(dstLoginId, ctx);

    //msg-type:
    //07    plain_msg      authorization denied
    //08    empty          authorization granted
    //au2
    sendGenericMessage002(
      op,
      getBosConnNotNull(),
      dstLoginId,
      new byte[] {},
      (grant ? MSGKIND_AUTH_GRANTED : MSGKIND_AUTH_DENIED),
      DEFAULT_MSG_FMT,
      !on,   //boolean sendServerAckRequest
      false, //boolean sendClientAckRequest
      !on,   //boolean waitForAck
      ctx);
  }

  public void isAuthorizationRequired0(
    final AsyncOperations.OpIsAuthorizationRequired op,
    final String dstLoginId, final PluginContext ctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    //long startGetUserDetailsInternal(String srcLoginId, String dstLoginId) adds an interna operation;
    //events from this operationId are never delivered to a external listeners
    //they are delivered to internal listeners only.
    //addInternalMessagingNetworkListener() adds an internal listener.
    //removeInternalMessagingNetworkListener() removes an internal listener.
    MessagingNetworkListener l =
      new MessagingNetworkAdapter()
      {
        public void getUserDetailsFailed(byte networkId, long operationId,
          String originalSrcLoginId, String originalDstLoginId,
          MessagingNetworkException ex)
        {
          if (loginId.equals(originalSrcLoginId) && originalDstLoginId.equals(dstLoginId))
          {
            op.fail(ex);
            if (Defines.DEBUG && CAT.isDebugEnabled())
                CAT.debug("isAuthorizationRequired for "+dstLoginId+" failed", ex);
          }
        }

        public void getUserDetailsSuccess(byte networkId, long operationId,
          String originalSrcLoginId, String originalDstLoginId,
          UserDetails userDetails)
        {
          //CAT.debug("gu src "+originalSrcLoginId+" dst "+originalDstLoginId);
          if (loginId.equals(originalSrcLoginId) && originalDstLoginId.equals(dstLoginId))
          {
            try
            {
              boolean result = userDetails.isAuthorizationRequired();
              if (Defines.DEBUG && CAT.isDebugEnabled())
                CAT.debug("isAuthorizationRequired for "+dstLoginId+" returned: "+result);
              op.resultReceived(result);
              CAT.debug("rr done");
              op.success();
              CAT.debug("succ done");
            }
            catch (MessagingNetworkException exar)
            {
              op.fail(exar);
              if (Defines.DEBUG && CAT.isDebugEnabled())
                CAT.debug("isAuthorizationRequired for "+dstLoginId+" failed", exar);
            }
          }
        }
      };
    ctx.getICQ2KMessagingNetwork().startGetUserDetails(loginId, dstLoginId, l);
  }

  void sendGenericMessageComplete()
  {
    synchronized (operationResponseLock)
    {
      this.operationRequestId = -1;
      this.currentMsgCookie = null;
    }
  }

  public final void addOperation(AsyncOperation op)
  throws MessagingNetworkException, InterruptedException
  {
    synchronized (asyncOps)
    {
      checkShuttingdown();
      asyncOps.add(op);
    }
  }

  final void addCurrentOperation(AsyncOperation op)
  throws MessagingNetworkException, InterruptedException
  {
    synchronized (asyncOps)
    {
      synchronized (asyncOps)
      {
        checkShuttingdown();
        currentOps.remove(op);
        currentOps.add(op);
      }
    }
  }

  public final void removeOperation(AsyncOperation op)
  {
    synchronized (asyncOps)
    {
      asyncOps.remove(op);
      synchronized (asyncOps)
      {
        currentOps.remove(op);
      }
    }
  }


  private final void failAsyncOps(MessagingNetworkException mex)
  {
    asyncOpQueue.stop(mex);

    List l;
    int len;
    synchronized (asyncOps)
    {
      l = (List) asyncOps.clone();
      len = asyncOps.size();
      asyncOps.clear();
      currentOps.clear();
    }
    int len2 = l.size();
    if (len != len2) Lang.ASSERT_EQUAL(len, len2, "len", "len2");
    for (int i = 0; i < len; ++i)
    {
      AsyncOperation op = (AsyncOperation) l.get(i);
      try
      {
        op.fail(mex);
      }
      catch (Exception ex) {}
    }
  }


  private final ArrayList asyncOps = new ArrayList(3);
  private final ArrayList currentOps = new ArrayList(2);

  private final AsyncOperationQueue asyncOpQueue;

  public long enqueue(AsyncOperationImpl op)
  {
    return enqueue(op, 0);
  }
    
  final long enqueue(AsyncOperationImpl op, long startTime)
  {
    try
    {
      checkShuttingdown();
      return asyncOpQueue.enqueue(op, startTime, ICQ2KMessagingNetwork.serverResponseTimeoutMillis);
    }
    catch (Exception ex)
    {
      op.fail(ex);
      return op.getId();
    }
  }

  public void runSynchronous(AsyncOperationImpl op)
  throws MessagingNetworkException
  {
    runSynchronous(op, 0);
  }
  
  public void runSynchronous(AsyncOperationImpl op, long startTime)
  throws MessagingNetworkException
  {
    try
    {
      checkShuttingdown();
      addCurrentOperation(op);
      asyncOpQueue.runSynchronous(op, startTime, ICQ2KMessagingNetwork.serverResponseTimeoutMillis);
    }
    catch (Exception ex)
    {
      op.fail(ex);
      handleException(ex, op.ctx);
    }
  }

  final void enqueue(AsyncOperationImpl op, MessagingNetworkListener l)
  throws MessagingNetworkException
  {
    try
    {
      op.setInternalListener(l);
      checkShuttingdown();
      op.start();
    }
    catch (Exception ex)
    {
      op.fail(ex);
    }
  }

  private OutgoingMessageQueue getMessageQueueNotNull()
  throws MessagingNetworkException
  {
    synchronized (shuttingDownLock)
    {
      if (messageQueue == null) throwLastError();
      return messageQueue;
    }
  }

  public String toString() { return loginId; }

  public void checkValid() throws MessagingNetworkException {}

  public final void resumeAsyncOps()
  {
    asyncOpQueue.resume();
  }

  public ContactListItem fakeplug_randomContactListItem()
  {
    synchronized (contactListUinInt2cli)
    {
      if (contactListUinInt2cli.size() == 0) return null;
      int n = RandomUtil.random((int) 0, contactListUinInt2cli.size() - 1);
      Enumeration e = contactListUinInt2cli.elements();
      Object it = null;
      while (n-- > -1) it = e.nextElement();
      return (ContactListItem) it;
    }
  }

  public void fakeplug_fakeOperationResponse()
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fakeOperationResponse() 1");
    synchronized (operationResponseLock)
    {
      this.userSearchResults = null;
      this.noUsersFound = true;
      errorFetchinguserDetails = true;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("fakeOperationResponse() 2");
      operationResponseLock.notify();
    }
  }

  void sendDeferredStatus(ContactListItem cli)
  throws Exception
  {
    synchronized (getFireStatusLock())
    {
      if (Defines.DEBUG && CAT.isDebugEnabled())
        CAT.debug("status delivery delay expired, delivering.");
      cli.setScheduledStatusChangeSendTimeMillis(Long.MAX_VALUE);
      ctx.getICQ2KMessagingNetwork().fireStatusChanged_Mim_Uncond(
        this.loginId,
        cli.getDstLoginId(),
        StatusUtil.translateStatusOscarToMim_cl_entry(cli.getStatusOscar()),
        MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
        null,
        MessagingNetworkException.ENDUSER_NO_ERROR);
    }
  }
}
