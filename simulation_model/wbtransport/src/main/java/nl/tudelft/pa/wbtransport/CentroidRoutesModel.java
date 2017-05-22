package nl.tudelft.pa.wbtransport;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.naming.NamingException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opentrafficsim.core.dsol.OTSDEVSSimulatorInterface;
import org.opentrafficsim.core.dsol.OTSModelInterface;
import org.opentrafficsim.core.dsol.OTSSimTimeDouble;
import org.opentrafficsim.core.geometry.OTSGeometryException;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.gtu.GTUType;
import org.opentrafficsim.core.network.Link;
import org.opentrafficsim.core.network.LinkEdge;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LongitudinalDirectionality;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.core.network.OTSNode;
import org.opentrafficsim.core.network.route.CompleteRoute;

import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import nl.tudelft.pa.wbtransport.bridge.BridgeBGD;
import nl.tudelft.pa.wbtransport.bridge.animation.BridgeAnimation;
import nl.tudelft.pa.wbtransport.district.District;
import nl.tudelft.pa.wbtransport.district.DistrictReader;
import nl.tudelft.pa.wbtransport.road.Gap;
import nl.tudelft.pa.wbtransport.road.GapPoint;
import nl.tudelft.pa.wbtransport.road.LRP;
import nl.tudelft.pa.wbtransport.road.Road;
import nl.tudelft.pa.wbtransport.road.RoadSegment;
import nl.tudelft.pa.wbtransport.road.SegmentN;
import nl.tudelft.pa.wbtransport.road.SegmentR;
import nl.tudelft.pa.wbtransport.road.SegmentZ;
import nl.tudelft.pa.wbtransport.road.animation.LRPAnimation;
import nl.tudelft.pa.wbtransport.road.animation.RoadNAnimation;
import nl.tudelft.pa.wbtransport.road.animation.RoadRAnimation;
import nl.tudelft.pa.wbtransport.road.animation.RoadZAnimation;
import nl.tudelft.pa.wbtransport.util.ExcelUtil;
import nl.tudelft.pa.wbtransport.water.Waterway;
import nl.tudelft.pa.wbtransport.water.animation.WaterwayAnimation;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.language.d3.DirectedPoint;
import nl.tudelft.simulation.language.io.URLResource;

/**
 * Model to test the Esri Shape File Format parser.
 * <p>
 * Copyright (c) 2013-2017 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. <br>
 * All rights reserved. BSD-style license. See <a href="http://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * <p>
 * $LastChangedDate: 2016-08-26 16:34:41 +0200 (Fri, 26 Aug 2016) $, @version $Revision: 2150 $, by $Author: gtamminga $,
 * initial version un 27, 2015 <br>
 * @author <a href="http://www.tbm.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class CentroidRoutesModel implements OTSModelInterface
{
    /** */
    private static final long serialVersionUID = 20141121L;

    /** The simulator. */
    private OTSDEVSSimulatorInterface simulator;

    /** The network. */
    private final OTSNetwork network = new OTSNetwork("Bangladesh waterway test network");

    /** the districts by code. */
    private SortedMap<String, District> districtCodeMap = new TreeMap<>();

    /** the districts by name. */
    private SortedMap<String, District> districtNameMap = new TreeMap<>();

    /** the districts by 2-letter code. */
    private SortedMap<String, District> districtCode2Map = new TreeMap<>();

    /** lrps. */
    private SortedMap<String, LRP> lrpMap = new TreeMap<>();

    /** lrps per district. */
    private SortedMap<District, List<LRP>> districtLRPMap = new TreeMap<>();

    /** coordinates. */
    private Map<OTSPoint3D, LRP> pointLRPMap = new HashMap<>();

    /** the roads. */
    private SortedMap<String, Road> roadMap = new TreeMap<>();

    /** road graph. */
    SimpleDirectedWeightedGraph<Node, LinkEdge<Link>> graph;

    /** flooded districts by code2. */
    private Set<String> floodSet = new HashSet<>();

    /** products. */
    private List<String> productList = new ArrayList<>();

    /** the production per good per district by 2-letter code. */
    private SortedMap<String, SortedMap<String, District>> productProdMap = new TreeMap<>();

    /** the consumption per good per district by 2-letter code. */
    private SortedMap<String, SortedMap<String, District>> productConsMap = new TreeMap<>();

    /** the production - consumption interarrival times per metric ton per product. */
    private SortedMap<String, SortedMap<String, SortedMap<String, Double>>> pcMap = new TreeMap<>();

    /** the model parameters. */
    Map<String, Double> parameters = new HashMap<>();

    /** the parameter floodArea. */
    String parameterFloodArea;

    /** the results. */
    Map<String, Double> outputTransportCost = new HashMap<>();

    /** the results. */
    Map<String, Double> outputTravelTime = new HashMap<>();

    /** the results. */
    Map<String, Double> outputUnsatisfiedDemand = new HashMap<>();

    /** the file folder as the root for all files. */
    String fileFolder;

    /** {@inheritDoc} */
    @Override
    public final void constructModel(final SimulatorInterface<Time, Duration, OTSSimTimeDouble> pSimulator)
            throws SimRuntimeException
    {
        this.simulator = (OTSDEVSSimulatorInterface) pSimulator;

        try
        {
            readProductList(this.fileFolder + "/infrastructure/products.txt");
            readFloodSet(this.parameterFloodArea, this.fileFolder + "/infrastructure/flood_locations.csv");
            readDistricts(this.fileFolder + "/gis/gadm/BGD_adm2.shp");
            getWaterways(
                    this.fileFolder + "/infrastructure/water/WaterwaysSailable/waterways_53routes_routable_final_processed.shp");

            // readBridgesWorldBank("/infrastructure/Bridges.xlsx");
            readBMMS(this.fileFolder + "/infrastructure/BMMS_overview.xlsx");

            URL roadCsv5 = URLResource.getResource(this.fileFolder + "/infrastructure/_roads5.csv");
            URL roadCsv4 = URLResource.getResource(this.fileFolder + "/infrastructure/_roads4.csv");
            if (roadCsv5 != null && new File(roadCsv5.getPath()).canRead())
            {
                readLRPsCsv5(this.fileFolder + "/infrastructure/_lrps5.csv");
                readRoadsCsv5(this.fileFolder + "/infrastructure/_roads5.csv");
                findDistrictCentroids();
                calculateDistrictDistances();
            }
            else if (roadCsv4 != null && new File(roadCsv4.getPath()).canRead())
            {
                readRoadsCsv34(this.fileFolder + "/infrastructure/_roads4.csv");
                findDistrictCentroids();
                repairSmallGapsCrossings();
                calculateDistrictDistances();
                writeLRPsCsv5(this.fileFolder + "/infrastructure/_lrps5.csv");
                writeRoadsCsv5(this.fileFolder + "/infrastructure/_roads5.csv");
            }
            else
            {
                System.err.println("Cannot read input file with LRPs");
                System.exit(-1);
            }
        }
        catch (Exception nwe)
        {
            nwe.printStackTrace();
        }
    }

    private void readProductList(final String filename) throws Exception
    {
        System.out.println("Read " + filename);
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            String[] parts;
            while ((parts = reader.readNext()) != null)
            {
                String product = parts[0].trim();
                if (product.length() > 0)
                {
                    this.productList.add(product);
                    this.outputTransportCost.put(product, 0.0);
                    this.outputTravelTime.put(product, 0.0);
                    this.outputUnsatisfiedDemand.put(product, 0.0);
                    System.out.println("Added product: " + product);
                }
            }
        }
    }

    /**
     * Import a list of waterway (link) elements from a shape file
     * @param filename file name
     * @throws NetworkException on read error
     * @throws OTSGeometryException when design line not proper
     * @throws IOException on i/o error
     */
    private void getWaterways(String filename) throws NetworkException, OTSGeometryException, IOException
    {
        System.out.println("Read 53 waterways");
        URL url;
        if (new File(filename).canRead())
            url = new File(filename).toURI().toURL();
        else
            url = DistrictReader.class.getResource(filename);
        FileDataStore dataStoreLink = FileDataStoreFinder.getDataStore(url);

        // iterate over the features
        SimpleFeatureSource featureSource = dataStoreLink.getFeatureSource();
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        SimpleFeatureIterator featureIterator = featureCollection.features();
        while (featureIterator.hasNext())
        {
            SimpleFeature feature = featureIterator.next();
            try
            {
                Waterway ww = getPropertiesWW(feature);
                // System.out.println(ww);
            }
            catch (OTSGeometryException ge)
            {
                ge.printStackTrace();
            }
        }
        featureIterator.close();
        dataStoreLink.dispose();
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
            URL url = CentroidRoutesEMA.class.getResource("/");
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
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
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
                    BridgeBGD b = new BridgeBGD(name, new DirectedPoint(lon, lat, 5.0), roadNo, condition, type, width, length,
                            constructionYear, numberOfSpans);

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
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
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
     * Read the Districts
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void readDistricts(final String filename) throws Exception
    {
        System.out.println("read districts");
        URL url;
        if (new File(filename).canRead())
            url = new File(filename).toURI().toURL();
        else
            url = DistrictReader.class.getResource(filename);
        FileDataStore storeDistricts = FileDataStoreFinder.getDataStore(url);

        // CoordinateReferenceSystem worldCRS = CRS.decode("EPSG:4326");

        // iterate over the features
        SimpleFeatureSource featureSourceAdm2 = storeDistricts.getFeatureSource();
        SimpleFeatureCollection featureCollectionAdm2 = featureSourceAdm2.getFeatures();
        SimpleFeatureIterator iterator = featureCollectionAdm2.features();
        try
        {
            while (iterator.hasNext())
            {
                SimpleFeature feature = iterator.next();
                MultiPolygon polygon = (MultiPolygon) feature.getAttribute("the_geom");
                String code = feature.getAttribute("ID_2").toString();
                String name = feature.getAttribute("NAME_2").toString();
                String code2 = feature.getAttribute("HASC_2").toString().substring(6, 8);
                District district = new District(this.simulator, code, name, code2, polygon);
                this.districtCodeMap.put(code, district);
                this.districtNameMap.put(name, district);
                this.districtCode2Map.put(code2, district);
                this.districtLRPMap.put(district, new ArrayList<>());
                // System.out.println(code + "," + name + ", " + code2);

                if (this.floodSet.contains(code2))
                {
                    district.setFlooded(true);
                }
            }
        }
        catch (Exception problem)
        {
            problem.printStackTrace();
        }
        finally
        {
            iterator.close();
        }
        storeDistricts.dispose();
    }

    /**
     * Read the roads with LRP coordinates and make it part of a network for which we can calculate shortest path.
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void readRoadsCsv34(final String filename) throws Exception
    {
        System.out.println("Read " + filename);
        District lastDistrict = null;
        GeometryFactory geometryFactory = new GeometryFactory();
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            String[] parts;
            LRP lastLRP = null;
            String lastRoad = "";
            while ((parts = reader.readNext()) != null)
            {
                String roadId = parts[0];
                boolean newRoad = !roadId.equals(lastRoad);
                if (newRoad)
                {
                    lastLRP = null;
                }
                LRP lrp = null;
                String lrps = roadId + "_" + parts[2];
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
                    GapPoint gapPoint = GapPoint.getInstance(bf);

                    // see what district.
                    District district = null;
                    Point p = geometryFactory.createPoint(new Coordinate(lon, lat));
                    if (lastDistrict != null)
                    {
                        if (lastDistrict.getPolygon().contains(p))
                        {
                            district = lastDistrict;
                        }
                    }
                    if (district == null)
                    {
                        for (District d : this.districtCodeMap.values())
                        {
                            if (d.getPolygon().contains(p))
                            {
                                district = d;
                                lastDistrict = d;
                                break;
                            }
                        }
                    }
                    if (district == null)
                    {
                        System.out.print(
                                "cannot find district of LRP " + lrps + " at (" + lat + "," + lon + "). Searching boxes... ");
                        for (District d : this.districtCodeMap.values())
                        {
                            if (d.getPolygon().getEnvelope().contains(p))
                            {
                                district = d;
                                System.out.println("Found " + d.getCode2() + ": " + d.getName());
                                lastDistrict = null;
                                break;
                            }
                        }
                    }
                    if (district == null)
                    {
                        System.out.println("Assumed Dhaka...");
                        district = this.districtCode2Map.get("DH");
                    }

                    if (!this.roadMap.containsKey(roadId))
                    {
                        this.roadMap.put(roadId, new Road(roadId));
                    }
                    Road road = this.roadMap.get(roadId);

                    OTSPoint3D coordinate = new OTSPoint3D(lon, lat, 0.0);
                    if (this.pointLRPMap.containsKey(coordinate))
                    {
                        // use the existing LRP
                        lrp = this.pointLRPMap.get(coordinate);
                        lrp.addRoad(road);
                    }
                    else
                    {
                        lrp = new LRP(this.network, lrps, coordinate, road, chainage, type, name, gapPoint, district);
                        this.pointLRPMap.put(coordinate, lrp);
                        this.lrpMap.put(name, lrp);
                        if (district != null)
                        {
                            this.districtLRPMap.get(district).add(lrp);
                        }
                        new LRPAnimation(lrp, this.simulator, Color.BLUE);
                    }

                    if (lastLRP != null) // 2 points needed for a line
                    {
                        String linkName = lastLRP.getId() + "-" + lrp.getId();
                        if (!this.network.containsLink(linkName))
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
                            if (roadId.startsWith("N"))
                            {
                                SegmentN r = new SegmentN(this.network, linkName, road, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadNAnimation(r, roadId, this.simulator, (float) (5.0 * 0.0005), Color.BLACK);
                                road.addSegment(r);
                            }
                            if (roadId.startsWith("R"))
                            {
                                SegmentR r = new SegmentR(this.network, linkName, road, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadRAnimation(r, roadId, this.simulator, (float) (3.0 * 0.0005), Color.BLACK);
                                road.addSegment(r);
                            }
                            if (roadId.startsWith("Z"))
                            {
                                SegmentZ r = new SegmentZ(this.network, linkName, road, lastLRP, lrp, LinkType.ALL, designLine,
                                        this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                                new RoadZAnimation(r, roadId, this.simulator, (float) (1.0 * 0.0005), Color.BLACK);
                                road.addSegment(r);
                            }
                        }
                    }
                    lastLRP = lrp;
                }
                lastRoad = roadId;
            }
        }
    }

    /**
     * Write the LRP coordinates as well as other time consuming calculated information.
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void writeLRPsCsv5(final String filename) throws Exception
    {
        System.out.println("Write " + filename);
        try (PrintWriter writer = new PrintWriter(new File(filename)))
        {
            writer.println("\"lrp\",\"lat\",\"lon\",\"type\",\"name\",\"district\"");
            for (Node node : this.network.getNodeMap().values())
            {
                if (node instanceof LRP)
                {
                    LRP lrp = (LRP) node;
                    writer.println("\"" + lrp.getId() + "\",\"" + lrp.getLocation().y + "\",\"" + lrp.getLocation().x + "\",\""
                            + lrp.getType() + "\",\"" + lrp.getName() + "\",\"" + lrp.getDistrict().getCode2() + "\"");
                }
            }
        }
    }

    /**
     * Write the roads with LRP references as well as other time consuming calculated information.
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void writeRoadsCsv5(final String filename) throws Exception
    {
        System.out.println("Write " + filename);
        try (PrintWriter writer = new PrintWriter(new File(filename)))
        {
            writer.println("\"road\",\"lrp1\",\"lrp2\",\"name\",\"gap\"");
            for (Road road : this.roadMap.values())
            {
                for (RoadSegment segment : road.getSegments())
                {
                    writer.println("\"" + road.getId() + "\",\"" + segment.getStartLRP().getId() + "\",\""
                            + segment.getEndLRP().getId() + "\",\"" + segment.getId() + "\",\"" + segment.getGap().toString()
                            + "\"");
                }
            }
        }
    }

    /**
     * Read the LRPs and make it part of a network for which we can calculate shortest path.
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void readLRPsCsv5(final String filename) throws Exception
    {
        System.out.println("Read " + filename);
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            String[] parts;
            while ((parts = reader.readNext()) != null)
            {
                // writer.println("\"lrp\",\"lat\",\"lon\",\"type\",\"name\",\"district\"");
                LRP lrp = null;
                String lrps = parts[0];
                String lats = parts[1];
                String lons = parts[2];
                double lat = Double.parseDouble(lats);
                double lon = Double.parseDouble(lons);
                String type = parts[3];
                String name = parts[4];
                String district2 = parts[5];

                // see what district.
                District district = this.districtCode2Map.get(district2);
                OTSPoint3D coordinate = new OTSPoint3D(lon, lat, 0.0);
                lrp = new LRP(this.network, lrps, coordinate, null, 0.0, type, name, GapPoint.ROAD, district);
                this.pointLRPMap.put(coordinate, lrp);
                this.lrpMap.put(name, lrp);
                if (district != null)
                {
                    this.districtLRPMap.get(district).add(lrp);
                }
                Color color = lrp.getId().contains("LRPX") ? Color.GREEN : Color.BLUE;
                new LRPAnimation(lrp, this.simulator, color);
            }
        }
    }

    /**
     * Read the floodset
     * @param scenarioName scenario name
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void readFloodSet(final String scenarioName, final String filename) throws Exception
    {
        System.out.println("Read " + filename);
        System.out.println("Flood scenario: " + scenarioName);
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ';', '"', 1))
        {
            boolean ok = false;
            String[] parts;
            while ((parts = reader.readNext()) != null)
            {
                String scen = parts[0];
                if (scen.equals(scenarioName))
                {
                    ok = true;
                    for (int i = 1; i < parts.length; i++)
                    {
                        if (parts[i].length() > 0)
                        {
                            this.floodSet.add(parts[i]);
                        }
                    }
                    System.out.println("Flooded districts: " + this.floodSet);
                }
            }
            if (!ok)
            {
                System.err.println("ERROR: WAS NOT ABLE TO READ FLOOD SCENARIO: " + scenarioName + "from " + filename);
            }
        }
    }

    /**
     * Read the roads with LRP coordinates and make it part of a network for which we can calculate shortest path.
     * @param filename filename
     * @throws Exception on I/O error
     */
    private void readRoadsCsv5(final String filename) throws Exception
    {
        System.out.println("Read " + filename);
        FileInputStream fis;
        if (new File(filename).canRead())
            fis = new FileInputStream(filename);
        else
            fis = new FileInputStream(CentroidRoutesEMA.class.getResource(filename).getFile());
        try (CSVReader reader = new CSVReader(new InputStreamReader(fis), ',', '"', 1))
        {
            // writer.println("\"road\",\"lrp1\",\"lrp2\",\"name\",\"gap\"");
            String[] parts;
            String lastRoad = "";
            while ((parts = reader.readNext()) != null)
            {
                String roadId = parts[0];
                String lrps1 = parts[1];
                String lrps2 = parts[2];
                String linkName = parts[3];
                String gaps = parts[4];

                LRP lrp1 = (LRP) this.network.getNode(lrps1);
                if (lrp1 == null)
                {
                    System.err.println("cannot find " + lrps1);
                }
                LRP lrp2 = (LRP) this.network.getNode(lrps2);
                if (lrp1 == null)
                {
                    System.err.println("cannot find " + lrps2);
                }

                if (!this.network.containsLink(linkName))
                {
                    Gap gap = Gap.valueOf(gaps);
                    if (!this.roadMap.containsKey(roadId))
                    {
                        this.roadMap.put(roadId, new Road(roadId));
                    }
                    Road road = this.roadMap.get(roadId);
                    lrp1.addRoad(road);
                    lrp2.addRoad(road);

                    OTSLine3D designLine = new OTSLine3D(lrp1.getPoint(), lrp2.getPoint());
                    Color color = lrp2.getType().contains("SPUR") ? Color.ORANGE
                            : linkName.contains("LRPX") ? Color.RED : Color.BLACK;
                    if (roadId.startsWith("N"))
                    {
                        SegmentN r = new SegmentN(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                                this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                        new RoadNAnimation(r, roadId, this.simulator, (float) (5.0 * 0.0005), color);
                        road.addSegment(r);
                    }
                    if (roadId.startsWith("R"))
                    {
                        SegmentR r = new SegmentR(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                                this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                        new RoadRAnimation(r, roadId, this.simulator, (float) (3.0 * 0.0005), color);
                        road.addSegment(r);
                    }
                    if (roadId.startsWith("Z"))
                    {
                        SegmentZ r = new SegmentZ(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                                this.simulator, LongitudinalDirectionality.DIR_BOTH, gap);
                        new RoadZAnimation(r, roadId, this.simulator, (float) (1.0 * 0.0005), color);
                        road.addSegment(r);
                    }
                }
            }
        }
    }

    /** distance in m. */
    private final static double THRESHOLD = 500;

    private double distance(LRP lrp1, LRP lrp2)
    {
        double lat1 = lrp1.getLocation().y;
        double lon1 = lrp1.getLocation().x;
        double lat2 = lrp2.getLocation().y;
        double lon2 = lrp2.getLocation().x;
        double p = 0.017453292519943295; // Math.PI / 180
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2.0
                + Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1.0 - Math.cos((lon2 - lon1) * p)) / 2.0;
        return 12742000.0 * Math.asin(Math.sqrt(a)); // 2 * R; R = 6371 km, distance in m.
    }

    private void repairSmallGapsCrossings()
    {
        // test if start or end point of a road is very close to an LRP of another road.
        for (Node node : this.network.getNodeMap().values())
        {
            // find the nods with just one link
            if (node.getLinks().size() == 1 && node instanceof LRP)
            {
                LRP lrp1 = (LRP) node;
                Link link = lrp1.getLinks().iterator().next(); // there is only one...
                if (link instanceof RoadSegment)
                {
                    RoadSegment segment = (RoadSegment) link;
                    List<LRP> roadLRPs = segment.getRoad().getLRPs();
                    boolean first = roadLRPs.size() == 0
                            || distance(lrp1, roadLRPs.get(0)) < distance(lrp1, roadLRPs.get(roadLRPs.size() - 1));
                    this.lrpMap.values().stream().forEach(lrp2 -> testDistance(lrp1, lrp2, segment, first));
                }
            }
        }

        /*-
        // test crossings of roads without an LRP in the middle
        SortedSet<Link> links1 = new TreeSet<Link>(this.network.getLinkMap().values().toCollection());
        for (Link link1 : links1)
        {
            if (this.network.containsLink(link1)) // the network is changed in this method
            {
                Set<Link> links2 = new HashSet<Link>(this.network.getLinkMap().values().toCollection());
                for (Link link2 : links2)
                {
                    if (link1 instanceof RoadSegment && link2 instanceof RoadSegment && link1 != null && link2 != null
                            && this.network.containsLink(link1) && this.network.containsLink(link2))
                    {
                        if (!link1.getId().contains(((RoadSegment) link2).getRoad().getId())
                                && !link2.getId().contains(((RoadSegment) link1).getRoad().getId()))
                        {
        
                            if (boundsOverlap(link1, link2))
                            {
                                testCrossing(link1, link2);
                            }
                        }
                    }
                }
            }
        }
        */
    }

    private void testDistance(LRP lrp1, LRP lrp2, RoadSegment segment, boolean first)
    {
        try
        {
            Set<Road> roads12 = lrp1.getRoads();
            roads12.retainAll(lrp2.getRoads());
            if (!lrp1.equals(lrp2) && roads12.isEmpty())
            {
                if (distance(lrp1, lrp2) < THRESHOLD)
                {
                    String linkName = lrp1.getId() + "-" + lrp2.getId();
                    String link2 = lrp2.getId() + "-" + lrp1.getId();
                    if (!this.network.containsLink(linkName) && !this.network.containsLink(link2))
                    {
                        // make a connection between lrp1 and lrp2 with the "highest" road type
                        makeLink(lrp1, lrp2, segment.getRoad(), "SPUR ", Color.ORANGE);
                        if (first)
                            segment.getRoad().addSegment(0, segment);
                        else
                            segment.getRoad().addSegment(segment);
                    }
                }
            }
        }
        catch (NetworkException | RemoteException | NamingException | OTSGeometryException exception)
        {
            exception.printStackTrace();
        }
    }

    private boolean boundsOverlap(Link link1, Link link2)
    {
        OTSPoint3D ps = link1.getStartNode().getPoint();
        OTSPoint3D pe = link1.getStartNode().getPoint();
        OTSPoint3D qs = link1.getStartNode().getPoint();
        OTSPoint3D qe = link1.getStartNode().getPoint();
        double x1min = ps.x <= pe.x ? ps.x : pe.x;
        double x1max = ps.x >= pe.x ? ps.x : pe.x;
        double x2min = qs.x <= qe.x ? qs.x : qe.x;
        double x2max = qs.x >= qe.x ? qs.x : qe.x;
        double y1min = ps.y <= pe.y ? ps.y : pe.y;
        double y1max = ps.y >= pe.y ? ps.y : pe.y;
        double y2min = qs.y <= qe.y ? qs.y : qe.y;
        double y2max = qs.y >= qe.y ? qs.y : qe.y;
        if (x1max < x2min || y1max < y2min || x2max < x1min || y2max < y1min)
        {
            return false;
        }
        return true;
    }

    private static int lrpNumber = 100;

    private void testCrossing(Link link1, Link link2)
    {
        try
        {
            RoadSegment segment1 = (RoadSegment) link1;
            RoadSegment segment2 = (RoadSegment) link2;
            if (link1.equals(link2) || segment1.getRoad().equals(segment2.getRoad()) || !segment1.getGap().isRoad()
                    || !segment2.getGap().isRoad())
            {
                return;
            }
            if (linesIntersect(link1, link2))
            {
                // split both lines at the intersection point.
                OTSPoint3D mid = getLineLineIntersection(link1, link2);
                if (mid != null)
                {
                    System.out.println("Intersection " + segment1.getRoad() + " - " + segment2.getRoad());

                    // unregister
                    this.network.removeLink(link1);
                    this.network.removeLink(link2);

                    // remove in roads and store indexes
                    int index1 = segment1.getRoad().getSegments().indexOf(segment1);
                    if (index1 < 0)
                    {
                        System.err.println("Cannot find segment1 " + segment1 + " in road " + segment1.getRoad()
                                + ", segments = " + segment1.getRoad().getSegments());
                    }
                    segment1.getRoad().removeSegment(index1);
                    int index2 = segment2.getRoad().getSegments().indexOf(segment2);
                    if (index2 < 0)
                    {
                        System.err.println("Cannot find segment2 " + segment2 + " in road " + segment2.getRoad()
                                + ", segments = " + segment2.getRoad().getSegments());
                    }
                    segment2.getRoad().removeSegment(index2);

                    // register new
                    double chainage = (segment1.getStartLRP().getChainage() + segment1.getEndLRP().getChainage()) / 2.0;
                    String type = segment1.getStartLRP().getType();
                    String name = segment1.getStartLRP().getName();
                    LRP midLRP = new LRP(this.network,
                            segment1.getRoad().getId() + "x" + segment2.getRoad().getId() + "_LRPX" + (lrpNumber++), mid,
                            segment1.getRoad(), chainage, type, name, GapPoint.ROAD, segment1.getStartLRP().getDistrict());
                    midLRP.addRoad(segment2.getRoad());
                    RoadSegment road11 = makeLink(segment1.getStartLRP(), midLRP, segment1.getRoad(), "", Color.RED);
                    RoadSegment road12 = makeLink(midLRP, segment1.getEndLRP(), segment1.getRoad(), "", Color.RED);
                    RoadSegment road21 = makeLink(segment2.getStartLRP(), midLRP, segment2.getRoad(), "", Color.RED);
                    RoadSegment road22 = makeLink(midLRP, segment2.getEndLRP(), segment2.getRoad(), "", Color.RED);

                    // register in the roads
                    if (road11 != null)
                        segment1.getRoad().addSegment(index1, road11);
                    if (road12 != null)
                        segment1.getRoad().addSegment(index1 + 1, road12);
                    if (road21 != null)
                        segment2.getRoad().addSegment(index2, road21);
                    if (road22 != null)
                        segment2.getRoad().addSegment(index2 + 1, road22);
                }
            }
        }
        catch (NetworkException | RemoteException | NamingException | OTSGeometryException exception)
        {
            exception.printStackTrace();
        }
    }

    private RoadSegment makeLink(LRP lrp1, LRP lrp2, Road road, String prefix, Color color)
            throws NetworkException, RemoteException, NamingException, OTSGeometryException
    {
        String linkName = prefix + lrp1.getId() + "-" + lrp2.getId();
        String link2 = prefix + lrp2.getId() + "-" + lrp1.getId();
        if (!this.network.containsLink(linkName) && !this.network.containsLink(link2))
        {
            OTSLine3D designLine = new OTSLine3D(new OTSPoint3D[] { lrp1.getPoint(), lrp2.getPoint() });
            char roadType = road.getId().charAt(0);
            switch (roadType)
            {
                case 'N':
                {
                    SegmentN r = new SegmentN(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                    new RoadNAnimation(r, road.getId(), this.simulator, (float) (5.0 * 0.0005), color);
                    road.addSegment(r);
                    return r;
                }

                case 'R':
                {
                    SegmentR r = new SegmentR(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                    new RoadRAnimation(r, road.getId(), this.simulator, (float) (3.0 * 0.0005), color);
                    road.addSegment(r);
                    return r;
                }

                case 'Z':
                {
                    SegmentZ r = new SegmentZ(this.network, linkName, road, lrp1, lrp2, LinkType.ALL, designLine,
                            this.simulator, LongitudinalDirectionality.DIR_BOTH, Gap.ROAD);
                    new RoadZAnimation(r, road.getId(), this.simulator, (float) (1.0 * 0.0005), color);
                    road.addSegment(r);
                    return r;
                }

                default:
                    System.err.println("Road not N, R or Z but " + roadType + " based on road " + road.getId());
                    break;
            }
        }
        return null;
    }

    private static final double NFACTOR = 1;

    private static final double RFACTOR = 2;

    private static final double ZFACTOR = 10;

    private void findDistrictCentroids()
    {
        System.out.println("Find district centroids");
        for (District district : this.districtCode2Map.values())
        {
            OTSPoint3D c = district.getCentroid();
            double bestDist = Double.MAX_VALUE;
            LRP bestLRP = null;
            for (LRP lrp : this.districtLRPMap.get(district))
            {
                double distance = c.distanceSI(lrp.getPoint());
                distance = lrp.getId().charAt(0) == 'R' ? RFACTOR * distance
                        : lrp.getId().charAt(0) == 'Z' ? ZFACTOR * distance : NFACTOR * distance;
                if (distance < bestDist)
                {
                    bestDist = distance;
                    bestLRP = lrp;
                }
            }
            district.setCentroidLRP(bestLRP);
        }
    }

    /**
     * Buid a graph of the road network.
     */
    public final void buildGraph()
    {
        System.out.println("Build graph");
        @SuppressWarnings("unchecked")
        Class<? extends LinkEdge<Link>> linkEdgeClass = (Class<? extends LinkEdge<Link>>) LinkEdge.class;
        this.graph = new SimpleDirectedWeightedGraph<Node, LinkEdge<Link>>(linkEdgeClass);
        for (Node node : this.network.getNodeMap().values())
        {
            this.graph.addVertex(node);
        }
        for (Link link : this.network.getLinkMap().values())
        {
            if (!link.getStartNode().equals(link.getEndNode()))
            {
                if (link instanceof RoadSegment) // could also be waterway!
                {
                    RoadSegment road = (RoadSegment) link;
                    LRP lrp1 = road.getStartLRP();
                    LRP lrp2 = road.getEndLRP();
                    double length = distance(lrp1, lrp2);
                    length = road.getId().charAt(0) == 'N' ? length * NFACTOR
                            : road.getId().charAt(0) == 'R' ? length * RFACTOR : length * ZFACTOR;
                    LinkEdge<Link> linkEdge1 = new LinkEdge<>(link);
                    this.graph.addEdge(link.getStartNode(), link.getEndNode(), linkEdge1);
                    this.graph.setEdgeWeight(linkEdge1, length);
                    LinkEdge<Link> linkEdge2 = new LinkEdge<>(link);
                    this.graph.addEdge(link.getEndNode(), link.getStartNode(), linkEdge2);
                    this.graph.setEdgeWeight(linkEdge2, length);
                }
            }
        }
    }

    public final CompleteRoute getShortestRouteBetween(final Node nodeFrom, final Node nodeTo) throws NetworkException
    {
        CompleteRoute route = new CompleteRoute("Route from " + nodeFrom + "to " + nodeTo, GTUType.ALL);
        DijkstraShortestPath<Node, LinkEdge<Link>> path = new DijkstraShortestPath<>(this.graph, nodeFrom, nodeTo);
        if (path.getPath() == null)
        {
            return null;
        }
        route.addNode(nodeFrom);
        for (LinkEdge<Link> link : path.getPathEdgeList())
        {
            if (!link.getLink().getEndNode().equals(route.destinationNode()))
            {
                route.addNode(link.getLink().getEndNode());
            }
            else if (!link.getLink().getStartNode().equals(route.destinationNode()))
            {
                route.addNode(link.getLink().getStartNode());
            }
            else
            {
                throw new NetworkException("Cannot connect two links when calculating shortest route");
            }
        }
        return route;
    }

    private void calculateDistrictDistances() throws NetworkException
    {
        System.out.println("Calculate shortest routes between districts");
        buildGraph();
        for (District d1 : this.districtCode2Map.values())
        {
            for (District d2 : this.districtCode2Map.values())
            {
                if (!d1.equals(d2))
                {
                    LRP lrp1 = d1.getCentroidLRP();
                    Node node1 = this.network.getNode(lrp1.getId());
                    LRP lrp2 = d2.getCentroidLRP();
                    Node node2 = this.network.getNode(lrp2.getId());
                    CompleteRoute route = getShortestRouteBetween(node1, node2);
                    System.out.println("From " + d1.getCode2() + " to " + d2.getCode2() + " (" + lrp1.getId() + " - "
                            + lrp2.getId() + "): " + routeRoads(route));
                }
            }
        }
    }

    private List<String> routeRoads(CompleteRoute route)
    {
        List<String> result = new ArrayList<>();
        if (route != null)
        {
            Road lastRoad = null;
            Node lastNode = null;
            for (Node node : route.getNodes())
            {
                if (lastNode != null)
                {
                    Link link = null;
                    for (Link l : lastNode.getLinks())
                    {
                        if (l.getStartNode().equals(node) || l.getEndNode().equals(node))
                        {
                            link = l;
                            break;
                        }
                    }
                    Road road = ((RoadSegment) link).getRoad();
                    if (!road.equals(lastRoad))
                    {
                        lastRoad = road;
                        result.add(road.getId());
                    }
                }
                lastNode = node;
            }
        }
        return result;
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

    public static boolean linesIntersect(Link l1, Link l2)
    {
        try
        {
            return linesIntersect(l1.getStartNode().getLocation(), l1.getEndNode().getLocation(),
                    l2.getStartNode().getLocation(), l2.getEndNode().getLocation());
        }
        catch (RemoteException exception)
        {
            exception.printStackTrace();
            return false;
        }
    }

    /* from http://www.java-gaming.org/index.php?topic=22590.0. */
    public static boolean linesIntersect(DirectedPoint p1, DirectedPoint p2, DirectedPoint q1, DirectedPoint q2)
    {
        double x1 = p1.x;
        double y1 = p1.y;
        double x2 = p2.x;
        double y2 = p2.y;
        double x3 = q1.x;
        double y3 = q1.y;
        double x4 = q2.x;
        double y4 = q2.y;
        // Return false if either of the lines have zero length
        if (x1 == x2 && y1 == y2 || x3 == x4 && y3 == y4)
        {
            return false;
        }
        // Fastest method, based on Franklin Antonio's "Faster Line Segment Intersection" topic "in Graphics Gems III" book
        // (http://www.graphicsgems.org/)
        double ax = x2 - x1;
        double ay = y2 - y1;
        double bx = x3 - x4;
        double by = y3 - y4;
        double cx = x1 - x3;
        double cy = y1 - y3;

        double alphaNumerator = by * cx - bx * cy;
        double commonDenominator = ay * bx - ax * by;
        if (commonDenominator > 0)
        {
            if (alphaNumerator < 0 || alphaNumerator > commonDenominator)
            {
                return false;
            }
        }
        else if (commonDenominator < 0)
        {
            if (alphaNumerator > 0 || alphaNumerator < commonDenominator)
            {
                return false;
            }
        }
        double betaNumerator = ax * cy - ay * cx;
        if (commonDenominator > 0)
        {
            if (betaNumerator < 0 || betaNumerator > commonDenominator)
            {
                return false;
            }
        }
        else if (commonDenominator < 0)
        {
            if (betaNumerator > 0 || betaNumerator < commonDenominator)
            {
                return false;
            }
        }
        if (commonDenominator == 0)
        {
            // This code wasn't in Franklin Antonio's method. It was added by Keith Woodward.
            // The lines are parallel.
            // Check if they're collinear.
            double y3LessY1 = y3 - y1;
            double collinearityTestForP3 = x1 * (y2 - y3) + x2 * (y3LessY1) + x3 * (y1 - y2); // see
                                                                                              // http://mathworld.wolfram.com/Collinear.html
            // If p3 is collinear with p1 and p2 then p4 will also be collinear, since p1-p2 is parallel with p3-p4
            if (collinearityTestForP3 == 0)
            {
                // The lines are collinear. Now check if they overlap.
                if (x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 || x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4
                        || x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2)
                {
                    if (y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 || y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4
                            || y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2)
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public static OTSPoint3D getLineLineIntersection(Link l1, Link l2)
    {
        try
        {
            return getLineLineIntersection(l1.getStartNode().getLocation(), l1.getEndNode().getLocation(),
                    l2.getStartNode().getLocation(), l2.getEndNode().getLocation());
        }
        catch (RemoteException exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    /* from http://www.java-gaming.org/index.php?topic=22590.0. */
    public static OTSPoint3D getLineLineIntersection(DirectedPoint p1, DirectedPoint p2, DirectedPoint q1, DirectedPoint q2)
    {
        double x1 = p1.x;
        double y1 = p1.y;
        double x2 = p2.x;
        double y2 = p2.y;
        double x3 = q1.x;
        double y3 = q1.y;
        double x4 = q2.x;
        double y4 = q2.y;

        double det1And2 = det(x1, y1, x2, y2);
        double det3And4 = det(x3, y3, x4, y4);
        double x1LessX2 = x1 - x2;
        double y1LessY2 = y1 - y2;
        double x3LessX4 = x3 - x4;
        double y3LessY4 = y3 - y4;
        double det1Less2And3Less4 = det(x1LessX2, y1LessY2, x3LessX4, y3LessY4);
        if (det1Less2And3Less4 == 0)
        {
            // the denominator is zero so the lines are parallel and there's either no solution (or multiple solutions if the
            // lines overlap) so return null.
            return null;
        }
        double x = (det(det1And2, x1LessX2, det3And4, x3LessX4) / det1Less2And3Less4);
        double y = (det(det1And2, y1LessY2, det3And4, y3LessY4) / det1Less2And3Less4);
        return new OTSPoint3D(x, y);
    }

    protected static double det(double a, double b, double c, double d)
    {
        return a * d - b * c;
    }
}
