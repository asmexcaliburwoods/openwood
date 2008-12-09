package squirrel_util;

import java.util.*;
/**
 * Insert the type's description here.
 * Creation date: (30.01.01 22:40:06)
 * @author: 
 */
public final class TimeoutExpiryQueueImpl implements TimeoutExpiryQueue
{
	private long timeoutMillis;
	private Hashtable expirable2ee = new Hashtable();
	private List entries = Collections.synchronizedList(new LinkedList());
	class ExpirableEntry
	{
		Expirable expirable;
		long lastTouchTime;
	}
	class ExpiryThread extends Thread
	{
		public void run()
		{
			synchronized (entries)
			{
				setName("TimeoutExpirySet " + getName());
				Logger.log("started: " + this);
				try
				{
					while (true)
					{
						Enumeration e = expirable2ee.elements();
						while (e.hasMoreElements())
						{
							ExpirableEntry ee = (ExpirableEntry) e.nextElement();
							if (ee.lastTouchTime + timeoutMillis >= System.currentTimeMillis())
								break;
							else
							{
								unregister(ee);
								try
								{
									ee.expirable.expired();
								}
								catch (Exception ex)
								{
									Logger.printException(ex);
								}
							}
						}
						if (expirable2ee.isEmpty())
							return;
						ExpirableEntry oldest_ee = (ExpirableEntry) entries.get(0);
						long waitTimeMillis = Math.max(1, oldest_ee.lastTouchTime + timeoutMillis - System.currentTimeMillis());
						entries.wait(waitTimeMillis);
					}
				}
				catch (Throwable tr)
				{
					Logger.printException(tr);
				}
				finally
				{
					Logger.log("finished: " + this);
				}
			}
		}
	}
	private ExpiryThread expiryThread;
/**
 * TimeoutExpirySet constructor comment.
 */
public TimeoutExpiryQueueImpl(long timeoutMillis)
{
	setTimeoutMillis(timeoutMillis);
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
public void addExpirable(Expirable e)
{
	Lang.ASSERT_NOT_NULL(e, "expirable");
	final ExpirableEntry entry = new ExpirableEntry();
	entry.expirable = e;
	synchronized (entries)
	{
		entry.lastTouchTime = System.currentTimeMillis();
		entries.add(entry);
		expirable2ee.put(e, entry);
		log("added: " + entry.expirable);
		log("object count: " + entries.size());
		if (expiryThread == null || !expiryThread.isAlive())
		{
			expiryThread = new ExpiryThread();
			expiryThread.start();
		}
	}
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @return long
 */
public long getTimeoutMillis()
{
	synchronized (entries)
	{
		return timeoutMillis;
	}
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
void log(String s)
{
	Logger.log("expirable queue #"+hashCode()+": "+s);
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
public void setTimeoutMillis(long newTimeoutMillis)
{
	Lang.ASSERT_POSITIVE(newTimeoutMillis, "newTimeoutMillis");
	synchronized (entries)
	{
		timeoutMillis = newTimeoutMillis;
		entries.notify();
	}
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
public void touch(Expirable e)
{
	Lang.ASSERT_NOT_NULL(e, "expirable");
	ExpirableEntry ee = (ExpirableEntry) expirable2ee.get(e);
	if (ee != null)
		touch(ee);
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
private void touch(ExpirableEntry e)
{
	Lang.ASSERT_NOT_NULL(e, "expirableEntry");
	synchronized (entries)
	{
		e.lastTouchTime = System.currentTimeMillis();
		entries.remove(e);
		entries.add(e);
	}
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
public void unregister(Expirable e)
{
	Lang.ASSERT_NOT_NULL(e, "expirable");
	ExpirableEntry ee = (ExpirableEntry) expirable2ee.get(e);
	if (ee != null)
		unregister(ee);	
}
/**
 * Insert the method's description here.
 * Creation date: (30.01.01 22:42:41)
 * @param newTimeoutMillis long
 */
private void unregister(ExpirableEntry ee)
{
	Lang.ASSERT_NOT_NULL(ee, "expirableEntry");
	synchronized (entries)
	{
		entries.remove(ee);
		expirable2ee.remove(ee.expirable);
		log("removed: " + ee.expirable);
		log("object count: " + entries.size());
	}
}
}
