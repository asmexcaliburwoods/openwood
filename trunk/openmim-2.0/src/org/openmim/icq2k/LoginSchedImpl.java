package org.openmim.icq2k;

import java.util.*;

public final class LoginSchedImpl implements LoginSched
{
  private final long[] q;
  private final long td;
  private int pos;
  
  public LoginSchedImpl(int queueCount, long timeDistanceMillis)
  {
    if(timeDistanceMillis<0)throw new RuntimeException("bad param: timeDistanceMillis<0: "+timeDistanceMillis);
    if(queueCount<=0)throw new RuntimeException("bad param: queueCount<=0: "+queueCount);
    q=new long[queueCount];
    td=timeDistanceMillis;
  }
  
  public void init(){}
  public void deinit(){}
  
  public synchronized long allocateLoginStartTime()
  {
    long t=q[pos]+td;
    long now=System.currentTimeMillis();
    if(t<now)t=now;
    q[pos--]=t;
    if(pos<0)pos=q.length-1;
    return t;
  }
}