package org.openmim.irc.driver;

// Decompiled by Jad v1.5.6g. Copyright 1997-99 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: fieldsfirst splitstr
// Source File Name:   Identd.java

import com.egplab.exception_handling.ExceptionUtil;
import com.egplab.utils.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class Identd implements Runnable {
    private ServerSocket s;
    private Socket soc;
    private String User;
    private String OS;
    private Thread identdThread;

    public Identd(String userName) {
        OS = "UNIX";
        System.out.println("Identd starting");
        if (userName == null) {
            throw new RuntimeException("org.openmim.irc.Identd: userName is not specified.");
        } else {
            User = userName;
            identdThread = new Thread(this, "identd." + userName);
            identdThread.start();
            System.out.println("Identd started");
            return;
        }
    }

    public void close() {
        identdThread.interrupt();
        try {
            identdThread.join();
        } catch (InterruptedException e) {
            ExceptionUtil.handleException(e);
        }
    }

    public static void main(String[] args) {
        new Identd(args[0]);
    }

    public static void Out(String s1) {
        System.out.println("identd: " + s1);
    }

    public void run() {
        try {
            s = new ServerSocket(113);
            do {
                OutputStream outputstream;
                byte abyte0[];
                int i;
                do {
                    Out("accept");
                    soc = s.accept();
                    Out("new request");
                    InputStream inputstream = soc.getInputStream();
                    outputstream = soc.getOutputStream();
                    abyte0 = new byte[1000];
                    i = inputstream.read(abyte0);
                }
                while (i == -1);
                String s1 = new String(abyte0, 0, i);
                if (s1.indexOf("\r\n") != -1)
                    s1 = s1.substring(0, s1.indexOf("\r\n"));
                else if (s1.indexOf("\n") != -1)
                    s1 = s1.substring(0, s1.indexOf("\n"));
                Out(": " + s1);
                s1 = s1 + " : USERID : " + OS + " : " + User;
                byte abyte1[] = s1.getBytes();
                outputstream.write(abyte1, 0, s1.length());
                outputstream.flush();
                Out("replied with: " + s1);
                outputstream.close();
            }
            while (true);
        }
        catch (BindException exception) {
            System.err.println("identd server cannot be bound");
        }
        catch (Exception exception) {
            Logger.printException(exception);
        }
    }
}
