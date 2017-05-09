package nl.tudelft.pa.wbtransport.gis;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;

import org.opentrafficsim.core.dsol.OTSSimulatorInterface;

import nl.javel.gisbeans.io.esri.CoordinateTransform;
import nl.javel.gisbeans.io.esri.ShapeFile;
import nl.javel.gisbeans.map.Image;
import nl.javel.gisbeans.map.Layer;
import nl.javel.gisbeans.map.Map;
import nl.javel.gisbeans.map.MapInterface;
import nl.tudelft.simulation.dsol.animation.Locatable;
import nl.tudelft.simulation.dsol.animation.D2.GisRenderable2D;
import nl.tudelft.simulation.language.d3.BoundingBox;
import nl.tudelft.simulation.language.d3.DirectedPoint;
import nl.tudelft.simulation.language.io.URLResource;

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
public class ShapeFileLayer implements Locatable, Serializable
{
    /** */
    private static final long serialVersionUID = 20150130L;

    /** z-value. */
    final double z;

    /** the gis layer itself. */
    private GisRenderable2D gisRenderable2D;

    /**
     * Create a GISLayer with fill.
     * @param layerName name of the layer
     * @param filename name of the file or resource to get the GIS data from.
     * @param simulator the simulator
     * @param z z-value
     * @param outlineColor color of the line
     * @param fillColor fill color.
     * @throws IOException on i/o error
     */
    public ShapeFileLayer(final String layerName, final String filename, final OTSSimulatorInterface simulator, final double z,
            final Color outlineColor, final Color fillColor) throws IOException
    {
        this.z = z;

        Map map = new Map();
        map.setName(layerName);
        map.setExtent(new Rectangle2D.Double(-2000, -2000, 4000, 4000));
        map.setDrawBackground(false);
        map.setImage(new Image());
        map.setUnits(MapInterface.DD);
        Layer layer = new Layer();
        layer.setColor(fillColor);
        layer.setOutlineColor(outlineColor);
        layer.setAttributes(new ArrayList<>());
        URL resource = URLResource.getResource(filename);
        ShapeFile dataSource = new ShapeFile(resource, new CoordinateTransform.NoTransform());
        dataSource.setCache(true);
        layer.setDataSource(dataSource);
        layer.setName(layerName);
        layer.setTransform(true);
        map.addLayer(layer);

        this.gisRenderable2D = new GisRenderable2D(simulator, map, new CoordinateTransform.NoTransform(), z);
    }

    /** {@inheritDoc} */
    @Override
    public DirectedPoint getLocation() throws RemoteException
    {
        return new DirectedPoint(0.0, 0.0, this.z);
    }

    /** {@inheritDoc} */
    @Override
    public Bounds getBounds() throws RemoteException
    {
        return new BoundingBox(new Point3d(-100, -100, -1), new Point3d(200, 200, 1));
    }

    /**
     * @return gisRenderable2D
     */
    public final GisRenderable2D getGisRenderable2D()
    {
        return this.gisRenderable2D;
    }

}
