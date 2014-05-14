package org.openmim.icq2k;

import java.io.*;
import java.net.*;

import org.openmim.*;
import org.openmim.proxy_socks5.Socks5Util;
import org.openmim.icq.util.joe.*;

/**
  A [socks5 tcp connection/direct tcp connection] transport auto chooser.
  <p>
  <pre>
    - "5" force icq plugin to use socks5 server
    - "d" force icq plugin to use direct connection to icq server
    - "5d" make icq plugin first try socks5, если облом, то врубить auto-choose socks5 or direct_conn
    - "d5" make icq plugin first try direct connection to icq server, если облом,
      то врубить auto-choose socks5 or direct_conn
  </pre>
  <p>
  "5d" is the default specified in a static ICQ2Kxxx.properties resource,
  property name: <code>REQPARAM_TRANSPORTS_ALLOWED</code>.
*/
class TransportChooser
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(TransportChooser.class.getName());

  private final static char TRANSPORT_SOCKS5 = '5';
  private final static char TRANSPORT_DIRECT_CONNECTION = 'd';
  private final static char TRANSPORT_UNDEFINED = '*';

  /**
    transportsAllowed[0] is the most preferred one;
    transportsAllowed[transportsAllowed.length-1] is the least preferred one.
  */
  private static char[] transportsAllowed = null;

  /**
    Index in the TransportChooser.transportsAllowed array.

    TransportChooser.transportsAllowed[0] is attempted first.
  */
  private static int lastSuccesfulTransportPosition = 0;

  private final static Object lastSuccesfulTransportPositionLock = new Object();

  private static InetAddress socks5Host = null;
  private static int socks5Port = 0;
  private static String socks5UserName = null;
  private static String socks5Password = null;

  /**
    Throws a runtime exception if the value of REQPARAM_TRANSPORTS_ALLOWED is invalid.
  */
  static void setTransportsAllowed(String transportsAllowed) throws RuntimeException
  {
    if (transportsAllowed == null)
      throw new RuntimeException("REQPARAM_TRANSPORTS_ALLOWED property must be present, but it is null");
    transportsAllowed = transportsAllowed.trim().toLowerCase();
    if (transportsAllowed.length() < 1 || transportsAllowed.length() > 2)
      throw new RuntimeException("the length of the REQPARAM_TRANSPORTS_ALLOWED property must be 1 or 2, but it is " + transportsAllowed.length());
    TransportChooser.transportsAllowed = transportsAllowed.toCharArray();

    int i = 0;
    if (TransportChooser.transportsAllowed[i] != TRANSPORT_SOCKS5
    &&  TransportChooser.transportsAllowed[i] != TRANSPORT_DIRECT_CONNECTION)
      throw new RuntimeException("REQPARAM_TRANSPORTS_ALLOWED.trim().toLowerCase().charAt("+i+") must be '5' or 'd', but it is "+StringUtil.toPrintableString(""+TransportChooser.transportsAllowed[i]));

    if (TransportChooser.transportsAllowed.length > 1)
    {
      i = 1;
      if (TransportChooser.transportsAllowed[i] != TRANSPORT_SOCKS5
      &&  TransportChooser.transportsAllowed[i] != TRANSPORT_DIRECT_CONNECTION)
        throw new RuntimeException("REQPARAM_TRANSPORTS_ALLOWED.trim().toLowerCase().charAt("+i+") must be '5' or 'd', but it is "+StringUtil.toPrintableString(""+TransportChooser.transportsAllowed[i]));
    }
  }

  /**
    Should be called after setTransportsAllowed(String).
  */
  static boolean isSocks5Allowed()
  {
    if ((transportsAllowed) == null) Lang.ASSERT_NOT_NULL(transportsAllowed, "transportsAllowed");
    return transportsAllowed[0] == TRANSPORT_SOCKS5 || (transportsAllowed.length > 1 && transportsAllowed[1] == TRANSPORT_SOCKS5);
  }

  /**
    Should be called if socks5 are allowed as a transport.
  */
  static void setSocks5ProxyDetails(InetAddress socks5Host, int socks5Port, String socks5UserName, String socks5Password)
  {
    TransportChooser.socks5Host = socks5Host;
    TransportChooser.socks5Port = socks5Port;
    TransportChooser.socks5UserName = socks5UserName;
    TransportChooser.socks5Password = socks5Password;
  }

  static Socket connect(InetAddress host, int port, int socketTimeoutMillis) throws IOException
  {
    if ((transportsAllowed) == null) Lang.ASSERT_NOT_NULL(transportsAllowed, "transportsAllowed");

    int pos;
    synchronized (TransportChooser.lastSuccesfulTransportPositionLock)
    {
      pos = TransportChooser.lastSuccesfulTransportPosition;
    }

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
      synchronized (TransportChooser.lastSuccesfulTransportPositionLock)
      {
        TransportChooser.lastSuccesfulTransportPosition = pos;
        if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("connect: default transport is now "+transport2string(transportsAllowed[pos]));
      }
    }

    return sok;
  }

  private static Socket connect(char transport, InetAddress host, int port, int socketTimeoutMillis)
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
        sok = Socks5Util.proxyConnect(host.getHostAddress(), port, socks5Host, socks5Port, socks5UserName, socks5Password, socketTimeoutMillis);
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
        return "TRANSPORT_DIRECT_CONNECTION";
      case TRANSPORT_SOCKS5:
        return "TRANSPORT_SOCKS5";
      default:
        return "INVALID TRANSPORT ('"+transport+"')";
    }
  }
}