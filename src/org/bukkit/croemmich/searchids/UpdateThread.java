package org.bukkit.croemmich.searchids;
public class UpdateThread implements Runnable {
    private boolean running = false;
    private Thread thread;
    
    private SearchIds ids;

    public UpdateThread(SearchIds ids) {
        this.ids = ids;
    }

    public void run() {
        while (this.running) {
            try {
                Thread.sleep(SearchIds.autoUpdateInterval*1000);
            } catch (InterruptedException localInterruptedException) {
            }
            ids.updateData();
        }
    }

    public void start() {
        this.running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        this.running = false;
        thread.interrupt();
    }
}
