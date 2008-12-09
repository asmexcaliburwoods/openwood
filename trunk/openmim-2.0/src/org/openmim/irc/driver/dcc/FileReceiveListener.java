package org.openmim.irc.driver.dcc;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   FileReceiveListener.java
public interface FileReceiveListener
{

void onBytesReceived(byte[] abyte0, long l, long l1);
void onFileReceiveAborted(String s);
void onFileReceiveDone();
void onFileReceiveStart();
}
