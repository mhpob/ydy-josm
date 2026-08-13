# ydy-josm

ydy-josm is a plugin for JOSM (Java OpenStreetMap Editor) that displays historical photographs from the Yesterdays REST API.

## Prerequisites
 - JOSM: Ensure you are running a recent version of JOSM.
 - ImageIO Plugin: Because the Yesterdays API serves thumbnails in .webp format, JOSM requires the official ImageIO plugin display them.

### Enabling the ImageIO Plugin

 - Open JOSM and navigate to Edit -> Preferences (or press F12).
 - Click on the Plugins tab and click Download List.
 - Search for ImageIO, check the box to enable it, and click OK.
 - Restart JOSM.
 - Go back to Edit -> Preferences and select the Imagery tab.
 - Click the ImageIO tab and ensure webp is checked. Click OK and restart JOSM.