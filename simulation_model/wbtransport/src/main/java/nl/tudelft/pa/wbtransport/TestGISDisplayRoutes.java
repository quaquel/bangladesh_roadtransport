package nl.tudelft.pa.wbtransport;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;

/**
 * <br>
 * Copyright (c) 2012 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights
 * reserved.
 * 
 * See for project information <a href="http://www.simulation.tudelft.nl/"> www.simulation.tudelft.nl</a>.
 * 
 * The source code and binary code of this software is proprietary information of Delft University of Technology.
 * 
 * @version Sep 29, 2012 <br>
 * @author <a href="http://tudelft.nl/averbraeck">Alexander Verbraeck </a>
 */
public class TestGISDisplayRoutes
{
    /**
     * Displays shape file contents on the screen in a map frame
     * @param args args
     * @throws Exception on error 
     */
    public static void main(final String[] args) throws Exception
    {
        MapContent map = new MapContent();
        map.setTitle("53 sailable routes");
        URL url = TestGISDisplayRoutes.class.getResource("/");
        File f = new File(url.getFile() + "infrastructure/water/WaterwaysSailable/");
        String fn = f.getCanonicalPath();
        fn = fn.replace('\\', '/');
        System.out.println(fn);

        File initialDir = new File(fn);
        map.addLayer(newLayer(map, initialDir, "waterways_53routes_routable_v02"));

        JMapFrame.showMap(map);
    }

    /**
     * @param map map
     * @param initialDir dir
     * @param name filename without .shp
     * @return layer on the map
     * @throws IOException on read error
     */
    private static Layer newLayer(MapContent map, File initialDir, final String name) throws IOException
    {
        File file = new File(initialDir, name+".shp");
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        return new FeatureLayer(featureSource, style);
    }
}
