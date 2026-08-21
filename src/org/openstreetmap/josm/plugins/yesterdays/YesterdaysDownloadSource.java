package org.openstreetmap.josm.plugins.yesterdays;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.download.AbstractDownloadSourcePanel;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.download.DownloadSettings;
import org.openstreetmap.josm.gui.download.DownloadSource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class YesterdaysDownloadSource implements DownloadSource {

    private YesterdaysPanel lastCreatedPanel;

    @Override
    public String getLabel() {
        return I18n.tr("Yesterdays");
    }

    @Override
    public boolean onlyExpert() {
        return false;
    }

    @Override
    public AbstractDownloadSourcePanel<?> createPanel(DownloadDialog dialog) {
        lastCreatedPanel = new YesterdaysPanel(this);
        return lastCreatedPanel;
    }

    @Override
    public void doDownload(Object source, DownloadSettings settings) {
        Bounds bounds = null;

        if (source instanceof Bounds) {
            bounds = (Bounds) source;
        } else if (source instanceof List) {
            for (Object item : (List<?>) source) {
                if (item instanceof Bounds) {
                    bounds = (Bounds) item;
                    break;
                }
            }
        }

        if (bounds == null && DownloadDialog.getInstance() != null) {
            Optional<Bounds> opt = DownloadDialog.getInstance().getSelectedDownloadArea();
            bounds = opt.orElse(null);
        }

        if (bounds != null && lastCreatedPanel != null) {
            boolean fetchPoints = lastCreatedPanel.isPointsSelected();
            boolean fetchFromAbove = lastCreatedPanel.isFromAboveSelected();
            
            // Start with active OHM Time Filter parameters if enabled
            Map<String, String> filters = TimeFilterBridge.getActiveFilters();
            
            // Merge manual download panel entries (overriding or supplementing if specified)
            filters.putAll(lastCreatedPanel.getFilterMap());

            YesterdaysPlugin.loadImagesForBoundsAsync(bounds, filters, fetchPoints, fetchFromAbove);
        }
    }
    
    private static class YesterdaysPanel extends AbstractDownloadSourcePanel<YesterdaysDownloadSource> {
        private final YesterdaysDownloadSource source;
        private final JCheckBox pointsCheckBox;
        private final JCheckBox fromAboveCheckBox;

        // Basic Filters
        private final JTextField minYearField;
        private final JTextField maxYearField;
        private final JComboBox<String> confidenceCombo;

        // Entity Filters
        private final JTextField imageIdField;
        private final JTextField sourceIdField;
        private final JTextField collectionIdField;
        private final JTextField subjectIdField;
        private final JTextField georeferencedByField;

        public YesterdaysPanel(YesterdaysDownloadSource source) {
            super(source);
            this.source = source;

            setLayout(new GridBagLayout());

            JLabel infoLabel = new JLabel(I18n.tr("Download historical photos from Yesterdays for the selected area."));

            // Checkboxes
            pointsCheckBox = new JCheckBox(I18n.tr("Point Georeferences"), true);
            fromAboveCheckBox = new JCheckBox(I18n.tr("From-Above Georeferences"), true);

            pointsCheckBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.DESELECTED && !fromAboveCheckBox.isSelected()) {
                    fromAboveCheckBox.setSelected(true);
                }
            });

            fromAboveCheckBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.DESELECTED && !pointsCheckBox.isSelected()) {
                    pointsCheckBox.setSelected(true);
                }
            });

            JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            typePanel.add(pointsCheckBox);
            typePanel.add(fromAboveCheckBox);

            // Basic Filter Row (Years & Confidence)
            minYearField = createCompactTextField();
            maxYearField = createCompactTextField();
            confidenceCombo = new JComboBox<>(new String[]{"Any", "high", "medium", "low"});

            JPanel basicFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            basicFilterPanel.add(new JLabel(I18n.tr("Min Year:")));
            basicFilterPanel.add(minYearField);
            basicFilterPanel.add(new JLabel(I18n.tr("Max Year:")));
            basicFilterPanel.add(maxYearField);
            basicFilterPanel.add(new JLabel(I18n.tr("Confidence:")));
            basicFilterPanel.add(confidenceCombo);

            // Advanced ID Filters Sub-Panel
            imageIdField = createCompactTextField();
            sourceIdField = createCompactTextField();
            collectionIdField = createCompactTextField();
            subjectIdField = createCompactTextField();
            georeferencedByField = createCompactTextField();

            JPanel entityPanel = new JPanel(new GridBagLayout());
            entityPanel.setBorder(BorderFactory.createTitledBorder(I18n.tr("Filter by IDs")));

            entityPanel.add(new JLabel(I18n.tr("Image ID:")), GBC.std().insets(4, 4, 4, 4));
            entityPanel.add(imageIdField, GBC.std().insets(4, 4, 4, 12));
            entityPanel.add(new JLabel(I18n.tr("Source ID:")), GBC.std().insets(4, 4, 4, 4));
            entityPanel.add(sourceIdField, GBC.eol().insets(4, 4, 4, 4));

            entityPanel.add(new JLabel(I18n.tr("Collection ID:")), GBC.std().insets(4, 4, 4, 4));
            entityPanel.add(collectionIdField, GBC.std().insets(4, 4, 4, 12));
            entityPanel.add(new JLabel(I18n.tr("Subject ID:")), GBC.std().insets(4, 4, 4, 4));
            entityPanel.add(subjectIdField, GBC.eol().insets(4, 4, 4, 4));

            entityPanel.add(new JLabel(I18n.tr("OSM User ID:")), GBC.std().insets(4, 4, 4, 4));
            entityPanel.add(georeferencedByField, GBC.eol().fill(GBC.HORIZONTAL).insets(4, 4, 4, 4));

            // Main Panel Assembly
            add(infoLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
            add(typePanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));
            add(basicFilterPanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));
            add(entityPanel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));

            restoreSettings();
        }

        private JTextField createCompactTextField() {
            JTextField field = new JTextField(6);
            Dimension d = new Dimension(80, 26);
            field.setPreferredSize(d);
            field.setMinimumSize(d);
            return field;
        }

        public boolean isPointsSelected() {
            return pointsCheckBox.isSelected();
        }

        public boolean isFromAboveSelected() {
            return fromAboveCheckBox.isSelected();
        }

        public Map<String, String> getFilterMap() {
            Map<String, String> filters = new HashMap<>();

            putIfNotEmpty(filters, "year_min", minYearField.getText());
            putIfNotEmpty(filters, "year_max", maxYearField.getText());

            String conf = (String) confidenceCombo.getSelectedItem();
            if (conf != null && !conf.equalsIgnoreCase("Any")) {
                filters.put("confidence", conf.toLowerCase());
            }

            putIfNotEmpty(filters, "image", imageIdField.getText());
            putIfNotEmpty(filters, "source", sourceIdField.getText());
            putIfNotEmpty(filters, "collection", collectionIdField.getText());
            putIfNotEmpty(filters, "subject", subjectIdField.getText());
            putIfNotEmpty(filters, "georeferenced_by", georeferencedByField.getText());

            return filters;
        }

        private void putIfNotEmpty(Map<String, String> map, String key, String value) {
            if (value != null && !value.trim().isEmpty()) {
                map.put(key, value.trim());
            }
        }

        @Override
        public String getSimpleName() {
            return "yesterdays";
        }

        @Override
        public boolean checkDownload(DownloadSettings settings) {
            if (!pointsCheckBox.isSelected() && !fromAboveCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(
                    this,
                    I18n.tr("Please select at least one georeference type."),
                    I18n.tr("Selection Required"),
                    JOptionPane.WARNING_MESSAGE
                );
                return false;
            }
            rememberSettings();
            return true;
        }

        @Override
        public void restoreSettings() {
            pointsCheckBox.setSelected(Config.getPref().getBoolean("yesterdays.fetch_points", true));
            fromAboveCheckBox.setSelected(Config.getPref().getBoolean("yesterdays.fetch_from_above", true));
            minYearField.setText(Config.getPref().get("yesterdays.year_min", ""));
            maxYearField.setText(Config.getPref().get("yesterdays.year_max", ""));
            confidenceCombo.setSelectedItem(Config.getPref().get("yesterdays.confidence", "Any"));

            imageIdField.setText(Config.getPref().get("yesterdays.filter_image", ""));
            sourceIdField.setText(Config.getPref().get("yesterdays.filter_source", ""));
            collectionIdField.setText(Config.getPref().get("yesterdays.filter_collection", ""));
            subjectIdField.setText(Config.getPref().get("yesterdays.filter_subject", ""));
            georeferencedByField.setText(Config.getPref().get("yesterdays.filter_georeferenced_by", ""));
        }

        @Override
        public void rememberSettings() {
            Config.getPref().putBoolean("yesterdays.fetch_points", pointsCheckBox.isSelected());
            Config.getPref().putBoolean("yesterdays.fetch_from_above", fromAboveCheckBox.isSelected());
            Config.getPref().put("yesterdays.year_min", minYearField.getText().trim());
            Config.getPref().put("yesterdays.year_max", maxYearField.getText().trim());
            Config.getPref().put("yesterdays.confidence", (String) confidenceCombo.getSelectedItem());

            Config.getPref().put("yesterdays.filter_image", imageIdField.getText().trim());
            Config.getPref().put("yesterdays.filter_source", sourceIdField.getText().trim());
            Config.getPref().put("yesterdays.filter_collection", collectionIdField.getText().trim());
            Config.getPref().put("yesterdays.filter_subject", subjectIdField.getText().trim());
            Config.getPref().put("yesterdays.filter_georeferenced_by", georeferencedByField.getText().trim());
        }

        @Override
        public YesterdaysDownloadSource getData() {
            return this.source;
        }
    }
}