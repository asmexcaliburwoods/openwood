package org.openmim.msn;

import java.io.*;
import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;

import java.security.MessageDigest;

/** Dispatch server connection */
public class DS extends ServerConnection implements Constants
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(DS.class.getName());
  //private Chat chat;
  //int inpa;
  //private String chatId; //sessid;
  //char *auth;
  //int trId;
  //int total;
  //char *user;

  public static String getVersionString()
  {
    String rev = "$Revision: 1.10 $";
    rev = rev.substring("$Revision: ".length(), rev.length() - 2);

    String cvsTag = "$Name:  $";
    cvsTag = cvsTag.substring("$Name: ".length(), cvsTag.length() - 2);

    rev += ", cvs tag: '"+cvsTag+"'";

    return rev;
  }

  public DS(String host, int port, PluginContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    super(host, port, ctx);
  }

  static class USRLoginTransaction extends Transaction
  {
    final String loginId;
    StringTokenizer stringTokenizer;
    boolean isUSR;

    USRLoginTransaction(String loginId)
    {
      this.loginId = loginId;
    }

    //7.3 Authentication
    //C: USR TrID SP I{ AuthInitiateInfo}
    //S: USR TrID SP S{ AuthChallengeInfo}
    //C: USR TrID SP S{ AuthResponseInfo }
    //S: USR TrID OK UserHandle FriendlyName
    public void clientRequest(TransactionContext ctx)
    throws IOException, InterruptedException, MessagingNetworkException
    {
      post("USR", "MD5 I " + loginId, ctx);
    }

    public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
    throws IOException, InterruptedException, MessagingNetworkException
    {
      StringTokenizer r = new StringTokenizer(args);
      if (cmd.equals("USR"))
      {
        isUSR = true;
      }
      else
      {
        isUSR = false;
        if (!cmd.equals("XFR")) throwProtocolViolated("malformed input: USR or XFR expected, but "+StringUtil.quote(cmd)+" received");
        ses.setRunning(false);
      }
      this.stringTokenizer = r;
      finish();
    }
  }

  public static DS login(String host, int port, final String loginId, final String password, final Session ses, final PluginContext ctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    final PluginContext pctx = ctx;
    DS ds = null;
    boolean authSuccess = false;
    StringTokenizer r = null;
    int t;
    String sp = null; //security package

    if (password == null)  throw new AssertException("password is null");
    if ( password.indexOf(' ' ) != -1
      || password.indexOf('\t') != -1
      || password.indexOf('\r') != -1
      || password.indexOf('\n') != -1)
    {
      throw new MessagingNetworkException("MSN password cannot contain whitespace", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WRONG_PASSWORD);
    }

    long plannedAbortTime = System.currentTimeMillis() + 1000 * MSNMessagingNetwork.REQPARAM_SOCKET_TIMEOUT_SECONDS;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("connecting to DS "+host+":"+port);

    for (;;)
    {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
      ds = new DS(host, port, ctx);
      ses.add(ds);
      ses.setDS(ds);
      ses.setRunning(true);

      //7.1 Protocol Versioning
      //C: VER TrID dialect-name{ dialect-name...}
      //S: VER TrID dialect-name{ dialect-name...}
      //S: VER TrID{ dialect-name ... } 0{ dialect-name ... } //failure
      ds.start(new Transaction()
      {
        final String CMD1 = "VER";

        public void clientRequest(TransactionContext ctx)
        throws IOException, InterruptedException, MessagingNetworkException
        {
          post(CMD1, PROTOCOL_VERSIONS_STRING, ctx);
        }

        public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
        throws IOException, InterruptedException, MessagingNetworkException
        {
          StringTokenizer r = new StringTokenizer(args);
          if (!cmd.equals(CMD1)) throwProtocolViolated("cmd must be "+CMD1);
          while (r.hasMoreTokens())
          {
            String dialect = r.nextToken();
            if ("0".equals(dialect)) throwProtocolViolated("DS server reported version negotiation failure");
            //ignoring all dialect ids != "0"
          }
          finish();
        }
      }).waitFor(true, ses, pctx);

      ds.start(new Transaction()
      {
        //7.2 Server Policy Information
        //C: INF TrID
        //S: INF TrID SP{,SP...}
        final String CMD1 = "INF";

        public void clientRequest(TransactionContext ctx)
        throws IOException, InterruptedException, MessagingNetworkException
        {
          post(CMD1, "", ctx);
        }

        public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
        throws IOException, InterruptedException, MessagingNetworkException
        {
          StringTokenizer r = new StringTokenizer(args);
          if (!cmd.equals(CMD1)) throwProtocolViolated("cmd must be "+CMD1);
          r = new StringTokenizer(tok(r), ", \t");
          if (!r.hasMoreElements()) throwProtocolViolated("DS server did not report server security policy id; cannot proceed");
          boolean md5found = false;
          while (r.hasMoreElements())
          {
            String sp = r.nextToken(); //security package
            //"MD5" is used by the NS,
            //"CKI" by the SS.
            if ("MD5".equals(sp))
            {
              md5found = true;
              break;
            }
          }
          if (!md5found) throwProtocolViolated("DS server reported no known security packages; cannot proceed");
          finish();
        }
      }).waitFor(true, ses, pctx);

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("sending login id");
      USRLoginTransaction usrt = new USRLoginTransaction(loginId);
      ds.start(usrt).waitFor(true, ses, pctx);

      r = usrt.stringTokenizer;
      if (usrt.isUSR) break;

      ds.close("changing NS server", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, MessagingNetworkException.ENDUSER_NO_ERROR);
      ds = null;

      if (System.currentTimeMillis() > plannedAbortTime)
        throw new MessagingNetworkException("login timed out", MessagingNetworkException.CATEGORY_NOT_CATEGORIZED, MessagingNetworkException.ENDUSER_LOGGED_OFF_DUE_TO_MESSAGING_OPERATION_TIMEOUT);

      HostPort hp = parseReferral(r, "NS");
      host = hp.host;
      port = hp.port;

      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("referred to "+host+":"+port);
    }

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("handshaking");
    //load balancing finished.

    //S: USR TrID SP S{ AuthChallengeInfo}
    //C: USR TrID SP S{ AuthResponseInfo }
    //S: USR TrID OK UserHandle FriendlyName

    //USR trid SP S ...
    //now, r is after trid
    tok(r); //skip SP
    tok(r); //skip S
    String challenge = tok(r);

    String response;
    try
    {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] ba = md.digest((challenge + password).getBytes("ASCII"));
      md = null;
      response = byteArrayToHexString(ba);
      ba = null;
    }
    catch (java.security.NoSuchAlgorithmException ex)
    {
      throw new AssertException("MSN plugin reports: MD5 security provider is not installed, see java.security.MessageDigest class. MD5 provider is present in JDK1.3.1+.");
    }

    final String response_ = response;

    ds.start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        post("USR", "MD5 S " + response_, ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        StringTokenizer r = new StringTokenizer(args);
        if (!cmd.equals("USR")) throwProtocolViolated("cmd must be USR");
        finish();
      }
    }).waitFor(true, ses, pctx);
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("auth success");
    authSuccess = true;
    return ds;
  }

  private static char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  private static String byteArrayToHexString(byte[] ba)
  {
    StringBuffer sb = new StringBuffer(ba.length << 1);
    for (int i = 0; i < ba.length; i++)
    {
      byte b = ba[i];
      sb.append(HEX_DIGITS[(b >> 4) & (byte) 0xf])
        .append(HEX_DIGITS[b & (byte) 0xf]);
    }
    return sb.toString();
  }

  public void addRemoveSingleContactListItem(final String dst, final boolean add, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    final String cmd1 = (add ? "ADD" : "REM");

    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        post(cmd1, "FL "+dst+(add ? " "+dst : ""), ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        StringTokenizer r = new StringTokenizer(args);
        if (!cmd.equals(cmd1)) throwProtocolViolated("cmd must be "+cmd1);
        expect(r, "FL");
        tok(r); //ser#
        if (!LoginIdUtil.normalize(tok(r)).equals(dst))
          throwProtocolViolated("must be "+dst);
        finish();
      }
    }).waitFor(false, ses, pctx);
  }

  /*
  public void syncAccountProperties(final Session ses, final PluginContext pctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //SYN 95 0
        post("SYN", "0", ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //  //details for a local user
        //  PRP 95 60 PHH 3%20(1234)342343\r\n    //home
        //  PRP 95 60 PHW 3%20(5678)556454\r\n    //work
        //  PRP 95 60 PHM 3%20(1212)555656\r\n    //mobile
        //  //PRP 95 60 MOB N\r\n                   //msn mobile is not defined
        //  //PRP 95 60 MBE N\r\n                   //???
        //
        //  //details for buddies
        //  //handling of this: see DS.serverRequest() & SC.handleInput()
        //  BPR 60 buddy2@xxx.com PHH 3%20(2343)%20546456\r\n
        //  BPR 60 buddy2@xxx.com PHW 3%20(3454)%20456456\r\n
        //  BPR 60 buddy2@xxx.com PHM 3%20(6666)%20456456\r\n
        //  //BPR 60 buddy2@xxx.com MOB N\r\n
        //
        //  //the following line ends the transaction:
        //  LST 95 RL 60 2 2 buddy2@xxx.com buddy2@xxx.com\r\n
        //

        if (cmd.equals("PRP"))
        {
          StringTokenizer r = new StringTokenizer(args);
          tok(r); //ser#
          String propName = tok(r);
          if (!AccountPropertiesUtil.isKnownPhoneProperty(propName)) return;
          String propValue = (r.hasMoreTokens() ? r.nextToken() : null);
          if (propValue == null) return;
          ses.setAccountProperty(ses.getLoginId(), propName, "+"+java.net.URLDecoder.decode(propValue));
        }
        else
        if (cmd.equals("LST"))
        {
          StringTokenizer r = new StringTokenizer(args);
          if ( !(  tok(r).equals("RL")  ) ) return;
          tok(r); //ser#
          String i = tok(r);
          if ( !(  tok(r).equals(i)     ) ) return;
          finish();
        }
      }
    }).waitFor(true, ses, pctx);
  }
  */


  public void syncContactList(final Session ses, String[] contactList, PluginContext pctx)
  throws MessagingNetworkException, IOException, InterruptedException
  {
    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //C: GTC TrID [A | N]
        post("GTC", "N", ctx); //N is no auth, A is auth
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //S: GTC TrID Ser# [A | N]
        finish();
      }
    });

    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //LST 8 FL
        post("LST", "FL", ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //LST 8 RL 69 1 19 venky_dude@hotmail.com venkat
        //
        //LST TrID LIST Ser# Item# TtlItems UserHandle CustomUserName

        if (!cmd.equals("LST"))
          throwProtocolViolated("must be LST");
        StringTokenizer r = new StringTokenizer(args);
        expect(r, "FL");
        tok(r); //ser#
        String item = tok(r); //item#
        String total = tok(r); //total item count
        String dst = tok(r);
        LoginIdUtil.checkValid_Fatal(dst);
        dst = LoginIdUtil.normalize(dst);
        ses.ignoreContactListItem(dst, true, false, pctx);
        if (item.equals(total))
          finish();
      }
    }).waitFor(true, ses, pctx);

    if (contactList == null) return;

    for (int i = 0; i < contactList.length; i++)
    {
      String dstLoginId = contactList[i];
      if (dstLoginId == null)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("BUG!", new Exception("contactList["+i+"] is null"));
        continue;
      }
      try
      {
        LoginIdUtil.checkValid_Ignorable(dstLoginId);
        dstLoginId = LoginIdUtil.normalize(dstLoginId);
      }
      catch (Exception ex)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("cl item ignored", ex);
        continue;
      }

      if (ses.getLoginId().equals(dstLoginId))
        throw new MessagingNetworkException(
          "cannot login with yourself on a contact list",
          MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
          MessagingNetworkException.ENDUSER_CANNOT_LOGIN_WITH_YOURSELF_ON_CONTACT_LIST);

      if (!ses.contactListItemExists(dstLoginId))
      {
        ses.addToContactList(dstLoginId, false, pctx);
      }
    }
  }

  public void setClientStatus(final int status, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    start(new Transaction()
    {
      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        post("CHG", StatusUtil.nativeStatusAsMnemonicString(status), ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        int st = StatusUtil.mnemonicStringAsNativeStatus(args);
        if (!cmd.equals("CHG") || st != status)
          throwProtocolViolated("must be CHG "+StatusUtil.nativeStatusAsMnemonicString(status));
        //CHG 7 NLN
        if (st == StatusUtil.NATIVE_STATUS_OFFLINE)
          ses.logout(pctx,
            MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
            "arrived from server",
            MessagingNetworkException.ENDUSER_LOGGED_OFF_MESSAGING_SERVER_REPORTED_YOU_AS_OFFLINE);
        else
          ses.setStatus_Native_Internal(st, false, pctx,
            MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
            "arrived from server",
            MessagingNetworkException.ENDUSER_STATUS_CHANGED_UNDEFINED_REASON);
        finish();
      }
    }).waitFor(false, ses, pctx);
  }

  public SSS createSSS(Session ses, final String dst, final PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    start(new Transaction()
    {
      final Transaction xfrTransaction = getThisTransaction();

      public void clientRequest(TransactionContext ctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        post("XFR", "SB", ctx);
      }

      public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
      throws IOException, InterruptedException, MessagingNetworkException
      {
        //XFR 9 SB 64.4.13.88:1863 CKI 989487642.2070896604
        StringTokenizer r = new StringTokenizer(args);

        if (!cmd.equals("XFR"))
          throwProtocolViolated("must be XFR");
        expect(r, "SB");
        final HostPort hp;
        String s = tok(r);
        try
        {
          hp = HostPortUtil.parse(s, MSN_DEFAULT_PORT);
        }
        catch (IllegalArgumentException ex)
        {
          throwProtocolViolated("must be host:port, but it is "+StringUtil.quote(s));
          return;
        }
        expect(r, "CKI");
        final String hash = tok(r);

        if (this.CAT.isDebugEnabled()) this.CAT.debug("connecting to SSS");

        SSS sss_ = null;
        try
        {
          sss_ = connectSSS(hp.host, hp.port, dst, ses, pctx);
          if (sss_ == null) return; //we became offline
        }
        finally
        {
          if (sss_ == null)
            if (Defines.DEBUG && this.CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) this.CAT.error("cannot connect to SSS");
        }

        final SSS sss = sss_;

        sss.start(new Transaction()
        {
          int state = 0;

          public void clientRequest(TransactionContext ctx)
          throws IOException, InterruptedException, MessagingNetworkException
          {
            //USR 1 venky_dude@hotmail.com 989487642.2070896604
            post("USR", ses.getLoginId()+" "+hash, ctx);
          }

          public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
          throws IOException, InterruptedException, MessagingNetworkException
          {
            StringTokenizer r = new StringTokenizer(args);
            //USR 1 OK venky_dude@hotmail.com venkat
            if (cmd.equals("USR") && tok(r).equals("OK"))
            {
              //CAL 2 deadxxx@hotmail.com
              post("CAL", dst, ctx);
              return;
            }
            //CAL 2 RINGING 11717653
            if (cmd.equals("CAL"))
            {
              //return; //ignore session id
              finish();
              xfrTransaction.finish();
            }
          }

          public void errorServerResponse(ErrorInfo errorInfo, String args, TransactionContext tctx, Session ses, PluginContext pctx)
          throws IOException, InterruptedException, MessagingNetworkException
          {
            xfrTransaction.finish(Errors.createException(errorInfo));
            super.errorServerResponse(errorInfo, args, tctx, ses, pctx);
          }
        });
      }
    }).waitFor(false, ses, pctx);

    SSS sss = ses.peekSSS(dst);
    if (sss != null && !sss.isClosed())
    {
      //JOI deadlee@hotmail.com Venkatesh
      sss.prepare(); //wait for JOI
    }
    return sss;
  }

  SSS connectSSS(String host, int port, String dst, Session ses, PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    SSS sss_ = new SSS(host, port, dst, pctx);
    ses.addSSS(dst, sss_);
    if (ses.getStatus_Native() == StatusUtil.NATIVE_STATUS_OFFLINE)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("hl session logged out, closing SSS.");
      sss_.close(ses);
      return null;
    }
    return sss_;
  }

  protected void serverRequest(final String cmd, final String args, final Session ses, final PluginContext pctx)
  throws IOException, InterruptedException, MessagingNetworkException
  {
    StringTokenizer r = new StringTokenizer(args);

    switch (cmd.charAt(0))
    {
      case 'A':
        if (cmd.equals("ADD"))
        {
          //ADD //async add to a contact list/other list
          //ADD TrID LIST ser# UserHandle CustomUserName
          if (!"RL".equals(tok(r))) break; //ignore additions to FL/AL
          tok(r); //ser#
          String dst = tok(r);
          String nick = decodeNick(tok(r));
          LoginIdUtil.checkValid_Ignorable(dst);
          dst = LoginIdUtil.normalize(dst);
          ses.fireYouAreAddedToContactList(dst, nick, pctx);
          return;
        }
        break;
      /*
      case 'B':
        if (cmd.equals("BPR"))
        {
          //
          //  //account details for buddies
          //  BPR 60 buddy2@xxx.com PHH 3%20(2343)%20546456\r\n
          //  BPR 60 buddy2@xxx.com PHW 3%20(3454)%20456456\r\n
          //  BPR 60 buddy2@xxx.com PHM 3%20(6666)%20456456\r\n
          //  //BPR 60 buddy2@xxx.com MOB N\r\n

          tok(r); //ser#
          String buddyLoginId = tok(r);
          String propName = tok(r);
          if (!AccountPropertiesUtil.isKnownPhoneProperty(propName)) return;
          String propValue = (r.hasMoreTokens() ? r.nextToken() : null);
          if (propValue == null) return;
          ses.setAccountProperty(buddyLoginId, propName, "+"+java.net.URLDecoder.decode(propValue));
          return;
        }
        break;
      */
      case 'I':
      case 'N':
        if (cmd.equals("NLN") || cmd.equals("ILN"))
        {
          /*
            NLN 9 NLN deaxxxx@hotmail.com Venka tesh //online
            NLN 9 BSY deaxxxx@hotmail.com Venka tesh //busy
            NLN 9 IDL deaxxxx@hotmail.com Venka tesh //idle
            NLN 9 BRB deaxxxx@hotmail.com Venka tesh //brb
            NLN 9 AWY deaxxxx@hotmail.com Venka tesh //away
            NLN 9 PHN deaxxxx@hotmail.com Venka tesh //phone
            NLN 9 LUN deaxxxx@hotmail.com Venka tesh //lunch
          */
          int st = StatusUtil.mnemonicStringAsNativeStatus(tok(r));
          String dst = tok(r);
          //friendly name ignored
          if (ses.getLoginId().equals(dst))
            ses.setStatus_Native_Internal(
              st, false, pctx,
              MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
              "arrived from server",
              MessagingNetworkException.ENDUSER_STATUS_CHANGED_UNDEFINED_REASON);
          else
            ses.setContactStatus_Native(dst, st, pctx,
              MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
              "arrived from server");
          return;
        }
        break;
      case 'F':
        if (cmd.equals("FLN"))
        {
          //FLN 9 FLN deaxxxx@hotmail.com
          //FLN joe@openmim.ru
          //int st = StatusUtil.mnemonicStringAsNativeStatus(tok(r));
          String dst = tok(r);
          if (ses.getLoginId().equals(dst))
          {
            //status change ignored, waiting for "OUT <reason>"
          }
          else
            ses.setContactStatus_Native(dst, StatusUtil.NATIVE_STATUS_OFFLINE, pctx,
              MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
              "arrived from server");
          return;
        }
        break;
      case 'O':
        if (cmd.equals("OUT"))
        {
          //OUT //connection-close
          String reason = null;
          if (r.hasMoreTokens()) reason = r.nextToken();
          if (reason.equals("OTH"))
            ses.logout(pctx,
              MessagingNetworkException.CATEGORY_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER,
              "logged on from another location",
              MessagingNetworkException.ENDUSER_LOGGED_OFF_YOU_LOGGED_ON_FROM_ANOTHER_COMPUTER);
          else
            ses.logout(pctx,
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_SERVER_OR_PROTOCOL_ERROR,
              "arrived from server",
              MessagingNetworkException.ENDUSER_LOGGED_OFF_MESSAGING_SERVER_REPORTED_YOU_AS_OFFLINE);
          return;
        }
        break;
      case 'X':
        if (cmd.equals("XFR"))
        {
          //XFR referral to other NS or SS
          throwProtocolViolated("XFR NOT_IMPLEMENTED"); //not implemented for DS/NS.
          //moveTo(String host, int port);
          //return;
        }
        break;
      case 'R':
        if (cmd.equals("RNG"))
        {
          //RNG 11742066 64.4.13.74:1863 CKI 989495494.750408580 deaxxxx@hotmail.com Venkatesh
          //SS session invitation
          final String sssId = tok(r);
          final HostPort hp;
          String s = tok(r);
          try
          {
            hp = HostPortUtil.parse(s, MSN_DEFAULT_PORT);
          }
          catch (IllegalArgumentException ex)
          {
            throwProtocolViolated("must be host:port, but it is "+StringUtil.quote(s));
            break;
          }
          expect(r, "CKI");
          final String hash = tok(r);
          final String dst = tok(r);

          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("SSS invitation by "+dst+", connecting");

          SSS sss_ = null;
          try
          {
            sss_ = connectSSS(hp.host, hp.port, dst, ses, pctx);
            if (sss_ == null) return; //we became offline
          }
          catch (Exception ex)
          {
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("cannot connect to SSS, invitation ignored.");
            return;
          }

          final SSS sss = sss_;

          sss.start(new Transaction()
          {
            int state = 0;
            public void clientRequest(TransactionContext ctx)
            throws IOException, InterruptedException, MessagingNetworkException
            {
              //ANS 1 venky_dude@hotmail.com 989495494.750408580 11742066
              post("ANS", ses.getLoginId()+" "+hash+" "+sssId, ctx);
            }

            public void serverResponse(String cmd, String args, TransactionContext ctx, final Session ses, final PluginContext pctx)
            throws IOException, InterruptedException, MessagingNetworkException
            {
              //  IRO      Switchboard      User_       Provides the initial roster
              //                                         information for new users
              //                                         joining the session.
              //
              //  S: IRO 1 1 1 joe@idisys.iae.nsk.su joe@idisys

              /*
                  S: IRO TrID Participant# TotalParticipants UserHandle FriendlyName

                  The IRO commands relay to the newly joined client roster information
                  about the current session. Each IRO command message from the
                  Switchboard contains one participant in the session.
                  - Participant# contains the index of the participant described in
                    this IRO command (e.g. 1 of N, 2 of N).
                  - TotalParticipants contains the total number of participants
                    currently in the session.

                  The entire session roster will be sent to the new client joining the
                  session before any JOI or BYE commands described below.
              */
              if (cmd.equals("IRO"))
              {
                StringTokenizer r = new StringTokenizer(args);
                tok(r); //p#
                String total = tok(r);
                if (!"1".equals(total))
                {
                  sss.BYE(ses);
                  sss.close("we must leave the SSS when we are participant number >= 3", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF);
                  ses.removeSSS(sss);
                  finish(new MessagingNetworkException("we must leave the SSS when we are participant number >= 3", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_TOO_MANY_PARTICIPANTS_IN_THE_ROOM_NOT_LOGGED_OFF));
                  return;
                }
                state = 1;
                return;
              }
              //ANS 1 OK
              if (cmd.equals("ANS") && args.equals("OK"))
              {
                if (state == 0)
                {
                  sss.close("recipient went offline", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE);
                  ses.removeSSS(sss);
                  finish(new MessagingNetworkException("recipient went offline", MessagingNetworkException.CATEGORY_STILL_CONNECTED, MessagingNetworkException.ENDUSER_CANNOT_COMPLETE_REQUEST_RECIPIENT_IS_OFFLINE));
                  return;
                }
                else
                {
                  //we'll wait for incoming messages there.
                  sss.loggedIn();
                }
                finish();
              }
              else
                throwProtocolViolated("unknown cmd during SSS handshake"); //unknown command
            }
          });
          return;
        }
        break;
        //MSG //ignored, hotmail inbox
        //LST //ignored, contact list item/other list item
        //REM //ignored, notifies of async removals from lists
        //BLP //ignored, privacy settings
        //GTC //ignored, privacy settings
        //ILN //???, notifies the client of the
              /* initial online state of a user in the FL, while
                 either logging on or adding a user to the FL. */
        //SYN //ignored, initiates client-server property synchronization.
    }

    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("unknown cmd ignored");
  }

  protected boolean isNS()
  {
    return true;
  }
}