package nl.tudelft.pa.wbtransport;

import java.rmi.RemoteException;

import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;

import nl.tudelft.simulation.language.d3.BoundingBox;
import nl.tudelft.simulation.language.d3.DirectedPoint;

/**
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 26, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="http://www.tudelft.nl/pknoppers">Peter Knoppers</a>
 * @author <a href="http://www.transport.citg.tudelft.nl">Wouter Schakel</a>
 */
public class BridgeBGD extends Bridge
{
    /** */
    private static final long serialVersionUID = 1L;

    /** road number. */
    private final String roadNo;

    /** condition A-D. */
    private final String condition;

    /** description of the bridge type. */
    private final String type;

    /** width in m. */
    private final double width;

    /** length in m. */
    private final double length;

    /** year of construction */
    private final int constructionYear;

    /** number of spans */
    private final int numberOfSpans;

    /**
     * @param name name
     * @param location point
     * @param roadNo road number
     * @param condition condition A-D
     * @param type type
     * @param width in m
     * @param length in m
     * @param constructionYear year
     * @param numberOfSpans spans
     */
    public BridgeBGD(String name, DirectedPoint location, String roadNo, String condition, String type, double width,
            double length, int constructionYear, int numberOfSpans)
    {
        super(name, location);
        this.roadNo = roadNo;
        this.condition = condition;
        this.type = type;
        this.width = width;
        this.length = length;
        this.constructionYear = constructionYear;
        this.numberOfSpans = numberOfSpans;
    }

    /**
     * @return roadNo
     */
    public final String getRoadNo()
    {
        return this.roadNo;
    }

    /**
     * @return condition
     */
    public final String getCondition()
    {
        return this.condition;
    }

    /**
     * @return type
     */
    public final String getType()
    {
        return this.type;
    }

    /**
     * @return width
     */
    public final double getWidth()
    {
        return this.width;
    }

    /**
     * @return length
     */
    public final double getLength()
    {
        return this.length;
    }

    /**
     * @return constructionYear
     */
    public final int getConstructionYear()
    {
        return this.constructionYear;
    }

    /**
     * @return numberOfSpans
     */
    public final int getNumberOfSpans()
    {
        return this.numberOfSpans;
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
        return (this.roadNo != null && this.roadNo.length() > 0) ? "Bridge " + this.roadNo + ": " + getName()
                : "Bridge " + getName();
    }

}
