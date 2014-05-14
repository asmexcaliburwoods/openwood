package org.openmim.msn;

import java.util.*;

import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.Defines;
import org.openmim.icq.utils.*;
import org.openmim.icq.util.*;

/**
  Manages sessions and threads that belong to each given
  MSNMessagingNetwork instance.
*/
class ResourceManager
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ResourceManager.class.getName());
  private final Hashtable loginId2session = new java.util.Hashtable(25, 25);
  private ServeSessionsThread sessionThread;
  //### private LoadBalancer loadBalancer;
  private final PluginContext ctx;

  ResourceManager(PluginContext ctx)
  {
    this.ctx = ctx;
  }

  final Session createSession(String srcLoginId) throws MessagingNetworkException
  {
    Session session = getSession(srcLoginId);
    MLang.EXPECT(
      session == null, "Second login request for " + srcLoginId + " ignored.",
      MessagingNetworkException.CATEGORY_STILL_CONNECTED,
      MessagingNetworkException.ENDUSER_SECOND_LOGIN_REQUEST_IGNORED);

    synchronized (loginId2session)
    {
      session = makeSessionInstance(srcLoginId);
      loginId2session.put(srcLoginId, session);
      notifyThread();
      return session;
    }
  }

  /** Session factory */
  protected Session makeSessionInstance(String srcLoginId) throws MessagingNetworkException
  {
    return new Session(srcLoginId);
  }

  Connection createTCPConnection(java.net.InetAddress ia, int port, PluginContext ctx)
  throws java.io.IOException, MessagingNetworkException
  {
    /*###
    if (MSNMessagingNetwork.multiplexorsUsed)
    {
      LoadBalancer lb = this.loadBalancer;
      if ((lb) == null) Lang.ASSERT_NOT_NULL(lb, "this.loadBalancer");
      return new MultiplexorConnectionWrapper(ia, port, ctx, lb);
    }
    else
    */
    {
      return new TCPConnection(ia, port, ctx);
    }
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


  Enumeration getSessions()
  {
    synchronized (loginId2session)
    {
      return ((Hashtable) loginId2session.clone()).elements();
    }
  }

  private void notifyThread()
  {
    if (loginId2session.size() == 0)
    {
      loginId2session.notify();
    }
    else
    {
      if (sessionThread == null)
      {
        sessionThread = new ServeSessionsThread(ctx);
        sessionThread.start();
      }
      else
      {
        loginId2session.notify();
      }
    }
  }


  void notifyToHandleData()
  {
    synchronized (loginId2session)
    {
      notifyThread();
    }
  }


  void removeSession(Session session)
  {
    if ((session.getStatus_Native()) != (StatusUtil.NATIVE_STATUS_OFFLINE)) Lang.ASSERT_EQUAL(session.getStatus_Native(), StatusUtil.NATIVE_STATUS_OFFLINE, "status for '" + session.getLoginId() + "'", "StatusUtil.NATIVE_STATUS_OFFLINE");
    synchronized (loginId2session)
    {
      loginId2session.remove(session.getLoginId());
      notifyThread();
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
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUG!!!", new AssertException("loginId2session must be empty after deinit()"));
        loginId2session.clear(); //to be more safe.

      /* ###
        if (MSNMessagingNetwork.multiplexorsUsed)
        {
          try
          {
            LoadBalancer lb;
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("creating LoadBalancer");
            this.loadBalancer = lb = new LoadBalancer();
            if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("initializing LoadBalancer");
            lb.init();
            lb.addMultiplexorEventListener(new MultiplexorEventListener()
            {
              public void dataReceived()
              {
                notifyToHandleData();
              }
            });
          }
          catch (InterruptedException ex)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("interrupted", ex);
          }
        }
        */
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
        /* ###
        LoadBalancer lb = loadBalancer;
        loadBalancer = null;
        if (lb != null)
          lb.deinit(); //close all transport connections if they exist
        */
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
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ex during deinit(), ignored", ex);
          }
        }
        if (loginId2session.size() != 0)
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("BUGGGGG", new AssertException("loginId2session must be empty after deinit()"));
        loginId2session.clear(); //to be more safe.

        Thread t = sessionThread;
        if (t != null)
        {
          t.interrupt();

          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("waiting "+(MSNMessagingNetwork.REQPARAM_INPUTDATA_TASKMANAGER_STOP_TIME_MILLIS/1000)+" sec. for the dispatcher thread to die...");
          try
          {
            t.join(MSNMessagingNetwork.REQPARAM_INPUTDATA_TASKMANAGER_STOP_TIME_MILLIS);
          }
          catch (InterruptedException ex)
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("interrupted ex", ex);
          }
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("waiting for dispatcher thread finished");
          if (t.isAlive())
            if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("thread "+t+" is still alive!", new Exception("still alive!"));
          synchronized (loginId2session)
          {
            sessionThread = null;
          }
        }
      }
    }
    finally
    {
      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("ResourceManager.deinit() leave");
    }
  }
}