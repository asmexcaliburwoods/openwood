package squirrel_util;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   Ordered.java

public interface Ordered
{

public abstract int compareTo(Object obj, Object obj1);
public abstract Object getItem(int i) throws IndexOutOfBoundsException;
public abstract int getItemCount();
public abstract void insertAt(int i, Object obj) throws IndexOutOfBoundsException;
}
