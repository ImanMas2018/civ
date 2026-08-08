package civ.view;

import civ.model.Game;
import civ.model.Hex;
import civ.model.Terrain;
import civ.util.HexGeometry;
import javax.swing.JPanel;
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
 * Draws the hex map with fog of war. Supports discrete zoom and camera pan.
 */
public class MapPanel extends JPanel {

    /** Distance from hex centre to a vertex, in world pixels (before zoom). */
    private static final double HEX_SIZE = 40;

    /** Discrete zoom steps — lowest still keeps hexes clearly 6-sided. */
    private static final double[] ZOOM_LEVELS = {0.7, 0.85, 1.0, 1.25, 1.55};

    private static final Color FOG_FILL = new Color(52, 60, 78);
    private static final Color FOG_EDGE = new Color(90, 100, 122);
    private static final Color BG = new Color(18, 20, 28);

    // Terrain colours, plus the dimmed variant used when a hex carries a deposit.
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

    private static final BasicStroke THIN = new BasicStroke(1.0f);
    private static final BasicStroke THICK = new BasicStroke(2.0f);

    /**
     * One path reused for every hex in a frame. Building a fresh Path2D per hex
     * created ~400 short-lived objects per repaint, which made dragging stutter.
     */
    private final Path2D.Double hexShape = new Path2D.Double();

    private final Game game;

    /**
     * The finished map drawn once at the current zoom. Antialiased hex drawing
     * costs ~25 ms per frame, but copying a ready-made image costs ~0.2 ms, so
     * panning blits this instead of redrawing every hex.
     */
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

    private void centerCameraOnTownHall() {
        double worldX = HexGeometry.centerX(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        double worldY = HexGeometry.centerY(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        cameraX = worldX - (getWidth() / 2.0) / zoom();
        cameraY = worldY - (getHeight() / 2.0) / zoom();
    }

    private double zoom() {
        return ZOOM_LEVELS[zoomIndex];
    }

    /** On-screen outer radius of one hex at the current zoom. */
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
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - dragStartX;
                int dy = e.getY() - dragStartY;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!cameraReady && getWidth() > 0 && getHeight() > 0) {
            centerCameraOnTownHall();
            cameraReady = true;
        }

        // Panning only changes where the cached map sits, so just move the image.
        g.drawImage(mapCache(), (int) Math.round(-cameraX * zoom()),
                (int) Math.round(-cameraY * zoom()), null);
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

    /** Draws the whole map into an offscreen image, in world pixels times zoom. */
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

        // The label font only depends on zoom, so set it once instead of per hex.
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
        // 0.98 leaves a 1-pixel hairline so edges stay readable without breaking the hex shape.
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
}
