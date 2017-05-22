package nl.tudelft.pa.wbtransport.road.animation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.opentrafficsim.core.animation.ClonableRenderable2DInterface;
import org.opentrafficsim.core.animation.TextAlignment;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;

import nl.tudelft.pa.wbtransport.road.SegmentN;
import nl.tudelft.simulation.dsol.animation.D2.Renderable2D;
import nl.tudelft.simulation.language.d3.DirectedPoint;

/**
 * Draws a RoadN.
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * <p>
 * $LastChangedDate: 2017-01-16 01:48:07 +0100 (Mon, 16 Jan 2017) $, @version $Revision: 3281 $, by $Author: averbraeck $,
 * initial version Sep 13, 2014 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class RoadNAnimation extends Renderable2D<SegmentN> implements ClonableRenderable2DInterface<SegmentN>, Serializable
{
    /** */
    private static final long serialVersionUID = 20140000L;

    /** */
    private float width;

    /** */
    final String printName;

    /** */
    private final Color color;

    /**
     * @param roadN RoadN
     * @param printName name to print for the road
     * @param simulator simulator
     * @param width width
     * @param color color
     * @throws NamingException for problems with registering in context
     * @throws RemoteException on communication failure
     */
    public RoadNAnimation(final SegmentN roadN, final String printName, final OTSSimulatorInterface simulator, final float width,
            final Color color) throws NamingException, RemoteException
    {
        super(roadN, simulator);
        this.width = width;
        this.printName = printName;
        this.color = color;

        RoadNTextAnimation wta =
                new RoadNTextAnimation(roadN, printName, 0.0f, 25.0f, TextAlignment.CENTER, Color.BLACK, 10.0f, simulator);
        // wta.setRotate(false);
        wta.setScale(false);
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics2D graphics, final ImageObserver observer) throws RemoteException
    {
        Color roadColor;
        float w = this.width;
        switch (getSource().getGap())
        {
            case ROAD:
                roadColor = this.color;
                break;

            case FERRY:
                roadColor = Color.YELLOW;
                break;

            case BRIDGE:
                roadColor = Color.MAGENTA;
                break;

            case GAP:
                roadColor = Color.WHITE;
                break;

            default:
                roadColor = this.color;
                break;
        }
        if (getSource().getGap().isRoad())
        {
            RoadNAnimation.paintLine(graphics, roadColor, w, getSource().getLocation(), getSource().getDesignLine(),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        else
        {
            RoadNAnimation.paintLine(graphics, roadColor, w, getSource().getLocation(), getSource().getDesignLine(),
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:designforextension")
    public ClonableRenderable2DInterface<SegmentN> clone(final SegmentN newSource, final OTSSimulatorInterface newSimulator)
            throws NamingException, RemoteException
    {
        // the constructor also constructs the corresponding Text object
        return new RoadNAnimation(newSource, this.printName, newSimulator, this.width, this.color);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return "RoadNAnimation [width=" + this.width + ", link=" + super.getSource() + "]";
    }

    /**
     * Paint line.
     * @param graphics Graphics2D; the graphics environment
     * @param color Color; the color to use
     * @param width the width to use
     * @param referencePoint DirectedPoint; the reference point
     * @param line array of points
     * @param cap cap, e.g. BasicStroke.CAP_BUTT
     * @param join join, e.g., BasicStroke.JOIN_MITER
     */
    public static void paintLine(final Graphics2D graphics, final Color color, final double width,
            final DirectedPoint referencePoint, final OTSLine3D line, final int cap, final int join)
    {
        graphics.setColor(color);
        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke((float) width, cap, join));
        Path2D.Double path = new Path2D.Double();
        OTSPoint3D point = line.getFirst();
        path.moveTo(point.x - referencePoint.x, -point.y + referencePoint.y);
        for (int i = 1; i < line.getPoints().length; i++)
        {
            point = line.getPoints()[i];
            path.lineTo(point.x - referencePoint.x, -point.y + referencePoint.y);
        }
        graphics.draw(path);
        graphics.setStroke(oldStroke);
    }

}
