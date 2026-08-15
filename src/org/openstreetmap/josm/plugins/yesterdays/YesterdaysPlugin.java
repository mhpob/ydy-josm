package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.*;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YesterdaysPlugin extends Plugin {
    
    public YesterdaysPlugin(PluginInformation info) {
        super(info);
        ensureWebpSupport();
        DownloadDialog.addDownloadSource(new YesterdaysDownloadSource());
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new YesterdaysPreferenceSetting();
    }

    private static void ensureWebpSupport() {
        try {
            if (!ImageIO.getImageReadersByMIMEType("image/webp").hasNext()) {
                IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
                System.out.println("Yesterdays: registered bundled webp image reader.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        super.mapFrameInitialized(oldFrame, newFrame);
        if (newFrame != null && newFrame.mapView != null) {
            newFrame.addToggleDialog(YesterdaysInfoPanel.getInstance());

            newFrame.mapView.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                        MapView mv = newFrame.mapView;
                        Point clickPoint = e.getPoint();

                        for (YesterdaysLayer layer : MainApplication.getLayerManager().getLayersOfType(YesterdaysLayer.class)) {
                            if (layer.isVisible()) {
                                YesterdaysImage hitImage = layer.getImageAtPoint(clickPoint, mv);
                                if (hitImage != null) {
                                    SwingUtilities.invokeLater(() -> {
                                        YesterdaysInfoPanel panel = YesterdaysInfoPanel.getInstance();
                                        panel.displayImage(hitImage);
                                        panel.setVisible(true);
                                        panel.requestFocusInWindow();
                                    });
                                    mv.repaint();
                                    break;
                                }
                            }
                        }
                    }
                }
            });

            JMenu dataMenu = MainApplication.getMenu().dataMenu;
            if (dataMenu != null) {
                boolean alreadyAdded = false;
                for (int i = 0; i < dataMenu.getItemCount(); i++) {
                    JMenuItem item = dataMenu.getItem(i);
                    if (item != null && "Load Yesterdays Photos".equals(item.getText())) {
                        alreadyAdded = true;
                        break;
                    }
                }
                
                if (!alreadyAdded) {
                    dataMenu.add(new AbstractAction("Load Yesterdays Photos") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            triggerLoadPhotos();
                        }
                    });
                }
            }
        }
    }

    private void triggerLoadPhotos() {
        if (MainApplication.getMap() == null || MainApplication.getMap().mapView == null) {
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Please open a map view first.",
                "Yesterdays",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        MapView mv = MainApplication.getMap().mapView;
        Bounds bounds = mv.getLatLonBounds(mv.getBounds());
        
        System.out.println("Fetching Yesterdays photos for bounds: " + bounds);
        loadImagesForBoundsAsync(bounds, null, null);
    }

    public static void loadImagesForBoundsAsync(Bounds bounds) {
        loadImagesForBoundsAsync(bounds, null, null);
    }

    public static void loadImagesForBoundsAsync(Bounds bounds, Integer yearMin, Integer yearMax) {
        PleaseWaitRunnable task = new PleaseWaitRunnable("Loading Yesterdays Photos") {
            private List<YesterdaysImage> allImages = new ArrayList<>();
            private boolean success = false;

            @Override
            protected void realRun() {
                String baseUrl = Config.getPref().get(
                    YesterdaysPreferenceSetting.PREF_BASE_URL, 
                    YesterdaysPreferenceSetting.DEFAULT_BASE_URL
                );
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                StringBuilder urlBuilder = new StringBuilder(baseUrl)
                    .append("/api/v2/georeferences/?in_bbox=")
                    .append(bounds.getMinLon()).append(",")
                    .append(bounds.getMinLat()).append(",")
                    .append(bounds.getMaxLon()).append(",")
                    .append(bounds.getMaxLat());

                if (yearMin != null) {
                    urlBuilder.append("&year_min=").append(yearMin);
                }
                if (yearMax != null) {
                    urlBuilder.append("&year_max=").append(yearMax);
                }

                String currentUrl = urlBuilder.toString();
                int page = 1;

                try {
                    while (currentUrl != null && !currentUrl.isEmpty()) {
                        if (progressMonitor.isCanceled()) {
                            break;
                        }

                        progressMonitor.subTask("Loading page " + page + " (" + allImages.size() + " items collected)...");

                        URL url = new URL(currentUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setRequestProperty("Accept", "application/json");

                        if (conn.getResponseCode() != 200) {
                            System.err.println("API request failed with HTTP code: " + conn.getResponseCode());
                            break;
                        }

                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                        );
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        reader.close();

                        String jsonResponse = responseBuilder.toString();

                        List<YesterdaysImage> pageImages = parseImagesFromJson(jsonResponse);
                        if (pageImages.isEmpty()) {
                            break;
                        }
                        allImages.addAll(pageImages);

                        currentUrl = extractNextUrl(jsonResponse);
                        page++;

                        if (page > 50) {
                            System.out.println("Pagination safety limit reached.");
                            break;
                        }
                    }
                    success = !allImages.isEmpty();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected void finish() {
                if (success) {
                    YesterdaysLayer layer = new YesterdaysLayer(allImages);
                    MainApplication.getLayerManager().addLayer(layer);
                    System.out.println("Successfully added layer with " + allImages.size() + " total images.");
                } else if (!progressMonitor.isCanceled()) {
                    JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        "No historical photos found in this view area.",
                        "Yesterdays",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }

            @Override
            protected void cancel() {
                System.out.println("User canceled photo loading.");
            }
        };

        MainApplication.worker.submit(task);
    }

    public static void loadDetailsForImageAsync(YesterdaysImage image, Runnable onComplete) {
        MainApplication.worker.submit(() -> {
            try {
                String baseUrl = Config.getPref().get(
                    YesterdaysPreferenceSetting.PREF_BASE_URL, 
                    YesterdaysPreferenceSetting.DEFAULT_BASE_URL
                );
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                String detailsUrl = baseUrl + "/api/v2/images/" + image.getImageId() + "/";
                URL url = new URL(detailsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    String json = sb.toString();

                    Matcher dateMatcher = Pattern.compile("\"date_display\"\\s*:\\s*(?:\"([^\"]*)\"|null)").matcher(json);
                    if (dateMatcher.find() && dateMatcher.group(1) != null) {
                        image.setDateDisplay(dateMatcher.group(1));
                    } else {
                        image.setDateDisplay("Unknown");
                    }

                    Matcher licenseMatcher = Pattern.compile("\"license\"\\s*:\\s*(?:\"([^\"]*)\"|null)").matcher(json);
                    if (licenseMatcher.find() && licenseMatcher.group(1) != null) {
                        image.setLicense(licenseMatcher.group(1));
                    } else {
                        image.setLicense("None specified");
                    }

                    Matcher descMatcher = Pattern.compile("\"description\"\\s*:\\s*(?:\"([^\"]*)\"|null)").matcher(json);
                    if (descMatcher.find() && descMatcher.group(1) != null) {
                        image.setDescription(descMatcher.group(1));
                    }

                    Matcher origUrlMatcher = Pattern.compile("\"original_url\"\\s*:\\s*(?:\"([^\"]*)\"|null)").matcher(json);
                    if (origUrlMatcher.find() && origUrlMatcher.group(1) != null) {
                        image.setOriginalUrl(origUrlMatcher.group(1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }
            }
        });
    }

    private static String extractNextUrl(String json) {
        Matcher m = Pattern.compile("\"next\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (m.find()) {
            String next = m.group(1);
            if (next != null && !next.equals("null") && !next.isEmpty()) {
                return next;
            }
        }
        return null;
    }

    private static List<YesterdaysImage> parseImagesFromJson(String json) {
        List<YesterdaysImage> imageList = new ArrayList<>();
        try {
            Matcher matcher = Pattern.compile("\\{\\s*\"id\":\\s*(\\d+),.*?\"geometry\":\\s*\\{.*?\"coordinates\":\\s*\\[([\\d\\.\\-]+),\\s*([\\d\\.\\-]+)\\].*?\"properties\":\\s*\\{(.*?)\\}\\s*\\}", Pattern.DOTALL).matcher(json);

            while (matcher.find()) {
                String featureId = matcher.group(1);
                double lon = Double.parseDouble(matcher.group(2));
                double lat = Double.parseDouble(matcher.group(3));
                String propertiesBlock = matcher.group(4);

                LatLon latLon = new LatLon(lat, lon);

                String imageId = featureId;
                Matcher imgIdMatcher = Pattern.compile("\"image_id\":\\s*\"?([^\",}]+)\"?").matcher(propertiesBlock);
                if (imgIdMatcher.find()) {
                    imageId = imgIdMatcher.group(1);
                }

                String title = "Untitled";
                Matcher titleMatcher = Pattern.compile("\"image_title\":\\s*\"([^\"]*)\"").matcher(propertiesBlock);
                if (titleMatcher.find()) {
                    title = titleMatcher.group(1);
                }

                String thumbnailUrl = "";
                Matcher thumbMatcher = Pattern.compile("\"image_thumbnail\":\\s*\"([^\"]*)\"").matcher(propertiesBlock);
                if (thumbMatcher.find()) {
                    thumbnailUrl = thumbMatcher.group(1);
                }

                int direction = 0;
                Matcher dirMatcher = Pattern.compile("\"direction\":\\s*([\\d\\.]+)").matcher(propertiesBlock);
                if (dirMatcher.find()) {
                    direction = (int) Double.parseDouble(dirMatcher.group(1));
                }
                
                YesterdaysImage img = new YesterdaysImage(latLon, title, thumbnailUrl, imageId, direction);
                imageList.add(img);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageList;
    }
}