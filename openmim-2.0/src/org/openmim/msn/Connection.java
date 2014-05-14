package org.openmim.msn;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;

public abstract class Connection
{
  private final static boolean TRAKTOR_USED = false;

  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(Connection.class.getName());

  private Frame traktor;
  private static boolean traktorConnectionDown = false;

  /**
    Creates a new connection to the specified
    host of specified type.
    <br>
    <br>type: Type of connection to create
    <br>destination: Host to connect to (in "host:port" or "host" syntax)
  */
  public Connection(java.net.InetAddress host, int port, PluginContext ctx)
    throws MessagingNetworkException,
    java.io.IOException
  {
    if (TRAKTOR_USED)
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
      traktor.setLocation(230,450);
      traktor.setVisible(true);
    }
    else
    {
      traktor = null;
    }
  }

  public final int available()
  throws IOException
  {
    if (Thread.currentThread().isInterrupted())
      throw new InterruptedIOException();
    if (isClosed())
      throw new IOException("connection closed");
    return getInputStream().available();
  }

  public abstract void closeSocket();

  public final void flush()
  throws IOException, MessagingNetworkException
  {
    OutputStream os = getOutputStream();
    synchronized (os) { os.flush(); }
  }

  protected final java.io.InputStream getInputStream()
  throws IOException
  {
    if (isClosed())
      throw new IOException("connection closed");
    InputStream is = getInputStream0();
    if (is == null)
      throw new IOException("connection closed");
    return is;
  }

  protected abstract java.io.InputStream getInputStream0()
  throws IOException;

  protected final java.io.OutputStream getOutputStream()
  throws IOException
  {
    if (isClosed())
    {
      if (TRAKTOR_USED && traktor != null && !traktorConnectionDown) { traktor.dispose(); traktor = null; }
      throw new IOException("connection closed");
    }
    OutputStream os = getOutputStream0();
    if (os == null)
      throw new IOException("connection closed");
    return os;
  }

  protected abstract java.io.OutputStream getOutputStream0()
  throws IOException;

  public abstract boolean isClosed();

  /*
  private byte[] buf = new byte[8192];
  private int ofs = 0;
  private int len = 0;

  private void fill(InputStream is, long abortTime)
  throws IOException
  {
    for (;;)
    {
      if (System.currentTimeMillis() > abortTime) throw new IOException("connection timed out");
      int av = is.available();
      if (av == 0)
      {
        try
        {
          Thread.currentThread().sleep(100);
        }
        catch (InterruptedException ex)
        {
          throw new InterruptedIOException();
        }
        continue;
      }
      int capacity = buf.length - len;
      if (av > capacity) av = capacity;
      int rc = capacity - ofs;
      int pos = ofs + len;
      if (rc >= av)
      {
        int read = is.read(buf, pos, av);
        if (read < 0) read = 0;
        len += read;
        return;
      }
      //rc < av
      while (rc > 0)
      {
        int read = is.read(buf, ofs, rc);
        if (read < 0) read = 0;
        len += read;
        rc -= read;
        if (System.currentTimeMillis() > abortTime) throw new IOException("connection timed out");
      }
      //rc <= 0
      //case 1, when buf.length-1 >= ofs+len-1  (=> rc > 0)
      //cells ofs, ..., ofs+len-1 are filled
      //case 2, when buf.length-1 < ofs+len-1 (<=> buf.length-1 <= ofs+len-2)
      //cells ofs, ..., buf.length-1 are filled
      //cells 0, ..., ofs+len-2 - (buf.length-1) are filled, too
      int lm = ofs+len-1 - buf.length;
      if (lm > av) lm = av;
      rc = 0;
      while (lm > 0)
      {
        int read = is.read(buf, ofs, rc);
        if (read < 0) read = 0;
        len += read;
        rc -= read;
        if (System.currentTimeMillis() > abortTime) throw new IOException("connection timed out");
      }

    }
  }
  */

  public final String readCommand(byte[] b)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    InputStream is = getInputStream();
    synchronized (is)
    {
      long abortTime = System.currentTimeMillis() + 1000 * MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS;
      int ofs = 0;
      boolean d = false;
      for (;;)
      {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        int by = is.read();
        if (by == -1) throw new IOException("unexpected EOF");
        if (by == 10 && d) break;
        d = (by == 13);
        if (ofs < b.length)
        {
          b[ofs++] = (byte) by;
        }
        if (System.currentTimeMillis() > abortTime) throw new IOException("connection timed out");
        /*
        if (len >= buffer.length)
        {
          ...
          return ...;
        }
        int pos = findCRLF();
        if (pos != -1) break;
        fill(is, abortTime);
        */
      }
      if (b[ofs-1] == 13) --ofs;

      String line = new String(b, 0, ofs, "ASCII");

      if (StringUtil.startsWith(line, "MSG"))
      {
        StringTokenizer st = new StringTokenizer(line);
        String len_s = null;
        while (st.hasMoreTokens())
        {
          len_s = st.nextToken();
        }
        if (len_s == null)  throw new AssertException("len_s is null");
        int len;
        try
        {
          len = Integer.parseInt(len_s);
        }
        catch (NumberFormatException ex)
        {
          ServerConnection.throwProtocolViolated("MSG length must be int");
          len = 0;
        }
        String msg = readMSG(len);
        line = line + "\r\n" + msg;
      }
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("S: "+line);
      return line;
    }
  }

  private final String readMSG(final int len)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    if (len > 65000)
      ServerConnection.throwProtocolViolated("incoming message is too long: "+len+" bytes");
    byte[] b = new byte[len];
    InputStream is = getInputStream();
    synchronized (is)
    {
      long abortTime = System.currentTimeMillis() + 1000 * MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS;
      int ofs = 0;

      while (ofs < len)
      {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        int read = is.read(b, ofs, len - ofs);
        if (read < 0) read = 0;
        ofs += read;
        if (System.currentTimeMillis() > abortTime) throw new IOException("connection timed out");
        /*
        if (len >= buffer.length)
        {
          ...
          return ...;
        }
        int pos = findCRLF();
        if (pos != -1) break;
        fill(is, abortTime);
        */
      }

      String msg = new String(b, 0, len, "UTF-8");
      return msg;
    }
  }

  public final void writeASCII(String s)
  throws IOException
  {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    OutputStream os = getOutputStream();
    synchronized (os)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("C: "+s);
      os.write(s.getBytes("ASCII"));
      os.write((byte) 13);
      os.write((byte) 10);
      os.flush();
    }
  }

  /** UTF byte count is appended to the asciiPrefix,
      then UTF bytes are appended to the result;
      the final result is sent.
  */
  public final void writeMSG(String asciiPrefix, String msgBody)
  throws IOException
  {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    OutputStream os = getOutputStream();
    synchronized (os)
    {
      byte[] utfBytes = msgBody.getBytes("UTF-8");
      asciiPrefix = asciiPrefix + ' ' + utfBytes.length;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("C: "+asciiPrefix+"\r\n"+msgBody);
      os.write(asciiPrefix.getBytes("ASCII"));
      os.write((byte) 13);
      os.write((byte) 10);
      os.write(utfBytes);
      os.flush();
    }
  }
}