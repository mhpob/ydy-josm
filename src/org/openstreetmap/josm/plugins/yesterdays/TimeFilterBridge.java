package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFilterBridge implements PreferenceChangedListener {

    private static final int DEBOUNCE_MS = 400;
    private static TimeFilterBridge instance;
    private final Timer debounceTimer;

    private Integer activeMinYear = null;
    private Integer activeMaxYear = null;
    private boolean isFilterActive = false;

    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\s*([+-]?\\d{1,6})");

    public static synchronized void initialize(MapFrame mapFrame) {
        if (mapFrame == null) return;

        if (instance == null) {
            instance = new TimeFilterBridge();
            Config.getPref().addPreferenceChangeListener(instance);
            
            SwingUtilities.invokeLater(() -> {
                Component dialog = findTimeFilterComponent(mapFrame);
                if (dialog == null && MainApplication.getMainFrame() != null) {
                    dialog = findTimeFilterComponent(MainApplication.getMainFrame());
                }
                if (dialog instanceof Container) {
                    hookDialogButtons((Container) dialog);
                }
            });

            System.out.println("Yesterdays: Connected to OHM Time Filter via preferences and UI button hooks.");
        }
    }

    public TimeFilterBridge() {
        this.debounceTimer = new Timer(DEBOUNCE_MS, e -> triggerTimeFilteredReload());
        this.debounceTimer.setRepeats(false);
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e != null && e.getKey() != null && e.getKey().startsWith("OHM_Time_Filter")) {
            SwingUtilities.invokeLater(this::checkAndTriggerReload);
        }
    }

    public synchronized void checkAndTriggerReload() {
        try {
            String setPoint = Config.getPref().get("OHM_Time_Filter.set_point", null);
            int offsetDays = Config.getPref().getInt("OHM_Time_Filter.offset_days", 0);

            Integer newMinYear = null;
            Integer newMaxYear = null;

            boolean activeState = (setPoint != null && !setPoint.trim().isEmpty());

            if (activeState) {
                Matcher matcher = YEAR_PATTERN.matcher(setPoint.trim());
                if (matcher.find()) {
                    int baseYear = Integer.parseInt(matcher.group(1));
                    int yearOffset = Math.max(0, (int) Math.ceil(offsetDays / 365.25));
                    newMinYear = baseYear - yearOffset;
                    newMaxYear = baseYear + yearOffset;
                }
            }

            if (this.isFilterActive != activeState ||
                !Objects.equals(this.activeMinYear, newMinYear) ||
                !Objects.equals(this.activeMaxYear, newMaxYear)) {

                boolean wasActiveBefore = this.isFilterActive;
                this.isFilterActive = activeState;
                this.activeMinYear = activeState ? newMinYear : null;
                this.activeMaxYear = activeState ? newMaxYear : null;

                System.out.println("Yesterdays: Filter sync -> active=" 
                    + activeState + ", range=[" + this.activeMinYear + ".." + this.activeMaxYear + "]");

                if (wasActiveBefore && !activeState) {
                    debounceTimer.stop();
                    triggerTimeFilteredReload();
                } else {
                    debounceTimer.restart();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Component findTimeFilterComponent(Container container) {
        if (container == null) return null;
        for (Component comp : container.getComponents()) {
            if (comp.getClass().getName().toLowerCase().contains("timefilter")) {
                return comp;
            }
            if (comp instanceof Container) {
                Component found = findTimeFilterComponent((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void hookDialogButtons(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String text = btn.getText();
                if ("Apply".equals(text)) {
                    btn.addActionListener(e -> {
                        if (instance != null) {
                            System.out.println("Yesterdays: OHM Apply clicked. Forcing reload.");
                            SwingUtilities.invokeLater(() -> {
                                instance.checkAndTriggerReload();
                                // Force reload even if bounds haven't changed mathematically
                                instance.debounceTimer.restart();
                            });
                        }
                    });
                } else if ("Clear".equals(text)) {
                    btn.addActionListener(e -> {
                        if (instance != null) {
                            instance.isFilterActive = false;
                            instance.activeMinYear = null;
                            instance.activeMaxYear = null;
                            System.out.println("Yesterdays: OHM Clear clicked. Resetting photo filters.");
                            instance.debounceTimer.stop();
                            instance.triggerTimeFilteredReload();
                        }
                    });
                }
            }
            if (comp instanceof Container) {
                hookDialogButtons((Container) comp);
            }
        }
    }

    public static Map<String, String> getActiveFilters() {
        Map<String, String> filters = new HashMap<>();
        if (instance != null && instance.isFilterActive) {
            if (instance.activeMinYear != null) {
                filters.put("year_min", String.valueOf(instance.activeMinYear));
            }
            if (instance.activeMaxYear != null) {
                filters.put("year_max", String.valueOf(instance.activeMaxYear));
            }
        }
        return filters;
    }

    private void triggerTimeFilteredReload() {
        if (MainApplication.getMap() == null || MainApplication.getMap().mapView == null) {
            return;
        }

        MapView mv = MainApplication.getMap().mapView;
        Bounds bounds = mv.getLatLonBounds(mv.getBounds());

        Map<String, String> filters = getActiveFilters();

        System.out.println("Yesterdays: Executing API reload with filters: " + filters);
        YesterdaysPlugin.loadImagesForBoundsAsync(bounds, filters, true, true, true);
    }
}