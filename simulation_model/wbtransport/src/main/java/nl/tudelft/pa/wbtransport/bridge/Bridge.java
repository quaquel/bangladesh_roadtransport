package nl.tudelft.pa.wbtransport.bridge;

import java.rmi.RemoteException;

import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;

import org.opentrafficsim.water.AbstractNamedLocated;

import nl.tudelft.simulation.language.d3.BoundingBox;
import nl.tudelft.simulation.language.d3.DirectedPoint;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Bridge extends AbstractNamedLocated
{
    /** */
    private static final long serialVersionUID = 1L;

    /**
     * @param name name
     * @param location point
     */
    public Bridge(final String name, final DirectedPoint location)
    {
        super(name, location);
    }

    /** {@inheritDoc} */
    @Override
    public Bounds getBounds() throws RemoteException
    {
        return new BoundingBox(new Point3d(-0.005, -0.005, -10), new Point3d(0.01, 0.01, 10));
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return getName();
    }

}
