package org.openstreetmap.josm.plugins.yesterdays;

import org.openstreetmap.josm.data.coor.LatLon;

public class YesterdaysImage {
    private final LatLon coordinates;
    private final String title;
    private final String thumbnailUrl;
    private final String imageId;
    private final int direction;
    
    // Detailed fields fetched on click
    private String dateDisplay;
    private String license;
    private String description;
    private String originalUrl;

    public YesterdaysImage(LatLon coordinates, String title, String thumbnailUrl, String imageId, int direction) {
        this.coordinates = coordinates;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.imageId = imageId;
        this.direction = direction;
    }

    public LatLon getCoordinates() { return coordinates; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getImageId() { return imageId; }
    public int getDirection() { return direction; }

    public String getDateDisplay() { return dateDisplay; }
    public void setDateDisplay(String dateDisplay) { this.dateDisplay = dateDisplay; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
}