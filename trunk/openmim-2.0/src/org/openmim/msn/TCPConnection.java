package org.openmim.msn;

import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;

import java.io.*;

/**
  Represents a single TCP/IP socket connection
  with some functionality for dispatching
  incoming NATIVE events.
*/
public final class TCPConnection extends Connection
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TCPConnection.class.getName());
  private java.net.Socket fd;
  private InputStream inputStream;
  private OutputStream outputStream;
  /**
   Creates a new connection to the specified
   host of specified type.
   <br>
   <br>type: Type of connection to create
   <br>destination: Host to connect to (in "host:port" or "host" syntax)
   */
  public TCPConnection(java.net.InetAddress host, int port, PluginContext ctx)
  throws MessagingNetworkException,
         java.io.IOException
  {
    super(host, port, ctx);
    java.net.Socket sok = transportChooserConnect(host, port, MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS*1000, ctx);
    synchronized (this)
    {
      fd = sok;
    }
    inputStream = new BufferedInputStream(sok.getInputStream(), 1024);
    outputStream = new BufferedOutputStream(sok.getOutputStream(), 1024);
  }

  protected InputStream getInputStream0()
  {
    return (fd == null ? null : inputStream);
  }

  protected OutputStream getOutputStream0()
  {
    return (fd == null ? null : outputStream);
  }

  /**
    Closes the connection.
    <p>
    Never throws exceptions.
  */
  public void closeSocket()
  {
    java.net.Socket fd;
    InputStream is;
    OutputStream os;
    synchronized (this)
    {
      fd = this.fd;
      is = inputStream;
      os = outputStream;
    }
    if (fd == null)
      return;
    log("closing socket "+fd);
    synchronized (this)
    {
      //log("1 "+fd);
      this.fd = null;
      inputStream = null;
      outputStream = null;
    }
    try
    {
      is.close();
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
    }
    try
    {
      os.flush();
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
    }
    try
    {
      os.close();
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
    }
    try
    {
      fd.close();
    }
    catch (Exception ex)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
    }
    try
    {
      getOutputStream();
    }
    catch (Exception ex)
    {
    }
    log("closed");
  }

  public boolean isClosed()
  {
    synchronized (this)
    {
      return fd == null;
    }
  }

  private void log(String s)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(MSNMessagingNetwork.LOG_PREFIX + s);
  }

  private java.net.Socket transportChooserConnect(java.net.InetAddress host, int port, int timeoutMillis, PluginContext ctx)
  throws MessagingNetworkException, java.io.IOException
  {
    return ctx.getTransportChooser().connect(host, port, timeoutMillis);
  }
}