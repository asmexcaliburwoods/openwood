package org.openmim.icq2k;

import org.openmim.icq.utils.*;
import org.openmim.*;
import org.openmim.messaging_network.MessagingNetworkException;
import org.openmim.stuff.AsyncOperation;

public abstract class MessagingTask extends TaskImpl
{
  public MessagingTask(String taskName, AsyncOperation op)
  {
    if (op == null) Lang.ASSERT_NOT_NULL(op, "op");
    this.taskName = taskName;
    this.op = op;
    this.session = null;
    this.ctx = null;
  }

  public MessagingTask(String taskName, Session session, PluginContext ctx)
  {
    this(taskName, session);
  }
  
  public MessagingTask(String taskName, Session session)
  {
    this.taskName = taskName;
    this.op = null;
    this.session = session;
    this.ctx = session.ctx;
  }

  public abstract void run() throws Exception;
  
  public void cancel(MessagingNetworkException ex) throws Exception {}

  private final String taskName;
  private final AsyncOperation op;
  private final Session session;
  private final PluginContext ctx;

  public final void execute()
  {
    try
    {
      run();
    }
    catch (Throwable tr)
    {
      if (op != null) op.fail(tr);
      if (session != null) session.eatException(tr, ctx);
    }
    if (op != null) op.cancel(this);
    else session.cancel(ctx, this);
  }

  public String toString()
  {
    return (op == null ? "session task: " + taskName+", session: "+session : taskName+", op: "+op);
  }
}
