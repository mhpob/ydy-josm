package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.OpenBrowser;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class YesterdaysInfoPanel extends ToggleDialog {
    private static YesterdaysInfoPanel instance;
    private final JLabel titleLabel;
    private final JLabel idLabel;
    private final JLabel dateLabel;
    private final JLabel licenseLabel;
    private final JLabel imageLabel;
    private BufferedImage currentImage;
    private String currentWebUrl;
    private YesterdaysImage selectedImage;

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

        JPanel textPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(1, 2, 1, 2);

        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.7;
        textPanel.add(titleLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        textPanel.add(idLabel, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.5;
        textPanel.add(dateLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        textPanel.add(licenseLabel, gbc);

        panel.add(textPanel, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);

        JosmAction sourceTagAction = new JosmAction(
            I18n.tr("Add Source Tags"),
            "copy",
            I18n.tr("Add source tags from Yesterdays photo to selected map objects"),
            null,
            false
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySourceTagsToSelection();
            }
        };

        SideButton sourceTagButton = new SideButton(sourceTagAction);
        createLayout(panel, false, Collections.singletonList(sourceTagButton));
    }

    private void applySourceTagsToSelection() {
        if (selectedImage == null) {
            JOptionPane.showMessageDialog(
                this, 
                I18n.tr("Please select an image first."), 
                I18n.tr("No Image Selected"), 
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (editLayer == null || editLayer.getDataSet() == null) {
            JOptionPane.showMessageDialog(
                this, 
                I18n.tr("No active edit layer found."), 
                I18n.tr("No OSM Layer"), 
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Collection<OsmPrimitive> selection = editLayer.getDataSet().getSelected();
        if (selection.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, 
                I18n.tr("Please select one or more objects on the map first."), 
                I18n.tr("No Selection"), 
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<Command> commands = new ArrayList<>();

        for (OsmPrimitive primitive : selection) {
            boolean hasSourceTags = false;
            Set<Integer> usedIndices = new HashSet<>();

            for (String key : primitive.keySet()) {
                if (key.equals("source") || key.startsWith("source:")) {
                    hasSourceTags = true;
                    if (key.startsWith("source:")) {
                        String subKey = key.substring(7);
                        int colonIndex = subKey.indexOf(':');
                        String indexStr = (colonIndex != -1) ? subKey.substring(0, colonIndex) : subKey;
                        try {
                            usedIndices.add(Integer.parseInt(indexStr));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            String sourceKey;
            String sourceNameKey;
            String sourceUrlKey;
            String sourceDateKey;
            String sourceLicenseKey;

            if (!hasSourceTags) {
                sourceKey = "source";
                sourceNameKey = "source:name";
                sourceUrlKey = "source:url";
                sourceDateKey = "source:date";
                sourceLicenseKey = "source:license";
            } else {
                int nextIndex = 1;
                while (usedIndices.contains(nextIndex)) {
                    nextIndex++;
                }
                sourceKey = "source:" + nextIndex;
                sourceNameKey = "source:" + nextIndex + ":name";
                sourceUrlKey = "source:" + nextIndex + ":url";
                sourceDateKey = "source:" + nextIndex + ":date";
                sourceLicenseKey = "source:" + nextIndex + ":license";
            }

            List<OsmPrimitive> singlePrim = Collections.singletonList(primitive);

            if (selectedImage.getTitle() != null && !selectedImage.getTitle().trim().isEmpty()) {
                String sourceValue = "Historic Photo: " + selectedImage.getTitle().trim();
                commands.add(new ChangePropertyCommand(singlePrim, sourceKey, sourceValue));
            }
            if (selectedImage.getSourceName() != null && !selectedImage.getSourceName().trim().isEmpty()) {
                commands.add(new ChangePropertyCommand(singlePrim, sourceNameKey, selectedImage.getSourceName().trim()));
            }
            if (selectedImage.getOriginalUrl() != null && !selectedImage.getOriginalUrl().trim().isEmpty()) {
                commands.add(new ChangePropertyCommand(singlePrim, sourceUrlKey, selectedImage.getOriginalUrl().trim()));
            }
            if (selectedImage.getDateDisplay() != null && !selectedImage.getDateDisplay().trim().isEmpty() 
                && !selectedImage.getDateDisplay().equalsIgnoreCase("Unknown") 
                && !selectedImage.getDateDisplay().equals("...")) {
                commands.add(new ChangePropertyCommand(singlePrim, sourceDateKey, selectedImage.getDateDisplay().trim()));
            }
            if (selectedImage.getLicenseName() != null && !selectedImage.getLicenseName().trim().isEmpty()) {
                commands.add(new ChangePropertyCommand(singlePrim, sourceLicenseKey, selectedImage.getLicenseName().trim()));
            }
        }

        if (!commands.isEmpty()) {
            Command seq = new SequenceCommand(I18n.tr("Add source tags from Yesterdays photo"), commands);
            seq.executeCommand();
        } else {
            JOptionPane.showMessageDialog(
                this, 
                I18n.tr("No source metadata available to apply yet. Wait for details to finish loading."), 
                I18n.tr("Missing Tags"), 
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public static synchronized YesterdaysInfoPanel getInstance() {
        if (instance == null) {
            instance = new YesterdaysInfoPanel();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void displayImage(YesterdaysImage img) {
        if (img == null) return;
        this.selectedImage = img;

        try {
            String titleText = img.getTitle() != null ? img.getTitle() : "Untitled";
            titleLabel.setText(titleText);
            titleLabel.setToolTipText(titleText);

            String baseUrl = Config.getPref().get(
                YesterdaysPreferenceSetting.PREF_BASE_URL, 
                YesterdaysPreferenceSetting.DEFAULT_BASE_URL
            );
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            String imageId = img.getImageId();
            currentWebUrl = baseUrl + "/" + imageId;
            idLabel.setText("<html><a href=\"\">#" + imageId + "</a></html>");

            updateDetailLabels(img);

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
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", YesterdaysPlugin.getUserAgent());
                        BufferedImage downloadedImg = ImageIO.read(conn.getInputStream());
                        
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