package squirrel_util;

/**
 * Insert the type's description here.
 * Creation date: (03.02.01 23:43:04)
 * @author: 
 */
public interface ExpiryListener
{
	void expired(ExpiryEvent e);
	void unregistered(ExpiryEvent e);
}
