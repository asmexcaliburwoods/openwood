package org.openmim.icq.util.joe;

public class HostPort
{
  public final String host;
  public final int port;

  public HostPort(String host, int port)
  {
    if (host == null)
      throw new IllegalArgumentException("host can't be null");
    this.host = host.trim();
    this.port = port;
  }
  
  public String toString()
  {
    return host+":"+port;
  }
}
