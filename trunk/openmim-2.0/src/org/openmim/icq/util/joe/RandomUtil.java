package org.openmim.icq.util.joe;

public class RandomUtil
{
  public static final java.util.Random random = new java.util.Random();
  
  public static int random(int min, int max)
  {
    if (min == max) return min;
    int r = min + random.nextInt(max - min + 1);
    if (r > max) r = max;
    else
      if (r < min) r = min;
    return r;
  }
  
  public static long random(long min, long max)
  {
    if (min == max) return min;
    long r = Math.round(min + random.nextDouble() * (max - min));
    if (r > max) r = max;
    else
      if (r < min) r = min;
    return r;
  }
}