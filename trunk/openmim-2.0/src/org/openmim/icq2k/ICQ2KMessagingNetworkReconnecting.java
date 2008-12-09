package org.openmim.icq2k;

public class ICQ2KMessagingNetworkReconnecting extends ICQ2KMessagingNetwork
{
  //private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ICQ2KMessagingNetworkReconnecting.class.getName());

  public ICQ2KMessagingNetworkReconnecting()
  {
  }

  protected ResourceManager makeResourceManagerInstance()
  {
    if (!REQPARAM_RECONNECTOR_USED)
    {
      return super.makeResourceManagerInstance();
    }
    else
    {
      return new ResourceManagerReconnecting(ctx);
    }
  }
}
