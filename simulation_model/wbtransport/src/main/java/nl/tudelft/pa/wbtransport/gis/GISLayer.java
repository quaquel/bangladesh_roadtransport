package nl.tudelft.pa.wbtransport.gis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Bounds;
import javax.naming.NamingException;
import javax.vecmath.Point3d;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.opengis.feature.simple.SimpleFeature;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSGeometryException;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import nl.tudelft.pa.wbtransport.district.DistrictReader;
import nl.tudelft.simulation.dsol.animation.Locatable;
import nl.tudelft.simulation.dsol.animation.D2.Renderable2D;
import nl.tudelft.simulation.language.d3.BoundingBox;
import nl.tudelft.simulation.language.d3.DirectedPoint;

/**
 * Animation of a GIS layer based on a shape file.
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class GISLayer implements Locatable, Serializable
{
    /** */
    private static final long serialVersionUID = 20150130L;

    /** the simulator. */
    private final OTSSimulatorInterface simulator;

    /** the centroid. */
    private OTSPoint3D centroid;

    /** color of the lines. */
    protected Color outlineColor;

    /** color of the fill. */
    protected Color fillColor;

    /** objects to draw. */
    protected final List<OTSLine3D> objects = new ArrayList<>();

    /** line width. */
    protected float lineWidth;

    /** z-value. */
    private double z;

    /** transform? */
    private boolean transform;

    /** fill? */
    protected boolean fill;

    // status?

    /** Dummy coordinate that forces the drawing operation to start a new path. */
    static final OTSPoint3D NEWPATH = new OTSPoint3D(Double.NaN, Double.NaN, Double.NaN);

    /**
     * Create a GISLayer with fill.
     * @param filename name of te file or resource to get the GIS data from.
     * @param simulator the simulator
     * @param z z-value
     * @param outlineColor color of the line
     * @param lineWidth width of the line
     * @param fillColor fill color.
     * @throws IOException on i/o error
     */
    public GISLayer(final String filename, final OTSSimulatorInterface simulator, final double z, final Color outlineColor,
            final float lineWidth, final Color fillColor) throws IOException
    {
        this.simulator = simulator;
        this.z = z;
        this.outlineColor = outlineColor;
        this.lineWidth = lineWidth;
        this.fillColor = fillColor;
        this.fill = true;
        readGISLayer(filename);
    }

    /**
     * Create a GISLayer without fill.
     * @param filename name of te file or resource to get the GIS data from.
     * @param simulator the simulator
     * @param z z-value
     * @param outlineColor color of the line
     * @param lineWidth width of the line
     * @throws IOException on i/o error
     */
    public GISLayer(final String filename, final OTSSimulatorInterface simulator, final double z, final Color outlineColor,
            final float lineWidth) throws IOException
    {
        this.simulator = simulator;
        this.z = z;
        this.outlineColor = outlineColor;
        this.lineWidth = lineWidth;
        this.fill = false;
        readGISLayer(filename);
    }

    /**
     * Read a GIS layer for animation.
     * @param filename the file to read
     * @throws IOException on i/o error
     */
    private void readGISLayer(final String filename) throws IOException
    {
        URL url;
        if (new File(filename).canRead())
            url = new File(filename).toURI().toURL();
        else
            url = DistrictReader.class.getResource(filename);
        FileDataStore storeGIS = FileDataStoreFinder.getDataStore(url);

        // iterate over the features
        SimpleFeatureSource featureSourceGIS = storeGIS.getFeatureSource();
        SimpleFeatureCollection featureCollectionGIS = featureSourceGIS.getFeatures();
        SimpleFeatureIterator iterator = featureCollectionGIS.features();

        try
        {
            while (iterator.hasNext())
            {
                SimpleFeature feature = iterator.next();
                Geometry geom = (Geometry) feature.getAttribute("the_geom");
                List<Geometry> geometryList = new ArrayList<>();
                if (geom instanceof MultiPolygon)
                {
                    MultiPolygon mp = (MultiPolygon) geom;
                    for (int i = 0; i < mp.getNumGeometries(); i++)
                    {
                        if (mp.getGeometryN(i) instanceof MultiPolygon)
                        {
                            System.err.println("MP");
                        }
                        Polygon p = (Polygon) mp.getGeometryN(i);
                        geometryList.addAll(JTS.makeValid(p, true)); // remove holes
                    }
                }
                else if (geom instanceof MultiLineString)
                {
                    MultiLineString mls = (MultiLineString) geom;
                    for (int i = 0; i < mls.getNumGeometries(); i++)
                    {
                        LineString l = (LineString) mls.getGeometryN(i);
                        geometryList.add(l);
                    }
                }
                else if (geom instanceof Polygon)
                {
                    Polygon pp = (Polygon) geom;
                    for (int i = 0; i < pp.getNumGeometries(); i++)
                    {
                        Polygon p = (Polygon) pp.getGeometryN(i);
                        geometryList.addAll(JTS.makeValid(p, true)); // remove holes
                    }
                }
                else if (geom instanceof LineString)
                {
                    geometryList.add(geom);
                }
                else
                {
                    throw new IOException("Cannot recognize GIS layer with geometry " + geom.getClass());
                }

                this.centroid = new OTSPoint3D(geom.getCentroid().getX(), geom.getCentroid().getY());

                OTSLine3D mBorder = null;

                List<OTSPoint3D> pointList = new ArrayList<>();
                // for (int geomNr = 0; geomNr < polygon.getNumGeometries(); geomNr++)
                for (Geometry geomN : geometryList)
                {
                    if (!pointList.isEmpty())
                    {
                        pointList.add(NEWPATH);
                    }
                    {
                        for (Coordinate c : geomN.getCoordinates())
                        {
                            pointList.add(new OTSPoint3D(c.x, c.y, this.z));
                        }
                    }
                }
                mBorder = OTSLine3D.createAndCleanOTSLine3D(pointList);
                this.objects.add(mBorder);
            }
            new GISLayerAnimation(this, this.simulator);
        }
        catch (RemoteException | NamingException | OTSGeometryException exception)
        {
            exception.printStackTrace();
        }

        iterator.close();
        storeGIS.dispose();
    }

    /** {@inheritDoc} */
    @Override
    public DirectedPoint getLocation() throws RemoteException
    {
        return new DirectedPoint(this.centroid.x, this.centroid.y, this.z);
    }

    /** {@inheritDoc} */
    @Override
    public Bounds getBounds() throws RemoteException
    {
        // return new BoundingBox(this.objects.get(0).getBounds());
        return new BoundingBox(new Point3d(-100, -100, -1), new Point3d(200, 200, 1));
    }

    /**
     * Animation implementation of a GIS layer based on a shape file.
     * <p>
     * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
     * </p>
     * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
     * initial version May 5, 2017 <br>
     * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
     */
    protected static class GISLayerAnimation extends Renderable2D<GISLayer> implements Serializable
    {
        /** */
        private static final long serialVersionUID = 1L;

        /**
         * Construct an Animation for a GIS layer.
         * @param gisLayer the district to draw
         * @param simulator OTSSimulatorInterface; the simulator to schedule on
         * @throws NamingException in case of registration failure of the animation
         * @throws RemoteException in case of remote registration failure of the animation
         */
        public GISLayerAnimation(final GISLayer gisLayer, final OTSSimulatorInterface simulator)
                throws NamingException, RemoteException
        {
            super(gisLayer, simulator);
        }

        /** {@inheritDoc} */
        @Override
        public final void paint(final Graphics2D graphics, final ImageObserver observer)
        {
            GISLayer gisLayer = getSource();
            try
            {
                for (OTSLine3D line : gisLayer.objects)
                {
                    if (gisLayer.fill)
                    {
                        paintMultiPolygon(graphics, gisLayer.fillColor, gisLayer.getLocation(), line, true);
                    }
                    paintMultiLine(graphics, gisLayer.outlineColor, gisLayer.lineWidth, gisLayer.getLocation(), line);
                }
            }
            catch (RemoteException exception)
            {
                exception.printStackTrace();
            }
        }

        /**
         * Paint (fill) a polygon or a series of polygons.
         * @param graphics Graphics2D; the graphics environment
         * @param color Color; the color to use
         * @param lineWidth width
         * @param referencePoint DirectedPoint; the reference point
         * @param line array of points
         */
        public static void paintMultiLine(final Graphics2D graphics, final Color color, final float lineWidth,
                final DirectedPoint referencePoint, final OTSLine3D line)
        {
            graphics.setColor(color);
            Stroke oldStroke = graphics.getStroke();
            graphics.setStroke(new BasicStroke(lineWidth));
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
            boolean withinPath = false;
            for (OTSPoint3D point : line.getPoints())
            {
                if (NEWPATH.equals(point))
                {
                    path = new Path2D.Double(Path2D.WIND_NON_ZERO);
                    withinPath = false;
                }
                else if (!withinPath)
                {
                    withinPath = true;
                    path.moveTo(point.x - referencePoint.x, -point.y + referencePoint.y);
                }
                else
                {
                    path.lineTo(point.x - referencePoint.x, -point.y + referencePoint.y);
                }
            }
            if (withinPath)
            {
                graphics.draw(path);
            }
            graphics.setStroke(oldStroke);
        }

        /**
         * Paint (fill) a polygon or a series of polygons.
         * @param graphics Graphics2D; the graphics environment
         * @param color Color; the color to use
         * @param referencePoint DirectedPoint; the reference point
         * @param line array of points
         * @param fill fill or just contour
         */
        public static void paintMultiPolygon(final Graphics2D graphics, final Color color, final DirectedPoint referencePoint,
                final OTSLine3D line, final boolean fill)
        {
            graphics.setColor(color);
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
            boolean withinPath = false;
            for (OTSPoint3D point : line.getPoints())
            {
                if (NEWPATH.equals(point))
                {
                    if (withinPath)
                    {
                        path.closePath();
                        if (fill)
                        {
                            graphics.fill(path);
                        }
                    }
                    path = new Path2D.Double(Path2D.WIND_NON_ZERO);
                    withinPath = false;
                }
                else if (!withinPath)
                {
                    withinPath = true;
                    path.moveTo(point.x - referencePoint.x, -point.y + referencePoint.y);
                }
                else
                {
                    path.lineTo(point.x - referencePoint.x, -point.y + referencePoint.y);
                }
            }
            if (withinPath)
            {
                path.closePath();
                if (fill)
                {
                    graphics.fill(path);
                }
                else
                {
                    graphics.draw(path);
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean contains(final Point2D pointWorldCoordinates, final Rectangle2D extent, final Dimension screen)
        {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public final String toString()
        {
            return "DistrictAnimation [getSource()=" + this.getSource() + "]";
        }

    }
}
