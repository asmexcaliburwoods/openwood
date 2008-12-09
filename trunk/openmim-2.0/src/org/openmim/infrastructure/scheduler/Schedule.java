package org.openmim.infrastructure.scheduler;

import org.apache.log4j.Logger;

import java.util.*;

public class Schedule {
    private final static Logger CAT = Logger.getLogger(Schedule.class);

    /**
     Sorted by time:
     ev1.time <= ev2.time  <=>  events.~indexOf(ev1) <= events.~indexOf(ev2).

     TODO: full optimize of events + event2time.
     */
    private final SortedList events = new SortedList();
    private final Map event2time = new HashMap();

    public Schedule() {
    }

    public synchronized void add(long time, Object o) {
        if (o == null) throw new NullPointerException();
        //if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("before sync block");
        //if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("in sync block");
        /*int p = list.indexOf(ev);
        if (p > -1)
        {
          if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId()+" is already list to "+new Date(ev.time)+", ignored");
          return;
        }
        */
        //Event e = new Event(time, o);
        Long timeL = new Long(time);

        if (Defs.DEBUG && CAT.isDebugEnabled()) {
            long delta = time - System.currentTimeMillis();
            if (delta < 0) delta = 0;
            CAT.debug("scheduling " + o + " to " +
                    (delta == 0 ?
                    "run now" :
                    (delta / 1000) + " seconds later (" + new Date(time) + ")")
            );
        }

        events.put(timeL, o);
        event2time.put(o, timeL);

        if (getFirstEventTime().longValue() == time) {
            notify(); //handle time of the first list event may have changed.  scheduler thread may wakeup.
        }
        //if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug(ev.getLoginId() + ": reconnect list");
    }

    public synchronized void cancel(Object o) {
        if (o == null) throw new NullPointerException();
        Object timeO = event2time.remove(o);
        if (timeO == null) return;
        Long timeL = (Long) timeO;
        events.remove(timeL, o);
        if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("canceled: " + o);
    }

    /*synchronized(this) externally*/
    Object cancelFirst(Long timeL) {
        Object o = events.removeFirst(timeL);
        event2time.remove(o);
        //if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("canceled(cancelFirst): " + o);
        return o;
    }

    /*synchronized(this) externally*/
    Long getFirstEventTime() {
        return (Long) events.firstKey();
    }

    /*synchronized(this) externally*/
    boolean isEmpty() {
        return events.isEmpty();
    }

    /*synchronized(this) externally*/
    int size() {
        return event2time.size();
    }

    public synchronized void clear() {
        events.clear();
        event2time.clear();
    }
}
