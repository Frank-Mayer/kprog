package second;

public class WaitNotifyExample {
    private boolean flag = false; // Eine gemeinsame Flagge

    public static void main(String[] args) {
        WaitNotifyExample example = new WaitNotifyExample();

        Thread t1 = new Thread(() -> {
            try {
                example.waitForFlagChange();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(2000); // Warten für 2 Sekunden
                example.setFlag(); // Flagge setzen und notify aufrufen
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start(); // Thread 1 starten
        t2.start(); // Thread 2 starten
    }

    public synchronized void waitForFlagChange() throws InterruptedException {
        while (!this.flag) { // Solange die Flagge nicht gesetzt ist
            System.out.println("Thread wartet auf Flaggenänderung.");
            this.wait(); // Thread wartet
        }
        System.out.println("Flagge wurde geändert.");
    }

    public void setFlag() {
        this.flag = true; // Flagge wird gesetzt
        System.out.println("Flagge wurde geändert. Notify wird aufgerufen.");
        this.notify(); // Ein Thread wird benachrichtigt
    }
}