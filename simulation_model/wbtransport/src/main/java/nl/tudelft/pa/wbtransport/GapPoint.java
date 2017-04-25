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
public enum GapPoint
{
    /** gap start. */
    GAP_START,
    
    /** gap end. */
    GAP_END,
    
    /** bridge start. */
    BRIDGE_START,
    
    /** bridge end. */
    BRIDGE_END,
    
    /** ferry start. */
    FERRY_START,
    
    /** ferry end. */
    FERRY_END,

    /** no gap. */
    ROAD;
    
    /** 
     * @return true if gap start.
     */
    public boolean isGapStart()
    {
        return this.equals(GAP_START);
    }

    /** 
     * @return true if gap end.
     */
    public boolean isGapEnd()
    {
        return this.equals(GAP_END);
    }

    /** 
     * @return true if bridge start.
     */
    public boolean isBridgeStart()
    {
        return this.equals(BRIDGE_START);
    }

    /** 
     * @return true if bridge end.
     */
    public boolean isBridgeEnd()
    {
        return this.equals(BRIDGE_END);
    }
    
    /** 
     * @return true if ferry start.
     */
    public boolean isFerryStart()
    {
        return this.equals(FERRY_START);
    }

    /** 
     * @return true if ferry end.
     */
    public boolean isFerryEnd()
    {
        return this.equals(FERRY_END);
    }

    /** 
     * @return true if normal road.
     */
    public boolean isRoad()
    {
        return this.equals(ROAD);
    }

}

