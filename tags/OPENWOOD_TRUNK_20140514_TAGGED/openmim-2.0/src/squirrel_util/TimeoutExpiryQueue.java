package squirrel_util;

/**
 * Insert the type's description here.
 * Creation date: (04.02.01 05:26:55)
 * @author: 
 */
public interface TimeoutExpiryQueue extends ExpiryQueue
{
	long getTimeoutMillis();
	void setTimeoutMillis(long timeoutMillis);
}
