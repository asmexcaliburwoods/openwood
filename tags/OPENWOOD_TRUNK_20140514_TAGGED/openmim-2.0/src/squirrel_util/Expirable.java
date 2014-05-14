package squirrel_util;

/**
 * Insert the type's description here.
 * Creation date: (30.01.01 22:33:01)
 * @author: 
 */
public abstract class Expirable
{
	private java.util.Vector expiryListeners = new java.util.Vector(1, 1);
	private boolean expired = false;
	private ExpiryQueue expiryQueue;
protected Expirable(ExpiryQueue q)
{
	Lang.ASSERT_NOT_NULL(q, "q");
	expiryQueue = q;
}
	
public void addExpiryListener(ExpiryListener el)
{
	Lang.ASSERT_NOT_NULL(el, "el");
	expiryListeners.addElement(el);
}
	
/**
 * Insert the method's description here.
 * Creation date: (03.02.01 22:58:53)
 * @param newExpired boolean
 */
public void expired()
{
	expired = true;
	synchronized (expiryListeners)
	{
		final ExpiryEvent ee = new ExpiryEvent(this, ExpiryEvent.EXPIRED);
		java.util.Enumeration e = expiryListeners.elements();
		while (e.hasMoreElements())
		{
			ExpiryListener el = (ExpiryListener) e.nextElement();
			try
			{
				el.expired(ee);
			}
			catch (Exception ex)
			{
				Logger.printException(ex);
			}
		}
	}
}
/**
 * Insert the method's description here.
 * Creation date: (03.02.01 23:40:16)
 * @return squirrel_util.ExpiryQueue
 */
public final ExpiryQueue getExpiryQueue() {
	return expiryQueue;
}
/**
 * Insert the method's description here.
 * Creation date: (03.02.01 22:58:53)
 * @return boolean
 */
public boolean isExpired() {
	return expired;
}
public void removeExpiryListener(ExpiryListener el)
{
	Lang.ASSERT_NOT_NULL(el, "el");
	expiryListeners.removeElement(el);
}
public void touch()
{
	Lang.ASSERT_NOT_NULL(expiryQueue, "expiryQueue");
	expiryQueue.touch(this);
}
public void unregisterFromExpiryQueue()
{
	Lang.ASSERT_NOT_NULL(expiryQueue, "expiryQueue");
	expiryQueue.unregister(this);
	synchronized (expiryListeners)
	{
		final ExpiryEvent ee = new ExpiryEvent(this, ExpiryEvent.UNREGISTERED);
		java.util.Enumeration e = expiryListeners.elements();
		while (e.hasMoreElements())
		{
			ExpiryListener el = (ExpiryListener) e.nextElement();
			try
			{
				el.unregistered(ee);
			}
			catch (Exception ex)
			{
				Logger.printException(ex);
			}
		}
	}
}
}
