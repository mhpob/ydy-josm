# Yesterdays JOSM Plugin

`yesterdays` is a plugin for JOSM (Java OpenStreetMap Editor) that displays historical photographs from the Yesterdays REST API.

The Yesterdays API serves thumbnails in .webp format. The plugin bundles the pure-Java [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) webp decoder (BSD 3-Clause license), so no additional plugins or configuration are required.

## Install the plugin
 - Ensure you are running a recent version of JOSM.
 - Download `yesterdays.jar` from the [releases page](https://github.com/mhpob/ydy-josm/releases).
 - Place the file directly into your local JOSM plugins directory based on your operating system:
   - Linux: `~/.local/share/JOSM/plugins/`
   - macOS: `~/Library/JOSM/plugins/`
   - Windows: `%APPDATA%\JOSM\plugins\`
 - Restart JOSM.

## Building from source

Run `ant` in the repository root. The build automatically downloads `josm-tested.jar` and the TwelveMonkeys jars into `lib/` and produces `dist/yesterdays.jar`.
