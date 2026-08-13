package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.coor.LatLon;

public class YesterdaysImage {
    private final LatLon coordinates;
    private final String title;
    private final String thumbnailUrl;
    private final String uuid;
    private final int direction;

    public YesterdaysImage(LatLon coordinates, String title, String thumbnailUrl, String uuid, int direction) {
        this.coordinates = coordinates;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.uuid = uuid;
        this.direction = direction;
    }

    public LatLon getCoordinates() { return coordinates; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getUuid() { return uuid; }
    public int getDirection() { return direction; }
}