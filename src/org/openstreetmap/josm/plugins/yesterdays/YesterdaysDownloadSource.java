package org.openstreetmap.josm.plugins.yesterdays;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.JCheckBox;
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
            Integer minYear = lastCreatedPanel.getMinYear();
            Integer maxYear = lastCreatedPanel.getMaxYear();

            YesterdaysPlugin.loadImagesForBoundsAsync(bounds, minYear, maxYear, fetchPoints, fetchFromAbove);
        }
    }

    private static Integer parseYear(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class YesterdaysPanel extends AbstractDownloadSourcePanel<YesterdaysDownloadSource> {
        private final YesterdaysDownloadSource source;
        private final JCheckBox pointsCheckBox;
        private final JCheckBox fromAboveCheckBox;
        private final JTextField minYearField;
        private final JTextField maxYearField;

        public YesterdaysPanel(YesterdaysDownloadSource source) {
            super(source);
            this.source = source;

            setLayout(new GridBagLayout());

            JLabel infoLabel = new JLabel(I18n.tr("Download historical photos from Yesterdays for the selected area."));

            pointsCheckBox = new JCheckBox(I18n.tr("Point Georeferences"), true);
            fromAboveCheckBox = new JCheckBox(I18n.tr("From-Above Georeferences"), true);

            // Enforce having at least one checkbox selected
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

            JLabel minLabel = new JLabel(I18n.tr("Min Year:"));
            minYearField = new JTextField(8);

            JLabel maxLabel = new JLabel(I18n.tr("Max Year:"));
            maxYearField = new JTextField(8);

            Dimension fieldSize = new Dimension(100, 28);
            minYearField.setPreferredSize(fieldSize);
            minYearField.setMinimumSize(fieldSize);
            maxYearField.setPreferredSize(fieldSize);
            maxYearField.setMinimumSize(fieldSize);

            JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            yearPanel.add(minLabel);
            yearPanel.add(minYearField);
            yearPanel.add(maxLabel);
            yearPanel.add(maxYearField);

            add(infoLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 5));
            add(typePanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));
            add(yearPanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));

            restoreSettings();
        }

        public boolean isPointsSelected() {
            return pointsCheckBox.isSelected();
        }

        public boolean isFromAboveSelected() {
            return fromAboveCheckBox.isSelected();
        }

        public Integer getMinYear() {
            return parseYear(minYearField.getText());
        }

        public Integer getMaxYear() {
            return parseYear(maxYearField.getText());
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
        }

        @Override
        public void rememberSettings() {
            Config.getPref().putBoolean("yesterdays.fetch_points", pointsCheckBox.isSelected());
            Config.getPref().putBoolean("yesterdays.fetch_from_above", fromAboveCheckBox.isSelected());
            Config.getPref().put("yesterdays.year_min", minYearField.getText().trim());
            Config.getPref().put("yesterdays.year_max", maxYearField.getText().trim());
        }

        @Override
        public YesterdaysDownloadSource getData() {
            return this.source;
        }
    }
}