package nl.tudelft.pa.wbtransport.road.animation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.opentrafficsim.core.animation.TextAlignment;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.network.OTSNode;

import nl.tudelft.simulation.dsol.animation.D2.Renderable2D;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class LRPAnimation extends Renderable2D implements Serializable
{
    /** */
    private static final long serialVersionUID = 20150130L;

    /** The color of the sensor. */
    private final Color color;

    /**
     * Construct a SensorAnimation.
     * @param node the obstacleto draw
     * @param simulator OTSSimulatorInterface; the simulator to schedule on
     * @param color Color; the display color of the sensor
     * @throws NamingException in case of registration failure of the animation
     * @throws RemoteException in case of remote registration failure of the animation
     */
    public LRPAnimation(final OTSNode node, final OTSSimulatorInterface simulator, final Color color)
            throws NamingException, RemoteException
    {
        super(node, simulator);
        this.color = color;
        this.setScale(false);

        LRPTextAnimation bta =
                new LRPTextAnimation(node, node.getId(), 0.0f, 9.0f, TextAlignment.CENTER, Color.BLUE, 10.0f, simulator);
        bta.setRotate(false);
        bta.setScale(false);
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics2D graphics, final ImageObserver observer)
    {
        graphics.setColor(this.color);
        Rectangle2D rectangle = new Rectangle2D.Double(-2, -2, 4, 4);
        graphics.fill(rectangle);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return "LRPAnimation [getSource()=" + this.getSource() + "]";
    }
}
