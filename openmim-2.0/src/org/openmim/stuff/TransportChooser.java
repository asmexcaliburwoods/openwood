package org.openmim.stuff;

import java.io.*;
import java.net.*;

import org.openmim.icq.utils.*;
import org.openmim.proxy_socks5.Socks5Util;

/**
  A [socks5 tcp connection/direct tcp connection] transport auto chooser.
  <p>
  <pre>
    - "5" force icq plugin to use socks5 server
    - "d" force icq plugin to use direct connection to icq server
    - "5d" make icq plugin first try socks5, �᫨ �����, � ��㡨�� auto-choose socks5 or direct_conn
    - "d5" make icq plugin first try direct connection to icq server, �᫨ �����,
      � ��㡨�� auto-choose socks5 or direct_conn
  </pre>
  <p>
  "5d" is the default.
*/

public class TransportChooser
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TransportChooser.class.getName());

  private final static char TRANSPORT_SOCKS5 = '5';
  private final static char TRANSPORT_DIRECT_CONNECTION = 'd';
  private final static char TRANSPORT_UNDEFINED = '*';

  /**
    transportsAllowed[0] is the most preferred one;
    transportsAllowed[transportsAllowed.length-1] is the least preferred one.
  */
  private char[] transportsAllowed = null;

  /**
    Index in the TransportChooser.transportsAllowed array.

    TransportChooser.transportsAllowed[0] is attempted first.
  */
  private int lastSuccesfulTransportPosition;

  private final Object lastSuccesfulTransportPositionLock = new Object();

  private String socks5Host;
  private InetAddress socks5inetAddress = null;
  private final int socks5Port;
  private final String socks5UserName;
  private final String socks5Password;

  public TransportChooser(String transportsAllowed)
  {
    this(transportsAllowed, null, 0, null, null);
  }

  public TransportChooser(
      String transportsAllowed,
      String socks5Host, int socks5Port,
      String socks5UserName, String socks5Password)
  {
    this.socks5Host = socks5Host;
    this.socks5Port = socks5Port;
    this.socks5UserName = socks5UserName;
    this.socks5Password = socks5Password;
    
    setTransportsAllowed(transportsAllowed);
  }

  public void setTransportsAllowed(String transportsAllowed) 
  throws RuntimeException
  {
    if (transportsAllowed == null) Lang.ASSERT_NOT_NULL(transportsAllowed, "transportsAllowed");
    if (isSocks5Allowed(transportsAllowed) && StringUtil.isNullOrTrimmedEmpty(socks5Host))
    {
      throw new RuntimeException("socks5Host is null, setTransportsAllowed() request ignored");
    }

    setTransportsAllowed0(transportsAllowed);
  }
  
  public String getPossibleTransports()
  {
    return "d is for "+transport2string('d')+"; 5 is for "+transport2string('5');
  }
  
  public String getTransportsAllowed()
  {
    synchronized (this.lastSuccesfulTransportPositionLock)
    {
      return new String(this.transportsAllowed);
    }
  }
  
  public char getCurrentTransport()
  {
    synchronized (lastSuccesfulTransportPositionLock)
    {
      return transportsAllowed[lastSuccesfulTransportPosition];
    }
  }

  public void setCurrentTransport(char c) throws RuntimeException
  {
    checkValid(0, c);
    char[] transportsAllowed;
    synchronized (this.lastSuccesfulTransportPositionLock)
    {
      transportsAllowed = this.transportsAllowed;
    }
    for(int i = 0; i < transportsAllowed.length; ++i)
    {
      if (transportsAllowed[i] == c) 
      {
        synchronized (this.lastSuccesfulTransportPositionLock)
        {
          this.lastSuccesfulTransportPosition = i;
          return;
        }
      }
    }
    throw new RuntimeException("transport `"+c+"' is currently disabled; the only valid transports are: `"+getTransportsAllowed()+"'.");
  }
  
  /**
    Throws a runtime exception if the value of transportsAllowed is invalid.
  */
  private void setTransportsAllowed0(String transportsAllowed)
  throws RuntimeException
  {
    if (transportsAllowed == null)
      throw new RuntimeException("'transportsAllowed' property must be present, but it is null");
    transportsAllowed = transportsAllowed.trim().toLowerCase();
    if (transportsAllowed.length() < 1 || transportsAllowed.length() > 2)
      throw new RuntimeException("the length of the 'transportsAllowed' property must be 1 or 2, but it is " + transportsAllowed.length());
    synchronized (this.lastSuccesfulTransportPositionLock)
    {
      this.transportsAllowed = transportsAllowed.toCharArray();
      this.lastSuccesfulTransportPosition = 0;
    }

    checkValid(0, this.transportsAllowed[0]);
    if (this.transportsAllowed.length > 1)
    {
      checkValid(1, this.transportsAllowed[1]);
    }
  }

  public void checkValid(int i, char c)
  {
    if (c != TRANSPORT_SOCKS5
    &&  c != TRANSPORT_DIRECT_CONNECTION)
      throw new RuntimeException("transportsAllowed.trim().toLowerCase().charAt("+i+") must be '5' or 'd', but it is "+StringUtil.toPrintableString(""+c));
  }

  /**
    Should be called after setTransportsAllowed(String).
  */
  public boolean isSocks5Allowed(String transportsAllowed)
  {
    if ((transportsAllowed) == null) Lang.ASSERT_NOT_NULL(transportsAllowed, "transportsAllowed");
    return transportsAllowed.charAt(0) == TRANSPORT_SOCKS5
        || transportsAllowed.charAt(1) == TRANSPORT_SOCKS5;
  }

  public Socket connect(InetAddress host, int port, int socketTimeoutMillis)
      throws IOException
  {
    char[] transportsAllowed;
    int pos;
    synchronized (this.lastSuccesfulTransportPositionLock)
    {
      pos = this.lastSuccesfulTransportPosition;
      transportsAllowed = this.transportsAllowed;
    }
    
    if ((transportsAllowed) == null) Lang.ASSERT_NOT_NULL(transportsAllowed, "transportsAllowed");

    int previousLastSucessful = pos;

    int attempts = transportsAllowed.length;

    if ((attempts) <= 0) Lang.ASSERT_POSITIVE(attempts, "attempts");

    Socket sok = null;

    if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("connect: attempting last succesful transport: "+transport2string(transportsAllowed[pos]).toLowerCase());
    for (;;)
    {
      try
      {
        sok = connect(transportsAllowed[pos], host, port, socketTimeoutMillis);
        if ((sok) == null) Lang.ASSERT_NOT_NULL(sok, "sok/1");
        break;
      }
      catch (IOException ex)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex while tcp connect", ex);
        if ((--attempts) <= 0)
          throw new IOException("cannot connect: all transports failed");
        pos++;
        if (pos >= transportsAllowed.length)
          pos = 0;
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("connect: changing transport to "+transport2string(transportsAllowed[pos]));
      }
    }
    if ((sok) == null) Lang.ASSERT_NOT_NULL(sok, "sok/2");

    if (previousLastSucessful != pos)
    {
      synchronized (this.lastSuccesfulTransportPositionLock)
      {
        this.lastSuccesfulTransportPosition = pos;
        if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("connect: default transport is now "+transport2string(transportsAllowed[pos]));
      }
    }

    return sok;
  }

  @SuppressWarnings("resource")
private Socket connect(
      char transport,
      InetAddress host, int port, int socketTimeoutMillis)
      throws IOException
  {
    Socket sok;
    switch (transport)
    {
      case TRANSPORT_DIRECT_CONNECTION:
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("direct connect: creating tcp connection to " + host + ":" + port + ", timeout: " + (socketTimeoutMillis/1000) + " sec.");
        sok = new java.net.Socket(host, port);
        sok.setSoTimeout(socketTimeoutMillis);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("direct connect: tcp connection created [" + host + ":" + port + "], timeout: " + (socketTimeoutMillis/1000) + " sec.");
        //throw new IOException("test1");
        break;

      case TRANSPORT_SOCKS5:
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("socks5 connect: creating tcp connection to " + host + ":" + port + ", timeout: " + (socketTimeoutMillis/1000) + " sec.");
        if (socks5inetAddress == null)
        {
          if (StringUtil.isNullOrTrimmedEmpty(socks5Host)) Lang.ASSERT_NOT_NULL_NOR_TRIMMED_EMPTY(socks5Host, "socks5Host");
          try
          {
            socks5inetAddress = InetAddress.getByName(socks5Host);
            socks5Host = null;
          }
          catch (IOException ex)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("dns: cannot resolve socks5 host for msn", ex);
            throw new IOException("dns: cannot resolve socks5 host for msn" + ex);
          }
        }
        if ((socks5inetAddress) == null) Lang.ASSERT_NOT_NULL(socks5inetAddress, "socks5inetAddress");
        sok = Socks5Util.proxyConnect(host.getHostAddress(), port, socks5inetAddress, socks5Port, socks5UserName, socks5Password, socketTimeoutMillis);
        sok.setSoTimeout(socketTimeoutMillis);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("socks5 connect: tcp connection created [" + host + ":" + port + "], timeout: " + (socketTimeoutMillis/1000) + " sec.");
        break;

      default:
        throw new AssertException("invalid transport: '"+transport+"'");
    }
    return sok;
  }

  private static String transport2string(char transport)
  {
    switch (transport)
    {
      case TRANSPORT_DIRECT_CONNECTION:
        return "transport_direct_connection";
      case TRANSPORT_SOCKS5:
        return "transport_socks5";
      default:
        return "invalid transport ('"+transport+"')";
    }
  }
}