package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   Order.java

// Referenced classes of package squirrel_util.util:
//      Ordered

public class Order
{
  protected Ordered host;

public Order(Ordered ordered)
{
  host = ordered;
}
public final int add(Object obj)
{
  synchronized (host)
  {
	return addTo(0, host.getItemCount(), obj);
  }
}
protected final int addTo(int i, int j, Object obj) throws IndexOutOfBoundsException
{
  synchronized (host)
  {
	boolean flag = false;
	if (i > j)
	  throw new IndexOutOfBoundsException("start (" + i + ") must be <= end (" + j + ")");
	while (i < j)
	{
	  int k = i + j >> 1;
	  int l = host.compareTo(host.getItem(k), obj);
	  if (l == 0)
	  {
		j = k;
		break;
	  }
	  if (l > 0)
		j = k;
	  else
		i = k + 1;
	}
	host.insertAt(j, obj);
	return j;
  }
}
public String toString()
{
  synchronized (host)
  {
	StringBuffer stringbuffer;
	stringbuffer = new StringBuffer("(\n");
	int i = host.getItemCount();
	for (int j = 0; j < i;)
	{
	  stringbuffer.append(host.getItem(j).toString());
	  if (j < i)
		stringbuffer.append(",\n");
	}
	return stringbuffer.append(")").toString();
  }
}
}
