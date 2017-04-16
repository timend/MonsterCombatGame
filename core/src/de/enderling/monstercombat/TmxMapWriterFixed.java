package de.enderling.monstercombat;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import net.dermetfan.gdx.maps.tiled.TmxMapWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import static com.badlogic.gdx.math.MathUtils.round;
import static net.dermetfan.gdx.maps.MapUtils.getProperty;


public class TmxMapWriterFixed extends TmxMapWriter {
    /**
     * creates a new {@link TmxMapWriter} using the given {@link Writer}
     *
     * @param writer
     */
    public TmxMapWriterFixed(Writer writer) {
        super(writer);
    }

    @Override
    public TmxMapWriter tmx(TiledMapTileSet set) throws IOException {
        MapProperties props = set.getProperties();
        element("tileset");
        attribute("firstgid", getProperty(props, "firstgid", 1));
        attribute("name", set.getName());
        attribute("tilewidth", getProperty(props, "tilewidth", 0));
        attribute("tileheight", getProperty(props, "tileheight", 0));
        float spacing = getProperty(props, "spacing", Float.NaN), margin = getProperty(props, "margin", Float.NaN);
        if(!Float.isNaN(spacing))
            attribute("spacing", round(spacing));
        if(!Float.isNaN(margin))
            attribute("margin", round(margin));

        Iterator<TiledMapTile> iter = set.iterator();
        if(iter.hasNext()) {
            TiledMapTile tile = iter.next();
            element("tileoffset");
            attribute("x", round(tile.getOffsetX()));
            attribute("y", round(-tile.getOffsetY()));
            pop();
        }

        element("image");
        attribute("source", getProperty(props, "imagesource", ""));
        attribute("imagewidth", getProperty(props, "imagewidth", 0));
        attribute("imageheight", getProperty(props, "imageheight", 0));
        pop();

        iter = set.iterator();
        if(iter.hasNext()) {
            @SuppressWarnings("unchecked")
            Array<String> asAttributes = Pools.obtain(Array.class);
            asAttributes.clear();
            boolean elementEmitted = false;
            for(TiledMapTile tile = iter.next(); iter.hasNext(); tile = iter.next()) {



                MapProperties tileProps = tile.getProperties();
                for(String attribute : asAttributes)
                    if(tileProps.containsKey(attribute)) {
                        if(!elementEmitted) {
                            element("tile");
                            elementEmitted = true;
                        }
                        attribute(attribute, tileProps.get(attribute));
                    }

                if (tile.getProperties().getValues().hasNext() || tile instanceof  AnimatedTiledMapTile) {
                    element("tile");
                    attribute("id", tile.getId()-1);

                    if (tile instanceof AnimatedTiledMapTile) {
                        AnimatedTiledMapTile animatedTiledMapTile = (AnimatedTiledMapTile)tile;
                        element("animation");
                        int i = 0;
                        for (StaticTiledMapTile staticTiledMapTile : animatedTiledMapTile.getFrameTiles()) {
                            element("frame");
                            attribute("tileid", staticTiledMapTile.getId()-1);
                            attribute("duration", animatedTiledMapTile.getAnimationIntervals()[i]);
                            pop();
                            i++;
                        }

                        pop();
                    }

                    tmx(tileProps, asAttributes);
                    pop();
                }
            }
            asAttributes.clear();
            Pools.free(asAttributes);
            if(elementEmitted)
                pop();
        }

        pop();
        return this;
    }

    @Override
    public TmxMapWriter tmx(MapProperties properties, Array<String> exclude) throws IOException {
        Iterator<String> keys = properties.getKeys();
        if(!keys.hasNext())
            return this;

        boolean elementEmitted = false;
        while(keys.hasNext()) {
            String key = keys.next();
            if(exclude != null && exclude.contains(key, false))
                continue;
            if(!elementEmitted) {
                element("properties");
                elementEmitted = true;
            }

            String type = null;
            String className = properties.get(key).getClass().getSimpleName();

            if (className.equals("Boolean")) {
                type = "bool";
            } else if (className.equals("Float")) {
                type = "float";
            } else if (className.equals("Integer")) {
                type = "int";
            } else if (className.equals("String")) {
                type = null;
            }

            element("property").attribute("name", key).attribute("value", properties.get(key));

            if (type != null) {
                attribute("type", type);
            }
            pop();
        }

        if(elementEmitted)
            pop();
        return this;
    }
}
