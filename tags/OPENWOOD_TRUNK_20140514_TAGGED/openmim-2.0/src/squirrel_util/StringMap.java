package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   StringMap.java

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
public interface StringMap
{

String getValue(String s);
Constructor getValueConstructor(Class class1) throws NoSuchMethodException, SecurityException;
Object newValueInstance(Constructor constructor, String s) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}
