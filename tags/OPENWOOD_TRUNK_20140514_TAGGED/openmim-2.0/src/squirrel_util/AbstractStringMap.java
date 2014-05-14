package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   AbstractStringMap.java

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

// Referenced classes of package squirrel_util.util:
//      StringMap

public abstract class AbstractStringMap implements StringMap
{
  static Class class$java$lang$String;

public AbstractStringMap()
{
}
public abstract String getValue(String s);
public Constructor getValueConstructor(Class class1) throws NoSuchMethodException, SecurityException
{
  return class1.getConstructor(new Class[] {String.class});
}
public Object newValueInstance(Constructor constructor, String s) throws InvocationTargetException, InstantiationException, IllegalAccessException, IllegalArgumentException
{
  return constructor.newInstance(new Object[] {s});
}
}
