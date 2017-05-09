package nl.tudelft.pa.wbtransport.gis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.media.j3d.Bounds;
import javax.naming.NamingException;
import javax.vecmath.Point3d;

import org.opentrafficsim.core.dsol.OTSSimulatorInterface;

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
public class BackgroundLayer implements Locatable, Serializable
{
    /** */
    private static final long serialVersionUID = 20150130L;

    /** the simulator. */
    private final OTSSimulatorInterface simulator;

    /** color of the fill. */
    protected Color fillColor;

    /** z-value. */
    private double z;

    /**
     * Create a background layer with fill.
     * @param simulator the simulator
     * @param z z-value
     * @param fillColor fill color.
     */
    public BackgroundLayer(final OTSSimulatorInterface simulator, final double z, final Color fillColor)
    {
        this.simulator = simulator;
        this.z = z;
        this.fillColor = fillColor;
        try
        {
            new BackgroundLayerAnimation(this, this.simulator);
        }
        catch (RemoteException | NamingException exception)
        {
            exception.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override
    public DirectedPoint getLocation() throws RemoteException
    {
        return new DirectedPoint(0, 0, this.z);
    }

    /** {@inheritDoc} */
    @Override
    public Bounds getBounds() throws RemoteException
    {
        return new BoundingBox(new Point3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE),
                new Point3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
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
    protected static class BackgroundLayerAnimation extends Renderable2D<BackgroundLayer> implements Serializable
    {
        /** */
        private static final long serialVersionUID = 1L;

        /**
         * Construct an Animation for a GIS layer.
         * @param backgroundLayer the layer to draw
         * @param simulator OTSSimulatorInterface; the simulator to schedule on
         * @throws NamingException in case of registration failure of the animation
         * @throws RemoteException in case of remote registration failure of the animation
         */
        public BackgroundLayerAnimation(final BackgroundLayer backgroundLayer, final OTSSimulatorInterface simulator)
                throws NamingException, RemoteException
        {
            super(backgroundLayer, simulator);
        }

        /** {@inheritDoc} */
        @Override
        public final void paint(final Graphics2D graphics, final ImageObserver observer)
        {
            BackgroundLayer backgroundLayer = getSource();
            graphics.setColor(backgroundLayer.fillColor);
            graphics.fillRect(-1000, -1000, 2000, 2000);
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
            return "BackgroundLayerAnimation [getSource()=" + this.getSource() + "]";
        }

    }
}
