package org.openmim.test;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestKeepAlive
{
  static boolean done = false;
  static Object lock = new Object();
  
  public static void main(String[] args)
  {
    try
    {
      Thread t = new Thread("ss")
      {
        public void run()
        {
          synchronized (lock)
          {
            try
            {
              ServerSocket ss = new ServerSocket(8889);
              log("accepting");
              Socket s2 = ss.accept();
              log("accepted");
              ss.close();
              //s2.close();
              log("closed");
            }
            catch (Throwable tr)
            {
              log(tr);
            }
            done = true;
            lock.notifyAll();
            log("notified, thread finished");
          }
        }
      };
      t.start();
      Socket s = new Socket("simphonick2", 8888);
      log("ka1: "+s.getKeepAlive());
      s.close();
      log("sleep 5");
      Thread.currentThread().sleep(5000);
      log("sleep 5 done");
      s = new Socket("localhost", 8889);
      synchronized (lock)
      {
        for (;;)
        {
          if (done) break;
          log("waiting");           
          lock.wait();
        }
      }
      log("done");
      log("ka2: "+s.getKeepAlive());
      log("in.available() returns: "+s.getInputStream().available());
      log("in.read(new byte[]{}) returns: "+s.getInputStream().read(new byte[]{0}));
      s.close();
      t = new Thread("ss2")
      {
        public void run()
        {
          synchronized (lock)
          {
            try
            {
              ServerSocket ss = new ServerSocket(8889);
              log("accepting2");
              Socket s2 = ss.accept();
              log("accepted2");
              s2.setKeepAlive(true);
              ss.close();
              s2.close();
              log("closed2");
            }
            catch (Throwable tr)
            {
              log(tr);
            }
            done = true;
            lock.notifyAll();
            log("notified, thread finished2");
          }
        }
      };
      synchronized (lock)
      {
        done = false;
      }
      t.start();
      log("sleep 5");
      Thread.currentThread().sleep(5000);
      log("sleep 5 done");
      s = new Socket("localhost", 8889);
      s.setKeepAlive(true);
      synchronized (lock)
      {
        for (;;)
        {
          if (done) break;
          log("waiting2");
          lock.wait();
        }
      }
      log("done");
      log("ka2: "+s.getKeepAlive());
      log("in.available() returns: "+s.getInputStream().available());
      log("in.read(new byte[]{}) returns: "+s.getInputStream().read(new byte[]{}));
      s.close();
    }
    catch (Throwable tr)
    {
      log(tr);
    }
  }
  
  static void log(Throwable tr)
  {
    tr.printStackTrace(System.out);
  }
  
  static void log(String s)
  {
    System.out.println(s);
  }
}