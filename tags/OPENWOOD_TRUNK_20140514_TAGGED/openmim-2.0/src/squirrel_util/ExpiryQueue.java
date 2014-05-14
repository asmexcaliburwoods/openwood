package squirrel_util;

/**
 * Insert the type's description here.
 * Creation date: (30.01.01 22:33:01)
 * @author: 
 */
public interface ExpiryQueue
{
	void touch(Expirable e);
	void unregister(Expirable e);
}
