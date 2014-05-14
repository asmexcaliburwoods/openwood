package org.openmim.icq2k;

import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;
import org.openmim.icq.util.*;

/**
  Manages sessions and threads that belong to each given
  ICQ2KMessagingNetwork instance.
*/
class ResourceManager
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ResourceManager.class.getName());
  private final Hashtable loginId2session = new java.util.Hashtable();
  protected final PluginContext ctx;

  ResourceManager(PluginContext ctx)
  {
    this.ctx = ctx;
  }

  private void printState(Session session, boolean added)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled())
      CAT.debug("session count: "+loginId2session.size()+"; session just "+(added?"added":"removed")+": "+session);
  }

  final Session getCreateSession(String srcLoginId) throws MessagingNetworkException
  {
    synchronized (loginId2session)
    {
      Session session = getSession(srcLoginId);
      if (session==null)
      {
        session = makeSessionInstance(srcLoginId);
        loginId2session.put(srcLoginId, session);
        Stats.STAT_ONLINE_USERS.increase();
        printState(session, true);
      }
      return session;
    }
  }

  /*
  final Session createAnonymousSession()
  {
    return new Session();
  }
   */

  /** Session factory */
  protected Session makeSessionInstance(String srcLoginId) throws MessagingNetworkException
  {
    return new Session(ctx, srcLoginId);
  }


  Aim_conn_t createTCPConnection(Session session, java.net.InetAddress ia, int port, FlapConsumer fc, PluginContext ctx)
  throws java.io.IOException, MessagingNetworkException
  {
    if (Defines.ENABLE_FAKE_PLUGIN)
    {
      return new org.openmim.icq2k.fake.FakeConnection(session, ia, port, fc, ctx);
    }
    else
    {
      return new SimpleTCPConnection(session, 0, ia, port, ctx, fc);
    }
  }

  boolean sessionExists(Session session)
  {
    return loginId2session.get(session.getLoginId()) != null;
  }

  Session getSession(String loginId)
  {
    if (StringUtil.isNullOrEmpty(loginId)) Lang.ASSERT_NOT_NULL_NOR_EMPTY(loginId, "loginId");
    return (Session) loginId2session.get(loginId);
  }


  Session getSessionNotNull(String srcLoginId, int endUserOperationErrorCode) throws MessagingNetworkException
  {
    Session session = getSession(srcLoginId);
    MLang.EXPECT(
      session != null, "Please login first. Cannot perform this operation while " + srcLoginId + " is logged out.",
      MessagingNetworkException.CATEGORY_NOT_CATEGORIZED,
      endUserOperationErrorCode);
    return session;
  }

  void removeSession(Session session)
  {
    if (session.getStatus_Oscar() != StatusUtil.OSCAR_STATUS_OFFLINE)
      Lang.ASSERT_EQUAL(session.getStatus_Oscar(), StatusUtil.OSCAR_STATUS_OFFLINE,
        "status for '" + session.getLoginId() + "'", "StatusUtil.OSCAR_STATUS_OFFLINE");
    synchronized (loginId2session)
    {
      if (loginId2session.remove(session.getLoginId()) != null) Stats.STAT_ONLINE_USERS.decrease();
      printState(session, false);
    }
  }

  void waitForSessionListChange(long timeoutMillis) throws InterruptedException
  {
    synchronized (loginId2session)
    {
      loginId2session.wait(timeoutMillis);
    }
  }

  public void init()
  {
    try
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ResourceManager.init() enter");

      synchronized (loginId2session)
      {
        if (loginId2session.size() != 0)
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUGGGGG", new AssertException("loginId2session must be empty after deinit()"));
        loginId2session.clear(); //to be more safe.
      }
    }
    finally
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ResourceManager.init() leave");
    }
  }

  public void deinit()
  {
    try
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ResourceManager.deinit() enter");

      synchronized (loginId2session)
      {
        Enumeration e = loginId2session.elements();
        while (e.hasMoreElements())
        {
          Session ses = (Session) e.nextElement();
          try
          {
            ses.setLastError(
              new MessagingNetworkException(
                "mim server restarts",
                MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
                MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN));
            ses.logout(ctx,
              MessagingNetworkException.CATEGORY_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN,
              "mim server restarts",
              MessagingNetworkException.ENDUSER_LOGGED_OFF_ON_BEHALF_OF_MESSAGING_PLUGIN_ADMIN);
          }
          catch (Exception ex)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex during icq2k.deinit(), ignored", ex);
          }
        }
        if (loginId2session.size() != 0)
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUGGGGG", new AssertException("loginId2session must be empty after deinit()"));
        loginId2session.clear(); //to be more safe.
      }
    }
    finally
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ResourceManager.deinit() leave");
    }
  }
}
