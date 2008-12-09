package org.openmim.msn;

import java.io.*;
import java.net.*;
import java.util.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;
import org.openmim.icq.util.*;
import org.openmim.icq.util.joe.*;

/** Base class for SS, DS, and NS. */
public abstract class ServerConnection
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ServerConnection.class.getName());
  private Connection conn;
  private final TransactionManager tm;

  public ServerConnection(String host, int port, PluginContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    tm = new TransactionManager(this);
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": connecting to "+host+":"+port);
    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    conn = ctx.getResourceManager().createTCPConnection(InetAddress.getByName(host), port, ctx);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": connection opened");
  }

  /*
  protected synchronized void moveTo(String host, int port, String loginId, String password, PluginContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": referred to "+host+":"+port);
    close();
    conn = ctx.getResourceManager().createTCPConnection(InetAddress.getByName(host), port, ctx);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": connection opened");
  }
  */

  public final Transaction start(Transaction t)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
    if (isClosed()) throw new IOException("connection closed");
    markActive();
    //try
    //{
    tm.start(t);
    //}
    //catch (Exception ex)
    //{
    //  ses.eatException(ex);
    //}
    return t;
  }

  public synchronized boolean isClosed()
  {
    Connection conn = this.conn;
    if (conn == null) return true;
    else return conn.isClosed();
  }

  public void close(Session ses)
  {
    close(ses.getLastErrorMessage(), ses.getLastErrorLogger(), ses.getLastErrorEndUserReason());
  }

  public synchronized void close(String msg, int cat, int endUserCode)
  {
    Connection conn = this.conn;
    if (conn != null)
    {
      this.conn = null;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": closing connection ("+msg+")");
      conn.closeSocket();
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": closing tm");
    tm.close(msg, cat, endUserCode);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(getServerName() + ": closed");
  }

  public String getServerName()
  {
    String s = getClass().getName();
    int p = s.lastIndexOf('.');
    return s.substring((p < 0 ? 0 : p+1));
  }

  protected synchronized Connection getConn()
  throws IOException
  {
    Connection conn = this.conn;
    if (conn == null) throw new IOException("connection closed");
    return conn;
  }

  protected StringTokenizer read_st()
  throws IOException, InterruptedException, MessagingNetworkException
  {
    return read_st(false);
  }

  protected StringTokenizer read_st(boolean returnSeparators)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    String reply = read_s_line();
    return new StringTokenizer(reply, " \t\r\n", returnSeparators);
  }

  protected StringTokenizer read_st(String expectedCmd, int expectedTrid)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    String s;
    StringTokenizer r = read_st();
    if (!r.hasMoreTokens()) throwProtocolViolated("unexpected end of input: command mnemonic expected");
    if (!expectedCmd.equals(s = r.nextToken())) throwProtocolViolated("malformed input: command " + StringUtil.quote(expectedCmd) + " expected, but it is "+StringUtil.quote(s));
    if (!r.hasMoreTokens()) throwProtocolViolated("unexpected end of input: trid expected");
    int i = getTrID(r);
    if (expectedTrid != i) throwProtocolViolated("malformed input: trid must be "+expectedTrid+", but it is "+i);
    return r;
  }

  public static int getTrID(StringTokenizer r)
  throws MessagingNetworkException
  {
    String s = r.nextToken();
    try
    {
      return Integer.parseInt(s);
    }
    catch (Exception ex)
    {
      throwProtocolViolated("malformed input: int expected, but it is " + StringUtil.quote(s));
      return -1;
    }
  }

  protected static HostPort parseReferral(StringTokenizer r, String expectedReferralType)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    if (r == null)  throw new AssertException("r is null");
    //S: XFR TrID ReferralType Address[:PortNo]
    //r contains "ReferralType Address[:PortNo]"
    if (!r.hasMoreTokens()) throwUnexpectedEOI();
    String refType = r.nextToken();
    if (!refType.equals(expectedReferralType)) throwProtocolViolated("malformed input: "+StringUtil.quote(expectedReferralType)+" referral expected here, but it is "+StringUtil.quote(refType));
    if (!r.hasMoreTokens()) throwUnexpectedEOI();
    String hostPort = r.nextToken();
    try
    {
      return HostPortUtil.parse(hostPort, Constants.MSN_DEFAULT_PORT);
    }
    catch (IllegalArgumentException ex)
    {
      throwProtocolViolated("malformed input: \"host[:port]\" expected, but it is "+StringUtil.quote(hostPort));
      return null; //never executed
    }
  }

  public static void throwProtocolViolated(String msg)
  throws MessagingNetworkException
  {
    throw new MExpectException(
      msg,
      MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
      MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_PROTOCOL_ERROR);
  }

  public static void throwUnexpectedEOI()
  throws MessagingNetworkException
  {
    throwProtocolViolated("unexpected end of input");
  }

  private byte[] buf = new byte[8192];

  protected String read_s_line()
  throws IOException, InterruptedException, MessagingNetworkException
  {
    String s = getConn().readCommand(buf);
    //if (StringUtil.startsWith(s, "MSG"))
    //  throwProtocolViolated("malformed input: unexpected MSG.");
    return s;
  }

  public int available()
  throws IOException, InterruptedException, MessagingNetworkException
  {
    int av = getConn().available();
    return (av <= 0 ? 0 : av);
  }

  public static String tok(StringTokenizer r)
  throws MessagingNetworkException
  {
    if (!r.hasMoreTokens()) throwUnexpectedEOI();
    return r.nextToken();
  }

  /*
  public void flushQueue()
  throws IOException, InterruptedException
  {
    while (queue.size() > 0)
    {
      Object o;
      synchronized(queue)
      {
        o = queue.remove(0);
      }

      if (o instanceof String)
        getConn().writeASCII((String) o);
      else
      {
        String[] sa = (String[]) o;
        getConn().writeMSG(sa[0], sa[1]);
      }
    }
  }


  private List queue = new LinkedList();
  */

  /**
    Emits a line to server.
  */
  public synchronized final void post(int trid, String cmd, String args)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    markActive();
    getConn().writeASCII(cmd + " " + TransactionManager.trid2string(trid) + " " + args);
    /*
    synchronized (queue)
    {
      queue.add(cmd+" "+TransactionManager.trid2string(trid)+" "+args);
      queue.notify();
    }
    */
  }

  /**
    Emits a MSG to server.  args do not include msgBody length in bytes.
  */
  public synchronized final void postMSG(int trid, String args, String msgBody)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    markActive();
    getConn().writeMSG("MSG " + TransactionManager.trid2string(trid) + " " + args, msgBody);
    /*
    synchronized (queue)
    {
      queue.add(new String[] {"MSG "+TransactionManager.trid2string(trid)+" "+args, msgBody});
      queue.notify();
    }
    */
  }

  /**
  Ticks.

  <pre>
  TaskMan calls:
    Session tick() for each Session

  Session tick() calls:
    SC tick(Session ses) for each SC in this Session

  SC tick(Session ses) calls:
    SC public void handleServerMessage();
          //exceptions are explicitly delivered to session by SC
    TransMan public final void cleanup()
          //no exceptions
  </pre>
  */
  public void tick(Session ses, PluginContext ctx)
  {
    handleServerMessage(ses, ctx);
    tm.cleanup();
  }

  private final Object lock1 = new Object();

  public long getLastActivityTime()
  {
    synchronized (lock1)
    {
      return lastActivityTime;
    }
  }

  private long lastActivityTime = System.currentTimeMillis();

  private void markActive()
  {
    synchronized (lock1)
    {
      lastActivityTime = System.currentTimeMillis();
    }
  }

  /**
  ...

  <pre>
  SC handleServerMessage() calls:
    TransMan public bool deliver(int trid, String cmd, String args)
          //exceptions are delivered to handleInput() & subsequently to session
    SC serverRequest()
          //server requests with no trid or with unknown trid are delivered
          //to SC's serverRequest() method
  </pre>
  */
  private synchronized void handleServerMessage(Session ses, PluginContext pctx)
  {
    try
    {
      //if (available() <= 0) return;

      markActive();

      String ln = read_s_line();
      StringTokenizer r = new StringTokenizer(ln, " \t\r\n", true);
      String cmd = tok(r);
      String tok;
      if (cmd.length() < 2) throwProtocolViolated("cmd too short (must be at least 2 chars): "+StringUtil.quote(cmd));

      ErrorInfo ei = null;
      if (Character.isDigit(cmd.charAt(0)))
      {
        ei = Errors.getInfo(cmd);
        if (ei == null)  throw new AssertException("errorInfo must not be null here");
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("MSN server error "+cmd+": "+ei.errorMessage);
      }

      int delta = 0;

      for (;;) { tok = tok(r); if (!Character.isWhitespace(tok.charAt(0))) break; delta++; }

      String s = tok;
      int trid = 0;
      boolean noTrid = false;

      if (cmd.equals("RNG") || cmd.equals("BPR"))
      {
        noTrid = true;
      }
      else
      {
        try
        {
          trid = Integer.parseInt(s);
        }
        catch (NumberFormatException exx)
        {
          noTrid = true;
        }
      }

      String args;
      if (noTrid)
      {
        args = ln.substring(cmd.length()+delta);
      }
      else
      {
        for (;;) { if (!r.hasMoreTokens()) break; tok = tok(r); if (!Character.isWhitespace(tok.charAt(0))) break; delta++; }
        args = ln.substring(cmd.length()+delta+s.length());
      }

      if (cmd.equals("BPR"))
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("args: "+StringUtil.quote(args));

      if (noTrid || !tm.deliver(trid, cmd, args, ei, ses, pctx))
      {
        if (ei == null)
          serverRequest(cmd, args, ses, pctx); //ignore trid if was present
        else
          serverErrorNotification(ei, args, ses, pctx);
      }
    }
    catch (Exception ex)
    {
      ses.eatException(ex, getServerName()+" input handling", pctx);
    }
  }

  protected abstract void serverRequest(final String cmd, final String args, final Session ses, final PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException;

  protected void serverErrorNotification(ErrorInfo ei, final String args, final Session ses, final PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    //unhandled server error notifications close the connection
    MessagingNetworkException ex = Errors.createException(ei);
    close(ex.getMessage(), ex.getLogger(), ex.getEndUserReasonCode());
    throw Errors.createException(ei);
  }

  public static String decodeNick(String nick)//todo check what encoding mantras should be in place
  {
    try
    {
      //nick = java.net.URLDecoder.decode(nick, "ASCII"); //got ASCII8
      //return new String(nick.getBytes(), "UTF-8");
      return java.net.URLDecoder.decode(nick, "UTF-8");
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new AssertException("encodings ( ASCII || UTF-8 ) not present: "+ex);
    }
    catch (Exception exx)
    {
      throw new RuntimeException(exx);
    }
  }

  public static void expect(StringTokenizer r, String s)
  throws MessagingNetworkException
  {
    if (!s.equals(tok(r))) throwProtocolViolated("token expected: "+StringUtil.quote(s));
  }

  public void OUT()
  {
    try
    {
      getConn().writeASCII("OUT");
    }
    catch (IOException ex)
    {
    }
  }

  protected abstract boolean isNS();
}