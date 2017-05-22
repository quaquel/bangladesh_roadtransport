package nl.tudelft.pa.wbtransport.util;

import java.net.URL;

import nl.javel.gisbeans.io.esri.CoordinateTransform;
import nl.tudelft.simulation.dsol.animation.D2.GisRenderable2D;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.language.d3.DirectedPoint;

/**
 * This renderable draws CAD/GIS objects.
 * <p>
 * (c) copyright 2002-2005 <a href="http://www.simulation.tudelft.nl">Delft University of Technology </a>, the
 * Netherlands. <br>
 * See for project information <a href="http://www.simulation.tudelft.nl">www.simulation.tudelft.nl </a> <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser General Public License (LGPL) </a>, no
 * warranty.
 * @author <a href="https://www.linkedin.com/in/peterhmjacobs">Peter Jacobs </a>
 * @version $Revision: 1.1 $ $Date: 2010/08/10 11:37:20 $
 * @since 1.5
 */
public class WBGisRenderable2D extends GisRenderable2D
{
    /**
     * constructs a new GisRenderable2D.
     * @param simulator the simulator.
     * @param mapFile the mapfile to use.
     */
    public WBGisRenderable2D(final SimulatorInterface<?, ?, ?> simulator, final URL mapFile)
    {
        this(simulator, mapFile, new CoordinateTransform.NoTransform());
    }

    /**
     * constructs a new GisRenderable2D.
     * @param simulator the simulator.
     * @param mapFile the mapfile to use.
     * @param coordinateTransform the transformation of (x, y) coordinates to (x', y') coordinates.
     */
    public WBGisRenderable2D(final SimulatorInterface<?, ?, ?> simulator, final URL mapFile,
            final CoordinateTransform coordinateTransform)
    {
        super(simulator, mapFile, coordinateTransform);
    }

    /** {@inheritDoc} */
    @Override
    public DirectedPoint getLocation()
    {
        return new DirectedPoint(super.getLocation().x, super.getLocation().y, -10); 
    }
    
}
