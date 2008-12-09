package org.openmim.irc.driver.dcc;

import squirrel_util.TimeoutExpiryQueue;
import squirrel_util.Logger;
import squirrel_util.Expirable;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   DCCRequest.java
public abstract class DCCRequest extends Expirable
{
  public static final int INCOMING_FILE_TRANSFER = 0;
  public static final int OUTCOMING_FILE_TRANSFER = 1;
  private int type;
protected DCCRequest(TimeoutExpiryQueue q, int i)
{
	super(q);
  checkType(i);
  type = i;
}
protected void checkType(int i)
{
  if (i != 0 && i != 1)
	throw new IllegalArgumentException("bad request type: " + i);
  else
	return;
}
public TimeoutExpiryQueue getTimeoutExpiryQueue()
{
	return (TimeoutExpiryQueue) getExpiryQueue();
}
public int getType()
{
  checkType(type);
  return type;
}
public void unhandledException(Throwable tr)
{
  Logger.printException(tr);
}
}
