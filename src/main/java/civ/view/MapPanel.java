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
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;

/**
 * Draws the hex map with fog of war. Supports discrete zoom and camera pan.
 */
public class MapPanel extends JPanel {

    private static final double HEX_SIZE = 34;
    private static final double[] ZOOM_LEVELS = {0.55, 0.75, 1.0, 1.35, 1.8};

    /** Undiscovered hexes — dark but clearly visible as a grid, not invisible black. */
    private static final Color FOG_FILL = new Color(42, 48, 62);
    private static final Color FOG_EDGE = new Color(72, 80, 98);

    private final Game game;

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
        setBackground(new Color(18, 20, 28));
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

    /** Puts the Town Hall hex in the middle of the panel. */
    private void centerCameraOnTownHall() {
        double worldX = HexGeometry.centerX(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        double worldY = HexGeometry.centerY(game.getCentreCol(), game.getCentreRow(), HEX_SIZE);
        cameraX = worldX - (getWidth() / 2.0) / zoom();
        cameraY = worldY - (getHeight() / 2.0) / zoom();
    }

    private double zoom() {
        return ZOOM_LEVELS[zoomIndex];
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
            // Keep the world point under the mouse stable while changing zoom.
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
                // Zoom toward the centre of the panel.
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

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int col = 0; col < game.getMap().getCols(); col++) {
            for (int row = 0; row < game.getMap().getRows(); row++) {
                drawHex(g2, game.getMap().get(col, row));
            }
        }
    }

    private double screenX(int col, int row) {
        return (HexGeometry.centerX(col, row, HEX_SIZE) - cameraX) * zoom();
    }

    private double screenY(int col, int row) {
        return (HexGeometry.centerY(col, row, HEX_SIZE) - cameraY) * zoom();
    }

    private void drawHex(Graphics2D g2, Hex hex) {
        double cx = screenX(hex.getCol(), hex.getRow());
        double cy = screenY(hex.getCol(), hex.getRow());
        // Slightly smaller than centre spacing so edges stay crisp and don't smear together.
        Polygon shape = HexGeometry.polygon(cx, cy, HEX_SIZE * zoom() * 0.95);

        if (!hex.isDiscovered()) {
            g2.setColor(FOG_FILL);
            g2.fillPolygon(shape);
            g2.setColor(FOG_EDGE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawPolygon(shape);
            return;
        }

        g2.setColor(colourOf(hex));
        g2.fillPolygon(shape);

        g2.setColor(new Color(0, 0, 0, 90));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(shape);

        if (hex.isOwned()) {
            g2.setColor(new Color(255, 220, 90));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawPolygon(shape);
            g2.setStroke(new BasicStroke(1f));
        }

        drawHexContents(g2, hex, cx, cy);

        // Town Hall marker at the map centre (building class arrives in a later step).
        if (hex.getCol() == game.getCentreCol() && hex.getRow() == game.getCentreRow()) {
            int size = Math.max(8, (int) (16 * zoom()));
            g2.setColor(new Color(240, 240, 250));
            g2.fillRect((int) (cx - size / 2.0), (int) (cy - size / 2.0), size, size);
            g2.setColor(Color.BLACK);
            g2.drawRect((int) (cx - size / 2.0), (int) (cy - size / 2.0), size, size);
        }
    }

    private Color colourOf(Hex hex) {
        Color base;
        if (hex.getTerrain() == Terrain.FOREST) {
            base = new Color(34, 102, 51);
        } else if (hex.getTerrain() == Terrain.MOUNTAIN) {
            base = new Color(120, 118, 112);
        } else if (hex.getTerrain() == Terrain.GRASSLAND) {
            base = new Color(126, 176, 76);
        } else {
            base = new Color(196, 186, 130);
        }
        if (hex.hasResource()) {
            return base.darker();
        }
        return base;
    }

    private void drawHexContents(Graphics2D g2, Hex hex, double cx, double cy) {
        int fontSize = Math.max(9, (int) (11 * zoom()));
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));

        if (hex.hasResource()) {
            g2.setColor(Color.WHITE);
            String letter = hex.getDeposit().getLabel().substring(0, 1);
            g2.drawString(letter + " " + hex.getDepositAmount(),
                    (int) (cx - 14 * zoom()), (int) (cy - 8 * zoom()));
        } else if (hex.isExhausted()) {
            g2.setColor(new Color(210, 90, 90));
            g2.drawString("empty", (int) (cx - 16 * zoom()), (int) (cy - 8 * zoom()));
        }
    }
}
