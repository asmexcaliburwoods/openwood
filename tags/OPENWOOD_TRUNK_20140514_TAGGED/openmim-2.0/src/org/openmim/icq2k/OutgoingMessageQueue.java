package org.openmim.icq2k;

import java.io.*;
import java.util.*;
import org.openmim.icq.util.joe.*;
import org.openmim.icq.util.joe.jsync.*;
import org.openmim.*;
import org.openmim.mn.MessagingNetworkException;

public class OutgoingMessageQueue
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(OutgoingMessageQueue.class.getName());

  /** MAX_SENDMSG_QUEUE_SIZE must be > AsyncOperationQueue.MAX_QUEUE_SIZE */
  private final static int MAX_SENDMSG_QUEUE_SIZE = 11;

  OutgoingMessageQueue(Aim_conn_t conn, Session session, PluginContext ctx)
  {
    if (conn == null) throw new AssertException("conn is null"); 
    this.conn = conn;
    this.session = session;
    this.ctx = ctx;
  }

  private class Message
  {
    AsyncOperations.OpSendGenericMessage op; //can be null
    byte[] pak;
    boolean packetHandled = false;
    public String toString()
    {
      return "genericmsg; op: "+op;
    }
  }

  private final Aim_conn_t conn;

  /*  
  static
  {
    Lang.ASSERT(AsyncOperationQueue.MAX_QUEUE_SIZE < MAX_SENDMSG_QUEUE_SIZE,
      "assert violated: AsyncOperationQueue.MAX_QUEUE_SIZE < OutgoingMessageQueue.MAX_SENDMSG_QUEUE_SIZE");
  }
  */

  private final List queue = new ArrayList(1);
  private final PluginContext ctx;
  private final Session session;

  /**
    Delivery time of last RATE2_MAXIMUM_MSGCOUNT msgs, in milliseconds.
    Time is kept as several Long values.
  */
  private final Wheel rate2msgsSendTimeQueue = new Wheel();

  private Message currentlyBeingSent = null;

  private final PacketListener packetListener = new PacketListener()
  {
    public void packetSent()
    {
      try
      {
        synchronized (OutgoingMessageQueue.this)
        {
          if (currentlyBeingSent == null) 
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("currentlyBeingSent is null", new Exception("null"));
            return;
          }
          if (currentlyBeingSent.packetHandled) return;
          currentlyBeingSent.packetHandled = true;
          try
          {
            if (rate2msgsSendTimeQueue.size() >= ICQ2KMessagingNetwork.REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT)
            {
              //remove the oldest msg
              rate2msgsSendTimeQueue.get();
            }
            rate2msgsSendTimeQueue.put(new Long(System.currentTimeMillis()));
            
            if (currentlyBeingSent.op != null)
              currentlyBeingSent.op.messageBodySent();
          }
          finally
          {
            next();
          }
        }
      }
      catch (Throwable tr)
      {
        session.eatException(tr);
      }
    }
    public void packetCanceled(MessagingNetworkException ex)
    {
      try
      {
        synchronized (OutgoingMessageQueue.this)
        {
          if (currentlyBeingSent == null) 
          {
            if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("currentlyBeingSent is null", new Exception("null"));
            return;
          }
          if (currentlyBeingSent.packetHandled) return;
          currentlyBeingSent.packetHandled = true;
          try
          {
            if (currentlyBeingSent.op != null)
              currentlyBeingSent.op.fail(ex);
          }
          finally
          {
            next();
          }
        }
      }
      catch (Throwable tr)
      {
        session.eatException(tr);
      }
    }
  };

  /** op can be null here */
  synchronized final public void sendMessage(byte[] packet, AsyncOperations.OpSendGenericMessage op)
  throws IOException, MessagingNetworkException, InterruptedException
  {
    if (isStopped()) 
    {
      op.fail(error);
      return;
    }
    
    while (!isStopped() && queue.size() >= MAX_SENDMSG_QUEUE_SIZE)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.WARN))
        CAT.warn("smq is full; waiting. op: "+op);
      /*  throw new MessagingNetworkException(
            "outgoing message queue is full",
            MessagingNetworkException.CATEGORY_STILL_CONNECTED,
            MessagingNetworkException.ENDUSER_QUEUE_FULL);   
      */
      wait();
    }
    
    if (isStopped()) 
    {
      if (op != null) op.fail(error);
      return;
    }
    
    if (op != null) 
    {
      op.rethrowExceptionIfFailed();
      if (op.isFinished()) return;
    }
        
    Message m = new Message();
    m.pak = packet;
    m.op = op;
    queue.add(m);
    printState(true, m);

    if (currentlyBeingSent == null) enqueueFirst();
  }

  private void enqueueFirst()
    /*throws MessagingNetworkException, InterruptedException */
  {
    final Message m = (Message) queue.remove(0);
    notify();
    printState(false, m);
    currentlyBeingSent = m;
    long time;
    if (rate2msgsSendTimeQueue.size() >= ICQ2KMessagingNetwork.REQPARAM_ADVANCED_RATECONTROL_RATE2_MAXIMUM_MSGCOUNT)
    {
      Long oldest = (Long) rate2msgsSendTimeQueue.peek();
      time = oldest.longValue() + ICQ2KMessagingNetwork.REQPARAM_ADVANCED_RATECONTROL_RATE2_PERIOD_MILLIS;
    }
    else
    {
      time = 0;
    }

    if (m.op != null)
    {
      try
      {
        m.op.runAt(
          time,
          new MessagingTask("sendm", m.op)
          {
            public void run() throws Exception
            {
              conn.send(m.pak, 2, m.op, OutgoingMessageQueue.this.packetListener);
            }
            
            public void cancel(MessagingNetworkException ex)
            {
              OutgoingMessageQueue.this.packetListener.packetCanceled(ex);
            }
          }
        );
      }
      catch (MessagingNetworkException e11)
      {
        m.op.fail(e11);
      }
    }
    else
    {
      session.runAt(
        time,
        new MessagingTask("sendm", session)
        {
          public void run() throws Exception
          {
            conn.send(m.pak, 2, null, OutgoingMessageQueue.this.packetListener);
          }
          
          public void cancel(MessagingNetworkException ex)
          {
            OutgoingMessageQueue.this.packetListener.packetCanceled(ex);
          }
        }
      );
    }
  }
  
  private void next()
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(this+": next(); smq size: "+queue.size()+"; currentmsg: "+currentlyBeingSent);
    Lang.ASSERT_NOT_NULL(currentlyBeingSent, "currentlyBeingSent");
    if (queue.isEmpty()) currentlyBeingSent = null;
    else enqueueFirst();
  }

  private void abortMessage(Message m, Throwable reason)
  {
    try
    {
      m.op.fail(reason);
    }
    catch (Exception ex) {}
  }

  private Throwable error = null;
  
  public synchronized void stop(Throwable error)
  {
    this.error = error;
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug(this+": stopped");
    for (int i = 0; i < queue.size(); ++i) abortMessage((Message) queue.get(i), error);
    queue.clear();
    printState(false, null);
    currentlyBeingSent = null;
    notifyAll();
  }

  public synchronized boolean isStopped()
  {
    return error != null;
  }
  
  private void printState(boolean added, OutgoingMessageQueue.Message m)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled())
    {
      if (m == null)
      {
        CAT.debug(this+": smq size: "+queue.size()+" (just cleared)");
      }
      else
      {
        CAT.debug(this+": smq size: "+queue.size()+", msg just "+(added?"added":"removed")+": "+m);
      }
    }
  }
  
  public final String toString()
  {
    return "smq for conn " + conn;
  }
}