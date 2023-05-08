package second;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GameOfLife extends JPanel {

  private final int worldWidth;
  private final int worldSize;
  private final int worldSizeMinusWorldWidth;
  private final int worldSizePlusWorldWidth;
  private final int worldSizeMinusOne;
  private final WorldUI worldUI;
  private final TPS tpsLabel;
  private final JLabel threadLabel;
  private BitSet worldDataA;
  private BitSet worldDataB;
  private boolean threadNameUnknown = true;
  private boolean paused = false;

  public GameOfLife(final int width, final int height) {

    // # initialize ui
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // ## tick time label
    this.tpsLabel = new TPS();
    this.add(this.tpsLabel);
    // ## thread name label
    this.threadLabel = new JLabel();
    this.add(this.threadLabel);
    // ## run button
    final var startBtn = new JButton("Pause");
    startBtn.addActionListener(e -> {
      startBtn.setText((this.paused = !this.paused) ? "Run" : "Pause");
    });
    this.add(startBtn);

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
    this.worldUI.setPreferredSize(new Dimension(width, height));
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
    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(this::tick, 100, 1, TimeUnit.MILLISECONDS);
  }

  public static void main(final String[] args) {
    // # ensure that the style is the same on all platforms
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
             UnsupportedLookAndFeelException ignore) {
      // ## dann halt nicht 😒
    }

    // # create the window
    final var window = new JFrame();
    window.setTitle("Game of Life");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // # Create a JDesktopPane for use with JInternalFrames.
    final var deskPane = new JDesktopPane();
    window.add(deskPane);

    // JInternalFrame wird erstellt
    final var inFrame = new JInternalFrame(null, true, true, true, true);
    inFrame.add(new GameOfLife(512, 512));
    deskPane.add(inFrame);
    inFrame.pack();
    inFrame.show();

    window.setVisible(true);
    window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }

  // # calculate next generation
  private void tick() {
    // ## set thread name
    if (this.threadNameUnknown) {
      this.threadNameUnknown = false;
      this.threadLabel.setText(Thread.currentThread().getName());
    }

    // ## check if the game is running
    if (this.paused) {
      return;
    }

    // ## initialize local variables
    int i;
    int livingNeighbors;
    int neighborIndex;
    boolean alive;

    // ## save start time
    final var start = System.nanoTime();

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
      neighborIndex = i + GameOfLife.this.worldWidth - 1 + GameOfLife.this.worldSize
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

    final var tmp = GameOfLife.this.worldDataA;
    GameOfLife.this.worldDataA = GameOfLife.this.worldDataB;
    GameOfLife.this.worldDataB = tmp;

    // ## calculate time spend for this tick
    this.tpsLabel.add(System.nanoTime() - start);

    // ## pass the new generation to the UI
    this.worldUI.draw(GameOfLife.this.worldDataA);
  }

  // # component to render the world
  private static class WorldUI extends JPanel {

    private static final int colorAlive = 0xFFFFFF;
    private static final int colorDead = 0x000000;
    private final int worldSize;
    private final int worldWidth;
    private final int logWorldWidth;
    private final int worldWidthMinusOne;
    private final BufferedImage buffer;
    private BitSet worldData;

    public WorldUI(final BitSet worldData, final int worldWidth, final int worldHeight) {
      this.worldData = (BitSet) worldData.clone();
      this.worldWidth = worldWidth;
      this.logWorldWidth = (int) (Math.log(worldWidth) / Math.log(2));
      this.worldWidthMinusOne = worldWidth - 1;
      this.worldSize = worldWidth * worldHeight;
      this.buffer = new BufferedImage(worldWidth, worldHeight, BufferedImage.TYPE_INT_RGB);
      this.draw();
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

    // ## default draw method
    @Override
    public void paint(final Graphics g) {
      int i;
      int x;
      int y;
      for (i = 0; i < this.worldSize; ++i) {
        x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
        y = i >> this.logWorldWidth;// i / this.worldWidth;
        this.buffer.setRGB(x, y, this.worldData.get(i) ? WorldUI.colorAlive : WorldUI.colorDead);
      }
      // ### draw the buffer
      g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
    }
  }

  // # component to render the TPS (ticks per second)
  private static class TPS extends JLabel {

    private final double[] timings = new double[10];
    private int index = 0;

    public TPS() {
      this.setText("TPS: NaN");
    }

    // ## add a new timing in nanoseconds
    public void add(final double time) {
      this.timings[this.index] = time;
      if (this.index == 9) {
        this.index = 0;
        final var tickTimeMS = this.get();
        this.setText(String.format("TPS: %.2f", 1000d / tickTimeMS));
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
      return sum / 10000000d;
    }
  }
}