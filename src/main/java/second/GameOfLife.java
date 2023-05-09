package second;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class GameOfLife extends JPanel {

  // # component to render the world
  private static class WorldUI extends JPanel {

    private final int worldSize;
    // private final int worldWidth;
    private final int logWorldWidth;
    private final int worldWidthMinusOne;
    private final BufferedImage buffer;
    private int colorAlive = 0xFFFFFF;
    private int colorDead = 0x000000;
    private BitSet worldData;

    public WorldUI(final BitSet worldData, final int worldWidth, final int worldHeight) {
      this.worldData = (BitSet) worldData.clone();
      // this.worldWidth = worldWidth;
      this.logWorldWidth = (int) (Math.log(worldWidth) / Math.log(2));
      this.worldWidthMinusOne = worldWidth - 1;
      this.worldSize = worldWidth * worldHeight;
      this.buffer = new BufferedImage(worldWidth, worldHeight, BufferedImage.TYPE_INT_RGB);
      this.draw();
    }

    public Color getAliveColor() {
      return new Color(this.colorAlive);
    }

    public void setAliveColor(final Color color) {
      this.colorAlive = color.getRGB();
      this.draw();
    }

    public Color getDeadColor() {
      return new Color(this.colorDead);
    }

    public void setDeadColor(final Color color) {
      this.colorDead = color.getRGB();
      this.draw();
    }

    @Override
    public void paintComponent(final Graphics g) {
      g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
    }

    // ## default draw method
    @Override
    public void paint(final Graphics g) {
      int i;
      int x;
      int y;
      for (i = 0; i < this.worldSize; ++i) {
        x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
        y = i >> this.logWorldWidth; // i / this.worldWidth;
        this.buffer.setRGB(x, y, this.worldData.get(i) ? this.colorAlive : this.colorDead);
      }
      // ### draw the buffer
      g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
    }

    // ## free resources
    public void dispose() {
      this.buffer.flush();
      this.buffer.getGraphics().dispose();
      this.worldData = null;
    }

    // ## draw the given worlds state
    private void draw(final BitSet newData) {
      this.worldData = newData;
      this.draw();
    }

    // ## draw the current worlds state
    private void draw() {
      final var g = this.getGraphics();
      if (g == null) {
        return;
      }

      this.paint(g);
    }
  }

  // # component to render the TPS (ticks per second)
  private static class TPS extends JLabel {

    private final double[] timings = new double[32];
    private int index = 0;

    public TPS() {
      this.setText("Tick Time: Unknown");
    }

    // ## add a new timing in nanoseconds
    public void add(final double time) {
      this.timings[this.index] = time;
      if (this.index == 31) { // if (this.index == this.timings.length - 1) {
        this.index = 0;
        this.setText(String.format("Tick Time: %.4fms", this.get()));
      } else {
        ++this.index;
      }
    }

    // ## calculate the average time spend for a tick in milliseconds
    public double get() {
      double sum = 0d;
      for (final double timing : this.timings) {
        sum += timing;
      }
      return sum / 32000000d; // sum / (this.timings.length * 1000000d);
    }
  }

  private static class MyMenuBar extends JMenuBar {

    // ## create a menu bar for an internal frame
    private static JMenuBar makeInternalFrameMenuBar(final JInternalFrame inFrame) {
      final var menuBar = new JMenuBar();

      // ### add a menu to control the internal frame
      final var ctrlMenu = new JMenu("Control");
      menuBar.add(ctrlMenu);

      // #### add a menu item to close the internal frame
      final var closeMenuItem = new JMenuItem("Close");
      closeMenuItem.addActionListener(e -> inFrame.dispose());
      ctrlMenu.add(closeMenuItem);

      // #### add a menu item to pause / resume the game
      final var pauseMenuItem = new JMenuItem("Start");
      pauseMenuItem.addActionListener(
          e -> {
            final var gol = (GameOfLife) inFrame.getContentPane();
            pauseMenuItem.setText(gol.togglePaused() ? "Resume" : "Pause");
          });
      ctrlMenu.add(pauseMenuItem);

      // ### add a menu to control the world
      final var worldMenu = new JMenu("World");
      menuBar.add(worldMenu);
      final var clearMenuItem = new JMenuItem("Clear");
      clearMenuItem.addActionListener(
          e -> {
            final var gol = (GameOfLife) inFrame.getContentPane();
            gol.clear();
          });
      worldMenu.add(clearMenuItem);
      final var aliveColorMenuItem = new JMenuItem("Alive Color");
      aliveColorMenuItem.addActionListener(
          e -> {
            final var gol = (GameOfLife) inFrame.getContentPane();
            final var color =
                JColorChooser.showDialog(inFrame, "Choose Alive Color", gol.getAliveColor());
            if (color != null) {
              gol.setAliveColor(color);
            }
          });
      worldMenu.add(aliveColorMenuItem);
      final var deadColorMenuItem = new JMenuItem("Dead Color");
      deadColorMenuItem.addActionListener(
          e -> {
            final var gol = (GameOfLife) inFrame.getContentPane();
            final var color =
                JColorChooser.showDialog(inFrame, "Choose Dead Color", gol.getDeadColor());
            if (color != null) {
              gol.setDeadColor(color);
            }
          });
      worldMenu.add(deadColorMenuItem);

      return menuBar;
    }

    private final JDesktopPane deskPane;

    private final InternalFrameAdapter internalFrameClosed =
        new InternalFrameAdapter() {
          @Override
          public void internalFrameClosed(final InternalFrameEvent e) {
            final var inFrame = e.getInternalFrame();
            final var gol = (GameOfLife) inFrame.getContentPane();
            gol.dispose();
            MyMenuBar.this.deskPane.remove(inFrame);
          }
        };

    public MyMenuBar(final JDesktopPane deskPane) {
      final var newInstanceMenu = new JMenu("New Instance");
      this.add(newInstanceMenu);
      this.deskPane = deskPane;

      // ## calculate preferred sub-frame size
      final var maxWindowBoulds =
          GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
      final var maxWindowSide =
          (int) (Math.min(maxWindowBoulds.width, maxWindowBoulds.height) * 0.75);
      final var preferredFrameSize = new Dimension(maxWindowSide, maxWindowSide);

      // ## add menu items for different resolutions
      for (int i = 5; i < 12; ++i) {
        final var res = 1 << i;
        newInstanceMenu.add(
            this.makeInternalFrameCreatorMenuItem(deskPane, preferredFrameSize, res));
      }
    }

    // ## create a menu item to create a new internal frame
    private JMenuItem makeInternalFrameCreatorMenuItem(
        final JDesktopPane deskPane, final Dimension preferredFrameSize, final int res) {
      final var menuItem = new JMenuItem(String.format("%dx%d", res, res));
      menuItem.addActionListener(
          e -> {
            // ## create a new internal frame
            final var inFrame =
                new JInternalFrame(
                    String.format("Game of Life %dx%d", res, res), true, true, true, true);
            inFrame.setPreferredSize(preferredFrameSize);

            inFrame.setJMenuBar(MyMenuBar.makeInternalFrameMenuBar(inFrame));
            final var gol = new GameOfLife(res, res);
            inFrame.setContentPane(gol);
            deskPane.add(inFrame);
            inFrame.pack();
            inFrame.show();
            inFrame.addInternalFrameListener(this.internalFrameClosed);
          });
      return menuItem;
    }
  }

  public static void main(final String[] args) {
    // # ensure that the style is the same on all platforms
    try {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Game of Life");
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | UnsupportedLookAndFeelException ignore) {
      // ## dann halt nicht 😒
    }

    // # create the window
    final var window = new JFrame();
    window.setTitle("Game of Life");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // # Create a JDesktopPane for use with JInternalFrames.
    final var deskPane = new JDesktopPane();
    window.add(deskPane);

    // # add a menu bar
    final var menuBar = new MyMenuBar(deskPane);
    window.setJMenuBar(menuBar);

    window.setVisible(true);
    window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }

  private final int worldWidth;
  private final int worldSize;
  private final int worldSizeMinusWorldWidth;
  private final int worldSizePlusWorldWidth;
  private final int worldSizeMinusOne;
  private final WorldUI worldUI;
  private final TPS tpsLabel;
  private final ScheduledExecutorService sheduler;

  // # world data
  // ## primary world data
  private BitSet worldDataA;

  // ## secondary world data; temporary storage for the next generation
  private BitSet worldDataB;

  private boolean paused = true;

  private long minTickTime = 0;

  public GameOfLife(final int width, final int height) {

    // # initialize ui
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // ## tick time label
    this.tpsLabel = new TPS();
    this.add(this.tpsLabel);
    // ## min tick time slider (delay)
    final var minTickTimeLabel = new JLabel("Min Tick Time (0ms)");
    this.add(minTickTimeLabel);
    final var minTickTimeSlider = new JSlider(0, 500, 0);
    minTickTimeSlider.addChangeListener(
        e -> {
          this.minTickTime = minTickTimeSlider.getValue();
          minTickTimeLabel.setText(String.format("Min Tick Time (%d ms)", this.minTickTime));
        });
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.setPaintTicks(true);
    this.add(minTickTimeSlider);

    // # initialize the "world"
    this.worldWidth = width;
    this.worldSize = width * height;
    this.worldSizeMinusWorldWidth = this.worldSize - this.worldWidth;
    this.worldSizePlusWorldWidth = this.worldSize + this.worldWidth;
    this.worldSizeMinusOne = this.worldSize - 1;
    this.worldDataA = new BitSet(this.worldSize);
    this.worldDataB = new BitSet(this.worldSize);
    final var rand = new Random();
    for (int i = 0; i < this.worldSize; ++i) {
      // ## randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      this.worldDataA.set(i, alive);
      this.worldDataB.set(i, alive);
    }
    // ## world ui to display the world
    this.worldUI = new WorldUI(this.worldDataA, width, height);
    this.add(this.worldUI);
    // ### add mouse listener to toggle cells
    // this.worldUI.addMouseListener(new MouseAdapter() {
    // @Override
    // public void mousePressed(final MouseEvent e) {
    // final double clickX = e.getX();
    // final double clickY = e.getY();
    // final double renderWidth = GameOfLife.this.worldUI.getWidth();
    // final double renderHeight = GameOfLife.this.worldUI.getHeight();
    // final int x = (int) (clickX / (renderWidth / width));
    // final int y = (int) (clickY / (renderHeight / height));
    // GameOfLife.this.worldUI.draw();
    // }
    // });

    // # start the game loop
    this.sheduler = Executors.newSingleThreadScheduledExecutor();
    this.sheduler.scheduleWithFixedDelay(this::tick, 500, 1, TimeUnit.MILLISECONDS);
  }

  // # free resources
  public void dispose() {
    this.sheduler.shutdownNow();
    this.worldDataA = null;
    this.worldDataB = null;
    this.worldUI.dispose();
  }

  public Color getAliveColor() {
    if (this.worldUI == null) {
      return null;
    }
    return this.worldUI.getAliveColor();
  }

  public void setAliveColor(final Color color) {
    if (this.worldUI == null) {
      return;
    }
    this.worldUI.setAliveColor(color);
  }

  public Color getDeadColor() {
    if (this.worldUI == null) {
      return null;
    }
    return this.worldUI.getDeadColor();
  }

  public void setDeadColor(final Color color) {
    if (this.worldUI == null) {
      return;
    }
    this.worldUI.setDeadColor(color);
  }

  // # calculate next generation
  private void tick() {
    // ## check if the game is running
    if (this.paused) {
      return;
    }

    // ## save start time
    final var start = System.nanoTime();

    // ## initialize local variables
    int i;
    int livingNeighbors;
    int neighborIndex;
    boolean alive;

    // ## check all cells in the world
    for (i = 0; i < GameOfLife.this.worldSize; ++i) {
      // ### is this cell currently alive?
      alive = GameOfLife.this.worldDataA.get(i);

      // ### check all neighbors
      livingNeighbors = 0;
      // #### 1: north
      neighborIndex =
          i + GameOfLife.this.worldSizeMinusWorldWidth & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 2: north-east
      neighborIndex =
          i + GameOfLife.this.worldSizeMinusWorldWidth + 1 & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 3: east
      neighborIndex = i + 1 & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 4: south-east
      neighborIndex = i + GameOfLife.this.worldWidth + 1 & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 5: south
      neighborIndex = i + GameOfLife.this.worldWidth & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 6: south-west
      neighborIndex =
          i + GameOfLife.this.worldWidth - 1 + GameOfLife.this.worldSize
              & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 7: west
      neighborIndex = i - 1 + GameOfLife.this.worldSize & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 8: north-west
      neighborIndex =
          i - GameOfLife.this.worldSizePlusWorldWidth - 1 & GameOfLife.this.worldSizeMinusOne;
      if (GameOfLife.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }

      // #### alive1 = alive0 ? (2 or 3 neighbors) : (3 neighbors)
      GameOfLife.this.worldDataB.set(i, livingNeighbors == 3 || alive && livingNeighbors == 2);
    }

    // ## calculate time spend for this tick
    final var tickTime = System.nanoTime() - start;
    this.tpsLabel.add(tickTime);

    // ## sleep
    final var sleepTime = this.minTickTime - tickTime / 1000000L;
    if (sleepTime > 0L) {
      try {
        Thread.sleep(sleepTime);
      } catch (final Exception e) {
        // ### ignore
      }
    }

    // ## swap the world data a and b; a stays primary
    final var tmp = GameOfLife.this.worldDataA;
    GameOfLife.this.worldDataA = GameOfLife.this.worldDataB;
    GameOfLife.this.worldDataB = tmp;

    // ## pass the new generation to the UI
    this.worldUI.draw(GameOfLife.this.worldDataA);
  }

  private boolean togglePaused() {
    return this.paused = !this.paused;
  }

  // # clear the world
  private void clear() {
    this.worldDataA.clear();
    this.worldDataB.clear();
    this.worldUI.draw(this.worldDataA);
  }
}