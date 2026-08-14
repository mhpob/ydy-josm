package org.openstreetmap.josm.plugins.yesterdays;

import java.util.List;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.download.AbstractDownloadSourcePanel;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.download.DownloadSettings;
import org.openstreetmap.josm.gui.download.DownloadSource;
import org.openstreetmap.josm.tools.I18n;

public class YesterdaysDownloadSource implements DownloadSource {

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
        return new YesterdaysPanel(this);
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

        // Fallback to retrieving the selected bounding box from the active DownloadDialog instance
        if (bounds == null && DownloadDialog.getInstance() != null) {
            bounds = DownloadDialog.getInstance().getSelectedDownloadArea().orElse(null);
        }

        if (bounds != null) {
            YesterdaysPlugin.loadImagesForBoundsAsync(bounds);
        }
    }

    private static class YesterdaysPanel extends AbstractDownloadSourcePanel<YesterdaysDownloadSource> {
        private final YesterdaysDownloadSource source;

        public YesterdaysPanel(YesterdaysDownloadSource source) {
            super(source);
            this.source = source;
            add(new JLabel(I18n.tr("Download historical photos from MapRVA Yesterdays for the selected area.")));
        }

        @Override
        public String getSimpleName() {
            return "yesterdays";
        }

        @Override
        public boolean checkDownload(DownloadSettings settings) {
            return true;
        }

        @Override
        public void restoreSettings() {
            // No configurable UI inputs to restore
        }

        @Override
        public void rememberSettings() {
            // No configurable UI inputs to save
        }

        @Override
        public YesterdaysDownloadSource getData() {
            return this.source;
        }
    }
}