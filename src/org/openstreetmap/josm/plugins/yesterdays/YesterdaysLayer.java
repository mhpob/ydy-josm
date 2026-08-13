package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class YesterdaysLayer extends Layer {
    private final List<YesterdaysImage> images;
    private final MouseAdapter mouseAdapter;

    public YesterdaysLayer(List<YesterdaysImage> images) {
        super("Yesterdays Historical Photos");
        this.images = images;
        System.out.println("YesterdaysLayer initialized with " + images.size() + " images.");

        this.mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (MainApplication.getMap() == null || MainApplication.getMap().mapView == null) return;
                        MapView mv = MainApplication.getMap().mapView;

                        Point clickPoint = e.getPoint();
                        System.out.println("Map clicked at screen coords: " + clickPoint.x + ", " + clickPoint.y);

                        YesterdaysImage closest = null;
                        double minDistance = Double.MAX_VALUE;
                        double thresholdPixels = 30.0; // Generous hit-test radius

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

                        if (closest != null) {
                            System.out.println("Closest photo: '" + closest.getTitle() + "' at distance " + minDistance + "px");
                        }

                        final YesterdaysImage targetImage = closest;

                        if (targetImage != null && minDistance <= thresholdPixels) {
                            System.out.println("Hit detected! Opening sidebar for: " + targetImage.getTitle());
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                try {
                                    YesterdaysInfoPanel panel = YesterdaysInfoPanel.getInstance();
                                    panel.displayImage(targetImage);
                                    
                                    // Make the panel visible in JOSM's sidebar panel container
                                    panel.setVisible(true);
                                    panel.requestFocusInWindow();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            });
                        } else {
                            System.out.println("Click was too far from any point (closest was " + minDistance + "px away).");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        try {
            if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
                MainApplication.getMap().mapView.addMouseListener(mouseAdapter);
                System.out.println("Mouse listener successfully attached to MapView.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        try {
            if (MainApplication.getMap() != null && MainApplication.getMap().mapView != null) {
                if (visible) {
                    MainApplication.getMap().mapView.addMouseListener(mouseAdapter);
                } else {
                    MainApplication.getMap().mapView.removeMouseListener(mouseAdapter);
                }
            }
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