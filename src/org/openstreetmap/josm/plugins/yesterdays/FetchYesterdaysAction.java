package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchYesterdaysAction extends JosmAction {

    public FetchYesterdaysAction() {
        super("Download Yesterdays Photos", null, "Fetch historical photos for the current view", null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (MainApplication.getMap() == null || MainApplication.getMap().mapView == null) return;
        Bounds bounds = MainApplication.getMap().mapView.getRealBounds();
        
        String bbox = String.format("%f,%f,%f,%f", 
            bounds.getMin().lon(), bounds.getMin().lat(), 
            bounds.getMax().lon(), bounds.getMax().lat());

        String apiUrl = "https://yesterdays.maprva.org/api/v2/georeferences/?in_bbox=" + bbox;

        new Thread(() -> {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/geo+json, application/json");

                if (conn.getResponseCode() == 200) {
                    List<YesterdaysImage> fetchedImages = new ArrayList<>();
                    
                    StringBuilder jsonResponse = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            jsonResponse.append(line);
                        }
                    }

                    String responseStr = jsonResponse.toString();
                    String[] features = responseStr.split("\\},\\s*\\{\\s*\"id\"");

                    Pattern coordPattern = Pattern.compile("\\[\\s*([+-]?\\d*\\.\\d+)\\s*,\\s*([+-]?\\d*\\.\\d+)\\s*\\]");
                    Pattern titlePattern = Pattern.compile("\"image_title\"\\s*:\\s*\"([^\"]*)\"");
                    Pattern idPattern = Pattern.compile("\"image_id\"\\s*:\\s*([0-9]+)");
                    Pattern thumbPattern = Pattern.compile("\"image_thumbnail\"\\s*:\\s*\"([^\"]*)\"");

                    for (String feat : features) {
                        Matcher mCoord = coordPattern.matcher(feat);
                        if (mCoord.find()) {
                            double lon = Double.parseDouble(mCoord.group(1));
                            double lat = Double.parseDouble(mCoord.group(2));

                            String title = "Untitled";
                            Matcher mTitle = titlePattern.matcher(feat);
                            if (mTitle.find()) {
                                title = mTitle.group(1);
                            }

                            String uuid = "";
                            Matcher mId = idPattern.matcher(feat);
                            if (mId.find()) {
                                uuid = mId.group(1);
                            }

                            String thumbUrl = "";
                            Matcher mThumb = thumbPattern.matcher(feat);
                            if (mThumb.find()) {
                                thumbUrl = mThumb.group(1);
                            }

                            fetchedImages.add(new YesterdaysImage(new LatLon(lat, lon), title, thumbUrl, uuid));
                        }
                    }

                    if (!fetchedImages.isEmpty()) {
                        YesterdaysLayer layer = new YesterdaysLayer(fetchedImages);
                        javax.swing.SwingUtilities.invokeLater(() -> 
                            MainApplication.getLayerManager().addLayer(layer)
                        );
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}