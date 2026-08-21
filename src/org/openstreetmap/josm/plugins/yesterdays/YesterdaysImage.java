package org.openstreetmap.josm.plugins.yesterdays;

import java.awt.image.BufferedImage;
import java.util.List;
import org.openstreetmap.josm.data.coor.LatLon;

public class YesterdaysImage {
    private final LatLon coordinates;
    private final List<LatLon> polygon;
    private final boolean fromAbove;
    private final String title;
    private final String thumbnailUrl;
    private final String imageId;
    private final int direction;

    private String dateDisplay;
    private String license;
    private String licenseName;
    private String sourceName;
    private String description;
    private String originalUrl;

    private BufferedImage cachedThumbnail;
    private boolean loadingThumbnail = false;

    public YesterdaysImage(LatLon coordinates, String title, String thumbnailUrl, String imageId, int direction) {
        this(coordinates, null, false, title, thumbnailUrl, imageId, direction);
    }

    public YesterdaysImage(LatLon coordinates, List<LatLon> polygon, boolean fromAbove, String title, String thumbnailUrl, String imageId, int direction) {
        this.coordinates = coordinates;
        this.polygon = polygon;
        this.fromAbove = fromAbove;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.imageId = imageId;
        this.direction = direction;
    }

    public LatLon getCoordinates() { return coordinates; }
    public LatLon getLatLon() { return coordinates; }
    public List<LatLon> getPolygon() { return polygon; }
    public boolean isFromAbove() { return fromAbove; }
    public String getTitle() { return title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getImageId() { return imageId; }
    public int getDirection() { return direction; }

    public String getDateDisplay() { return dateDisplay; }
    public void setDateDisplay(String dateDisplay) { this.dateDisplay = dateDisplay; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public BufferedImage getCachedThumbnail() { return cachedThumbnail; }
    public void setCachedThumbnail(BufferedImage cachedThumbnail) { this.cachedThumbnail = cachedThumbnail; }

    public boolean isLoadingThumbnail() { return loadingThumbnail; }
    public void setLoadingThumbnail(boolean loadingThumbnail) { this.loadingThumbnail = loadingThumbnail; }
}