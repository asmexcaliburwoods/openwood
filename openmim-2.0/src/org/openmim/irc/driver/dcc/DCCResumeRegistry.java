package org.openmim.irc.driver.dcc;

import squirrel_util.*;

import java.util.*;

/**
 * Insert the type's description here. Creation date: (04.10.00 7:09:56)
 * @author:
 */
public class DCCResumeRegistry
{
	public static final long TIMEOUT_MILLIS_DEFAULT = 60*1000;
  private Hashtable dccResumeId2dccReceiverWaitingForResume = new Hashtable();
  private Hashtable intPort2dccSender = new Hashtable();
	private TimeoutExpiryQueueImpl expiryQueue = new TimeoutExpiryQueueImpl(TIMEOUT_MILLIS_DEFAULT);
/** DCCResumeRegistry constructor comment. */
public DCCResumeRegistry()
{
  super();
}
public void addDccReceiverWaitingForResume(final DccReceiver dccreceiver) throws ExpectException
{
	Lang.ASSERT_NOT_NULL(dccreceiver, "dccreceiver");
	expiryQueue.addExpirable(dccreceiver);
	dccreceiver.addExpiryListener(new ExpiryListenerImpl(dccreceiver));
	Object dccResumeId = dccreceiver.getDccResumeId();
	Lang.EXPECT(getDccReceiverWaitingForResume(dccResumeId) == null, "Your party is probably using bad client, cannot proceed.  Details: this entry in dcc resume table is used, resume connection id: \"" + dccResumeId + "\".");
	dccResumeId2dccReceiverWaitingForResume.put(dccResumeId, dccreceiver);
	log("added: dcc resume id=" + dccResumeId + "\n  dccreceiver=" + dccreceiver + "\n  dccResumeId entries count: " + dccResumeId2dccReceiverWaitingForResume.size());
}
public void addDccSender(final DccSender ds)
{
  Lang.ASSERT_NOT_NULL(ds, "dccsender object");
  expiryQueue.addExpirable(ds);
	ds.addExpiryListener(new ExpiryListener2(ds));
  Lang.ASSERT(intPort2dccSender.put(new Integer(ds.getServerPort()), ds) == null, "This entry in dcc resume table is replaced, port=" + ds.getServerPort() + ".");
  log("added: dcc sender on server port: " + ds.getServerPort() + "\n  dcc sender=" + ds + "\n  dcc sender port entries count: " + intPort2dccSender.size());
}
/** Can return null if DCC resume canceled by receiver OR if DCC ACCEPT (illegally) received before DCC RESUME is sent. */
public DccReceiver getDccReceiverWaitingForResume(Object dccResumeId)
{
  Lang.ASSERT_NOT_NULL(dccResumeId, "dccResumeId");
  return (DccReceiver) dccResumeId2dccReceiverWaitingForResume.get(dccResumeId);
}
/** Can return null. */
public DccSender getDccSender(int port)
{
  return (DccSender) intPort2dccSender.get(new Integer(port));
}
/**
 * Insert the method's description here.
 * Creation date: (03.02.01 23:57:33)
 * @return squirrel_util.util.TimeoutExpiryList
 */
public final TimeoutExpiryQueue getTimeoutExpiryQueue()
{
	return expiryQueue;
}
private void log(String s)
{
	Logger.log("dcc resume registry:\n  " + s);
}
public DccReceiver removeDccReceiverWaitingForResume(Object dccResumeId)
{
	Lang.ASSERT_NOT_NULL(dccResumeId, "dccResumeId");
	DccReceiver dr = (DccReceiver) dccResumeId2dccReceiverWaitingForResume.remove(dccResumeId);
	if (dr != null)
		dr.unregisterFromExpiryQueue();
	log("removed: dcc resume id=" + dccResumeId + "\n  dccreceiver removed=" + dr + "\n  dccResumeId entries count: " + dccResumeId2dccReceiverWaitingForResume.size());
	return dr;
}
/** Can return null. */
public DccSender removeDccSender(int port)
{
	DccSender ds = (DccSender) intPort2dccSender.remove(new Integer(port));
	if (ds != null) ds.unregisterFromExpiryQueue();
	log("removed: dcc sender on server port: " + port + "\n  dcc sender removed=" + ds + "\n  dcc sender port entries count: " + intPort2dccSender.size());
	return ds;
}

    private class ExpiryListenerImpl implements ExpiryListener {
        private final DccReceiver dccreceiver;

        public ExpiryListenerImpl(DccReceiver dccreceiver) {
            this.dccreceiver = dccreceiver;
        }

        public void expired(ExpiryEvent e)
        {
        }

        public void unregistered(ExpiryEvent e)
        {
            removeDccReceiverWaitingForResume(dccreceiver.getDccResumeId());
            dccreceiver.removeExpiryListener(ExpiryListenerImpl.this);
        }
    }

    private class ExpiryListener2 implements ExpiryListener {
        private final DccSender ds;

        public ExpiryListener2(DccSender ds) {
            this.ds = ds;
        }

        public void expired(ExpiryEvent e)
        {
        }

        public void unregistered(ExpiryEvent e)
        {
            removeDccSender(ds.getServerPort());
            ds.removeExpiryListener(ExpiryListener2.this);
        }
    }
}
