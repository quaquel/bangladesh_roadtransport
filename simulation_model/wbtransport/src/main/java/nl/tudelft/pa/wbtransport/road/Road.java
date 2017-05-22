package nl.tudelft.pa.wbtransport.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.tudelft.simulation.immutablecollections.Immutable;
import nl.tudelft.simulation.immutablecollections.ImmutableArrayList;
import nl.tudelft.simulation.immutablecollections.ImmutableList;
import nl.tudelft.simulation.language.Throw;

/**
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-Clause License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version May 19, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Road
{
    /** the id. */
    private final String id;

    /** the road segments. */
    private List<RoadSegment> segments = Collections.synchronizedList(new ArrayList<>());

    /**
     * @param id road id
     */
    public Road(final String id)
    {
        this.id = id;
    }

    /**
     * @return id
     */
    public final String getId()
    {
        return this.id;
    }

    /**
     * @return segments
     */
    public final ImmutableList<RoadSegment> getSegments()
    {
        return new ImmutableArrayList<>(this.segments, Immutable.WRAP);
    }

    /**
     * Add a segment to the road.
     * @param segment segment to add
     */
    public void addSegment(final RoadSegment segment)
    {
        Throw.whenNull(segment, "segment cannot be null");
        this.segments.add(segment);
    }

    /**
     * Add a segment to the road.
     * @param index location to insert
     * @param segment segment to add
     */
    public void addSegment(final int index, final RoadSegment segment)
    {
        Throw.whenNull(segment, "segment cannot be null");
        this.segments.add(index, segment);
    }

    /**
     * Remove a segment from the road.
     * @param index location to remove
     */
    public void removeSegment(final int index)
    {
        this.segments.remove(index);
    }

    /**
     * Remove a segment from the road.
     * @param segment segment to remove
     */
    public void removeSegment(final RoadSegment segment)
    {
        this.segments.remove(segment);
    }

    /**
     * @return segments
     */
    public final List<LRP> getLRPs()
    {
        List<LRP> lrps = new ArrayList<>();
        if (this.segments.size() > 0)
        {
            lrps.add(this.segments.get(0).getStartLRP());
            for (RoadSegment segment : this.segments)
            {
                lrps.add(segment.getEndLRP());
            }
        }
        return lrps;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return this.id;
    }

}
