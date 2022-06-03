package tokenring;

import java.util.TimerTask;

class TimeOutTask extends TimerTask {
    private final Thread thread;

    public TimeOutTask(Thread thread) {
        this.thread = thread;
    }

    @Override
    public void run() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
