package nl.tudelft.pa.wbtransport;

import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;

/**
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Feb 19, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="http://www.tudelft.nl/pknoppers">Peter Knoppers</a>
 * @author <a href="http://www.transport.citg.tudelft.nl">Wouter Schakel</a>
 */
public class RoadR extends RoadBGD
{
    /** */
    private static final long serialVersionUID = 20170219L;

    /**
     * Construct a new link, with a directionality for all GTUs as provided.
     * @param id the link id
     * @param network the network to which the link belongs
     * @param startNode start node (directional)
     * @param endNode end node (directional)
     * @param linkType Link type to indicate compatibility with GTU types
     * @param designLine the OTSLine3D design line of the Link
     * @param simulator the simulator on which events can be scheduled
     * @param directionality the directionality for all GTUs
     * @param gap gap in case of ferry, road, or bridge
     * @throws NetworkException if link already exists in the network, if name of the link is not unique, or if the start node
     *             or the end node of the link are not registered in the network.
     */
    public RoadR(Network network, String id, Node startNode, Node endNode, LinkType linkType, OTSLine3D designLine,
            OTSSimulatorInterface simulator, LongitudinalDirectionality directionality, final Gap gap) throws NetworkException
    {
        super(network, id, startNode, endNode, linkType, designLine, simulator, directionality, gap);
    }

}
