package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.*;
import java.util.List;

public class YesterdaysLayer extends Layer {
    private final List<YesterdaysImage> images;

    public YesterdaysLayer(List<YesterdaysImage> images) {
        super("Yesterdays Historical Photos");
        this.images = images;
        System.out.println("YesterdaysLayer initialized with " + images.size() + " images.");
    }

    /**
     * Checks if a screen click falls within the hit-test radius of any image point in this layer.
     */
    public YesterdaysImage getImageAtPoint(Point clickPoint, MapView mv) {
        if (images == null || clickPoint == null || mv == null) {
            return null;
        }

        YesterdaysImage closest = null;
        double minDistance = Double.MAX_VALUE;
        double thresholdPixels = 30.0; // Hit-test radius in pixels

        for (YesterdaysImage img : images) {
            if (img.getCoordinates() != null) {
                Point screenPt = mv.getPoint(img.getCoordinates());
                if (screenPt != null) {
                    double dist = screenPt.distance(clickPoint);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closest = img;
                    }
                }
            }
        }

        if (closest != null && minDistance <= thresholdPixels) {
            return closest;
        }

        return null;
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        try {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (YesterdaysImage img : images) {
                if (img.getCoordinates() != null) {
                    Point pt = mv.getPoint(img.getCoordinates());
                    if (pt != null) {
                        int x = pt.x;
                        int y = pt.y;

                        int dir = img.getDirection();
                        double radius = 32.0;    // Size of the FOV cone in pixels
                        double fovSpread = 60.0; // 60-degree camera field of view spread

                        // Convert map bearing (0=North, clockwise) to Java AWT screen angle
                        double awtCenterAngle = 90.0 - dir;
                        double startAngle = awtCenterAngle - (fovSpread / 2.0);

                        // Draw translucent FOV wedge
                        java.awt.geom.Arc2D.Double fovWedge = new java.awt.geom.Arc2D.Double(
                            x - radius, y - radius, radius * 2, radius * 2,
                            startAngle, fovSpread, java.awt.geom.Arc2D.PIE
                        );

                        g2d.setColor(new Color(255, 80, 80, 75)); // Soft translucent red fill
                        g2d.fill(fovWedge);
                        g2d.setColor(new Color(180, 30, 30, 200)); // Border outline
                        g2d.draw(fovWedge);

                        // Center camera point dot
                        g2d.setColor(new Color(200, 50, 50, 255));
                        g2d.fillOval(x - 4, y - 4, 8, 8);
                        g2d.setColor(Color.WHITE);
                        g2d.drawOval(x - 4, y - 4, 8, 8);
                    }
                }
            }
            g2d.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Icon getIcon() { return null; }

    @Override
    public boolean isMergable(Layer other) { return false; }

    @Override
    public void mergeFrom(Layer other) {}

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        try {
            for (YesterdaysImage img : images) {
                if (img.getCoordinates() != null) {
                    v.visit(img.getCoordinates());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getInfoComponent() {
        return "Displays historical photos from the Yesterdays API.";
    }

    @Override
    public String getToolTipText() {
        return "Yesterdays Historical Photos Layer (" + images.size() + " photos)";
    }

    @Override
    public Action[] getMenuEntries() { return null; }
}