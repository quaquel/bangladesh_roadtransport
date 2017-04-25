package nl.tudelft.pa.wbtransport;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

import javax.naming.NamingException;
import javax.swing.SwingUtilities;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.djunits.unit.TimeUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opentrafficsim.base.modelproperties.PropertyException;
import org.opentrafficsim.core.dsol.OTSDEVSSimulatorInterface;
import org.opentrafficsim.core.dsol.OTSModelInterface;
import org.opentrafficsim.core.dsol.OTSSimTimeDouble;
import org.opentrafficsim.core.geometry.OTSGeometryException;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.gtu.animation.GTUColorer;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.core.network.OTSNode;
import org.opentrafficsim.simulationengine.AbstractWrappableAnimation;
import org.opentrafficsim.simulationengine.OTSSimulationException;
import org.opentrafficsim.simulationengine.SimpleSimulatorInterface;

import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.animation.D2.GisRenderable2D;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.language.d3.DirectedPoint;
import nl.tudelft.simulation.language.io.URLResource;

/**
 * <p>
 * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="http://opentrafficsim.org/docs/current/license.html">OpenTrafficSim License</a>.
 * </p>
 * $LastChangedDate: 2015-07-24 02:58:59 +0200 (Fri, 24 Jul 2015) $, @version $Revision: 1147 $, by $Author: averbraeck $,
 * initial version Jan 5, 2017 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TestBmmsWwRmmsModelParser extends AbstractWrappableAnimation
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private GisWaterwayImport gisWaterwayImport;

    /**
     * Main program.
     * @param args String[]; the command line arguments (not used)
     * @throws SimRuntimeException should never happen
     */
    public static void main(final String[] args) throws SimRuntimeException
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    TestBmmsWwRmmsModelParser waterwayModel = new TestBmmsWwRmmsModelParser();
                    // 1 hour simulation run for testing
                    waterwayModel.buildAnimator(Time.ZERO, Duration.ZERO, new Duration(60.0, TimeUnit.MINUTE),
                            new ArrayList<org.opentrafficsim.base.modelproperties.Property<?>>(), null, true);
                }
                catch (SimRuntimeException | NamingException | OTSSimulationException | PropertyException exception)
                {
                    exception.printStackTrace();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public final String shortName()
    {
        return "TestWaterwayModelParser";
    }

    /** {@inheritDoc} */
    @Override
    public final String description()
    {
        return "TestWaterwayModelParser";
    }

    /** {@inheritDoc} */
    @Override
    public final void stopTimersThreads()
    {
        super.stopTimersThreads();
    }

    /** {@inheritDoc} */
    @Override
    protected final void addTabs(final SimpleSimulatorInterface simulator)
    {
        return;
    }

    /** {@inheritDoc} */
    @Override
    protected final OTSModelInterface makeModel(final GTUColorer colorer)
    {
        this.gisWaterwayImport = new GisWaterwayImport();
        return this.gisWaterwayImport;
    }

    /** {@inheritDoc} */
    @Override
    protected final java.awt.geom.Rectangle2D.Double makeAnimationRectangle()
    {
        return new Rectangle2D.Double(87.8, 20.2, 5.0, 6.6);
    }

    /** {@inheritDoc} */
    @Override
    protected void addAnimationToggles()
    {
        this.addToggleAnimationButtonText("LRPs", LRP.class, "Show/hide LRPs", false);
        this.addToggleAnimationButtonText("LRPId", LRPTextAnimation.class, "Show/hide LRP Ids", false);
        this.addToggleAnimationButtonText("Waterways", Waterway.class, "Show/hide waterways", true);
        this.addToggleAnimationButtonText("WaterwayId", WaterwayTextAnimation.class, "Show/hide waterway Ids", false);
        this.addToggleAnimationButtonText("N-Roads", RoadN.class, "Show/hide N-roads", true);
        this.addToggleAnimationButtonText("N-RoadId", RoadNTextAnimation.class, "Show/hide N-road Ids", false);
        this.addToggleAnimationButtonText("R-Roads", RoadR.class, "Show/hide R-roads", true);
        this.addToggleAnimationButtonText("R-RoadId", RoadRTextAnimation.class, "Show/hide R-road Ids", false);
        this.addToggleAnimationButtonText("Z-Roads", RoadZ.class, "Show/hide Z-roads", true);
        this.addToggleAnimationButtonText("Z-RoadId", RoadZTextAnimation.class, "Show/hide Z-road Ids", false);
        // this.addToggleAnimationButtonText("GTU", GTU.class, "Show/hide ships", true);
        // this.addToggleAnimationButtonText("GTUId", DefaultCarAnimation.Text.class, "Show/hide ship Ids", false);
        this.addToggleAnimationButtonText("Bridge", BridgeBGD.class, "Show/hide bridges", true);
        this.addToggleAnimationButtonText("BridgeId", BridgeTextAnimation.class, "Show/hide bridge Ids", false);

        this.panel.addToggleText(" ");
        this.panel.addToggleText(" GIS Layers");
        this.panel.addToggleGISButtonText("roads", "Roads", this.gisWaterwayImport.getGisMap(),
                "Turn GIS road map layer on or off");
        this.panel.addToggleGISButtonText("buildings", "Buildings", this.gisWaterwayImport.getGisMap(),
                "Turn GIS building map layer on or off");
        this.panel.addToggleGISButtonText("railways", "Railways", this.gisWaterwayImport.getGisMap(),
                "Turn GIS rail map layer on or off");
        this.panel.addToggleGISButtonText("waterways", "Waterways", this.gisWaterwayImport.getGisMap(),
                "Turn GIS waterway map layer on or off");
        this.panel.addToggleGISButtonText("wfp-river", "Rivers", this.gisWaterwayImport.getGisMap(),
                "Turn GIS waterway map layer of the WFP on or off");
        this.panel.hideGISLayer("buildings");
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return "TestWaterwayModelParser []";
    }

    /**
     * Model to test the Esri Shape File Format parser.
     * <p>
     * Copyright (c) 2013-2016 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. <br>
     * All rights reserved. BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim
     * License</a>.
     * <p>
     * $LastChangedDate: 2016-08-26 16:34:41 +0200 (Fri, 26 Aug 2016) $, @version $Revision: 2150 $, by $Author: gtamminga $,
     * initial version un 27, 2015 <br>
     * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
     */
    protected static class GisWaterwayImport implements OTSModelInterface
    {
        /** */
        private static final long serialVersionUID = 20141121L;

        /** The simulator. */
        private OTSDEVSSimulatorInterface simulator;

        /** The network. */
        private final OTSNetwork network = new OTSNetwork("Bangladesh waterway test network");

        /** the GIS map. */
        protected GisRenderable2D gisMap;

        /** {@inheritDoc} */
        @Override
        public final void constructModel(final SimulatorInterface<Time, Duration, OTSSimTimeDouble> pSimulator)
                throws SimRuntimeException
        {

            this.simulator = (OTSDEVSSimulatorInterface) pSimulator;

            try
            {
                getWaterways("infrastructure/water/WaterwaysSailable/", "waterways_53routes_routable_final_processed");

                // readBridgesWorldBank("/infrastructure/Bridges.xlsx");
                readBMMS("/infrastructure/BMMS_overview.xlsx");
                if (new File("/infrastructure/_roads3.csv").canRead())
                {
                    readRoadsCsv3("/infrastructure/_roads3.csv");
                }
                else if (new File("/infrastructure/_roads2.csv").canRead())
                {
                    readRoadsCsv2("/infrastructure/_roads2.csv");
                }
                else if (new File("/infrastructure/_roads.tsv").canRead())
                {
                    readRoadsTsv("/infrastructure/_roads.tcv");
                }
                else
                {
                    readRoadsCsv3("/infrastructure/_roads3.csv"); // try resource stream
                }
            }
            catch (Exception nwe)
            {
                nwe.printStackTrace();
            }

            // background
            URL gisURL = URLResource.getResource("/gis/map.xml");
            System.out.println("GIS-map file: " + gisURL.toString());
            this.gisMap = new GisRenderable2D(this.simulator, gisURL);
        }

        /**
         * @return gisMap
         */
        public final GisRenderable2D getGisMap()
        {
            return this.gisMap;
        }

        /**
         * Import a list of waterway (link) elements from a shape file
         * @param initialDir dir
         * @param fileName file name
         * @throws NetworkException on read error
         * @throws OTSGeometryException when design line not proper
         */
        private void getWaterways(String initialDir, String fileName) throws NetworkException, OTSGeometryException
        {
            FileDataStore dataStoreLink = null;
            try
            {
                dataStoreLink = newDatastore(initialDir, fileName);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // open and read shape file links
            SimpleFeatureIterator featureIterator = getFeatureIterator(dataStoreLink);

            // loop through the features and first retrieve the geometry
            while (featureIterator.hasNext())
            {
                Feature feature = featureIterator.next();
                try
                {
                    Waterway ww = getPropertiesWW(feature);
                    System.out.println(ww);
                }
                catch (OTSGeometryException ge)
                {
                    ge.printStackTrace();
                }
            }
        }

        /**
         * @param initialDir initial directory
         * @param fileName file name without .shp
         * @return shape file data store
         * @throws IOException on read error
         */
        private FileDataStore newDatastore(String initialDir, final String fileName) throws IOException
        {
            try
            {
                URL url = TestBmmsWwRmmsModelParser.class.getResource("/");
                File file = new File(url.getFile() + initialDir);
                String fn = file.getCanonicalPath();
                fn = fn.replace('\\', '/');
                File iniDir = new File(fn);
                file = new File(iniDir, fileName + ".shp");

                FileDataStore dataStoreLink = FileDataStoreFinder.getDataStore(file);
                return dataStoreLink;

            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
            return null;

        }

        /**
         * Return an iterator.
         * @param dataStore the shape file store
         * @return iterator
         */
        private SimpleFeatureIterator getFeatureIterator(FileDataStore dataStore)
        {
            try
            {
                String[] typeNameLink = dataStore.getTypeNames();
                SimpleFeatureSource sourceLink;
                sourceLink = dataStore.getFeatureSource(typeNameLink[0]);
                SimpleFeatureCollection featuresLink = sourceLink.getFeatures();
                return featuresLink.features();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        /** counter. */
        static int count = 0;

        /**
         * @param feature the feature from the shape file
         * @return one road element with properties
         * @throws NetworkException on error
         * @throws OTSGeometryException when design line not proper
         */
        private Waterway getPropertiesWW(final Feature feature) throws NetworkException, OTSGeometryException
        {
            Geometry theGeom = (Geometry) feature.getDefaultGeometryProperty().getValue();
            Coordinate[] coordinates = theGeom.getCoordinates();

            // {osm_id=6, null=0, fix_id=2, length=4, name=5, width=8, Class=1, id=3, type=7, the_geom=0, Linestring=9}

            Property property = feature.getProperty("osm_id");
            String osmId = property.getValue().toString();
            property = feature.getProperty("fix_id");
            String fixId = property.getValue().toString();
            property = feature.getProperty("length");
            double length = parseDouble(property);
            property = feature.getProperty("name");
            String name = property.getValue().toString();
            name = name.length() == 0 ? UUID.randomUUID().toString() : name;
            property = feature.getProperty("width");
            double width = parseDouble(property);
            property = feature.getProperty("Class");
            double wwClass = parseDouble(property);
            property = feature.getProperty("id");
            String id = property.getValue().toString();
            property = feature.getProperty("type");
            String type = property.getValue().toString();

            String wwIdBegin = name + ".b";
            String wwIdEnd = name + ".e";

            OTSNode startNode = null;
            OTSPoint3D ptBegin = new OTSPoint3D(coordinates[0]);
            while (this.network.getNodeMap().keySet().contains(wwIdBegin))
            {
                wwIdBegin += "$";
            }
            for (Node node : this.network.getNodeMap().values())
            {
                if (node.getPoint().equals(ptBegin))
                {
                    wwIdBegin = node.getId();
                    ptBegin = node.getPoint();
                    startNode = (OTSNode) node;
                    break;
                }
            }
            if (startNode == null)
            {
                startNode = new OTSNode(this.network, wwIdBegin, ptBegin);
            }

            OTSNode endNode = null;
            OTSPoint3D ptEnd = new OTSPoint3D(coordinates[coordinates.length - 1]);
            while (this.network.getNodeMap().keySet().contains(wwIdEnd))
            {
                wwIdEnd += "$";
            }
            for (Node node : this.network.getNodeMap().values())
            {
                if (node.getPoint().equals(ptEnd))
                {
                    wwIdEnd = node.getId();
                    ptEnd = node.getPoint();
                    endNode = (OTSNode) node;
                    break;
                }
            }
            if (endNode == null)
            {
                endNode = new OTSNode(this.network, wwIdEnd, ptEnd);
            }

            while (this.network.getLinkMap().keySet().contains(name))
            {
                name += "$";
            }

            OTSLine3D designLine = new OTSLine3D(coordinates);

            Waterway ww = new Waterway(this.network, name, startNode, endNode, LinkType.ALL, designLine, this.simulator,
                    LongitudinalDirectionality.DIR_BOTH);
            try
            {
                new WaterwayAnimation(ww, this.simulator, (float) ((5.0 - wwClass) * 0.002));
            }
            catch (RemoteException | NamingException exception)
            {
                exception.printStackTrace();
            }
            return ww;
        }

        /**
         * @param property the property
         * @return a double
         */
        private Double parseDouble(Property property)
        {
            if (property.getValue() != null)
            {
                if (property.getValue().toString() != null && property.getValue().toString().length() > 0)
                {
                    return Double.parseDouble(property.getValue().toString());
                }
            }
            return Double.NaN;
        }

        private void readBridgesWorldBank(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestBmmsWwRmmsModelParser.class.getResource(filename).getFile());
            XSSFWorkbook wbNuts = new XSSFWorkbook(fis);

            XSSFSheet sheet1 = wbNuts.getSheet("Bridge types and attributes");
            boolean firstRow = true;
            for (Row row : sheet1)
            {
                if (!firstRow)
                {
                    try
                    {
                        String roadNo = ExcelUtil.cellValue(row, "M");
                        String condition = ExcelUtil.cellValue(row, "D");
                        String type = ExcelUtil.cellValue(row, "C");
                        double width = ExcelUtil.cellValueDouble(row, "E");
                        double length = ExcelUtil.cellValueDouble(row, "F");
                        int constructionYear = ExcelUtil.cellValueInt(row, "G");
                        int numberOfSpans = ExcelUtil.cellValueInt(row, "H");

                        String name = ExcelUtil.cellValue(row, "B");
                        double latH = ExcelUtil.cellValueDouble(row, "S");
                        double latM = ExcelUtil.cellValueDouble(row, "T");
                        double latS = ExcelUtil.cellValueDouble(row, "U");
                        double lat = latH + latM / 60.0 + latS / 3600.0;
                        double lonH = ExcelUtil.cellValueDouble(row, "V");
                        double lonM = ExcelUtil.cellValueDouble(row, "W");
                        double lonS = ExcelUtil.cellValueDouble(row, "X");
                        double lon = lonH + lonM / 60.0 + lonS / 3600.0;
                        BridgeBGD b = new BridgeBGD(name, new DirectedPoint(lon, lat, 5.0), roadNo, condition, type, width,
                                length, constructionYear, numberOfSpans);

                        Color color;
                        switch (condition)
                        {
                            case "A":
                                color = Color.GREEN;
                                break;

                            case "B":
                                color = Color.ORANGE;
                                break;

                            case "C":
                                color = Color.RED;
                                break;

                            case "D":
                                color = Color.BLACK;
                                break;

                            default:
                                color = Color.DARK_GRAY;
                                break;
                        }

                        new BridgeAnimation(b, this.simulator, color);
                    }
                    catch (Exception e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
                firstRow = false;
            }
        }

        private void readBMMS(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestBmmsWwRmmsModelParser.class.getResource(filename).getFile());
            XSSFWorkbook wbNuts = new XSSFWorkbook(fis);

            XSSFSheet sheet1 = wbNuts.getSheet("BMMS_overview");
            boolean firstRow = true;
            for (Row row : sheet1)
            {
                if (!firstRow)
                {
                    try
                    {
                        String roadNo = ExcelUtil.cellValue(row, "A");
                        String condition = ExcelUtil.cellValue(row, "G");
                        String type = ExcelUtil.cellValue(row, "C");
                        double width = ExcelUtil.cellValueDoubleNull(row, "K");
                        double length = ExcelUtil.cellValueDoubleNull(row, "F");
                        int constructionYear = ExcelUtil.cellValueIntNull(row, "L");
                        int numberOfSpans = ExcelUtil.cellValueIntNull(row, "M");

                        String name = ExcelUtil.cellValue(row, "E");
                        double lat = ExcelUtil.cellValueDoubleNull(row, "R");
                        double lon = ExcelUtil.cellValueDoubleNull(row, "S");
                        if (lat > 0 && lon > 0)
                        {
                            BridgeBGD b = new BridgeBGD(name, new DirectedPoint(lon, lat, 5.0), roadNo, condition, type, width,
                                    length, constructionYear, numberOfSpans);

                            Color color;
                            switch (condition)
                            {
                                case "A":
                                    color = Color.GREEN;
                                    break;

                                case "B":
                                    color = Color.ORANGE;
                                    break;

                                case "C":
                                    color = Color.RED;
                                    break;

                                case "D":
                                    color = Color.BLACK;
                                    break;

                                default:
                                    color = Color.DARK_GRAY;
                                    break;
                            }

                            new BridgeAnimation(b, this.simulator, color);
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.println(e.getMessage());
                    }
                }
                firstRow = false;
            }
        }

        /**
         * Read the roads with LRP coordinates
         * @param filename filename
         * @throws Exception on I/O error
         */
        private void readRoadsTsv(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestBmmsWwRmmsModelParser.class.getResource(filename).getFile());
            String line = "";
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(fis)))
            {
                line = buf.readLine(); // skip 1st line with headers
                line = buf.readLine();
                while (line != null)
                {
                    String[] parts = line.split("\t");
                    String roadName = parts[0];
                    int i = 1;
                    LRP node = null;
                    LRP lastNode = null;
                    while (i < parts.length)
                    {
                        String lrps = roadName + "_" + parts[i];
                        String lats = parts[i + 1];
                        String lons = parts[i + 2];
                        double lat = Double.parseDouble(lats);
                        double lon = Double.parseDouble(lons);
                        if (lastNode == null || (lon != lastNode.getPoint().x && lat != lastNode.getPoint().y)) // degenerate
                        {
                            while (this.network.containsNode(lrps)) // name clash
                            {
                                lrps += "+";
                            }
                            node = new LRP(this.network, lrps, new OTSPoint3D(lon, lat, 0.0));
                            new LRPAnimation(node, this.simulator, Color.BLUE);
                            if (lastNode != null) // 2 points needed for a line
                            {
                                OTSLine3D designLine = new OTSLine3D(lastNode.getPoint(), node.getPoint());
                                if (roadName.startsWith("N"))
                                {
                                    String linkName = roadName + "_" + lastNode.getId() + "-" + node.getId();
                                    RoadN r = new RoadN(this.network, linkName, lastNode, node, LinkType.ALL, designLine,
                                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                    new RoadNAnimation(r, roadName, this.simulator, (float) (5.0 * 0.0005));
                                }
                                if (roadName.startsWith("R"))
                                {
                                    String linkName = roadName + "_" + lastNode.getId() + "-" + node.getId();
                                    RoadR r = new RoadR(this.network, linkName, lastNode, node, LinkType.ALL, designLine,
                                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                    new RoadRAnimation(r, roadName, this.simulator, (float) (3.0 * 0.0005));
                                }
                                if (roadName.startsWith("Z"))
                                {
                                    String linkName = roadName + "_" + lastNode.getId() + "-" + node.getId();
                                    RoadZ r = new RoadZ(this.network, linkName, lastNode, node, LinkType.ALL, designLine,
                                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                    new RoadZAnimation(r, roadName, this.simulator, (float) (1.0 * 0.0005));
                                }
                            }
                        }
                        lastNode = node;
                        i += 3;
                    }
                    line = buf.readLine();
                }
            }
        }

        /**
         * Read the roads with LRP coordinates
         * @param filename filename
         * @throws Exception on I/O error
         */
        private void readRoadsCsv2(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestBmmsWwRmmsModelParser.class.getResource(filename).getFile());
            try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
            {
                String[] parts;
                LRP lastLRP = null;
                String lastRoad = "";
                while ((parts = reader.readNext()) != null)
                {
                    String roadName = parts[0];
                    boolean newRoad = !roadName.equals(lastRoad);
                    if (newRoad)
                    {
                        lastLRP = null;
                    }
                    LRP lrp = null;
                    String lrps = roadName + "_" + parts[2];
                    String chas = parts[1];
                    String lats = parts[3];
                    String lons = parts[4];
                    double lat = Double.parseDouble(lats);
                    double lon = Double.parseDouble(lons);
                    double chainage = Double.parseDouble(chas);
                    String type = parts[5];
                    String name = parts[6];
                    if (lastLRP == null || lon != lastLRP.getPoint().x || lat != lastLRP.getPoint().y) // no degenerate
                    {
                        while (this.network.containsNode(lrps)) // name clash
                        {
                            lrps += "+";
                        }
                        lrp = new LRP(this.network, lrps, new OTSPoint3D(lon, lat, 0.0), chainage, type, name, GapPoint.ROAD);
                        new LRPAnimation(lrp, this.simulator, Color.BLUE);
                        if (lastLRP != null) // 2 points needed for a line
                        {
                            OTSLine3D designLine = new OTSLine3D(lastLRP.getPoint(), lrp.getPoint());
                            if (roadName.startsWith("N"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadN r = new RoadN(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                new RoadNAnimation(r, roadName, this.simulator, (float) (5.0 * 0.0005));
                            }
                            if (roadName.startsWith("R"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadR r = new RoadR(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                new RoadRAnimation(r, roadName, this.simulator, (float) (3.0 * 0.0005));
                            }
                            if (roadName.startsWith("Z"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadZ r = new RoadZ(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                                new RoadZAnimation(r, roadName, this.simulator, (float) (1.0 * 0.0005));
                            }
                        }
                        lastLRP = lrp;
                    }
                    lastRoad = roadName;
                }
            }
        }

        /**
         * Read the roads with LRP coordinates
         * @param filename filename
         * @throws Exception on I/O error
         */
        private void readRoadsCsv3(final String filename) throws Exception
        {
            FileInputStream fis;
            if (new File(filename).canRead())
                fis = new FileInputStream(filename);
            else
                fis = new FileInputStream(TestBmmsWwRmmsModelParser.class.getResource(filename).getFile());
            try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
            {
                String[] parts;
                LRP lastLRP = null;
                String lastRoad = "";
                while ((parts = reader.readNext()) != null)
                {
                    String roadName = parts[0];
                    boolean newRoad = !roadName.equals(lastRoad);
                    if (newRoad)
                    {
                        lastLRP = null;
                    }
                    LRP lrp = null;
                    String lrps = roadName + "_" + parts[2];
                    String chas = parts[1];
                    String lats = parts[3];
                    String lons = parts[4];
                    double lat = Double.parseDouble(lats);
                    double lon = Double.parseDouble(lons);
                    double chainage = Double.parseDouble(chas);
                    String bf = parts[5];
                    String type = parts[6];
                    String name = parts[7];
                    if (lastLRP == null || lon != lastLRP.getPoint().x || lat != lastLRP.getPoint().y) // no degenerate
                    {
                        while (this.network.containsNode(lrps)) // name clash
                        {
                            lrps += "+";
                        }
                        GapPoint gapPoint;
                        switch (bf)
                        {
                            case "GS":
                                gapPoint = GapPoint.GAP_START;
                                break;

                            case "GE":
                                gapPoint = GapPoint.GAP_END;
                                break;

                            case "BS":
                                gapPoint = GapPoint.BRIDGE_START;
                                break;

                            case "BE":
                                gapPoint = GapPoint.BRIDGE_END;
                                break;

                            case "FS":
                                gapPoint = GapPoint.FERRY_START;
                                break;

                            case "FE":
                                gapPoint = GapPoint.FERRY_END;
                                break;

                            default:
                                gapPoint = GapPoint.ROAD;
                                break;
                        }
                        lrp = new LRP(this.network, lrps, new OTSPoint3D(lon, lat, 0.0), chainage, type, name, gapPoint);
                        new LRPAnimation(lrp, this.simulator, Color.BLUE);
                        if (lastLRP != null) // 2 points needed for a line
                        {
                            Gap gap = Gap.ROAD;
                            if (gapPoint.isBridgeEnd() && lastLRP.getGapPoint().isBridgeStart())
                            {
                                gap = Gap.BRIDGE;
                            }
                            else if (gapPoint.isGapEnd() && lastLRP.getGapPoint().isGapStart())
                            {
                                gap = Gap.GAP;
                            }
                            else if (gapPoint.isFerryEnd() && lastLRP.getGapPoint().isFerryStart())
                            {
                                gap = Gap.FERRY;
                            }

                            OTSLine3D designLine = new OTSLine3D(lastLRP.getPoint(), lrp.getPoint());
                            if (roadName.startsWith("N"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadN r = new RoadN(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadNAnimation(r, roadName, this.simulator, (float) (5.0 * 0.0005));
                            }
                            if (roadName.startsWith("R"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadR r = new RoadR(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadRAnimation(r, roadName, this.simulator, (float) (3.0 * 0.0005));
                            }
                            if (roadName.startsWith("Z"))
                            {
                                String linkName = lastLRP.getId() + "-" + lrp.getId();
                                RoadZ r = new RoadZ(this.network, linkName, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadZAnimation(r, roadName, this.simulator, (float) (1.0 * 0.0005));
                            }
                        }
                        lastLRP = lrp;
                    }
                    lastRoad = roadName;
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public SimulatorInterface<Time, Duration, OTSSimTimeDouble> getSimulator()
        {
            return this.simulator;
        }

        /** {@inheritDoc} */
        @Override
        public OTSNetwork getNetwork()
        {
            return this.network;
        }

        /** {@inheritDoc} */
        @Override
        public final String toString()
        {
            return "GisWaterwayImport [simulator=" + this.simulator + "]";
        }

    }

}
