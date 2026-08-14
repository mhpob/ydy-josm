package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.swing.*;
import java.awt.event.ActionEvent;
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
    }

    /**
     * Registers the bundled TwelveMonkeys webp reader, unless another provider
     * (e.g. the ImageIO plugin) has already made webp readable.
     */
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
        if (newFrame != null) {
            newFrame.addToggleDialog(YesterdaysInfoPanel.getInstance());

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
        loadImagesForBoundsAsync(bounds);
    }

    public void loadImagesForBoundsAsync(Bounds bounds) {
        PleaseWaitRunnable task = new PleaseWaitRunnable("Loading Yesterdays Photos") {
            private List<YesterdaysImage> allImages = new ArrayList<>();
            private boolean success = false;

            @Override
            protected void realRun() {
                String initialUrl = "https://yesterdays.maprva.org/api/v2/georeferences/?in_bbox=" + 
                                    bounds.getMinLon() + "," + 
                                    bounds.getMinLat() + "," + 
                                    bounds.getMaxLon() + "," + 
                                    bounds.getMaxLat();
                                    
                String currentUrl = initialUrl;
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

    private String extractNextUrl(String json) {
        Matcher m = Pattern.compile("\"next\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (m.find()) {
            String next = m.group(1);
            if (next != null && !next.equals("null") && !next.isEmpty()) {
                return next;
            }
        }
        return null;
    }

    private List<YesterdaysImage> parseImagesFromJson(String json) {
        List<YesterdaysImage> imageList = new ArrayList<>();
        try {
            Matcher matcher = Pattern.compile("\\{\\s*\"id\":\\s*(\\d+),.*?\"geometry\":\\s*\\{.*?\"coordinates\":\\s*\\[([\\d\\.\\-]+),\\s*([\\d\\.\\-]+)\\].*?\"properties\":\\s*\\{(.*?)\\}\\s*\\}", Pattern.DOTALL).matcher(json);

            while (matcher.find()) {
                String featureId = matcher.group(1);
                double lon = Double.parseDouble(matcher.group(2));
                double lat = Double.parseDouble(matcher.group(3));
                String propertiesBlock = matcher.group(4);

                LatLon latLon = new LatLon(lat, lon);

                // Extract the true image_id from the properties block (with fallback to featureId)
                String imageId = featureId;
                Matcher imgIdMatcher = Pattern.compile("\"image_id\":\\s*\"?([^\",}]+)\"?").matcher(propertiesBlock);
                if (imgIdMatcher.find()) {
                    imageId = imgIdMatcher.group(1);
                }

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
                
                YesterdaysImage img = new YesterdaysImage(latLon, title, thumbnailUrl, imageId, direction);
                imageList.add(img);
            }

            System.out.println("Successfully parsed " + imageList.size() + " image features with correct image_ids.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageList;
    }
}