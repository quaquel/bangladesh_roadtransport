package nl.tudelft.pa.wbtransport.district;

import java.io.File;
import java.net.URL;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version May 4, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class DistrictReader
{
    /**
     * Read the Districts
     * @param filename filename
     * @throws Exception on I/O error
     */
    protected static void readDistricts(final String filename) throws Exception
    {
        URL url;
        if (new File(filename).canRead())
            url = new File(filename).toURI().toURL();
        else
            url = DistrictReader.class.getResource(filename);
        FileDataStore storeNuts3 = FileDataStoreFinder.getDataStore(url);

        // CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:4326");

        // iterate over the features
        SimpleFeatureSource featureSourceNuts3 = storeNuts3.getFeatureSource();
        SimpleFeatureCollection featureCollectionNuts3 = featureSourceNuts3.getFeatures();
        SimpleFeatureIterator iterator = featureCollectionNuts3.features();
        try
        {
            while (iterator.hasNext())
            {
                SimpleFeature feature = iterator.next();
                MultiPolygon mp = (MultiPolygon) feature.getAttribute("the_geom");
                String code = feature.getAttribute("ID_2").toString();
                String name = feature.getAttribute("NAME_2").toString();
                String code2 = feature.getAttribute("HASC_2").toString().substring(6, 8);
                System.out.println(code + "," + name + ", " + code2);
            }
        } catch (Exception problem)
        {
            problem.printStackTrace();
        } finally
        {
            iterator.close();
        }
    }
    


    /**
     * @param args args
     * @throws Exception on i/o error
     */
    public static void main(String[] args) throws Exception
    {
        readDistricts("/gis/gadm/BGD_adm2.shp");
    }

}
