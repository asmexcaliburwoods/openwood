package org.openmim.msn;

import org.openmim.mn.MessagingNetworkException;

/**
  Manages sessions and threads that belong to each given
  ICQ2KMessagingNetwork instance.
*/
class ResourceManagerReconnecting
extends ResourceManager
{
  //private final static org.apache.log4j.Logger CAT = org.apache.log4j.Logger.getLogger(ResourceManagerReconnecting.class.getName());

  private final ReconnectManager reconnectManager = new ReconnectManager();

  ResourceManagerReconnecting(PluginContext ctx)
  {
    super(ctx);
  }

  /** Session factory */
  protected Session makeSessionInstance(String srcLoginId) throws MessagingNetworkException
  {
    return new SessionReconnecting(srcLoginId);
  }

  ReconnectManager getReconnectManager()
  {
    return reconnectManager;
  }

  public void init()
  {
    reconnectManager.init();
    super.init();
  }
  public void deinit()
  {
    super.deinit();
    reconnectManager.deinit();
  }
}