package org.openmim.infrastructure.scheduler;

import org.openmim.infrastructure.taskmanager.*;
import org.apache.log4j.Logger;

public final class Scheduler {
    private final static org.apache.log4j.Logger CAT = Logger.getLogger(Scheduler.class);

    private final Schedule sched = new Schedule();
    private ThreadPool threadPool;
    private final int optThreads;
    private final int maxThreads;

    public Scheduler(int maxThreads, int optThreads) {
        this.optThreads = optThreads;
        this.maxThreads = maxThreads;
    }

    public final void runAt(long time, Task e) {
        sched.add(time, e);
    }

    public final void runAt(Task e, long time) {
        sched.add(time, e);
    }

    public final void runNow(Task e) {
        sched.add(0L, e);
    }

    public final void cancel(Task e) {
        sched.cancel(e);
    }

    private void initSchedulerTask() {
        threadPool.execute(new Task() {
            public void execute() throws Exception {
                Task e;
                for (; ;) {
                    if (Thread.currentThread().isInterrupted())
                        return;
                    synchronized (sched) {
                        if (sched.isEmpty()) {
                            if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("sched queue empty, scheduler waiting");
                            sched.wait();
                            continue;
                        } else {
                            Long timeL = sched.getFirstEventTime();
                            long time = timeL;
                            long pause = time - System.currentTimeMillis();
                            if (Defs.DEBUG && CAT.isDebugEnabled()) {
                                if (pause <= 0) pause = 0;
                                CAT.debug("sched queue size=" + sched.size() + ", nearest task is " + (pause / 1000) + " seconds away");
                            }
                            if (pause > 0) {
                                sched.wait(pause);
                                continue;
                            } else {
                                e = (Task) sched.cancelFirst(timeL);
                            }
                        }
                    }
                    if (e == null) throw new NullPointerException();
                    if (Defs.DEBUG && CAT.isDebugEnabled()) CAT.debug("executing task: " + e);
                    threadPool.execute(e);
                    e = null;
                }
            }

            public String getId() {
                return "";
            }

            public int getState() {
                return INPROCESS;
            }

            public void terminate() {
            }

            public long getStartTime() {
                return 0;
            }

            public boolean terminatable() {
                return false;
            }
        });
    }

    public synchronized void init() {
        threadPool = new ThreadPool(maxThreads, optThreads);
        sched.clear();
        initSchedulerTask();
    }

    public synchronized void deinit() {
        threadPool.stop(5000);
        threadPool = null;
        sched.clear();
    }
}
