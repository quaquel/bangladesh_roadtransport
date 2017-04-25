package nl.tudelft.pa.wbtransport;

/**
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 25, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="http://www.tudelft.nl/pknoppers">Peter Knoppers</a>
 * @author <a href="http://www.transport.citg.tudelft.nl">Wouter Schakel</a>
 */
public enum Gap
{
    /** gap. */
    GAP,

    /** bridge srt. */
    BRIDGE,

    /** ferry. */
    FERRY,

    /** no gap. */
    ROAD;

    /**
     * @return true if gap.
     */
    public boolean isGap()
    {
        return this.equals(GAP);
    }

    /**
     * @return true if bridge.
     */
    public boolean isBridge()
    {
        return this.equals(BRIDGE);
    }

    /**
     * @return true if ferry.
     */
    public boolean isFerry()
    {
        return this.equals(FERRY);
    }

    /**
     * @return true if normal road.
     */
    public boolean isRoad()
    {
        return this.equals(ROAD);
    }

}
