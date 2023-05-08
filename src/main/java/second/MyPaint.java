package second;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.List;

/**
 * Benutzen Sie das Eventaufzeichnungsprogramm um Ihre Kenntnisse über Events und
 * Listener zu verbessern. Als Ergebnis sollte ein Programm MyPaint entstehen und
 * abgegeben werden, das es erlaubt, einfache Malereien auf einen 2-dimensionalen Gitter
 * zu erzeugen (Phantasie erlaubt / gefragt).
 * <p>
 * <h2>Features: </h2>
 * <ul>
 * <li>Verschiedene Farben</li>
 * <li>Verschiedene Strichstärken</li>
 * <li>Drag & Drop von Bildern</li>
 * </ul>
 *
 * @author Frank Mayer 215965, Antonia Friese 215937, René Ott 215471
 * @version 1.1
 */
public class MyPaint extends JFrame {
    // get available screen space
    private final static Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getMaximumWindowBounds()
        .getSize();
    private final static Point topLeft = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getMaximumWindowBounds()
        .getLocation();
    // gap between windows
    private static final int gap = 8;
    // window instances
    public static MyPaint app;
    public static MyPaint.Tools tools;
    // Store previous and current mouse coordinates
    private int prevX = -1;
    private int prevY = -1;
    private int currX = -1;
    private int currY = -1;
    // Store color and line thickness
    private Color color = Color.BLACK;
    private int lineThickness = 5;
    private StrokeType strokeType = StrokeType.LINE;

    public MyPaint() {
        // Set up the JFrame
        this.setTitle("MyPaint");
        this.setBackground(Color.WHITE);
        this.setLocation(MyPaint.topLeft.x + MyPaint.Tools.width + ( MyPaint.gap << 1 ),
            MyPaint.topLeft.y + MyPaint.gap
        );
        this.setSize(MyPaint.screenSize.width - MyPaint.Tools.width - MyPaint.gap * 3,
            MyPaint.screenSize.height - ( MyPaint.gap << 1 )
        );
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add mouse listeners
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Set previous coordinates when mouse is pressed
                MyPaint.this.prevX = e.getX();
                MyPaint.this.prevY = e.getY();
            }

            public void mouseReleased(MouseEvent e) {
                // Reset previous coordinates when mouse is released
                MyPaint.this.prevX = -1;
                MyPaint.this.prevY = -1;
            }
        });

        // track mouse movement
        this.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                MyPaint.this.currX = e.getX();
                MyPaint.this.currY = e.getY();

                var delta = Math.sqrt(Math.pow(MyPaint.this.prevX - e.getX(),
                    2
                ) + Math.pow(MyPaint.this.prevY - e.getY(), 2));
                if (delta < 8) {
                    return;
                }

                // Get Graphics object and perform drawing
                var g = MyPaint.this.getGraphics();
                if (g instanceof Graphics2D g2d) {
                    g2d.setStroke(MyPaint.this.getStroke());
                }
                g.setColor(MyPaint.this.color);
                g.drawLine(MyPaint.this.prevX, MyPaint.this.prevY, MyPaint.this.currX, MyPaint.this.currY);

                MyPaint.this.prevX = MyPaint.this.currX;
                MyPaint.this.prevY = MyPaint.this.currY;
            }
        });

        this.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                try {
                    // accept the drop
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);

                    // get the dropped object
                    var dropped = dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                    // if it's a file
                    if (dropped instanceof List list && list.get(0) instanceof File file) {
                        // read the image
                        Image image = ImageIO.read(file);

                        // scale the image to fit the window
                        image = image.getScaledInstance(MyPaint.this.getWidth(),
                            MyPaint.this.getHeight(),
                            Image.SCALE_SMOOTH
                        );

                        // draw the image
                        var g = MyPaint.this.getGraphics();
                        g.drawImage(image, 0, 0, null);
                    }
                }
                catch (Exception ignore) {
                    // ignore
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // ensure that the style is the same on all platforms
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                   UnsupportedLookAndFeelException ignore) {
                // dann halt nicht 😒
            }

            MyPaint.app = new MyPaint();
            MyPaint.tools = new Tools();

            MyPaint.tools.setVisible(true);
            MyPaint.app.setVisible(true);

            SwingUtilities.invokeLater(() -> {
                MyPaint.app.drawGrid();
            });
        });
    }

    // get color of the line
    public Color getColor() {
        return this.color;
    }

    // set color of the line
    public void setColor(Color color) {
        this.color = color;
    }

    // get thickness of the line
    public int getLineThickness() {
        return this.lineThickness;
    }

    // set thickness of the line
    public void setLineThickness(int thickness) {
        this.lineThickness = thickness;
    }

    public StrokeType getStrokeType() {
        return this.strokeType;
    }

    public void setStrokeType(StrokeType stroke) {
        this.strokeType = stroke;
    }

    private Stroke getStroke() {
        return switch (this.strokeType) {
            case DOTTED_LINE -> new BasicStroke(this.lineThickness,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[] { 16 },
                8
            );
            default -> new BasicStroke(this.lineThickness);
        };
    }

    public void clear() {
        this.repaint();

        var g = this.getGraphics();
        if (g == null) {
            return;
        }

        if (g instanceof Graphics2D g2d) {
            SwingUtilities.invokeLater(() -> {
                this.drawGrid(g2d);
            });
        }
    }

    // draw a grid
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));

        int width = this.getWidth();
        int height = this.getHeight();

        for (var i = 0; i < width; i += 32) {
            g2d.drawLine(i, 0, i, height);
        }

        for (var i = 0; i < height; i += 32) {
            g2d.drawLine(0, i, width, i);
        }
    }

    private void drawGrid() {
        var g = this.getGraphics();
        if (g instanceof Graphics2D g2d) {
            this.drawGrid(g2d);
        }
    }

    enum StrokeType {
        LINE("Line"), DOTTED_LINE("Dotted Line");

        private final String name;

        StrokeType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * The tools window on the left side of the screen
     */
    private static class Tools extends JFrame {
        public static final int width = 100;

        public Tools() {
            // setup window
            this.setTitle("Tools");
            this.setBackground(Color.WHITE);
            this.setLocation(MyPaint.topLeft.x + MyPaint.gap, MyPaint.topLeft.y + MyPaint.gap);
            this.setSize(MyPaint.Tools.width, MyPaint.screenSize.height - ( MyPaint.gap << 1 ));
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // setup layout
            this.setLayout(new GridLayout(0, 1, MyPaint.gap, MyPaint.gap));

            // add button color
            {
                JButton btn = new JButton("Color \uD83D\uDD8C"); // 🖌️
                btn.setBackground(MyPaint.app.getColor());
                btn.setForeground(MyPaint.Tools.getComplementaryColor(MyPaint.app.getColor()));
                btn.addActionListener(e -> {
                    Color color = JColorChooser.showDialog(MyPaint.Tools.this,
                        "Choose a color",
                        MyPaint.app.getColor()
                    );

                    if (color != null) {
                        MyPaint.app.setColor(color);
                        btn.setBackground(color);
                        btn.setForeground(MyPaint.Tools.getComplementaryColor(color));
                    }
                });
                this.add(btn);
            }

            // add combo box for brush type
            JComboBox<StrokeType> strokeTypeComboBox = new JComboBox<>(StrokeType.values());
            strokeTypeComboBox.setSelectedItem(MyPaint.app.getStrokeType());
            strokeTypeComboBox.addActionListener(e -> {
                MyPaint.app.setStrokeType((StrokeType) strokeTypeComboBox.getSelectedItem());
            });
            this.add(strokeTypeComboBox);

            // add clear button
            JButton btn = new JButton("Clear \uD83D\uDDD1"); // 🗑
            btn.setBackground(Color.WHITE);
            btn.addActionListener(e -> MyPaint.app.clear());
            this.add(btn);

            // add stroke size slider
            JSlider slider = new JSlider(2, 10, MyPaint.app.getLineThickness());
            slider.setMajorTickSpacing(2);
            slider.setMinorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.addChangeListener(e -> MyPaint.app.setLineThickness(slider.getValue()));
            this.add(slider);
        }

        private static Color getComplementaryColor(Color color) {
            return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
        }
    }
}
