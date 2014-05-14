package org.openmim.icq2k;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.AbstractConsumer;
import org.openmim.stuff.AsyncOperation;
import org.openmim.stuff.Defines;
import org.openmim.infrastructure.ObjectCache;
import org.openmim.icq.util.*;
import org.openmim.icq.utils.*;
import org.openmim.icq.utils.PipedInputStream;
import org.openmim.icq.utils.PipedOutputStream;

/**
  Represents a base class for both usual TCP/IP socket connections and
  multiplexor-intermediated connections.
  Has with some functionality for dispatching incoming OSCAR events.
*/
public abstract class Aim_conn_t
{
  private final static boolean CONNECTION_BROKEN_TEST = Defines.ENABLE_RECONNECTOR_TESTER;

  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Aim_conn_t.class.getName());
  protected final Session session;
  protected OutputStream outputStream;
  private int seqnum;
  private long nextPacketSendTimeMillis = 0;
  private boolean rateControlOn = false;
  protected final PluginContext ctx;
  private final FlapConsumer flapConsumer;

  private final Frame traktor;
  private static boolean traktorConnectionDown = false;

  private static ObjectCache buffers;
  private static final Object buffersLock = new Object();
  protected PipedInputStream pin;
  private PipedOutputStream pout;
  private int flapChannel = -1;
  private int expectedFlapDataLength = -1;

  /**
    Creates a new connection to the specified
    host of specified type.
    <br>
    <br>
    type: Type of connection to create
    <br>
    destination: Host to connect to (in "host:port" or "host" syntax)
  */
  public Aim_conn_t(Session session, java.net.InetAddress host, int port, PluginContext ctx, FlapConsumer flapConsumer)
  throws MessagingNetworkException, java.io.IOException
  {
    this.session = session;
//    seqnum = 0;
    this.ctx = ctx;
    if (flapConsumer == null) Lang.ASSERT_NOT_NULL(flapConsumer, "flapConsumer");
    this.flapConsumer = flapConsumer;
    if (CONNECTION_BROKEN_TEST)
    {
      if (traktorConnectionDown)
        throw new IOException("network is down");

      traktor = new Frame(""+host+":"+port+" - Traktor");
      final Checkbox c = new Checkbox("break this & ALL future connections");
      c.setState(traktorConnectionDown);
      c.addItemListener(
        new ItemListener()
        {
          public void itemStateChanged(ItemEvent e)
          {
            traktorConnectionDown = c.getState();
            try
            {
              if (traktorConnectionDown) closeSocket();
            }
            catch (Exception ex)
            {
            }
          }
        });
      traktor.add(c);
      traktor.setSize(100,50);
      traktor.setLocation(200,450);
      traktor.setVisible(true);
    }
    else
    {
      traktor = null;
    }
    if (buffers == null)
    {
      synchronized (buffersLock)
      {
        if (buffers == null)
        {
          buffers = new ObjectCache(getNumberOfBuffers(), getNumberOfBuffers())
          {
            protected final Object create() { return new byte[32*1024]; }
          };
        }
      }
    }
  }

  /**
    Closes the connection.<p>
    Never throws Exceptions.
  */
  public abstract void closeSocket();

  public synchronized void setRateControlOn(boolean on)
  {
    this.rateControlOn = on;
  }


  private class Packet
  {
    byte[] pak; //flap body
    int chan; //flap channel
    PacketListener listener;
    AsyncOperation op;
  }

  private ArrayList outgoingPacketQueue = new ArrayList(10);

  private final void doSend(byte[] flapBody, int flapChannel, AsyncOperation op)
  throws IOException
  {
    OutputStream out = outputStream;
    if (out == null || isClosed())
      throw new IOException("connection closed ("+this+")");

    int len = flapBody.length;
    byte[] b = new byte[len + 6];
    b[0] = 0x2a;
    b[1] = (byte) flapChannel;
    b[4] = (byte) (len >> 8);
    b[5] = (byte) len;
    System.arraycopy(flapBody, 0, b, 6, len);

    synchronized (out)
    {
      int seqnum = nextSeqnum();
      b[2] = (byte) (seqnum >> 8);
      b[3] = (byte) seqnum;
      if (Defines.DEBUG_FULL_DUMPS && CAT.isDebugEnabled())
        Log4jUtil.dump(CAT, "--- sending data ------- (context: "+
          (op == null ? "session "+session : op.toString())+")", b, "");
      out.write(b);
    }
    out.flush();
  }

  /** asynchronous */
  public final void send(byte[] flapBody, int flapChannel, AsyncOperation op)
  throws IOException//, MessagingNetworkException
  {
    //no PacketListener
    send(flapBody, flapChannel, op, null);
  }

  /** asynchronous */
  public synchronized final void send(byte[] flapBody, int flapChannel, AsyncOperation op, PacketListener pl)
  throws IOException//, MessagingNetworkException
  {
    if (isClosed())
      throw new IOException("connection closed");

    Packet p = new Packet();
    p.pak = flapBody;
    p.chan = flapChannel;
    p.listener = pl;
    p.op = op;
    outgoingPacketQueue.add(p);

    if (outgoingPacketQueue.size() == 1)
      scheduleSend();
  }

  private void scheduleSend()
  {
    //note: the task is executed outside the session context!
    session.runAt(
      ctx,
      nextPacketSendTimeMillis,
      new MessagingTask("sendPacket", session)
      {
        public void run() throws Exception
        {
          //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("111sending packet");
          Packet pak;
          boolean empty;
          while (true)
          {
            boolean rateControlOn;
            pak = null;

            //CAT.debug("111before sync");
            synchronized (Aim_conn_t.this)
            {
              //CAT.debug("111in sync");
              rateControlOn = Aim_conn_t.this.rateControlOn;
              pak = (Packet) outgoingPacketQueue.remove(0);
              empty = outgoingPacketQueue.isEmpty();
            }

            //CAT.debug("111after sync");
            doSend(pak.pak, pak.chan, pak.op);
            if (pak.listener != null) pak.listener.packetSent();

            if (rateControlOn)
            {
              synchronized (Aim_conn_t.this)
              {
                nextPacketSendTimeMillis = System.currentTimeMillis() + ICQ2KMessagingNetwork.REQPARAM_ADVANCED_RATECONTROL_SENDPACKET_MILLIS;
              }
              if (!empty) scheduleSend();
              break;
            }
            if (empty) break;
          }
        }
      }
    );
  }

  private final int nextSeqnum()
  {
    int next = seqnum++;
    return next;
  }

  public abstract boolean isClosed();

  //bb04
  public final void parseFlapPackets(int flapChannel, byte[] flapBody, int ofs, int len)
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("parseFlapPackets");
      flapConsumer.parse(flapChannel, flapBody, ofs, len);
    }
    catch (Exception ex)
    {
      session.eatException(ex, "parseFlapPackets", ctx);
    }
  }

  public String toString() { return session.getLoginId(); }

  public final boolean isBosConn() { return flapConsumer.isBosConn(); }

  protected final AbstractConsumer ac = new AbstractConsumer()
  {
    protected final byte[] allocateBuffer() throws InterruptedException
    {
      return Aim_conn_t.getBuffer();
    }
    
    protected final void deallocateBuffer(byte[] b)
    {
      buffers.release(b);
    }

    public final void connectionClosed()
    {
    }
    
    protected final void deliver(final byte[] b, final int ofs0, final int len0)
    {
      int expectedFlapDataLength = Aim_conn_t.this.expectedFlapDataLength;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume/enter.  ofs0: "+ofs0+", len0: "+len0+", expectedFlapDataLength: "+expectedFlapDataLength);
      
      if (len0 <= 0) 
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume/abandon: len0 <= 0.");
        return; //!!!!!! ABANDON1
      }

      byte[] buf2 = Aim_conn_t.getBuffer();
      try
      {
        int available = 0;
        if (pin != null) available = pin.available();
        
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume: previously deferred data: "+available+" bytes");
      
        session.checkShuttingdown();

        int exp = expectedFlapDataLength;
        int av = available + len0;
        int ofs = ofs0;
        
        //bb01
        for (boolean firstLoop = true; ; firstLoop = false)
        {
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume: exp: "+exp);
          if (exp == -1) //expecting flap header
          {
            if (av < 6) 
            {
              if (Defines.DEBUG && CAT.isDebugEnabled()) 
              {
                if (av < 0) Lang.ASSERT_NON_NEGATIVE(av, "av");
                if (av == 0)
                {
                  if (firstLoop)
                    CAT.debug("consume/abandon/alert: av == 0.");
                  else
                    CAT.debug("consume/done: av == 0.");
                }
                else
                  CAT.debug("consume/abandon: av < 6.  av: "+av);
              }
              break; //!!!!!! ABANDON2
            }

            //flapheader arrived, reading it.
            byte[] flapHeader = buf2;
            int br = 0;
            if (pin != null) br = pin.read(flapHeader, 0, Math.min(pin.available(), 6));
            if (br < 6) { System.arraycopy(b, ofs, flapHeader, br, 6 - br); ofs += 6 - br; }
            av -= 6;

            //six bytes:
            //0, byte - always 0x2a
            //1, byte - flap channel
            //2, word - flap seqnum //not needed
            //4, word - flap body length: number of data bytes that follow in this flap packet.
            if (flapHeader[0] != 0x2a)
              MLang.EXPECT_EQUAL(
                flapHeader[0], 0x2a, "flapHeader[0]", "0x2a ('*')",
                MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
                MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
            flapChannel = 0xff & (int) flapHeader[1];
            exp = Session.aimutil_get16(flapHeader, 4); //dataFieldLength
          }

          if (exp < 0) Lang.ASSERT_NON_NEGATIVE(exp, "exp");

          byte[] flapBody = buf2;

          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume: exp/2: "+exp);
          
          if (exp > 0) //expecting nonzero flapbody
          {
            if (av < exp) 
            {
              if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("consume/abandon: av < exp.  av: "+av+", exp: "+exp);
              break; //!!!!!! ABANDON3
            }

            //flapbody arrived, reading it.
            int br = 0;
            if (pin != null) br = pin.read(flapBody, 0, Math.min(pin.available(), exp));
            if (br < exp) { System.arraycopy(b, ofs, flapBody, br, exp - br); ofs += exp - br; }
            av -= exp;
          }

          session.handleRx(Aim_conn_t.this, flapChannel, flapBody, 0, exp);

          exp = -1;         //expecting flap header
          flapChannel = -1; //expecting flap header
          continue;
        } //of while

        if (ofs < ofs0 + len0)
        {
          if (pin == null)
          {
            pin = new PipedInputStream();
            pout = new PipedOutputStream(pin);
          }
          pout.write(b, ofs, ofs0 + len0 - ofs);
        }

        if (pin != null && pin.available() == 0) { pin.close(); pin = null; pout.close(); pout = null; }
        Aim_conn_t.this.expectedFlapDataLength = exp;
      }
      catch (Exception ex)
      {
        session.eatException(ex, ctx);
      }
      finally
      {
        buffers.release(buf2);
      }
      
      if (Defines.DEBUG && CAT.isDebugEnabled())
      {
        String available = "0";
        if (pin != null) 
        {
          try
          {
            available = Integer.toString(pin.available());
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
            available = "("+ex+")";
          }
        }
        CAT.debug("consume/leave.  expectedFlapDataLength: "+expectedFlapDataLength+", deferred data: "+available+" bytes" );
      }
    }
  };

  private static final byte[] getBuffer()
  {
    try
    {
      if (buffers == null) Lang.ASSERT_NOT_NULL(buffers, "buffers");
      byte[] b = (byte[]) buffers.get();
      if (b == null) Lang.ASSERT_NOT_NULL(b, "b");
      return b;
    }
    catch (RuntimeException ex)
    {
      CAT.error("", ex);
      throw ex;
    }
  }

  protected abstract int getNumberOfBuffers();
}
