package org.openstreetmap.josm.plugins.yesterdays;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class YesterdaysPreferenceSetting implements SubPreferenceSetting {

    public static final String PREF_BASE_URL = "yesterdays.baseurl";
    public static final String DEFAULT_BASE_URL = "https://yesterdays.maprva.org";

    private JTextField baseUrlField;

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getPluginPreference();
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new JPanel(new GridBagLayout());

        JLabel label = new JLabel(I18n.tr("API Base URL:"));
        String currentUrl = Config.getPref().get(PREF_BASE_URL, DEFAULT_BASE_URL);
        baseUrlField = new JTextField(currentUrl, 30);

        panel.add(label, GBC.std().insets(0, 5, 5, 5));
        panel.add(baseUrlField, GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 5, 5));

        gui.getPluginPreference().addSubTab(this, I18n.tr("Yesterdays"), panel);
    }

    @Override
    public boolean ok() {
        String url = baseUrlField.getText().trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.isEmpty()) {
            url = DEFAULT_BASE_URL;
        }
        Config.getPref().put(PREF_BASE_URL, url);
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }
}