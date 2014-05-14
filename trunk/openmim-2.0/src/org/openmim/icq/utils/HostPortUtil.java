package org.openmim.icq.utils;

public class HostPortUtil
{
  /** Parses "host:port" */
  public static HostPort parse(String hp, int defaultPort)
  throws IllegalArgumentException
  {
    if (hp == null)
      throw new IllegalArgumentException("hp can't be null");

    int colon = hp.lastIndexOf(':');
    String host = hp;
    int port = defaultPort;

    if (colon > -1)
    {
      try
      {
        port = Integer.parseInt(hp.substring(colon + 1).trim());
        host = hp.substring(0, colon);
      }
      catch (NumberFormatException ex)
      {
        throw new IllegalArgumentException("invalid port value, must be integer: \"" + hp + "\"");
      }
    }

    return new HostPort(host, port);
  }
}
