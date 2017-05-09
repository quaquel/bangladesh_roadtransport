package nl.tudelft.pa.wbtransport.district.animation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.io.Serializable;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.opentrafficsim.core.animation.TextAlignment;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.network.animation.PaintLine;
import org.opentrafficsim.core.network.animation.PaintPolygons;

import nl.tudelft.pa.wbtransport.district.District;
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
public class DistrictAnimation extends Renderable2D implements Serializable
{
    /** */
    private static final long serialVersionUID = 20150130L;

    /**
     * Construct a District Animation.
     * @param district the district to draw
     * @param simulator OTSSimulatorInterface; the simulator to schedule on
     * @throws NamingException in case of registration failure of the animation
     * @throws RemoteException in case of remote registration failure of the animation
     */
    public DistrictAnimation(final District district, final OTSSimulatorInterface simulator)
            throws NamingException, RemoteException
    {
        super(district, simulator);
        
        DistrictTextAnimation dta = new DistrictTextAnimation(district, district.getName(), 0.0f, 9.0f, TextAlignment.CENTER,
                Color.BLACK, 16.0f, simulator);
        dta.setRotate(false);
        dta.setScale(false);
    }

    /** {@inheritDoc} */
    @Override
    public final void paint(final Graphics2D graphics, final ImageObserver observer)
    {
        District district = (District) getSource();
        Color color = district.isFlooded() ? new Color(234, 129, 129) : Color.LIGHT_GRAY;
        try
        {
            Paint p = graphics.getPaint();
            PaintPolygons.paintMultiPolygon(graphics, color, district.getLocation(), district.getBorder(), true);
            Stroke oldStroke = graphics.getStroke();
            graphics.setStroke(new BasicStroke(0.0005f));
            PaintPolygons.paintMultiPolygon(graphics, Color.DARK_GRAY, district.getLocation(), district.getBorder(), false);
            graphics.setStroke(oldStroke);
            graphics.setPaint(p);
        }
        catch (RemoteException exception)
        {
            exception.printStackTrace();
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
