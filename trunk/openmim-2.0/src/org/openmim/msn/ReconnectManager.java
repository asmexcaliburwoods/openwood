package org.openmim.msn;

import java.util.*;

import org.openmim.*;
import org.openmim.icq.utils.*;
import org.openmim.infrastructure.taskmanager.*;
import org.openmim.stuff.Defines;

public class ReconnectManager
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ReconnectManager.class.getName());

  /**
    Sorted by scheduledReconnectTime,

    ev1.scheduledReconnectTime <= ev2.scheduledReconnectTime
    <=>
    scheduled.indexOf(ev1) <= scheduled.indexOf(ev2).
  */
  private final List scheduled = new LinkedList();

  static void initReconnectManagerState(SessionReconnecting ses)
  {
    ses.reconnectManagerState = 1;
  }

  static void reconnectFailed(SessionReconnecting ses)
  {
    if (ses.reconnectManagerState >= 3)
      return;
    ses.reconnectManagerState++;
  }

  public ReconnectManager()
  {
  }

  static int dbgCount = 1;

  private Task schedulerTask;

  private ThreadPool threadPool;

  /**
    "ReloginProcess:"
    Event state 1: Perform single relogin after 5+Math.random()*10seconds.  If it fails, enter stage 2.
    Event state 2: Perform single relogin after 50 seconds+Math.random()*20seconds.  If it fails, enter stage 3.
    Event state 3: Perform relogin after 4.5 minutes+Math.random()*60seconds. If it fails, perform relogins infinitely,
                   with the interval of 4.5 minutes+Math.random()*60seconds, until succeeded.
    During any event state, any offline status reason categories except for network errors remove
    the event from the relogin process, and cause the offline status notification to be sent.
  */
  void scheduleReconnect(SessionReconnecting ev)
  {
    if (ev.isRegisteredReloginsMaximumReached()) //relogins are too frequent
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("["+ev.getLoginId()+"]: relogins are too frequent, sleeping");
      scheduleEvent(ev, 1000L * (
        60 *
          MSNMessagingNetwork.REQPARAM_NETWORK_CONDITIONS_SWING__SLEEP_TIME_WHEN_MAXIMUM_REACHED_MINUTES
        +
        (int) (Math.random() * 60) //0..60
        ));
      return;
    }
    switch (ev.reconnectManagerState)
    {
      case 1:  scheduleEvent(ev, (long) (1000*(   5 + Math.random() * 10)));  break;
      case 2:  scheduleEvent(ev, (long) (1000*(  50 + Math.random() * 20)));  break;
      case 3:  scheduleEvent(ev, (long) (1000*(9*30 + Math.random() * 60)));  break;
      default: Lang.ASSERT_FALSE("invalid state: "+ev.reconnectManagerState);
    }
  }

  private void scheduleEvent(SessionReconnecting ev, long afterMillis)
  {
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("before sync block");
    synchronized (this)
    {
      //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("in sync block");
      int p = scheduled.indexOf(ev);
      if (p > -1)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId()+" is already scheduled to "+new Date(ev.scheduledReconnectTime)+", ignored");
        return;
      }
      ev.scheduledReconnectTime = System.currentTimeMillis() + afterMillis;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("scheduling reconnect of "+ev.getLoginId()+" to "+new Date(ev.scheduledReconnectTime)+" ("+(afterMillis/1000)+" seconds later)");
      Iterator it = scheduled.iterator();
      int i = 0;
      while (it.hasNext())
      {
        SessionReconnecting re = (SessionReconnecting) it.next();
        if (re.scheduledReconnectTime > ev.scheduledReconnectTime)
          break;
        i++;
      }
      scheduled.add(i, ev);
      if (i == 0)
        notify(); //handle time of the first scheduled event is changed.  scheduler thread will wakeup.
    }
    //if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId() + ": reconnect scheduled");
  }

  void cancelReconnect(SessionReconnecting ev)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId() + ": reconnect canceled");
    scheduled.remove(ev);
  }

  private void initSchedulerTask()
  {
    Task schedulerTask = new Task()
    {
      private int state = INPROCESS;

      public void execute() throws Exception
      {
        SessionReconnecting firstEv = null;
        while (getState() == INPROCESS)
        {
          if (Thread.currentThread().isInterrupted())
            return;
          synchronized (ReconnectManager.this)
          {
            if (scheduled.isEmpty())
            {
              if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("reconnect queue empty, scheduler waiting");
              ReconnectManager.this.wait();
              continue;
            }
            else
            {
              firstEv = (SessionReconnecting) scheduled.get(0);
              long pause = firstEv.scheduledReconnectTime - System.currentTimeMillis();
              if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("reconnect queue size="+scheduled.size()+", the nearest task is "+(pause/1000)+" seconds away");
              if (pause > 20)
              {
                firstEv = null;
                ReconnectManager.this.wait(pause);
                continue;
              }
              else
              {
                scheduled.remove(0);
              }
            }
          }
          if (getState() != INPROCESS || Thread.currentThread().isInterrupted())
            return;
          if ((firstEv) == null) Lang.ASSERT_NOT_NULL(firstEv, "firstEv");
          if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("launching reconnect task "+firstEv.getLoginId());
          startReloginTask(firstEv);
        }
      }

      public String getId()
      {
        return "ReconnectMgr_SchedulerTask"+(dbgCount++);
      }

      public synchronized int getState()
      {
        return state;
      }

      public synchronized void terminate()
      {
        state = TERMINATED;
      }

      public long getStartTime()
      {
        return 0;
      }

      public boolean terminatable()
      {
        return true;
      }
    };
    this.schedulerTask = schedulerTask;
    ThreadPool threadPool = new ThreadPool(
      MSNMessagingNetwork.REQPARAM_RECONNECTOR_THREADCOUNT_MAXIMUM,
      MSNMessagingNetwork.REQPARAM_RECONNECTOR_THREADCOUNT_OPTIMUM);
    this.threadPool = threadPool;
    threadPool.execute(schedulerTask);
  }

  private void startReloginTask(final SessionReconnecting ev)
  {
    Task reloginTask = new Task()
    {
      public void execute() throws Exception
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId() + ": task launched, attempting reconnect, session reconnect state="+ev.reconnectManagerState);
        ev.reconnect();
      }

      public String getId()
      {
        return "ReconnectMgr_ReloginTask"+(dbgCount++)+"_"+ev.getLoginId();
      }

      public int getState()
      {
        return INPROCESS;
      }

      public void terminate()
      {
      }

      public long getStartTime()
      {
        return 0;
      }

      public boolean terminatable()
      {
        return false;
      }
    };

    ThreadPool threadPool = this.threadPool;
    if (threadPool != null)
      threadPool.execute(reloginTask);
    else
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId() + ": MIM DEINIT in place, threadPool is null, skipped execute relogin task");
  }

  public void init()
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ReconnectManager.init() enter");
      initSchedulerTask();
    }
    finally
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ReconnectManager.init() leave");
    }
  }

  public void deinit()
  {
    try
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ReconnectManager.deinit() enter");
      Task schedulerTask = this.schedulerTask;
      if (schedulerTask != null)
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("terminating scheduler task");
        schedulerTask.terminate();
        schedulerTask = null;
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("terminating scheduler task done");
      }
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("waiting "+(MSNMessagingNetwork.REQPARAM_RECONNECTOR_TASKMANAGER_STOP_TIME_MILLIS/1000)+" sec. for reconnect taskman to die");
      threadPool.stop(MSNMessagingNetwork.REQPARAM_RECONNECTOR_TASKMANAGER_STOP_TIME_MILLIS);
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("reconnect taskman stopped");
      scheduled.clear();
    }
    finally
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("ReconnectManager.deinit() leave");
    }
  }
}