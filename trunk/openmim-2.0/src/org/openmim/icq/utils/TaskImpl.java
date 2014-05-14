package org.openmim.icq.utils;

import org.openmim.infrastructure.taskmanager.Task;

public abstract class TaskImpl implements Task {
  //public abstract void execute() throws Exception;
  public final String getId() { return ""; }
  public final int getState() { return INPROCESS; }
  public final void terminate() {}
  public final long getStartTime() { return 0; }
  public final boolean terminatable() { return false; }
}