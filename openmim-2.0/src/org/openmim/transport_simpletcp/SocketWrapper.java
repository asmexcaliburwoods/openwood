package org.openmim.transport_simpletcp;

import java.io.*;

import org.openmim.*;
import org.openmim.icq.util.joe.*;
import org.openmim.infrastructure.taskmanager.*;

final class SocketWrapper extends TaskImpl
{
  private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(SocketWrapper.class.getName());

  final InputStream inputStream;
  private final AbstractConsumer consumer;
  private final int timeoutMillis;

  SocketWrapper(InputStream is, AbstractConsumer c, int timeoutMillis)
  {
    this.inputStream = is;
    this.consumer = c;
    this.timeoutMillis = timeoutMillis;
  }

  public final String toString()
  {
    return inputStream.toString();
  }
  
  final static int STATE_IDLE = 1;
  final static int STATE_BEING_SERVICED = 2;
  final static int STATE_CLOSED = 3;

  private int state = STATE_IDLE;

  synchronized boolean isClosed()
  {
    return state == STATE_CLOSED;
  }

  void service(ThreadPool tm) throws Exception
  {
    synchronized(this)
    {
      if (state != STATE_IDLE) return;
      state = STATE_BEING_SERVICED;
    }
    tm.execute(this);
  }

  public final void execute() throws Exception
  {
    boolean closed = false;
    try
    {
      InputStream is = this.inputStream;
      if (is == null) Lang.ASSERT_NOT_NULL(is, "is");
      int av = is.available();
      if (av <= 0) return;

      fetch(is, av, System.currentTimeMillis() + timeoutMillis);
    }
    catch (Exception ex)
    {
      closed = true;
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("", ex);
    }
    finally
    {
      synchronized(this)
      {
        if (state == STATE_IDLE)
          Lang.ASSERT(state != STATE_IDLE, "state must be != STATE_IDLE, but it is.");

        if (closed) state = STATE_CLOSED;
        else
        if (state == STATE_BEING_SERVICED)
          state = STATE_IDLE;
      }
    }
  }

  private final void fetch(final InputStream is, int datalen, long abortTimeMillis)
  throws Exception
  {
    if (datalen <= 0) return;
    AbstractConsumer c = this.consumer;
    if (c == null) Lang.ASSERT_NOT_NULL(c, "c");
    c.consume(is, datalen, abortTimeMillis);
  }
}
