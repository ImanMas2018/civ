package civ.view;

import civ.controller.GameController;
import civ.model.Game;
import civ.model.Hex;
import civ.model.Terrain;
import civ.model.Unit;
import civ.util.HexGeometry;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Draws the hex map with fog of war, units, zoom and pan.
 * The terrain image is cached; units and move highlights are drawn every frame.
 */
public class MapPanel extends JPanel {

    /** Distance from hex centre to a vertex, in world pixels (before zoom). */
    private static final double HEX_SIZE = 40;

    /** Discrete zoom steps — lowest still keeps hexes clearly 6-sided. */
    private static final double[] ZOOM_LEVELS = {0.7, 0.85, 1.0, 1.25, 1.55};

    private static final Color FOG_FILL = new Color(52, 60, 78);
    private static final Color FOG_EDGE = new Color(90, 100, 122);
    private static final Color BG = new Color(18, 20, 28);

    private static final Color PLAINS = new Color(196, 186, 130);
    private static final Color GRASSLAND = new Color(126, 176, 76);
    private static final Color FOREST = new Color(34, 102, 51);
    private static final Color MOUNTAIN = new Color(120, 118, 112);
    private static final Color PLAINS_RES = PLAINS.darker();
    private static final Color GRASSLAND_RES = GRASSLAND.darker();
    private static final Color FOREST_RES = FOREST.darker();
    private static final Color MOUNTAIN_RES = MOUNTAIN.darker();

    private static final Color TILE_EDGE = new Color(0, 0, 0, 100);
    private static final Color OWNED_EDGE = new Color(255, 220, 90);
    private static final Color MARKER_FILL = new Color(240, 240, 250);
    private static final Color EXHAUSTED_TEXT = new Color(210, 90, 90);
    private static final Color MOVE_FILL = new Color(255, 255, 255, 70);
    private static final Color MOVE_EDGE = new Color(220, 240, 255);
    private static final Color UNIT_FILL = new Color(70, 120, 220);
    private static final Color UNIT_SELECTED = Color.WHITE;

    private static final BasicStroke THIN = new BasicStroke(1.0f);
    private static final BasicStroke THICK = new BasicStroke(2.0f);

    private final Path2D.Double hexShape = new Path2D.Double();

    private final Game game;
    private GameController controller;

    private BufferedImage mapCache;
    private int cacheZoomIndex = -1;

    private int zoomIndex = 2;
    private double cameraX = 0;
    private double cameraY = 0;
    private boolean cameraReady = false;

    private int dragStartX;
    private int dragStartY;
    private double dragStartCameraX;
    private double dragStartCameraY;
    private boolean dragged;

    private Unit movingUnit;
    private int fromCol;
    private int fromRow;
    private double progress;

    public MapPanel(Game game) {
        this.game = game;
        setBackground(BG);
        setFocusable(true);
        installMouse();
        installKeys();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!cameraReady && getWidth() > 0 && getHeight() > 0) {
                    centerCameraOnTownHall();
                    cameraReady = true;
                    repaint();
                }
            }
        });
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public boolean isAnimating() {
        return movingUnit != null;
    }

    private void centerCameraOnTownHall() {
        double worldX = HexGeometry.centerX(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        double worldY = HexGeometry.centerY(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        cameraX = worldX - (getWidth() / 2.0) / zoom();
        cameraY = worldY - (getHeight() / 2.0) / zoom();
    }

    private double zoom() {
        return ZOOM_LEVELS[zoomIndex];
    }

    private double screenHexSize() {
        return HEX_SIZE * zoom();
    }

    private void installMouse() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragStartCameraX = cameraX;
                dragStartCameraY = cameraY;
                dragged = false;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragged || controller == null) {
                    return;
                }
                double worldX = e.getX() / zoom() + cameraX;
                double worldY = e.getY() / zoom() + cameraY;
                int[] pos = HexGeometry.pixelToHex(
                        worldX, worldY, HEX_SIZE,
                        game.getMap().getCols(),
                        game.getMap().getRows());
                Hex hex = game.getMap().get(pos[0], pos[1]);
                if (hex != null) {
                    controller.onHexClicked(hex);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - dragStartX;
                int dy = e.getY() - dragStartY;
                if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                    dragged = true;
                }
                cameraX = dragStartCameraX - dx / zoom();
                cameraY = dragStartCameraY - dy / zoom();
                repaint();
            }
        });

        addMouseWheelListener((MouseWheelEvent e) -> {
            double mouseWorldX = e.getX() / zoom() + cameraX;
            double mouseWorldY = e.getY() / zoom() + cameraY;

            int newIndex = zoomIndex - e.getWheelRotation();
            zoomIndex = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, newIndex));

            cameraX = mouseWorldX - e.getX() / zoom();
            cameraY = mouseWorldY - e.getY() / zoom();
            repaint();
        });
    }

    private void installKeys() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double before = zoom();
                if (e.getKeyChar() == '+' || e.getKeyChar() == '=') {
                    zoomIndex = Math.min(ZOOM_LEVELS.length - 1, zoomIndex + 1);
                } else if (e.getKeyChar() == '-' || e.getKeyChar() == '_') {
                    zoomIndex = Math.max(0, zoomIndex - 1);
                } else {
                    return;
                }
                double cx = getWidth() / 2.0;
                double cy = getHeight() / 2.0;
                double worldX = cx / before + cameraX;
                double worldY = cy / before + cameraY;
                cameraX = worldX - cx / zoom();
                cameraY = worldY - cy / zoom();
                repaint();
            }
        });
    }

    /**
     * Slides the drawing of the unit from its old hex to its new one.
     * The model has already moved; this only changes where the circle is painted.
     */
    public void animateMove(Unit unit, int oldCol, int oldRow) {
        movingUnit = unit;
        fromCol = oldCol;
        fromRow = oldRow;
        progress = 0;

        Timer timer = new Timer(16, null);
        timer.addActionListener(e -> {
            progress += 0.08;
            if (progress >= 1.0) {
                progress = 1.0;
                movingUnit = null;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!cameraReady && getWidth() > 0 && getHeight() > 0) {
            centerCameraOnTownHall();
            cameraReady = true;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.drawImage(mapCache(),
                (int) Math.round(-cameraX * zoom()),
                (int) Math.round(-cameraY * zoom()),
                null);

        drawMoveHighlights(g2);
        for (Unit unit : game.getUnits()) {
            drawUnit(g2, unit);
        }
        g2.dispose();
    }

    /** Call this whenever the map itself changes (a hex is discovered, claimed, ...). */
    public void invalidateMap() {
        mapCache = null;
        repaint();
    }

    private BufferedImage mapCache() {
        if (mapCache == null || cacheZoomIndex != zoomIndex) {
            mapCache = renderMap();
            cacheZoomIndex = zoomIndex;
        }
        return mapCache;
    }

    private BufferedImage renderMap() {
        int cols = game.getMap().getCols();
        int rows = game.getMap().getRows();

        int width = (int) Math.ceil((HexGeometry.centerX(cols - 1, 1, HEX_SIZE) + HEX_SIZE) * zoom());
        int height = (int) Math.ceil((HexGeometry.centerY(0, rows - 1, HEX_SIZE) + HEX_SIZE) * zoom());

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(BG);
        g2.fillRect(0, 0, width, height);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int fontSize = Math.max(10, (int) (12 * zoom()));
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));

        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                Hex hex = game.getMap().get(col, row);
                drawHex(g2, hex,
                        HexGeometry.centerX(col, row, HEX_SIZE) * zoom(),
                        HexGeometry.centerY(col, row, HEX_SIZE) * zoom());
            }
        }
        g2.dispose();
        return image;
    }

    private void drawHex(Graphics2D g2, Hex hex, double cx, double cy) {
        HexGeometry.writeHexPath(hexShape, cx, cy, screenHexSize() * 0.98);

        if (!hex.isDiscovered()) {
            g2.setColor(FOG_FILL);
            g2.fill(hexShape);
            g2.setColor(FOG_EDGE);
            g2.setStroke(THIN);
            g2.draw(hexShape);
            return;
        }

        g2.setColor(colourOf(hex));
        g2.fill(hexShape);

        g2.setColor(TILE_EDGE);
        g2.setStroke(THIN);
        g2.draw(hexShape);

        if (hex.isOwned()) {
            g2.setColor(OWNED_EDGE);
            g2.setStroke(THICK);
            g2.draw(hexShape);
        }

        drawHexContents(g2, hex, cx, cy);

        if (hex.getCol() == game.getCentreCol() && hex.getRow() == game.getCentreRow()) {
            int marker = Math.max(10, (int) (14 * zoom()));
            g2.setColor(MARKER_FILL);
            g2.fillRect((int) (cx - marker / 2.0), (int) (cy - marker / 2.0), marker, marker);
            g2.setColor(Color.BLACK);
            g2.drawRect((int) (cx - marker / 2.0), (int) (cy - marker / 2.0), marker, marker);
        }
    }

    private Color colourOf(Hex hex) {
        boolean res = hex.hasResource();
        if (hex.getTerrain() == Terrain.FOREST) {
            return res ? FOREST_RES : FOREST;
        }
        if (hex.getTerrain() == Terrain.MOUNTAIN) {
            return res ? MOUNTAIN_RES : MOUNTAIN;
        }
        if (hex.getTerrain() == Terrain.GRASSLAND) {
            return res ? GRASSLAND_RES : GRASSLAND;
        }
        return res ? PLAINS_RES : PLAINS;
    }

    private void drawHexContents(Graphics2D g2, Hex hex, double cx, double cy) {
        if (hex.hasResource()) {
            g2.setColor(Color.WHITE);
            String letter = hex.getDeposit().getLabel().substring(0, 1);
            g2.drawString(letter + " " + hex.getDepositAmount(),
                    (int) (cx - 12 * zoom()), (int) (cy - 4 * zoom()));
        } else if (hex.isExhausted()) {
            g2.setColor(EXHAUSTED_TEXT);
            g2.drawString("empty", (int) (cx - 16 * zoom()), (int) (cy - 4 * zoom()));
        }
    }

    private double screenX(int col, int row) {
        return (HexGeometry.centerX(col, row, HEX_SIZE) - cameraX) * zoom();
    }

    private double screenY(int col, int row) {
        return (HexGeometry.centerY(col, row, HEX_SIZE) - cameraY) * zoom();
    }

    private void drawMoveHighlights(Graphics2D g2) {
        Unit selected = game.getSelected();
        if (selected == null || movingUnit != null) {
            return;
        }
        Hex here = game.hexOf(selected);
        if (here == null) {
            return;
        }
        for (Hex neighbour : game.getMap().neighbours(here)) {
            if (!game.canMove(selected, neighbour)) {
                continue;
            }
            double cx = screenX(neighbour.getCol(), neighbour.getRow());
            double cy = screenY(neighbour.getCol(), neighbour.getRow());
            HexGeometry.writeHexPath(hexShape, cx, cy, screenHexSize() * 0.98);
            g2.setColor(MOVE_FILL);
            g2.fill(hexShape);
            g2.setColor(MOVE_EDGE);
            g2.setStroke(THICK);
            g2.draw(hexShape);
        }
    }

    private void drawUnit(Graphics2D g2, Unit unit) {
        double cx = screenX(unit.getCol(), unit.getRow());
        double cy = screenY(unit.getCol(), unit.getRow());

        if (unit == movingUnit) {
            double sx = screenX(fromCol, fromRow);
            double sy = screenY(fromCol, fromRow);
            cx = sx + (cx - sx) * progress;
            cy = sy + (cy - sy) * progress;
        }

        int radius = Math.max(8, (int) (12 * zoom()));
        boolean selected = unit == game.getSelected();
        g2.setColor(selected ? UNIT_SELECTED : UNIT_FILL);
        g2.fillOval((int) (cx - radius), (int) (cy - radius + 6), radius * 2, radius * 2);
        g2.setColor(Color.BLACK);
        g2.setStroke(THIN);
        g2.drawOval((int) (cx - radius), (int) (cy - radius + 6), radius * 2, radius * 2);

        g2.setColor(selected ? Color.BLACK : Color.WHITE);
        int fontSize = Math.max(10, (int) (11 * zoom()));
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g2.drawString(unit.getLetter() + " " + unit.getAp(),
                (int) (cx - 8 * zoom()), (int) (cy + 10 * zoom()));
    }
}
