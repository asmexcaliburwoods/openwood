package org.openmim.wrapper;

import java.util.*;

import org.openmim.*;
import org.openmim.mn.MessagingNetworkListener;
import org.openmim.icq.util.joe.*;
import org.apache.log4j.Logger;


/**
  MR: rarify changestatus(src, dst) events for dst != src

  ,,,.messaging.wrapper.MessagingNetworkStatusFilter:
  //implements MessagingNetwork
  если статус src X dst Y (X != Y) меняется чаще, чем раз в минуту,
  то фильтровать прореживать статус нотификации до раза в минуту.
  Это учитывает from-core statuses & from-icq-server statuses.
*/
public class StatusFilterUtil
{
  private final static Logger CAT = Logger.getLogger(StatusFilterUtil.class.getName());

  private static long statusFilterPeriodMillis = -1;

  static
  {
    try
    {
      Properties p = PropertyUtil.loadResourceProperties(PropertyUtil.getResourceFilePathName(StatusFilterWrapper.class));

      if (Defines.DEBUG && CAT.isInfoEnabled()) CAT.info("StatusFilter properties: "+p);

      int statusFilterPeriodSeconds = PropertyUtil.getRequiredPropertyInt(p, StatusFilterWrapper.class+" properties resource", "statusFilterPeriodSeconds");
      if ((statusFilterPeriodSeconds) <= 0) Lang.EXPECT_POSITIVE(statusFilterPeriodSeconds, "statusFilterPeriodSeconds in "+StatusFilterWrapper.class+" properties resource");
      statusFilterPeriodMillis = 1000 * (long) statusFilterPeriodSeconds;
    }
    catch (RuntimeException ex)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ex in static init", ex);
      throw ex;
    }
    catch (Throwable tr)
    {
      if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("ex in static init", tr);
      throw new RuntimeException(""+tr);
    }
  }


  /** statusMim */
  public static void fireContactListEntryStatusChange(MessagingNetworkSession ses, byte networkId, String srcLoginId, String dstLoginId, int status, int reasonLogger, String reasonMessage, int endUserReasonCode, List mnlisteners)
  {
    if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("statusChanged(): srcLoginId="+srcLoginId);
    //if srcLoginId == dstLoginId, then pass status with no handling
    if (srcLoginId.equals(dstLoginId))
    {
      fireStatusChange_Uncond(networkId, srcLoginId, dstLoginId, status, reasonLogger, reasonMessage, endUserReasonCode, mnlisteners);
      return;
    }

    //otherwise, filter it
    if (ses == null)
    {
      if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("session for "+srcLoginId+" is null, ignoring statusChanged (...) event.");
      return;
    }
    
    synchronized (ses.getFireStatusLock())
    {
      MessagingNetworkContactListItem cli = ses.getContactListItem(dstLoginId);
      if (cli == null)
      {
        if (Defines.DEBUG && CAT.isEnabledFor(org.apache.log4j.Level.ERROR)) CAT.error("dst "+dstLoginId+" deleted from the ["+srcLoginId+"]'s contact list, statusChange event ignored");
        return;
      }
      
      long scheduledTimeMillis = cli.getScheduledStatusChangeSendTimeMillis();
      long lastChangeTimeMillis = cli.getContactStatusLastChangeTimeMillis();
      long now = System.currentTimeMillis();
      if (now >= scheduledTimeMillis || now >= lastChangeTimeMillis + statusFilterPeriodMillis)
      {
        //ready to send status event

        //Session.tick() should never update until status changes again
        cli.setScheduledStatusChangeSendTimeMillis(Long.MAX_VALUE);
        cli.setContactStatusLastChangeTimeMillis(now);

        fireStatusChange_Uncond(networkId, srcLoginId, dstLoginId, status, reasonLogger, reasonMessage, endUserReasonCode, mnlisteners);
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("statusChanged() sent by statusfilter: srcLoginId="+srcLoginId+" dst="+dstLoginId+" status="+StatusUtilMim.translateStatusMimToString(status));
      }
      else
      {
        if (Defines.DEBUG && CAT.isDebugEnabled()) CAT.debug("statusChanged() delayed by statusfilter (until "+new Date(lastChangeTimeMillis + statusFilterPeriodMillis)+"): srcLoginId="+srcLoginId+" dst="+dstLoginId+" status="+StatusUtilMim.translateStatusMimToString(status));
        cli.setScheduledStatusChangeSendTimeMillis(lastChangeTimeMillis + statusFilterPeriodMillis);
      }
    }
  }

  private static void fireStatusChange_Uncond(byte networkId, String srcLoginId, String dstLoginId, int statusMim, int reasonLogger, String reasonMessage, int endUserReasonCode, List mnlisteners)
  {
    synchronized (mnlisteners)
    {
      for(int i = 0; i < mnlisteners.size(); ++i)
      {
        MessagingNetworkListener l = (MessagingNetworkListener) mnlisteners.get(i);
        l.statusChanged(networkId, srcLoginId, dstLoginId, statusMim, reasonLogger, reasonMessage, endUserReasonCode);
      }
    }
  }
}