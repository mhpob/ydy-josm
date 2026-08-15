package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.OpenBrowser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

public class YesterdaysInfoPanel extends ToggleDialog {
    private static YesterdaysInfoPanel instance;
    private final JLabel titleLabel;
    private final JLabel idLabel;
    private final JLabel dateLabel;
    private final JLabel licenseLabel;
    private final JLabel imageLabel;
    private BufferedImage currentImage;
    private String currentWebUrl;

    public YesterdaysInfoPanel() {
        super("Yesterdays Photos", "geoimage", "Display historical photo details", null, 200);
        instance = this;

        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        titleLabel = new JLabel("Click photo point");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        
        idLabel = new JLabel("");
        idLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        idLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        idLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentWebUrl != null && !currentWebUrl.isEmpty()) {
                    OpenBrowser.displayUrl(currentWebUrl);
                }
            }
        });

        dateLabel = new JLabel("Date: -");
        dateLabel.setFont(dateLabel.getFont().deriveFont(11f));

        licenseLabel = new JLabel("Lic: -");
        licenseLabel.setFont(licenseLabel.getFont().deriveFont(11f));
        licenseLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        imageLabel = new JLabel("No image selected", JLabel.CENTER);
        imageLabel.setPreferredSize(new Dimension(200, 180));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        imageLabel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                redrawScaledImage();
            }
        });

        // 2-Row x 2-Column Layout
        JPanel textPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(1, 2, 1, 2);

        // Row 0: Title (Left) & ID (Right)
        gbc.gridy = 0;
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.weightx = 0.7;
        textPanel.add(titleLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        textPanel.add(idLabel, gbc);

        // Row 1: Date (Left) & License (Right)
        gbc.gridy = 1;

        gbc.gridx = 0;
        gbc.weightx = 0.5;
        textPanel.add(dateLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        textPanel.add(licenseLabel, gbc);

        panel.add(textPanel, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);

        createLayout(panel, false, null);
    }

    public static synchronized YesterdaysInfoPanel getInstance() {
        if (instance == null) {
            instance = new YesterdaysInfoPanel();
        }
        return instance;
    }

    public void displayImage(YesterdaysImage img) {
        if (img == null) return;

        try {
            String titleText = img.getTitle() != null ? img.getTitle() : "Untitled";
            titleLabel.setText(titleText);
            titleLabel.setToolTipText(titleText);
            
            String imageId = img.getImageId();
            currentWebUrl = "https://yesterdays.maprva.org/" + imageId;
            idLabel.setText("<html><a href=\"\">#" + imageId + "</a></html>");

            updateDetailLabels(img);

            // Fetch detailed metadata from API if not yet cached
            if (img.getDateDisplay() == null || img.getLicense() == null) {
                YesterdaysPlugin.loadDetailsForImageAsync(img, () -> {
                    updateDetailLabels(img);
                    revalidate();
                    repaint();
                });
            }

            String imageUrl = img.getThumbnailUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                imageLabel.setText("Loading image...");
                imageLabel.setIcon(null);
                currentImage = null;
                
                final String finalUrl = imageUrl;
                new Thread(() -> {
                    try {
                        URL url = new URL(finalUrl);
                        BufferedImage downloadedImg = ImageIO.read(url);
                        
                        if (downloadedImg != null) {
                            currentImage = downloadedImg;
                            SwingUtilities.invokeLater(() -> redrawScaledImage());
                        } else {
                            throw new Exception("ImageIO.read returned null.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(null);
                            imageLabel.setText("Failed to load image");
                        });
                    }
                }).start();
            } else {
                currentImage = null;
                imageLabel.setIcon(null);
                imageLabel.setText("No image URL available");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDetailLabels(YesterdaysImage img) {
        String dateVal = img.getDateDisplay() != null ? img.getDateDisplay() : "...";
        String licVal = img.getLicense() != null ? img.getLicense() : "...";
        dateLabel.setText("Date: " + dateVal);
        licenseLabel.setText("Lic: " + licVal);
        licenseLabel.setToolTipText("License: " + licVal);
    }

    private void redrawScaledImage() {
        if (currentImage == null) return;

        int panelWidth = imageLabel.getWidth();
        int panelHeight = imageLabel.getHeight();

        if (panelWidth <= 10 || panelHeight <= 10) return;

        int imgWidth = currentImage.getWidth();
        int imgHeight = currentImage.getHeight();

        double scaleX = (double) panelWidth / imgWidth;
        double scaleY = (double) panelHeight / imgHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = Math.max(1, (int) (imgWidth * scale));
        int newHeight = Math.max(1, (int) (imgHeight * scale));

        Image scaled = currentImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setText("");
        imageLabel.setIcon(new ImageIcon(scaled));
    }
}