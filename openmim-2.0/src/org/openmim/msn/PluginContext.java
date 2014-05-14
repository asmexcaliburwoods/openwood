package org.openmim.msn;

import org.openmim.*;
import org.openmim.stuff.TransportChooser;

/**
  Contains miscelanneous objects for the plugin instance context.
  <p>
  At the moment, contains a MessagingNetwork instance only.
  <p>
  Additionally, the ResourceManager & TransportChooser instances can be
  got from the MessagingNetwork instance.
*/
public final class PluginContext
{
  private final MSNMessagingNetwork plugin;

  public PluginContext(MSNMessagingNetwork plugin)
  {
    if ((plugin) == null) org.openmim.icq.utils.Lang.ASSERT_NOT_NULL(plugin, "plugin");
    this.plugin = plugin;
  }

  public final MSNMessagingNetwork getMSNMessagingNetwork()
  {
    return plugin;
  }

  public final TransportChooser getTransportChooser()
  {
    return plugin.getTransportChooser();
  }

  public final ResourceManager getResourceManager()
  {
    return plugin.getResourceManager();
  }
}