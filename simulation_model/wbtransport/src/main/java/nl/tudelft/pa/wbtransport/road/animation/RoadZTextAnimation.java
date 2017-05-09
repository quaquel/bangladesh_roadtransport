package nl.tudelft.pa.wbtransport.road.animation;

import java.awt.Color;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.opentrafficsim.core.animation.TextAlignment;
import org.opentrafficsim.core.animation.TextAnimation;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;

import nl.tudelft.simulation.dsol.animation.Locatable;

/**
 * Text animation for the Waterway. Separate class to be able to turn it on and off...
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Dec 11, 2016 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class RoadZTextAnimation extends TextAnimation
{
    /** */
    private static final long serialVersionUID = 20161211L;

    /**
     * @param source the object for which the text is displayed
     * @param text the text to display
     * @param dx the horizontal movement of the text, in meters
     * @param dy the vertical movement of the text, in meters
     * @param textPlacement where to place the text
     * @param color the color of the text
     * @param fontSize the font size. Default value is 2.0.
     * @param simulator the simulator
     * @throws NamingException when animation context cannot be created or retrieved
     * @throws RemoteException - when remote context cannot be found
     */
    public RoadZTextAnimation(final Locatable source, final String text, final float dx, final float dy,
            final TextAlignment textPlacement, final Color color, final float fontSize, final OTSSimulatorInterface simulator)
            throws RemoteException, NamingException
    {
        super(source, text, dx, dy, textPlacement, color, fontSize, simulator);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:designforextension")
    public TextAnimation clone(final Locatable newSource, final OTSSimulatorInterface newSimulator)
            throws RemoteException, NamingException
    {
        return new RoadZTextAnimation(newSource, getText(), getDx(), getDy(), getTextAlignment(), getColor(), getFontSize(),
                newSimulator);
    }

}
