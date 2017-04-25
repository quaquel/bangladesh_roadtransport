package nl.tudelft.pa.wbtransport;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.opentrafficsim.core.animation.ClonableRenderable2DInterface;
import org.opentrafficsim.core.animation.TextAlignment;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;

import nl.tudelft.simulation.dsol.animation.D2.Renderable2D;

/**
 * Draws a RoadR.
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * <p>
 * $LastChangedDate: 2017-01-16 01:48:07 +0100 (Mon, 16 Jan 2017) $, @version $Revision: 3281 $, by $Author: averbraeck $,
 * initial version Sep 13, 2014 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class RoadRAnimation extends Renderable2D<RoadR> implements ClonableRenderable2DInterface<RoadR>, Serializable
{
    /** */
    private static final long serialVersionUID = 20140000L;

    /** */
    private float width;

    /** */
    final String printName;

    /**
     * @param roadR RoadR
     * @param printName name to print for the road
     * @param simulator simulator
     * @param width width
     * @throws NamingException for problems with registering in context
     * @throws RemoteException on communication failure
     */
    public RoadRAnimation(final RoadR roadR, final String printName, final OTSSimulatorInterface simulator, final float width)
            throws NamingException, RemoteException
    {
        super(roadR, simulator);
        this.width = width;
        this.printName = printName;

        RoadRTextAnimation wta =
                new RoadRTextAnimation(roadR, printName, 0.0f, 25.0f, TextAlignment.CENTER, Color.BLACK, 10.0f, simulator);
        // wta.setRotate(false);
        wta.setScale(false);
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics2D graphics, final ImageObserver observer) throws RemoteException
    {
        Color color;
        float w = this.width;
        switch (getSource().getGap())
        {
            case ROAD:
                color = Color.BLACK;
                break;

            case FERRY:
                color = Color.YELLOW;
                break;

            case BRIDGE:
                color = Color.LIGHT_GRAY;
                break;

            case GAP:
                color = Color.WHITE;
                break;
            
            default:
                color = Color.BLACK;
                break;
        }
        if (getSource().getGap().isRoad())
        {
            RoadNAnimation.paintLine(graphics, color, w, getSource().getLocation(), getSource().getDesignLine(),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        else
        {
            RoadNAnimation.paintLine(graphics, color, w, getSource().getLocation(), getSource().getDesignLine(),
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:designforextension")
    public ClonableRenderable2DInterface<RoadR> clone(final RoadR newSource, final OTSSimulatorInterface newSimulator)
            throws NamingException, RemoteException
    {
        // the constructor also constructs the corresponding Text object
        return new RoadRAnimation(newSource, this.printName, newSimulator, this.width);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return "RoadRAnimation [width=" + this.width + ", link=" + super.getSource() + "]";
    }

}
