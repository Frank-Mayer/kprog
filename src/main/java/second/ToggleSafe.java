package second;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schreiben Sie das GUI-Programm mit den zehn Knöpfen für einen Safe aus der letzten Woche neu in Swing: Nun soll eine Kombination von 10 Knopfdrücken (8-2-2-4-7-2-5-3-0-1) das Schloss öffnen, also das Programm beenden. Die Knöpfe sollen aber diesmal wie in der Abbildung ringförmig angeordnet sein.
 * Zusätzlich soll sich die Beschriftung (und damit die Wirkung) der Knöpfe im Sekundentakt um eine Position drehen, zunächst nach rechts. Bei jedem falschen Knopfdruck ändert sich allerdings die Drehrichtung.
 * Geben Sie das Programm unter dem Namen ToggleSafe ab.
 * <p>
 * Ändern Sie das DrehSafe-Programm um in ToggleSafe, so dass mehrere aktive
 * Fenster (mit sich drehenden Knöpfen) gleichzeitig offen sein können. Die Kombination
 * 8-5-2-9-6-3-0-7-4-1 öffnet das Schloss. Die Drehrichtung ändert sich nach jeweils 9
 * Schritten. Bei jedem falschen Knopfdruck öffnet sich ein weiteres Fenster derselben
 * Art, in dem sich die Knöpfe um 33% schneller drehen als im vorherigen Fenster. Und
 * das Schoss geht erst dann auf, wenn alle Fenster geschlossen wurden.
 *
 * @author Frank Mayer 215965, Antonia Friese 215937, René Ott 215471
 * @version 1.1
 */
public class ToggleSafe extends JFrame implements ActionListener {
    private static final List<ToggleSafe> openWindows = new ArrayList<>();
    private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final MyButton[] buttons = new MyButton[ 10 ];
    private final double delay;
    private final List<JPanel> spacers = new ArrayList<>();
    private final int[] combination = {
        8,
        5,
        2,
        9,
        6,
        3,
        0,
        7,
        4,
        1
    };
    private int buttonStartIndex = 0;
    private boolean clockwise = true;
    private int stepCount = 0;
    private int currentCombinationIndex = 0;

    public ToggleSafe() {
        this(1.0);
    }
    protected ToggleSafe(double delay) {
        // ensure that the style is the same on all platforms
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
               UnsupportedLookAndFeelException ignore) {
            // dann halt nicht 😒
        }

        this.delay = delay;

        // window setup
        this.setTitle("ToggleSafe");
        this.setLayout(new GridLayout(4, 3));
        this.addWindowListener(new ToggleSafe.Close());

        // create all buttons
        for (int i = 0; i < 10; i++) {
            var btn = new MyButton(i);
            btn.addActionListener(this);
            btn.setBackground(Color.GREEN);
            btn.setForeground(Color.BLACK);
            btn.setFont(new Font("TimesRoman", Font.PLAIN, 20));
            this.buttons[ i ] = btn;
        }

        // add the buttons at the correct positions.
        this.add(this.buttons[ 3 ]);
        this.add(this.buttons[ 2 ]);
        this.add(this.buttons[ 1 ]);
        this.add(this.buttons[ 4 ]);
        var spacer = new JPanel();
        this.spacers.add(spacer);
        this.add(spacer);
        this.add(this.buttons[ 0 ]);
        this.add(this.buttons[ 5 ]);
        spacer = new JPanel();
        this.spacers.add(spacer);
        this.add(spacer);
        this.add(this.buttons[ 9 ]);
        this.add(this.buttons[ 6 ]);
        this.add(this.buttons[ 7 ]);
        this.add(this.buttons[ 8 ]);

        // Update the buttons every second.
        ToggleSafe.executor.scheduleWithFixedDelay(() -> {
            // Increase the buttonStartIndex by 1 in the correct direction.
            this.buttonStartIndex = ( this.buttonStartIndex + ( this.clockwise ? 9 : 1 ) ) % 10;

            // switch rotation every 9 steps
            if (++this.stepCount >= 9) {
                this.stepCount = 0;
                this.clockwise = !this.clockwise;
            }

            // Update the button components.
            this.updateButtons();
        }, 0, 1, TimeUnit.SECONDS);

        this.setSize(512, 512);
//        this.setModal(true);
        this.pack();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        ToggleSafe.openWindows.add(new ToggleSafe());
    }

    /**
     * Update the buttons values based on the current buttonStartIndex.
     */
    private void updateButtons() {
        var i = this.buttonStartIndex;
        var labelIndex = 0;
        while (labelIndex != 10) {
            var btn = this.buttons[ i ];
            btn.setValue(labelIndex);

            i = ( i + 1 ) % 10;
            ++labelIndex;
        }
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MyButton btn) {
            var btnVal = btn.getValue();
            var combVal = this.combination[ this.currentCombinationIndex ];
            if (btnVal == combVal) {
                this.setBackground(Color.GREEN);
                ++this.currentCombinationIndex;
                if (this.currentCombinationIndex == this.combination.length) {
                    this.dispose();
                    ToggleSafe.openWindows.remove(this);
                    if (ToggleSafe.openWindows.isEmpty()) {
                        System.exit(0);
                    }
                }
                System.out.println("Correct button pressed: " + btnVal);
            }
            else {
                this.setBackground(Color.RED);
                this.clockwise = !this.clockwise;
                this.stepCount = 0;
                this.currentCombinationIndex = 0;
                System.err.println("Wrong button pressed: " + btnVal);

                ToggleSafe.openWindows.add(new ToggleSafe(this.delay * 0.67));
            }
        }
    }

    /**
     * Sets the background color of the window.
     *
     * @param bg the color to become this window's background color.
     */
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (this.spacers != null) {
            for (var spacer : this.spacers) {
                spacer.setBackground(bg);
            }
        }
    }

    private static class MyButton extends JButton {
        private int value;

        public MyButton(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
            this.setText(String.valueOf(value));
        }
    }

    // exit program when all windows are closed
    private static class Close extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            System.out.println("Closing window");
            ToggleSafe.openWindows.remove(e.getWindow());
            if (ToggleSafe.openWindows.isEmpty()) {
                System.exit(0);
            }
        }
    }
}