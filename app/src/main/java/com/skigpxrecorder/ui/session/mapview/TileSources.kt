package com.skigpxrecorder.ui.session.mapview

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex

/**
 * Custom tile source definitions for osmdroid maps
 */
object TileSources {

    /**
     * Standard OpenStreetMap tiles
     */
    val OSM = TileSourceFactory.MAPNIK

    /**
     * OpenTopoMap - Topographic map style
     */
    val TOPO = object : OnlineTileSourceBase(
        "OpenTopoMap",
        0, 17, 256, "",
        arrayOf(
            "https://a.tile.opentopomap.org/",
            "https://b.tile.opentopomap.org/",
            "https://c.tile.opentopomap.org/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) +
                    "/" + MapTileIndex.getX(pMapTileIndex) +
                    "/" + MapTileIndex.getY(pMapTileIndex) + ".png"
        }
    }

    /**
     * OpenSnowMap - Ski piste overlay
     */
    val SNOW = object : OnlineTileSourceBase(
        "OpenSnowMap",
        0, 18, 256, "",
        arrayOf("https://tiles.opensnowmap.org/pistes/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) +
                    "/" + MapTileIndex.getX(pMapTileIndex) +
                    "/" + MapTileIndex.getY(pMapTileIndex) + ".png"
        }
    }

    /**
     * Esri World Imagery - Satellite view
     */
    val SATELLITE = object : OnlineTileSourceBase(
        "EsriWorldImagery",
        0, 19, 256, "",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) +
                    "/" + MapTileIndex.getY(pMapTileIndex) +
                    "/" + MapTileIndex.getX(pMapTileIndex)
        }
    }

    enum class MapType {
        STANDARD,
        TOPOGRAPHIC,
        SATELLITE
    }
}
