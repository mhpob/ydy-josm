package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.HttpClient;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YesterdaysPlugin extends Plugin {
    
    public YesterdaysPlugin(PluginInformation info) {
        super(info);
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        super.mapFrameInitialized(oldFrame, newFrame);
        if (newFrame != null) {
            // 1. Register and dock the sidebar toggle dialog
            newFrame.addToggleDialog(YesterdaysInfoPanel.getInstance());

            // 2. Add the "Load Yesterdays Photos" option to JOSM's Data menu
            JMenu dataMenu = MainApplication.getMenu().dataMenu;
            if (dataMenu != null) {
                dataMenu.add(new AbstractAction("Load Yesterdays Photos") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fetchPhotosForCurrentView();
                    }
                });
            }
        }
    }

    private void fetchPhotosForCurrentView() {
        if (MainApplication.getMap() == null || MainApplication.getMap().mapView == null) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), 
                "Please open a map view first.", "Yesterdays", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the bounding box of your current map view window
        MapView mv = MainApplication.getMap().mapView;
        Bounds bounds = mv.getLatLonBounds(mv.getBounds());
        
        // Run network fetch in a background thread to prevent freezing the UI
        new Thread(() -> {
            try {
                String urlString = String.format("https://yesterdays.maprva.org/api/v2/georeferences/?in_bbox=%f,%f,%f,%f",
                        bounds.getMinLon(), bounds.getMinLat(), bounds.getMaxLon(), bounds.getMaxLat());

                System.out.println("Fetching photos from: " + urlString);

                HttpClient client = HttpClient.create(new URL(urlString));
                HttpClient.Response response = client.connect();

                if (response.getResponseCode() == 200) {
                    try (InputStream in = response.getContent()) {
                        List<YesterdaysImage> images = parseImagesFromJson(in);

                        // Add the layer to JOSM on the Event Dispatch Thread
                        SwingUtilities.invokeLater(() -> {
                            YesterdaysLayer layer = new YesterdaysLayer(images);
                            MainApplication.getLayerManager().addLayer(layer);
                            System.out.println("Successfully loaded " + images.size() + " photos into layer.");
                        });
                    }
                } else {
                    System.err.println("API request failed with HTTP status: " + response.getResponseCode());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private List<YesterdaysImage> parseImagesFromJson(InputStream in) {
        List<YesterdaysImage> imageList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();

            // Simple, robust regex-based extraction for GeoJSON features to avoid external library dependencies
            // Matches individual feature blocks inside the FeatureCollection
            Pattern featurePattern = Pattern.compile("\\\\?\\{\\s*\"id\":\\s*(\\d+),.*?(?=\\}\\s*,\\s*\\{|$)", Pattern.DOTALL);
            
            // Alternatively, split by feature objects or extract properties directly using lightweight patterns
            // Let's use a simpler pattern matching loop over individual features:
            Matcher matcher = Pattern.compile("\\{\\s*\"id\":\\s*(\\d+),.*?\"geometry\":\\s*\\{.*?\"coordinates\":\\s*\\[([\\d\\.\\-]+),\\s*([\\d\\.\\-]+)\\].*?\"properties\":\\s*\\{(.*?)\\}\\s*\\}", Pattern.DOTALL).matcher(json);

            int count = 0;
            while (matcher.find()) {
                String id = matcher.group(1);
                double lon = Double.parseDouble(matcher.group(2));
                double lat = Double.parseDouble(matcher.group(3));
                String propertiesBlock = matcher.group(4);

                LatLon latLon = new LatLon(lat, lon);

                // Extract title
                String title = "Untitled";
                Matcher titleMatcher = Pattern.compile("\"image_title\":\\s*\"([^\"]*)\"").matcher(propertiesBlock);
                if (titleMatcher.find()) {
                    title = titleMatcher.group(1);
                }

                // Extract thumbnail URL
                String thumbnailUrl = "";
                Matcher thumbMatcher = Pattern.compile("\"image_thumbnail\":\\s*\"([^\"]*)\"").matcher(propertiesBlock);
                if (thumbMatcher.find()) {
                    thumbnailUrl = thumbMatcher.group(1);
                }

                // Extract direction
                int direction = 0;
                Matcher dirMatcher = Pattern.compile("\"direction\":\\s*([\\d\\.]+)").matcher(propertiesBlock);
                if (dirMatcher.find()) {
                    direction = (int) Double.parseDouble(dirMatcher.group(1));
                }
                
                YesterdaysImage img = new YesterdaysImage(latLon, title, thumbnailUrl, id, direction);
                imageList.add(img);
                count++;
            }

            System.out.println("Successfully parsed " + imageList.size() + " image features using robust pattern matching.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageList;
    }
}