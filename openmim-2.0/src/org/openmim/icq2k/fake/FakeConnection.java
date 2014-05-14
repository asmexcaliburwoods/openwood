package org.openmim.icq2k.fake;

import java.io.*;
import java.net.*;
import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq2k.*;
import org.openmim.icq.utils.*;
import org.openmim.icq.utils.PipedInputStream;
import org.openmim.icq.utils.PipedOutputStream;

public final class FakeConnection extends Aim_conn_t
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(FakeConnection.class.getName());

  //private final Object fd = new Object();
  //private final InputStream pin;
  //private final InputStream inputStream;
  private PipedInputStream pin;
  private PipedOutputStream pout;
  private boolean closed = false;
  //private final Object seqnum_lock = new Object();
  private volatile int state = 0;

  private byte[] flapHeader;
  private int flapChannel = -1;
  /*
  private int expectedFlapDataLength = -1;
  */

  /*
  public static int REQPARAM_DELAY_BETWEEN_INCOMING_TEXT_MSGS_MILLIS_MIN;
  public static int REQPARAM_DELAY_BETWEEN_INCOMING_TEXT_MSGS_MILLIS_MAX;
  public static int REQPARAM_DELAY_BETWEEN_INCOMING_STATUS_CHANGES_MILLIS_MIN;
  public static int REQPARAM_DELAY_BETWEEN_INCOMING_STATUS_CHANGES_MILLIS_MAX;

  private long nextTextMsgSendTimeMillis = 0;
  private long nextStatusChangeSendTimeMillis = 0;
  */

  /*
    Every Nth outcoming textmsg will force 1 incoming textmsg to be received,
    where N is REQPARAM_TEXTMSG_DIVISOR.

    Every outcoming status change will force (1 + N) incoming status change events to be received,
    where N is REQPARAM_STATUSCHANGE_FACTOR.
  */
  public static int REQPARAM_TEXTMSG_DIVISOR;
  public static int REQPARAM_STATUSCHANGE_FACTOR;

  static
  {
    AutoConfig.fetchFromClassLocalResourceProperties(FakeConnection.class, true, false);
  }

  public FakeConnection(
    final Session session,
    InetAddress host, int port, FlapConsumer fc, PluginContext ctx)
  throws IOException, MessagingNetworkException
  {
    super(session, host, port, ctx, fc);
    outputStream = new FakeOutputStream();
    postOnConnect();
  }

  private final void postOnConnect()
  {
    session.runAt(
      System.currentTimeMillis() + 20,
      new MessagingTask((isBosConn()?"fake/bos/postOnConnect":"fake/auth/postOnConnect"), session)
      {
        public void run()
        {
          try {
            if (!session.isConnUp(isBosConn())) postOnConnect();
            else onConnect();
          }
          catch (Exception ex)
          {
            session.eatException(ex);
          }
        }
      }
    );
  }

  /**
    Does not throw exceptions.
  */
  public synchronized void closeSocket()
  {
    if (closed) return;
    closed = true;
    try { outputStream.close(); } catch (Exception ex) { if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex); }
  }

  public synchronized boolean isClosed()
  {
    return closed;
  }

  private byte[] snacHeader;

  private int outputState = 1;

  private synchronized void onOutput(byte[] b, int ofs, int len) throws IOException, MessagingNetworkException
  {
    try
    {
      if (pin != null)
      {
        byte[] b2 = new byte[len + pin.available()];
        int ofs2 = pin.read(b2, 0, pin.available());
        System.arraycopy(b, ofs, b2, ofs2, len);
        len += ofs2;
        b = b2;
        ofs = 0;
      }
      for(;;)
      {
        if (outputState == 1)
        {
          if (len >= 6)
          {
            if (flapHeader == null) flapHeader = new byte[6];
            System.arraycopy(b, ofs, flapHeader, 0, 6); ofs += 6; len -= 6;
          }
          else
          {
            if (len > 0) save(b, ofs, len);
            return;
          }
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("3");
          if ((flapHeader[0]) != (0x2a)) Lang.ASSERT_EQUAL(flapHeader[0], 0x2a, "flapHeader[0]", "'*'");
        }
        int datalen = Session.aimutil_get16(flapHeader, 4);
        //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("datalen: "+datalen);
        if (len < datalen)
        {
          if (len > 0) save(b, ofs, len);
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("3a");
          outputState = 2;
          return;
        }

        outputState = 1;

        byte flapchan = flapHeader[1];
        if (flapchan != 2)
        {
          byte[] pak = new byte[datalen];
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("4");
          System.arraycopy(b, ofs, pak, 0, datalen); ofs += datalen; len -= datalen;

          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("5");
          nonsnac(flapchan, pak);
        }
        else
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("6");
          if (snacHeader == null) snacHeader = new byte[10];
          System.arraycopy(b, ofs, snacHeader, 0, 10); ofs += 10; len -= 10;
          int snacfamily = Session.aimutil_get16(snacHeader, 0);
          int snacsubtype = Session.aimutil_get16(snacHeader, 2);
          int snacflags = Session.aimutil_get16(snacHeader, 4);
          long snacreqid = Session.aimutil_get32(snacHeader, 6);
          byte[] pak = new byte[datalen-10];
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("7: avail: "+len+", pak.len: "+pak.length);
          if (pak.length > 0)
          {
            System.arraycopy(b, ofs, pak, 0, pak.length); ofs += pak.length; len -= pak.length;
          }
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("7a");
          snac(snacfamily, snacsubtype, snacflags, snacreqid, pak);
        }
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("9");
      }
    }
    finally
    {
      try { if (pin != null && pin.available() <= 0) { pin = null; pout = null; } } catch (IOException expin) {}
      if (outputState == 1 && flapHeader != null) flapHeader = null;
      snacHeader = null;
    }
  }

  private void save(byte[] b, int ofs, int len) throws IOException
  {
    if (pin == null) pin = new PipedInputStream();
    if (pout == null) pout = new PipedOutputStream(pin);
    pout.write(b, ofs, len);
  }
  public synchronized void onConnect() throws IOException, MessagingNetworkException
  {
    //hello-packet
    //identical for auth service & bos service
    writeFlapPacket(1, new byte[] {0,0,0,1});
    state = 1;
  }

  private void nonsnac(byte flapchannel, byte[] pak) throws IOException, MessagingNetworkException
  {
    switch(flapchannel)
    {
      case 1:
      {
        switch (state)
        {
          case 1:
          {
            state = 2;

            //login-packet
            //DWORD   1
            //TLV(1)  UIN
            //...

            //reconnect-packet
            //DWORD   1
            //TLV(6)  auth-cookie

            if (pak.length < 9 || Session.aimutil_get32(pak, 0) != 1) return;

            int type = Session.aimutil_get16(pak, 4);
            if (type == 1)
            {
              //login-packet
              String uin = session.aimutil_getString(pak, 7);

              //login-reply-packet
              /*
                TLV(1)  UIN
                TLV(5)  string, BOS-address:port
                TLV(6)  auth-cookie
              */
              ByteArrayOutputStream bas = new ByteArrayOutputStream(320);
              bas.write(new byte[] {0, 1, 0, (byte) (uin.length() & 0xff)});
              bas.write(uin.getBytes());
              String bos = "1.2.3.4:12345";
              bas.write(new byte[] {0, 5, 0, (byte) (bos.length() & 0xff)});
              bas.write(bos.getBytes());
              bas.write(new byte[] {0, 6, 1, 0});
              bas.write(new byte[256]);
              byte[] b = bas.toByteArray();
              writeFlapPacket(4, b);
              return;
            }
            else
            if (type == 6)
            {
              //reconnect-packet received.

              //send SNAC 01/03 server ready
              //12 WORD  unknown, usually 0001 0002 0003 0004 0006 0008
              //                          0009 000A 000B 000C 0013 0015
              SNAC p = new SNAC(1, 3);
              p.addByteArray(new byte[] {00,01,00,02,00,03,00,04,00,06,00,8,00,9,00,0x0A,00,0x0B,00,0x0C,00,0x13,00,0x15});
              writeFlapPacket(2, p.getFlapBody());
              state = 5;
              return;
            }
          }
        }
      }
    }
  }

  private void snac(int snacfamily, int snacsubtype, int snacflags, long snacreqid, byte[] pak) throws IOException, MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("8");
    switch (snacsubtype | (snacfamily >>> 16))
    {
      case 0x00010006:
      {
        //SNAC 01/07 rate info response, response to 1,06
        SNAC p = new SNAC(1, 7);
        p.addByteArray(new byte[181+2+4+17*4]);
        writeFlapPacket(2, p.getFlapBody());
        return;
      }
      case 0x00040006:
      {
        long msgid = snacreqid;
        byte[] cookie = new byte[8];
        System.arraycopy(pak, 0, cookie, 0, 8);
        int msgfmt = Session.aimutil_get16(pak, 8);
        String dstLoginId = session.aimutil_getString(pak, 10);
        /*
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
          }

          if (sendClientAckRequest)
          {
            //request server ack for online recipient
            p.addDWord(0x00030000);
          }
        */

        SNAC p = new SNAC(4, 0xC, 0, 0, msgid);
        p.addByteArray(new byte[10]);
        p.addByteArray(cookie);
        p.addWord(0);
        p.addStringPrependedWithByteLength(dstLoginId);
        //further data skipped
        writeFlapPacket(2, p.getFlapBody());
        return;
      }
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("8a");
    session.fakeplug_fakeOperationResponse();
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("8b");
  }

  //final private Object lock1 = new Object();
  private float sentt = 0;
  private float recvt = 0;
  private float sents = 0;
  private float recvs = 0;

  /*
    if we have X outcoming msgs, we send X/REQPARAM_TEXTMSG_DIVISOR incom. msgs.

    Every outcoming status change will force (1 + REQPARAM_STATUSCHANGE_FACTOR) incoming status change events to be received,
    */
  public void onSendTextMsg() throws IOException, MessagingNetworkException
  {
    if (REQPARAM_TEXTMSG_DIVISOR == 0) return;
    synchronized(this)
    {
      ++recvt;
      while (sentt < recvt/REQPARAM_TEXTMSG_DIVISOR) { sendTextMsg(); ++sentt; }
    }
  }

  public void onSendStatusChange() throws IOException, MessagingNetworkException
  {
    //(N+1)th statusChange is fired by the Session object.
    synchronized(this)
    {
      ++recvs;
      while (sents < recvs*REQPARAM_STATUSCHANGE_FACTOR) { sendStatusChange(); ++sents; }
    }
  }

  public void onCheckInput() throws IOException, MessagingNetworkException
  {
    /*

    if (state < 5) return; //do not send while logging in

    synchronized (checkInputLock)
    {
      long now = System.currentTimeMillis();
      if (now >= nextTextMsgSendTimeMillis)
      {
        nextTextMsgSendTimeMillis = now + RandomUtil.random(
          REQPARAM_DELAY_BETWEEN_INCOMING_TEXT_MSGS_MILLIS_MIN,
          REQPARAM_DELAY_BETWEEN_INCOMING_TEXT_MSGS_MILLIS_MAX);
        sendTextMsg();
      }
      if (now >= nextStatusChangeSendTimeMillis)
      {
        nextStatusChangeSendTimeMillis = now + RandomUtil.random(
          REQPARAM_DELAY_BETWEEN_INCOMING_STATUS_CHANGES_MILLIS_MIN,
          REQPARAM_DELAY_BETWEEN_INCOMING_STATUS_CHANGES_MILLIS_MAX);
        sendStatusChange();
      }
    }
    */
  }

  private void sendTextMsg() throws IOException, MessagingNetworkException
  {
    SNAC p = new SNAC(4, 7);     //10
    p.addByteArray(new byte[8]); //18
    int msgFormat = 1;
    p.addWord(msgFormat); //20
    /*
    if (msgFormat != 1 && msgFormat != 2 && msgFormat != 4)
      log("parseMessage(): msg format is unknown: msgFmt=" + msgFormat + ", incoming message ignored");
    */
    p.addStringPrependedWithByteLength(""+RandomUtil.random((int)1000000, (int)2000000)); //sender uin
    p.addWord(0); //WORD warning level
    p.addWord(1); //headerTlvCount
    p.addTlv(0xCAFE, new byte[]{}); //fake header tlv
    int type = (msgFormat == 1 ? 2 : 5);
    //byte[] msgBlock = tlvlist.skipNtlvsGetTlvOfTypeAsByteArray(headerTlvCount, type);
    /*
      switch (msgFormat)
      {
        case 1 : //msg format == 1
        {
          int msgOfs = 2;
          msgOfs += 2 + aimutil_get16(msgBlock, msgOfs);
          msgOfs += 2;

          //final block length
          msgOfs += 2;

          //flag1 = aimutil_get16(msgBlock);
          msgOfs += 2;
          //flag2 = aimutil_get16(msgBlock);
          msgOfs += 2;
          //      00 00 //enc: 0000 is "ASCII", 0002 is Unicode bib endian "UnicodeBig"
          //      00 00 //ignored 0000 when ASCII, FFFF when UnicodeBig
          //      74 79 70   65 20 6d 73 - 67 20 61 6e   64 20 70 72   "type ms g and pr e"
          //      65 73 73 20   65 6e 74 65 - 72   "ess ente r"
          byte enc = (byte) aimutil_get8(msgBlock, msgOfs - 3);
          byte[] msgBytes = aimutil_getByteArray(msgBlock, msgOfs, msgBlock.length - msgOfs);
          String msg = getMsgText(msgBytes, enc);
          fireMessageReceived(senderLoginId, msg, ctx);
          break;
        }
      }
    */
    ByteArrayOutputStream b = new ByteArrayOutputStream(300);
    b.write((byte) 0); b.write((byte) 0); //skipped
    b.write((byte) 0); b.write((byte) 0); //some count
    b.write((byte) 0); b.write((byte) 0); //skipped
    b.write((byte) 0); b.write((byte) 0); //fb len
    b.write((byte) 0); b.write((byte) 0); //flag1 //unk, enc: ASCII
    b.write((byte) 0); b.write((byte) 0); //flag2 //0000/FFFF
    b.write(RandomStringUtil.randomString(6, 50).getBytes());
    p.addTlv(type, b.toByteArray()); //msgBlock
    writeFlapPacket(2, p.getFlapBody());
  }

  private static final int[] OSCAR_STATUS = new int[] {
    StatusUtil.OSCAR_STATUS_OFFLINE,
    StatusUtil.OSCAR_STATUS_ONLINE,
    StatusUtil.OSCAR_STATUS_FREE_FOR_CHAT,
    StatusUtil.OSCAR_STATUS_AWAY,
    StatusUtil.OSCAR_STATUS_NA2,
    StatusUtil.OSCAR_STATUS_NA,
    StatusUtil.OSCAR_STATUS_OCCUPIED2,
    StatusUtil.OSCAR_STATUS_OCCUPIED,
    StatusUtil.OSCAR_STATUS_DND
  };

  private static final Hashtable OSCAR_STATUS_2_INDEX = new Hashtable(OSCAR_STATUS.length);

  static
  {
    for(int i = 0; i < OSCAR_STATUS.length; ++i)
    {
      OSCAR_STATUS_2_INDEX.put(new Integer(OSCAR_STATUS[i]), new Integer(i));
    }
  }

  private void sendStatusChange() throws IOException, MessagingNetworkException
  {
    ContactListItem it = session.fakeplug_randomContactListItem();
    if (it == null) return; //empty contact list

    Integer index_ = (Integer) OSCAR_STATUS_2_INDEX.get(new Integer(it.getStatusOscar()));
    int index = 0;
    if (index_ != null) index = index_.intValue();
    ++index;
    if (index >= OSCAR_STATUS.length) index = 0;
    int oscarStatus = OSCAR_STATUS[index];
    sendStatusChange(oscarStatus, it.getDstLoginId());
  }

  private void sendStatusChange(int newOscarStatus, String contactDstLoginId) throws IOException, MessagingNetworkException
  {
    if (newOscarStatus != StatusUtil.OSCAR_STATUS_OFFLINE)
    {
      SNAC p = new SNAC(0x0003, 0x000b);
      p.addByte(contactDstLoginId.length());
      p.addByteArray(contactDstLoginId.getBytes());
      p.addDWord(0);
      p.addTlvDWord(6, 0x0000ffff & newOscarStatus);
      writeFlapPacket(2, p.getFlapBody());
    }
    else //newOscarStatus == StatusUtil.OSCAR_STATUS_OFFLINE
    {
      SNAC p = new SNAC(0x0003, 0x000c);
      p.addByte(contactDstLoginId.length());
      p.addByteArray(contactDstLoginId.getBytes());
      writeFlapPacket(2, p.getFlapBody());
    }
  }

  /*
  private final int available()
  throws IOException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedIOException();
    return getInputStream().available();
  }
  */

  /** reads all incoming flap packets that are fully received and then fires FlapConsumer events */
  /*public final void poll() throws IOException, MessagingNetworkException
  {
    try
    {
      int exp;

      synchronized (fd)
      {
        exp = expectedFlapDataLength;
        if (exp == 0) return;
        expectedFlapDataLength = 0;
      }

      int av = available();

      synchronized (this)
      {
        while (true)
        {
          if (exp == -1) //expecting flap header
          {
            if (av < 6) break;

            //flapheader arrived, reading it.
            read(flapHeader, 0, 6);
            av -= 6;

            //six bytes:
            //0, byte - always 0x2a
            //1, byte - flap channel
            //2, word - flap seqnum //not needed
            //4, word - flap body length: number of data bytes that follow in this flap packet.
            MLang.EXPECT_EQUAL(
              flapHeader[0], 0x2a, "flapHeader[0]", "0x2a ('*')",
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
              MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
            flapChannel = 0xff & (int) flapHeader[1];
            exp = Session.aimutil_get16(flapHeader, 4); //dataFieldLength
          }

          if (exp < 0) Lang.ASSERT_NON_NEGATIVE(exp, "exp");

          byte[] flapBody = AllocUtil.createByteArray(session, exp);

          if (exp > 0) //expecting nonzero flapbody
          {
            if (av < exp) break;

            //flapbody arrived, reading it.
            read(flapBody, 0, exp);
            av -= exp;
          }

          session.handleRx(FakeConnection.this, flapChannel, flapBody, 0, exp);

          exp = -1;         //expecting flap header
          flapChannel = -1; //expecting flap header
          continue;
        } //of while
      }

      synchronized (fd)
      {
        if (exp == 0) Lang.ASSERT(exp != 0, "exp must not be zero here, but it is.");
        expectedFlapDataLength = exp;
      }
    }
    catch (InterruptedException ex)
    {
      throw new InterruptedIOException();
    }
  }
  private final java.io.InputStream getInputStream()
  throws IOException
  {
    if (isClosed()) throw new IOException("connection closed");
    return inputStream;
  }

  private final void read(byte[] b)
  throws IOException
  {
    read(b, 0, b.length);
  }

  private final void read(byte[] b, int ofs, int len)
  throws IOException
  {
    org.openmim.icq.util.Acme.Utils.readFullyWithTimeout(
      getInputStream(), b, ofs, len, ICQ2KMessagingNetwork.socketTimeoutMillis);

    if (Defines.DEBUG_FULL_DUMPS)
      Log4jUtil.dump(CAT, "recv/"+this, b, ofs, len, "IN: ");
  }
  */

  protected final int getNumberOfBuffers()
  {
    return 2 * ICQ2KMessagingNetwork.REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM;
  }

  class FakeOutputStream extends OutputStream
  {
    boolean closed = false;

    FakeOutputStream() {}

    public final void write(int b) throws IOException { write(new byte[] {(byte) b}); }
    public final synchronized void write(byte[] b, int off, int len) throws IOException
    {
      checkClosed();
      if (len <= 0) return;
      try
      {
        onOutput(b, off, len);
      }
      catch (Exception ex)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("", ex);
        throw new IOException(ex.getMessage());
      }
    }

    private final void checkClosed() throws IOException { if (closed) throw new IOException("connection closed"); }
    public final synchronized void flush() throws IOException { /* checkClosed();*/ }
    public final synchronized void close()  throws IOException { closed = true; }
  }

  private void writeFlapPacket(int channel, byte[] body)
  throws IOException, MessagingNetworkException
  {
    int len = body.length;
    byte[] b = new byte[len + 6];
    b[0] = 0x2a;
    b[1] = (byte) channel;
    b[4] = (byte) (len >> 8);
    b[5] = (byte) len;
    System.arraycopy(body, 0, b, 6, len);

    synchronized (this)
    {
      int seqnum = 0;
      b[2] = (byte) (seqnum >> 8);
      b[3] = (byte) seqnum;
      fetch(b); //see mpconnection implementation
    }
  }

  void fetch(byte[] bf) throws IOException
  {
    if (isClosed()) throw new IOException("connection closed");
    int datalen = bf.length;
    if (datalen <= 0) return;
    ac.consume(bf, 0, datalen);
  }
}
