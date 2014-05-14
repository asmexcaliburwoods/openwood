package org.openmim.msn;

import java.io.*;
import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;

/** SS session connection */
public class SSS extends ServerConnection implements Constants
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SSS.class.getName());
  //private Chat chat;
  //int inpa;
  //private String chatId; //sessid;
  //char *auth;
  //int trId;
  //int total;
  //char *user;

  public static String getVersionString()
  {
    String rev = "$Revision: 1.8 $";
    rev = rev.substring("$Revision: ".length(), rev.length() - 2);

    String cvsTag = "$Name:  $";
    cvsTag = cvsTag.substring("$Name: ".length(), cvsTag.length() - 2);

    rev += ", cvs tag: '"+cvsTag+"'";

    return rev;
  }

  private String dst;
  boolean loggingIn = true;

  public SSS(String host, int port, String dst, PluginContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    super(host, port, ctx);
    this.dst = dst;
  }

  public String getDst()
  {
    return dst;
  }

  public synchronized void loggedIn()
  {
    loggingIn = false;
    notifyAll();

    Object lock = getLockJOI();
    synchronized(lock)
    {
      ready = true;
      lock.notifyAll();
    }
  }

  public boolean closeOnIdleAllowed()
  {
    synchronized(this)
    {
      if (!loggingIn) return true;
    }

    Object lock = getLockJOI();
    synchronized(lock)
    {
      return ready;
    }
  }

  private synchronized void waitUntilLoggedIn()
  throws InterruptedException, IOException
  {
    long stopTime = System.currentTimeMillis() + MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS*1000;
    for(;;)
    {
      if (!loggingIn) break;
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      wait(Math.max(1, stopTime - System.currentTimeMillis()));
      if (System.currentTimeMillis() >= stopTime)
        throw new IOException("connection timed out");
    }
  }

  public void sendMsg(final String text)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        postMSG("N", OUTCOMING_MESSAGE_HEADER + text, ctx);
        /*
        //NAKs are too extremely rare to wait seconds on each message sent
        Object o = getThisTransaction();
        synchronized(o)
        {
          o.wait(MSNMessagingNetwork.REQPARAM_MESSAGE_DELIVERY_FAILURE_WAIT_TIME_MILLIS);
        }
        */
        finish();
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        if (cmd.equals("NAK"))
        {
          //
          //NAK      Switchboard      User_       Sends a negative message
          //                                       delivery acknowledgement.
          //C: MSG TrID [U | N | A] Length\r\nMessage
          //S: NAK TrID
          //S: ACK TrID
          //Acknowledgement mode is not currently implemented [by Microsoft].
          //
          //Object o = getThisTransaction();
          finish(new MessagingNetworkException("server reports: message delivery failed", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_MESSAGING_SERVER_REPORTS_CANNOT_SEND_MESSAGE));
          /*
          synchronized(o)
          {
            o.notifyAll();
          }
          */
        }
      }
    });
  }

  protected void serverRequest(final String cmd, final String args,
      final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
  {
    StringTokenizer r = new StringTokenizer(args);

    switch (cmd.charAt(0))
    {
      case 'B':
        if (cmd.equals("BYE"))
        {
          //S: BYE joe@idisys.iae.nsk.su
          BYE(ses);
          close(StringUtil.quote(args)+" left the SSS, so we do", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
          ses.removeSSS(this);
          return;
        }
        break;
      case 'J':
        if (cmd.equals("JOI"))
        {
          //xJOI      Switchboard      User_       Notifies a client that a
          //x                                       user is now in the session
          //JOI deadlee@hotmail.com Venkatesh

          Object lock = getLockJOI();

          boolean ready0;
          synchronized(lock)
          {
            ready0 = this.ready;
          }

          if (ready0)
          {
            BYE(ses);
            close("we must leave the SSS when third participant joins", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF);
            ses.removeSSS(this);
            return;
          }

          synchronized(lock)
          {
            ready = true;
            lock.notifyAll();
          }
          return;
        }
        break;
      case 'O':
        if (cmd.equals("OUT"))
        {
          //xOUT      All              All          Ends a client-server
          //x                                       Session.
          //OUT //connection-close
          close("server told that SSS is closed", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_NO_ERROR);
          ses.removeSSS(this);
          return;
        }
        break;
          //xBYE      Switchboard      User_       Notifies a client that a
          //x                                       user is no longer in the session.
      case 'N':
        if (cmd.equals("NAK"))
        {
          //
          //xNAK      Switchboard      User_       Sends a negative message
          //x                                       delivery acknowledgement.
          //C: MSG TrID [U | N | A] Length\r\nMessage
          //S: NAK TrID
          //S: ACK TrID
          //Acknowledgement mode is not currently implemented [by Microsoft].
          //
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("messaging server reports: message delivery failed: trid #"+args);
          ses.fireSystemNotice("message delivery failed", pctx);
        }
        break;
      case 'M':
        if (cmd.equals("MSG"))
        {
          /*
            MSG deaxxxx@hotmail.com Venkatesh 137
            MIME-Version: 1.0
            Content-Type: text/plain; charset=UTF-8
            X-MMS-IM-Format: FN=Microsoft%20Sans%20Serif; EF=; CO=0; CS=0; PF=22
            hello

            MSG deaxxxx@hotmail.com Venkatesh 100
            MIME-Version: 1.0
            Content-Type: text/x-msmsgscontrol
            TypingUser: deaxxxx@hotmail.com
          */
          String src = tok(r);
          LoginIdUtil.checkValid_Ignorable(src);
          src = LoginIdUtil.normalize(src);
          int pos = args.indexOf("\r\n\r\n");
          if (pos == -1) return; //foll msg or msg with no content
          if (args.indexOf("\r\nContent-Type: text/plain") == -1) return;
          String body = args.substring(pos+4);
          ses.fireMessageReceived(src, body, pctx);
          return;
        }
        break;

          // ACK      Switchboard      User_       Sends a positive message
          //                                        delivery acknowledgement.
    }
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unknown cmd ignored");
  }

  private final Object lockJOI = new Object();
  public Object getLockJOI()
  {
    return lockJOI;
  }

  private boolean ready = false;

  public void prepare()
  throws InterruptedException, IOException
  {
    long stopTime = System.currentTimeMillis() + MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS * 1000;
    Object lock = getLockJOI();
    for(;;)
    {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      synchronized(lock)
      {
        if (ready) return;
        lock.wait(Math.max(1, stopTime - System.currentTimeMillis()));
      }
      if (System.currentTimeMillis() >= stopTime)
        throw new IOException("timed out");
    }
  }

  public void BYE(Session ses)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    getConn().writeASCII("BYE "+ses.getLoginId());
  }

  protected boolean isNS()
  {
    return false;
  }
}