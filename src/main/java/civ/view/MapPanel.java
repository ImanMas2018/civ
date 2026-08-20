package civ.view;

import civ.controller.GameController;
import civ.model.BorderExpander;
import civ.model.Builder;
import civ.model.Building;
import civ.model.BuildingType;
import civ.model.Edge;
import civ.model.Game;
import civ.model.Hex;
import civ.model.MilitaryUnit;
import civ.model.ProductionBuilding;
import civ.model.Terrain;
import civ.model.TownHall;
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
import java.util.ArrayList;
import java.util.List;

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
    private static final Color MOUNTAIN_RANGE = new Color(62, 58, 56);
    private static final Color SEA = new Color(46, 110, 168);
    private static final Color SEA_FISH = new Color(36, 88, 148);
    private static final Color PLAINS_RES = PLAINS.darker();
    private static final Color GRASSLAND_RES = GRASSLAND.darker();
    private static final Color FOREST_RES = FOREST.darker();
    private static final Color MOUNTAIN_RES = MOUNTAIN.darker();
    private static final Color RIVER = new Color(70, 160, 210);
    private static final Color WALL_LINE = new Color(90, 90, 96);
    private static final Color ROAD = new Color(186, 150, 90);

    private static final Color TILE_EDGE = new Color(0, 0, 0, 100);
    private static final Color OWNED_EDGE = new Color(255, 220, 90);
    private static final Color MARKER_FILL = new Color(240, 240, 250);
    private static final Color EXHAUSTED_TEXT = new Color(210, 90, 90);
    private static final Color MOVE_FILL = new Color(40, 190, 255, 110);
    private static final Color MOVE_EDGE = new Color(20, 230, 255);
    private static final Color ATTACK_FILL = new Color(220, 70, 50, 120);
    private static final Color ATTACK_EDGE = new Color(255, 120, 80);
    private static final Color EXPAND_FILL = new Color(255, 200, 60, 90);
    private static final Color EXPAND_EDGE = new Color(255, 220, 90);
    private static final BasicStroke HIGHLIGHT = new BasicStroke(3.0f);
    private static final Color UNIT_FILL = new Color(70, 120, 220);
    private static final Color MILITARY_FILL = new Color(70, 140, 90);
    private static final Color HOSTILE_FILL = new Color(180, 50, 45);
    private static final Color UNIT_SELECTED = Color.WHITE;

    private static final BasicStroke THIN = new BasicStroke(1.0f);
    private static final BasicStroke THICK = new BasicStroke(2.0f);

    private final Path2D.Double hexShape = new Path2D.Double();

    private final Game game;
    private GameController controller;

    private BufferedImage mapCache;
    private int cacheZoomIndex = -1;

    /**
     * Scratch image the size of the panel. Everything is composed here and
     * copied across in one blit, because translucent fills painted straight
     * onto the window came out as partial rectangles on Linux.
     */
    private BufferedImage frame;

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

        BufferedImage target = frame();
        Graphics2D g2 = target.createGraphics();
        g2.setColor(BG);
        g2.fillRect(0, 0, target.getWidth(), target.getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2.drawImage(mapCache(),
                (int) Math.round(-cameraX * zoom()),
                (int) Math.round(-cameraY * zoom()),
                null);

        drawMoveHighlights(g2);
        drawExpandHighlights(g2);
        drawWallPickHighlights(g2);
        drawAttackHighlights(g2);
        Unit selected = game.getSelected();
        for (Unit unit : visibleUnits()) {
            if (unit != selected) {
                drawUnit(g2, unit);
            }
        }
        if (selected != null) {
            drawUnit(g2, selected);
        }
        g2.dispose();

        g.drawImage(target, 0, 0, null);
    }

    private BufferedImage frame() {
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
            frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        return frame;
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
        drawEdges(g2);
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

        Building building = hex.getBuilding();
        if (building instanceof TownHall && ((TownHall) building).hasDefensiveWall()) {
            g2.setColor(new Color(110, 110, 118));
            g2.setStroke(new BasicStroke(5.0f));
            g2.draw(hexShape);
        }

        drawHexContents(g2, hex, cx, cy);
    }

    private Color colourOf(Hex hex) {
        boolean res = hex.hasResource();
        if (hex.getTerrain() == Terrain.FOREST) {
            return res ? FOREST_RES : FOREST;
        }
        if (hex.getTerrain() == Terrain.MOUNTAIN_RANGE) {
            return MOUNTAIN_RANGE;
        }
        if (hex.getTerrain() == Terrain.MOUNTAIN) {
            return res ? MOUNTAIN_RES : MOUNTAIN;
        }
        if (hex.getTerrain() == Terrain.SEA) {
            return res ? SEA_FISH : SEA;
        }
        if (hex.getTerrain() == Terrain.GRASSLAND) {
            return res ? GRASSLAND_RES : GRASSLAND;
        }
        return res ? PLAINS_RES : PLAINS;
    }

    private void drawEdges(Graphics2D g2) {
        for (Edge edge : game.getMap().getEdges().all()) {
            if (!edge.hasRiver() && !edge.hasWall()) {
                continue;
            }
            Hex a = game.getMap().get(edge.getCol1(), edge.getRow1());
            Hex b = game.getMap().get(edge.getCol2(), edge.getRow2());
            if (a == null || b == null) {
                continue;
            }
            if (!a.isDiscovered() && !b.isDiscovered()) {
                continue;
            }
            double x1 = HexGeometry.centerX(a.getCol(), a.getRow(), HEX_SIZE) * zoom();
            double y1 = HexGeometry.centerY(a.getCol(), a.getRow(), HEX_SIZE) * zoom();
            double x2 = HexGeometry.centerX(b.getCol(), b.getRow(), HEX_SIZE) * zoom();
            double y2 = HexGeometry.centerY(b.getCol(), b.getRow(), HEX_SIZE) * zoom();
            double mx = (x1 + x2) / 2.0;
            double my = (y1 + y2) / 2.0;
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.hypot(dx, dy);
            if (len < 1) {
                continue;
            }
            double half = screenHexSize() * 0.48;
            double px = -dy / len * half;
            double py = dx / len * half;
            int ax = (int) Math.round(mx - px);
            int ay = (int) Math.round(my - py);
            int bx = (int) Math.round(mx + px);
            int by = (int) Math.round(my + py);
            if (edge.hasRiver()) {
                g2.setColor(RIVER);
                g2.setStroke(new BasicStroke(Math.max(3f, (float) (4.5 * zoom())),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(ax, ay, bx, by);
            }
            if (edge.hasWall()) {
                g2.setColor(WALL_LINE);
                g2.setStroke(new BasicStroke(Math.max(4f, (float) (6 * zoom())),
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g2.drawLine(ax, ay, bx, by);
            }
        }
    }

    private void drawHexContents(Graphics2D g2, Hex hex, double cx, double cy) {
        if (hex.hasResource()) {
            g2.setColor(Color.WHITE);
            String letter = hex.getTerrain().isSea() ? "Fi" : hex.getDeposit().getLabel().substring(0, 1);
            g2.drawString(letter + " " + hex.getDepositAmount(),
                    (int) (cx - 12 * zoom()), (int) (cy - 4 * zoom()));
        } else if (hex.isExhausted()) {
            g2.setColor(EXHAUSTED_TEXT);
            g2.drawString("empty", (int) (cx - 16 * zoom()), (int) (cy - 4 * zoom()));
        }

        if (hex.hasRoad()) {
            g2.setColor(ROAD);
            g2.setStroke(new BasicStroke(Math.max(2f, (float) (3.5 * zoom()))));
            int r = Math.max(4, (int) (8 * zoom()));
            g2.drawLine((int) (cx - r), (int) cy, (int) (cx + r), (int) cy);
            g2.drawLine((int) cx, (int) (cy - r), (int) cx, (int) (cy + r));
        }

        Building building = hex.getBuilding();
        if (building == null) {
            return;
        }
        int size = Math.max(10, (int) (16 * zoom()));
        g2.setColor(building.getType() == BuildingType.TOWN_HALL
                ? MARKER_FILL
                : new Color(200, 160, 100));
        g2.fillRect((int) (cx - size / 2.0), (int) (cy - size / 2.0), size, size);
        g2.setColor(Color.BLACK);
        g2.drawRect((int) (cx - size / 2.0), (int) (cy - size / 2.0), size, size);

        if (building instanceof ProductionBuilding) {
            ProductionBuilding production = (ProductionBuilding) building;
            g2.setColor(Color.WHITE);
            g2.drawString(production.getWorkers().size() + "/" + production.getType().getWorkerCapacity(),
                    (int) (cx - 8 * zoom()), (int) (cy + 18 * zoom()));
        }
    }

    private void drawWallPickHighlights(Graphics2D g2) {
        if (controller == null || game.getSelected() == null) {
            return;
        }
        if (!controller.isPlacingWall() && !controller.isDemolishingWall()) {
            return;
        }
        Unit selected = game.getSelected();
        if (!(selected instanceof Builder)) {
            return;
        }
        Builder builder = (Builder) selected;
        Hex here = game.hexOf(builder);
        if (here == null) {
            return;
        }
        for (Hex neighbour : game.getMap().neighbours(here)) {
            boolean ok = controller.isPlacingWall()
                    ? game.canBuildWall(builder, neighbour)
                    : game.canDemolishWall(builder, neighbour);
            if (ok) {
                paintHighlight(g2, neighbour, EXPAND_FILL, EXPAND_EDGE);
            }
        }
    }

    private void drawAttackHighlights(Graphics2D g2) {
        if (controller == null || movingUnit != null || game.getSelected() == null) {
            return;
        }
        Unit selected = game.getSelected();
        if (!(selected instanceof MilitaryUnit)) {
            return;
        }
        Hex from = game.hexOf(selected);
        if (from == null) {
            return;
        }
        if (controller.isAttackingWall()) {
            for (Hex neighbour : game.getMap().neighbours(from)) {
                if (game.canAttackWall(from, neighbour)) {
                    paintHighlight(g2, neighbour, ATTACK_FILL, ATTACK_EDGE);
                }
            }
            return;
        }
        if (!controller.isAttacking()) {
            return;
        }
        int cols = game.getMap().getCols();
        int rows = game.getMap().getRows();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                Hex hex = game.getMap().get(col, row);
                if (game.canAttack(from, hex)) {
                    paintHighlight(g2, hex, ATTACK_FILL, ATTACK_EDGE);
                }
            }
        }
    }

    private List<Unit> visibleUnits() {
        List<Unit> units = new ArrayList<>(game.getUnits());
        for (MilitaryUnit hostile : game.getHostiles()) {
            Hex tile = game.hexOf(hostile);
            if (tile != null && tile.isDiscovered()) {
                units.add(hostile);
            }
        }
        return units;
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
        if (controller != null && (controller.isAttacking() || controller.isAttackingWall())) {
            return;
        }
        if (selected instanceof BorderExpander) {
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
            paintHighlight(g2, neighbour, MOVE_FILL, MOVE_EDGE);
        }
    }

    /**
     * A Border Expander claims any discovered hex, not just its neighbours.
     * Those 6 cyan tiles were walk-steps, which made the ability look tiny.
     */
    private void drawExpandHighlights(Graphics2D g2) {
        Unit selected = game.getSelected();
        if (!(selected instanceof BorderExpander) || movingUnit != null) {
            return;
        }
        BorderExpander expander = (BorderExpander) selected;
        int cols = game.getMap().getCols();
        int rows = game.getMap().getRows();
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                Hex hex = game.getMap().get(col, row);
                if (game.canExpandBorder(expander, hex)) {
                    paintHighlight(g2, hex, EXPAND_FILL, EXPAND_EDGE);
                }
            }
        }
    }

    private void paintHighlight(Graphics2D g2, Hex hex, Color fill, Color edge) {
        HexGeometry.writeHexPath(hexShape,
                screenX(hex.getCol(), hex.getRow()),
                screenY(hex.getCol(), hex.getRow()),
                screenHexSize() * 0.94);
        g2.setColor(fill);
        g2.fill(hexShape);
        g2.setColor(edge);
        g2.setStroke(HIGHLIGHT);
        g2.draw(hexShape);
    }

    private void drawUnit(Graphics2D g2, Unit unit) {
        double cx = screenX(unit.getCol(), unit.getRow());
        double cy = screenY(unit.getCol(), unit.getRow());

        if (unit == movingUnit) {
            double sx = screenX(fromCol, fromRow);
            double sy = screenY(fromCol, fromRow);
            cx = sx + (cx - sx) * progress;
            cy = sy + (cy - sy) * progress;
        } else {
            int stackIndex = 0;
            for (Unit other : visibleUnits()) {
                if (other.getCol() == unit.getCol() && other.getRow() == unit.getRow()) {
                    if (other == unit) {
                        break;
                    }
                    stackIndex++;
                }
            }
            cx += stackIndex * 8 * zoom();
            cy += stackIndex * 6 * zoom();
        }

        int radius = Math.max(8, (int) (12 * zoom()));
        boolean selected = unit == game.getSelected();
        Hex tile = game.hexOf(unit);
        boolean boat = tile != null && tile.getTerrain().isSea();
        Color fill = UNIT_FILL;
        if (unit instanceof MilitaryUnit && ((MilitaryUnit) unit).isHostile()) {
            fill = HOSTILE_FILL;
        } else if (unit instanceof MilitaryUnit) {
            fill = MILITARY_FILL;
        }
        g2.setColor(selected ? UNIT_SELECTED : (boat ? new Color(150, 110, 70) : fill));
        if (boat) {
            g2.fillRoundRect((int) (cx - radius), (int) (cy - radius / 2.0 + 6),
                    radius * 2, radius, radius / 2, radius / 2);
            g2.setColor(Color.BLACK);
            g2.setStroke(THIN);
            g2.drawRoundRect((int) (cx - radius), (int) (cy - radius / 2.0 + 6),
                    radius * 2, radius, radius / 2, radius / 2);
        } else {
            g2.fillOval((int) (cx - radius), (int) (cy - radius + 6), radius * 2, radius * 2);
            g2.setColor(Color.BLACK);
            g2.setStroke(THIN);
            g2.drawOval((int) (cx - radius), (int) (cy - radius + 6), radius * 2, radius * 2);
        }

        g2.setColor(selected ? Color.BLACK : Color.WHITE);
        int fontSize = Math.max(10, (int) (11 * zoom()));
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        g2.drawString(unit.getLetter() + " " + unit.getAp(),
                (int) (cx - 8 * zoom()), (int) (cy + 10 * zoom()));
    }
}
