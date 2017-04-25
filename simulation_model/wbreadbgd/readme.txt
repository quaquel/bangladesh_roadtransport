The wbreadbgd folder contains the java code to read the RMMS and BMMS data from the Bangladesh Ministry of Transport.
- ReadRoads reads the RMMS data from the web
- ReadBridges read the BMMS data from the web
- ReadRoadWidths process the RMMS width data from Flash
- ProcessRoadWidths process the raw road data widths into a readable csv-file

The wbprocessbgd folder contains the java code to make more elaborate data struture files.
The most important ones are:
- MakeRoadsFile3 which makes a total roads file (RMMS/_roads3.csv) from all gathered data by earlier processes
- MakeBridgesFile3 which makes a total bridge file (BMMS/overview.tsv) from all gathered data by earlier processes
