package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class YesterdaysLayer extends Layer {
    private List<YesterdaysImage> images;
    private final boolean isFromAboveLayer;

    public YesterdaysLayer(List<YesterdaysImage> images) {
        this(images, false);
    }

    public YesterdaysLayer(List<YesterdaysImage> images, boolean isFromAboveLayer) {
        super(isFromAboveLayer ? "Yesterdays From-Above Photos" : "Yesterdays Historical Photos");
        this.images = (images != null) ? images : new ArrayList<>();
        this.isFromAboveLayer = isFromAboveLayer;
    }

    public synchronized void setImages(List<YesterdaysImage> newImages) {
        this.images = (newImages != null) ? newImages : new ArrayList<>();
        invalidate();
    }

    public synchronized List<YesterdaysImage> getImages() {
        return images;
    }

    public boolean isFromAboveLayer() {
        return isFromAboveLayer;
    }

    public YesterdaysImage getImageAtPoint(Point clickPoint, MapView mv) {
        if (images == null || clickPoint == null || mv == null) {
            return null;
        }

        for (YesterdaysImage img : images) {
            if (!img.isFromAbove() && img.getCoordinates() != null) {
                Point screenPt = mv.getPoint(img.getCoordinates());
                if (screenPt != null && screenPt.distance(clickPoint) <= 20.0) {
                    return img;
                }
            }
        }

        for (YesterdaysImage img : images) {
            if (img.isFromAbove() && img.getPolygon() != null && !img.getPolygon().isEmpty()) {
                Path2D path = new Path2D.Double();
                boolean first = true;
                for (org.openstreetmap.josm.data.coor.LatLon node : img.getPolygon()) {
                    Point pt = mv.getPoint(node);
                    if (pt != null) {
                        if (first) {
                            path.moveTo(pt.x, pt.y);
                            first = false;
                        } else {
                            path.lineTo(pt.x, pt.y);
                        }
                    }
                }
                path.closePath();

                if (path.contains(clickPoint)) {
                    return img;
                }
            }
        }

        return null;
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds box) {
        if (images == null || mv == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (YesterdaysImage img : images) {
            if (img.isFromAbove() && img.getPolygon() != null && !img.getPolygon().isEmpty()) {
                Path2D path = new Path2D.Double();
                boolean first = true;
                for (org.openstreetmap.josm.data.coor.LatLon node : img.getPolygon()) {
                    Point pt = mv.getPoint(node);
                    if (pt != null) {
                        if (first) {
                            path.moveTo(pt.x, pt.y);
                            first = false;
                        } else {
                            path.lineTo(pt.x, pt.y);
                        }
                    }
                }
                path.closePath();

                g2.setColor(new Color(255, 140, 0, 45));
                g2.fill(path);

                g2.setColor(new Color(255, 120, 0, 220));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(path);

                if (img.getCoordinates() != null) {
                    Point centerPt = mv.getPoint(img.getCoordinates());
                    if (centerPt != null) {
                        g2.setColor(new Color(255, 100, 0, 220));
                        g2.fillOval(centerPt.x - 3, centerPt.y - 3, 6, 6);
                    }
                }
            } else if (img.getCoordinates() != null) {
                Point pt = mv.getPoint(img.getCoordinates());
                if (pt != null) {
                    int x = pt.x;
                    int y = pt.y;

                    int dir = img.getDirection();
                    if (dir > 0 && dir <= 360) {
                        double radius = 32.0;
                        double fovSpread = 60.0;
                        double startAngle = (90.0 - dir) - (fovSpread / 2.0);

                        Arc2D fovWedge = new Arc2D.Double(
                            x - radius, y - radius, radius * 2, radius * 2,
                            startAngle, fovSpread, Arc2D.PIE
                        );

                        g2.setColor(new Color(255, 80, 80, 75));
                        g2.fill(fovWedge);
                        g2.setColor(new Color(180, 30, 30, 200));
                        g2.draw(fovWedge);
                    }

                    g2.setColor(new Color(200, 50, 50, 255));
                    g2.fillOval(x - 4, y - 4, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.drawOval(x - 4, y - 4, 8, 8);
                }
            }
        }
        g2.dispose();
    }

    @Override
    public Object getInfoComponent() {
        return getToolTipText();
    }

    @Override
    public String getToolTipText() {
        return getName() + " (" + images.size() + " photos)";
    }

    @Override
    public Icon getIcon() { return null; }

    @Override
    public boolean isMergable(Layer other) { return false; }

    @Override
    public void mergeFrom(Layer other) {}

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        if (images == null) return;
        for (YesterdaysImage img : images) {
            if (img.getCoordinates() != null) {
                v.visit(img.getCoordinates());
            }
        }
    }

    @Override
    public Action[] getMenuEntries() { return null; }
}