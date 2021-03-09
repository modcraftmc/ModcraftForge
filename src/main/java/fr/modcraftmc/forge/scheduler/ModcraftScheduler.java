package fr.modcraftmc.forge.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class ModcraftScheduler {

    private final AtomicInteger ids = new AtomicInteger(1);
    private volatile CraftTask head = new CraftTask();
    private final AtomicReference<CraftTask> tail = new AtomicReference<CraftTask>(head);
    volatile int currentTick = -1;

    /**
     * Main thread logic only
     */
    private final List<CraftTask> temp = new ArrayList<CraftTask>();

    final ConcurrentHashMap<Integer, CraftTask> runners = new ConcurrentHashMap<Integer, CraftTask>(); // Paper


    /**
     * Main thread logic only
     */
    final PriorityQueue<CraftTask> pending = new PriorityQueue<CraftTask>(10, // Paper
            new Comparator<CraftTask>() {
                @Override
                public int compare(final CraftTask o1, final CraftTask o2) {
                    int value = Long.compare(o1.getNextRun(), o2.getNextRun());

                    // If the tasks should run on the same tick they should be run FIFO
                    return value != 0 ? value : Integer.compare(o1.getTaskId(), o2.getTaskId());
                }
            });

    public BukkitTask scheduleInternalTask(Runnable run, int delay, String taskName) {
        final CraftTask task = new CraftTask(run, nextId(), "Internal - " + (taskName != null ? taskName : "Unknown"));
        task.internal = true;
        return handle(task, delay);
    }

    protected CraftTask handle(final CraftTask task, final long delay) { // Paper
        task.setNextRun(currentTick + delay);
        addTask(task);
        return task;
    }

    protected void addTask(final CraftTask task) {
        final AtomicReference<CraftTask> tail = this.tail;
        CraftTask tailTask = tail.get();
        while (!tail.compareAndSet(tailTask, task)) {
            tailTask = tail.get();
        }
        tailTask.setNext(task);
    }


    void parsePending() { // Paper
        CraftTask head = this.head;
        CraftTask task = head.getNext();
        CraftTask lastTask = head;
        for (; task != null; task = (lastTask = task).getNext()) {
            if (task.getTaskId() == -1) {
                task.run();
            } else if (task.getPeriod() >= CraftTask.NO_REPEATING) {
                pending.add(task);
                runners.put(task.getTaskId(), task);
            }
        }
        // We split this because of the way things are ordered for all of the async calls in CraftScheduler
        // (it prevents race-conditions)
        for (task = head; task != lastTask; task = head) {
            head = task.getNext();
            task.setNext(null);
        }
        this.head = lastTask;
    }

    private boolean isReady(final int currentTick) {
        return !pending.isEmpty() && pending.peek().getNextRun() <= currentTick;
    }

    private volatile CraftTask lastSyncTask = null;
    private volatile CraftTask currentTask = null;

    /**
     * This method is designed to never block or wait for locks; an immediate execution of all current tasks.
     */
    public void mainThreadHeartbeat(final int currentTick) {
        this.currentTick = currentTick;
        final List<CraftTask> temp = this.temp;
        parsePending();
        while (isReady(currentTick)) {
            final CraftTask task = pending.remove();
            lastSyncTask = task; // Yatopia - detailed report
            if (task.getPeriod() < CraftTask.NO_REPEATING) {
                if (task.isSync()) {
                    runners.remove(task.getTaskId(), task);
                }
                parsePending();
                continue;
            }
            if (task.isSync()) {
                currentTask = task;
                try {
                    task.run();
                } catch (final Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    currentTask = null;
                }
                parsePending();
            } else {
                //debugTail = debugTail.setNext(new CraftAsyncDebugger(currentTick + RECENT_TICKS, task.getOwner(), task.getTaskClass())); // Paper
                //task.getOwner().getLogger().log(Level.SEVERE, "Unexpected Async Task in the Sync Scheduler. Report this to Paper"); // Paper
                // We don't need to parse pending
                // (async tasks must live with race-conditions if they attempt to cancel between these few lines of code)
            }
            lastSyncTask = null; // Yatopia - detailed report
            final long period = task.getPeriod(); // State consistency
            if (period > 0) {
                task.setNextRun(currentTick + period);
                temp.add(task);
            } else if (task.isSync()) {
                runners.remove(task.getTaskId());
            }
        }
        pending.addAll(temp);
        temp.clear();
        //debugHead = debugHead.getNextHead(currentTick); // Paper
    }

    private int nextId() {
        return ids.incrementAndGet();
    }
}
