package org.openmim.icq2k;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;

import java.io.*;

/**
  Represents a single TCP/IP socket connection
  with some functionality for dispatching
  incoming OSCAR events.
*/
public final class SimpleTCPConnection extends Aim_conn_t
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SimpleTCPConnection.class.getName());
  private java.net.Socket fd;
  private final Object lock2 = new Object();
  //1private InputStream inputStream;

  /**
   Creates a new connection to the specified
   host of specified type.
   <br>
   <br>type: Type of connection to create
   <br>destination: Host to connect to (in "host:port" or "host" syntax)
   */
  public SimpleTCPConnection(
    Session session, int connType, java.net.InetAddress host, int port, 
    PluginContext ctx, FlapConsumer fc)
  throws MessagingNetworkException, java.io.IOException
  {
    super(session, host, port, ctx, fc);
    java.net.Socket sok = TransportChooser.connect(host, port, (int) ctx.getICQ2KMessagingNetwork().getSocketTimeoutMillis());
    synchronized (lock2)
    {
      fd = sok;
    }
    ctx.getSocketRegistry().addSocket(sok.getInputStream(), ac, (int) ctx.getICQ2KMessagingNetwork().getSocketTimeoutMillis());
    //1inputStream = sok.getInputStream();
    outputStream = new BufferedOutputStream(sok.getOutputStream(), 1024);
  }

  /**
  Closes the connection.
  <p>
  Never throws Exceptions.
  */
  public void closeSocket()
  {
    java.net.Socket fd;
    synchronized (lock2)
    {
      fd = this.fd;
      if (fd == null) return;
      this.fd = null;
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("closing socket "+fd);
    try
    {
      fd.close();
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex in java.net.Socket.close()", ex);
    }
  }

  public boolean isClosed()
  {
    synchronized (lock2)
    {
      return fd == null;
    }
  }

  //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug();

  /*//1
  private final int available()
  throws IOException
  {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    return getInputStream().available();
  }


  private final byte[] flapHeader = new byte[6];
  private int flapChannel = -1;
  private int expectedFlapDataLength = -1;
  */

  /** reads all incoming flap packets that are fully received and then fires FlapConsumer events */
  /*//1
  public final void poll() throws IOException, MessagingNetworkException
  {
    Object fds = this.fd;
    try
    {
      session.checkShuttingdown();

      if (fds == null) return;

      int exp;

      synchronized (fds)
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

          session.handleRx(this, flapChannel, flapBody, 0, exp);

          exp = -1;         //expecting flap header
          flapChannel = -1; //expecting flap header
          continue;
        } //of while
      }

      synchronized (fds)
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

    if (Defines.DEBUG_FULL_DUMPS && CAT.isDebugEnabled())
      Log4jUtil.dump(CAT, "recv/"+this, b, ofs, len, "");
  }
  */

  protected final int getNumberOfBuffers()
  {
    return 2 * ICQ2KMessagingNetwork.REQPARAM_INPUT_DATA_HANDLING_THREADCOUNT_MAXIMUM;
  }
}
