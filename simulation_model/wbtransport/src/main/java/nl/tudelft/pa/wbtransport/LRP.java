package nl.tudelft.pa.wbtransport;

import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;

import org.djunits.value.vdouble.scalar.Direction;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.OTSNode;

import nl.tudelft.simulation.language.d3.BoundingBox;

/**
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 20, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="http://www.tudelft.nl/pknoppers">Peter Knoppers</a>
 * @author <a href="http://www.transport.citg.tudelft.nl">Wouter Schakel</a>
 */
public class LRP extends OTSNode
{
    /** */
    private static final long serialVersionUID = 1L;

    /** the chainage in km. */
    private final double chainage;

    /** the type. */
    private final String type;

    /** the name. */
    private final String name;

    /** bridge start, bridge end, road start, road end, gap start, gap end. */
    private final GapPoint gapPoint;

    /**
     * Construction of a Node.
     * @param network the network.
     * @param id the id of the Node.
     * @param point the point with usually an x and y setting.
     * @param direction the 3D direction. "East" is 0 degrees. "North" is 90 degrees (1/2 pi radians).
     * @param slope the slope as an angle. Horizontal is 0 degrees.
     * @param chainage chainage
     * @param type type
     * @param name name
     * @param gapPoint gap
     * @throws NetworkException if node already exists in the network, or if name of the node is not unique.
     */
    public LRP(Network network, String id, OTSPoint3D point, Direction direction, Direction slope, final double chainage,
            final String type, final String name, final GapPoint gapPoint) throws NetworkException
    {
        super(network, id, point, direction, slope);
        this.chainage = chainage;
        this.type = type;
        this.name = name;
        this.gapPoint = gapPoint;
    }

    /**
     * Construction of a Node.
     * @param network the network.
     * @param id the id of the Node.
     * @param point the point with usually an x and y setting.
     * @param direction the 3D direction. "East" is 0 degrees. "North" is 90 degrees (1/2 pi radians).
     * @param slope the slope as an angle. Horizontal is 0 degrees.
     * @throws NetworkException if node already exists in the network, or if name of the node is not unique.
     */
    public LRP(Network network, String id, OTSPoint3D point, Direction direction, Direction slope) throws NetworkException
    {
        this(network, id, point, direction, slope, Double.NaN, "", "", GapPoint.ROAD);
    }

    /**
     * Construction of a Node.
     * @param network the network.
     * @param id the id of the Node.
     * @param point the point with usually an x and y setting.
     * @param chainage chainage
     * @param type type
     * @param name name
     * @param gapPoint gap
     * @throws NetworkException if node already exists in the network, or if name of the node is not unique.
     */
    public LRP(Network network, String id, OTSPoint3D point, final double chainage, final String type, final String name,
            final GapPoint gapPoint) throws NetworkException
    {
        super(network, id, point);
        this.chainage = chainage;
        this.type = type;
        this.name = name;
        this.gapPoint = gapPoint;
    }

    /**
     * Construction of a Node.
     * @param network the network.
     * @param id the id of the Node.
     * @param point the point with usually an x and y setting.
     * @throws NetworkException if node already exists in the network, or if name of the node is not unique.
     */
    public LRP(Network network, String id, OTSPoint3D point) throws NetworkException
    {
        this(network, id, point, Double.NaN, "", "", GapPoint.ROAD);
    }

    /**
     * @return chainage
     */
    public final double getChainage()
    {
        return this.chainage;
    }

    /**
     * @return type
     */
    public final String getType()
    {
        return this.type;
    }

    /**
     * @return name
     */
    public final String getName()
    {
        return this.name;
    }

    /**
     * @return gap
     */
    public final GapPoint getGapPoint()
    {
        return this.gapPoint;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("checkstyle:designforextension")
    public Bounds getBounds()
    {
        return new BoundingBox(new Point3d(-0.0025, -0.0025, -10), new Point3d(0.005, 0.005, 10));
    }

}
