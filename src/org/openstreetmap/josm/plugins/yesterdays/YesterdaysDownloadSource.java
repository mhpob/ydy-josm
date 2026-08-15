package org.openstreetmap.josm.plugins.yesterdays;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.List;
import javax.swing.JLabel;
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
        return I18n.tr("MapRVA Yesterdays");
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
            bounds = DownloadDialog.getInstance().getSelectedDownloadArea().orElse(null);
        }

        if (bounds != null) {
            Integer minYear = null;
            Integer maxYear = null;

            if (lastCreatedPanel != null) {
                minYear = lastCreatedPanel.getMinYear();
                maxYear = lastCreatedPanel.getMaxYear();
            } else {
                minYear = parseYear(Config.getPref().get("yesterdays.year_min", ""));
                maxYear = parseYear(Config.getPref().get("yesterdays.year_max", ""));
            }

            YesterdaysPlugin.loadImagesForBoundsAsync(bounds, minYear, maxYear);
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
        private final JTextField minYearField;
        private final JTextField maxYearField;

        public YesterdaysPanel(YesterdaysDownloadSource source) {
            super(source);
            this.source = source;

            setLayout(new GridBagLayout());

            JLabel infoLabel = new JLabel(I18n.tr("Download historical photos from MapRVA Yesterdays for the selected area."));

            JLabel minLabel = new JLabel(I18n.tr("Min Year:"));
            minYearField = new JTextField(8);

            JLabel maxLabel = new JLabel(I18n.tr("Max Year:"));
            maxYearField = new JTextField(8);

            // Explicit dimensions so layout managers cannot shrink text fields
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

            add(infoLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 10, 5));
            add(yearPanel, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 5, 5));

            restoreSettings();
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
            rememberSettings();
            return true;
        }

        @Override
        public void restoreSettings() {
            minYearField.setText(Config.getPref().get("yesterdays.year_min", ""));
            maxYearField.setText(Config.getPref().get("yesterdays.year_max", ""));
        }

        @Override
        public void rememberSettings() {
            Config.getPref().put("yesterdays.year_min", minYearField.getText().trim());
            Config.getPref().put("yesterdays.year_max", maxYearField.getText().trim());
        }

        @Override
        public YesterdaysDownloadSource getData() {
            return this.source;
        }
    }
}