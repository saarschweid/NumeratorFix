package com.sapiens;

import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test Cases:
 * 1. check that we don't change correct prev_id in the wrong place of referring table
 * 2. different column name for primary key in original table
 * 3. non separating commas in middle of insert to referring table
 * 4. multiple swaps in same line for referring table (many FK to same table)
 * 5. dealing with lower case statements
 * 6. none vs one vs many referring tables
 * 7. PK not on the first column
 * <p/>
 * Limitations:
 * 1. dealing with multiline statements.
 * 2. gap between = and spaces
 */

//TODO change to work with pattern and matcher http://www.regular-expressions.info/java.html in replace and everything else.
//TODO while should run by ; instead of new line. also cases where there are spaces between , or = or ( etc.
//TODO create a "special tables" list. test if a table belongs to them (to know which is the special column it has). all the dynamic tables and stuff like that.
//maybe use a new object for that (or temp tables)
//TODO create the part that makes the file I'm using now. print it for documentation but don't read from it (not efficient). this requires a second DB and methods i wrote in the past.
//TODO It takes FOREVER to run with lots of data. optimization: create threads. each thread "sees" each file from beginning to end but scans only several tables.  **************
//TODO since the file may be locked I may need to move all data to a buffer.
//TODO new design- use blocking queue of String (file paths) and threads (thread pool) that take files one at a time.
//each thread should create its own buffer- put the lines there and then print it if was changed. so that the lines won't mix up
//get a path, create a file, scan it, if line is changes get a file from (PATH, NEW_OUTPUT_FILE_FOR_THIS_PATH) map and put lines, when finished
//remove the file from the map and print it.

/**
 * Created by Saar Schweid on 8/8/2016
 * <p/>
 * Main Object to handle the change of Ids in promotion files
 */
public class NumeratorFix {

    static final Logger logger = Logger.getLogger(NumeratorFix.class.getName());

    //Filesystem
    public static InputStream inputStream = null;
    public static String ORIGINAL_TABLES_FILE = "";
    public static File rootFolder = null;
    public static String USER_RESULTS_LOG_PATH = "";
    private static final String CONFIGURATION_FILE = "config.properties";

    //Constants
    public static String PROJECT_NAME = "";
    public static String PROJECT_EXTENSION = "";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String UPDATE = "UPDATE ";
    public static final String DELETE_FROM = "DELETE FROM ";
    public static final String VALUES_REGEX = ".*VALUES.*[\\(]";
    public static final String WHERE_REGEX = ".*WHERE ";
    public static final String EQUAL_REGEX = ".*=.*"; //change to include only one whitespace at most
    private static String DML_FILE = "DMLChanges";

    //Regex
    private static Pattern VALUES_PATTERN = Pattern.compile("VALUES [(]");
    private static Pattern SELECT_MAX_PATTERN = Pattern.compile("SELECT  MAX");
    private static Pattern SELECT_PATTERN = Pattern.compile("SELECT ");

    //Internal use variables
    private static int filesScannedCounter = 0;
    private static int linesChangedCounter = 0;
    private static String currentFileBeingScanned = "";
    private static Map<String, Boolean> mapOfChangedFiles = new HashMap<>();
    private static Stack<String> columnWithCommasInIt = new Stack<>();
    private static Boolean isScanOnlyDML = false;
    private static List<Table> tablesList = null;
    private static BlockingQueue<String> filesQueue = null;
    private ArrayList<File> filesToScan = new ArrayList<>();

    private static long startTime = 0L;
    private static long finishTime = 0L;

    private GUIHandler guiHandler = null;

    /**
     * the NumeratorFix is running in a different Thread than the GUI
     * to allow scrolling and the data to be printed to the screen online
     *
     * @param guiHandler - instance of the GUI that runs the application
     */
    public NumeratorFix(GUIHandler guiHandler) {
        GUIHandler.logToGUIandLogger(logger, "info", "application started at: " + new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(Calendar.getInstance().getTime()));
        startTime = System.nanoTime();
        readPropertiesFile();
        this.tablesList = SQLUtil.getTablesList();
        this.guiHandler = guiHandler;
    }

    /**
     * method to initiate the search
     */
    public void start() {
        logger.debug("starting to search and replace...");
        createFileListFromScanning(this.rootFolder);
        scanAndReplaceInFiles();
        finish();
    }

    /**
     * inform the user that the run has finished
     */
    public void finish() {
        finishTime = System.nanoTime();
        GUIHandler.logToGUIandLogger(logger, "info", "\nIn Total: " + "\nFiles Scanned: " + filesScannedCounter + "\nFiles Changed: " + mapOfChangedFiles.size() + "\nLines Changed: " + linesChangedCounter + "\nTime Elapsed: " + TimeUnit.NANOSECONDS.toMinutes(finishTime - startTime) + " Minutes\n");
        GUIHandler.logToGUIandLogger(logger, "info", (char) 169 + " This tool was developed by Saar Schweid\n");
        performScanFinished();
    }

    /**
     * perform actions when the scan has finished
     */
    private void performScanFinished() {
        guiHandler.enableOpenLogButton();
        guiHandler.setTextToStatusLabel("Scan finished successfully!");
        guiHandler.stopLoadingIcon();
    }

    /**
     * method to read a properties file and set relevant constants. the file must be in the classPath (blue folder)
     */
    private void readPropertiesFile() {

        Properties prop = new Properties();

        if (inputStream == null) {
            inputStream = NumeratorFix.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE);
        }
        if (inputStream == null) {
            logger.error("couldn't find the properties file: " + CONFIGURATION_FILE);
            return;
        }

        try {
            prop.load(inputStream);
        } catch (IOException e) {
            logger.error("problem loading properties file");
        }

        SQLUtil.JDBC_DRIVER = prop.getProperty("JDBC_DRIVER");
        SQLUtil.DB_URL = prop.getProperty("DB_URL");
        SQLUtil.USER = prop.getProperty("DB_USER");
        SQLUtil.PASS = prop.getProperty("DB_PASSWORD");
        SQLUtil.TIMEOUT = Integer.parseInt(prop.getProperty("DB_TIMEOUT"));

        PROJECT_NAME = prop.getProperty("PROJECT_NAME");
        PROJECT_EXTENSION = '_' + PROJECT_NAME;
        isScanOnlyDML = Boolean.parseBoolean(prop.getProperty("SCAN_ONLY_DML"));
        DML_FILE = prop.getProperty("NAME_OF_DML");
        USER_RESULTS_LOG_PATH = prop.getProperty("RESULT_LOG_PATH");

        //init the tables list. do it now because of dependencies between the classes.
        SQLUtil.initTablePrimaryKeyList();
    }

    /**
     * method to scan the filesystem from a given root.
     *
     * @param rootFolder folder to recursively scan files in
     */
    private void createFileListFromScanning(File rootFolder) {
        File[] fileList = rootFolder.listFiles();
        if (fileList != null) {
            for (File currentFile : fileList) {
                if (currentFile.isDirectory()) {
                    createFileListFromScanning(currentFile);
                } else if (isScanOnlyDML && currentFile.getName().contains(DML_FILE)) {
                    filesToScan.add(currentFile);
                }
            }
        }
    }

    /**
     * method that goes through the files list to scan, and swaps the relevant IDs
     */
    private void scanAndReplaceInFiles() {
        Collections.sort(filesToScan);
        for (File currentFile : filesToScan) {
            GUIHandler.logToGUIandLogger(logger, "debug", "current file being scanned: " + currentFile.getAbsolutePath());
            replaceIdInFile(currentFile);
            ++filesScannedCounter;
            int progress = (int) ((float) filesScannedCounter / (float) filesToScan.size() * 100);
            guiHandler.setProgressValue(progress);
        }
    }

    /**
     * main method to check each file and replace id's.
     * for each line, the method checks if a table from the tables list exists, and if it has a prevID to be changed
     *
     * @param currentFile current file scanned
     */
    private void replaceIdInFile(File currentFile) {

        currentFileBeingScanned = currentFile.getAbsolutePath();
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(currentFile);

        } catch (FileNotFoundException e) {
            logger.error("File not found: currentFile = [" + currentFile + ']', e);
        }

        BufferedReader br = new BufferedReader(fileReader);
        String newFile = "";

        try {

            int rowsIteratedCounter = 1;
            String currLineInFile;
            while ((currLineInFile = br.readLine()) != null) {

                if (!currLineInFile.trim().startsWith("--")) { //exclude comments
                    String lineBeforeReplace = currLineInFile;

                    for (Table table : tablesList) {
                        String prevId = table.getPrevId().toString();
                        String newId = table.getNewId().toString();
                        String tableType = table.getTableType();
                        String columnName;
                        String tableName = table.getTableName();
                        List<Table.TableColumns> columns = table.getRelevantColumns();
                        if (tableType.equals(Table.TYPE_REGULAR)) {
                            //take the Primary Key column name of original table (not always ID)
                            columnName = columns.get(0).getColumnName();
                            currLineInFile = replaceIdForRegularTable((OriginalTable) table, columnName, prevId, newId, currLineInFile);
                        } else if (tableType.equals(Table.TYPE_FOREIGN_KEY)) {
                            for (Table.TableColumns column : columns) {
                                currLineInFile = replaceIdForReferringTable(tableName, column, prevId, newId, currLineInFile);
                            }
                        }
                    }
                    if (!lineBeforeReplace.equals(currLineInFile)) {
                        logFileChangedFirstTime();
                        logLineChanged(rowsIteratedCounter, currLineInFile);
                        ++linesChangedCounter;
                    }
                }
                ++rowsIteratedCounter;
                newFile += currLineInFile + "\r\n";
            }
        } catch (IOException e) {
            logger.error("Problem in reading file: currentFile = [" + currentFile + ']', e);
        }

        try {
            FileWriter fileWriter = new FileWriter(currentFile);
            fileWriter.write(newFile);
            fileWriter.close();

        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * main method to perform replacements for regular (non-referring) tables.
     * tests for INSERT, UPDATE, DELETE from regular table
     *
     * @param originalTable - regular table object
     * @param columnName    - PK column name
     * @param prevId        - prevID
     * @param newId         - newID
     * @param currLine      - current line in the file
     * @return the current line. if no change was made, the same line returns.
     */
    private String replaceIdForRegularTable(OriginalTable originalTable, String columnName, String prevId, String newId, String currLine) {
        String WHERE_PREV = "WHERE " + columnName + '=' + prevId;
        String WHERE_NEW = "WHERE " + columnName + '=' + newId;
        String VALUES_PREV = "VALUES (" + prevId;
        String VALUES_NEW = "VALUES (" + newId;

        Matcher insertMatcher = originalTable.getInsertPattern().matcher(currLine.toUpperCase());
        Matcher updateMatcher = originalTable.getUpdatePattern().matcher(currLine.toUpperCase());
        Matcher deleteMatcher = originalTable.getDeletePattern().matcher(currLine.toUpperCase());

        if (insertMatcher.find()) {
            currLine = replaceIgnoreCase(currLine, VALUES_PREV, VALUES_NEW);
        } else if (updateMatcher.find() || deleteMatcher.find()) {
            currLine = replaceIgnoreCase(currLine, WHERE_PREV, WHERE_NEW);
        }

        return currLine;
    }

    /**
     * main method to perform replacement for referring tables.
     * tests for INSERT, UPDATE, DELETE from referring table
     *
     * @param referringTableName - name of referring table
     * @param columnName         - name of FK column
     * @param prevId             - prevID
     * @param newId              - newID
     * @param currLine           - current line in the file
     * @return the current line. if no change was made, the same line returns.
     */
    private String replaceIdForReferringTable(String referringTableName, Table.TableColumns column, String prevId, String newId, String currLine) {
        String columnName = column.getColumnName();
        Long columnPosition = column.getColumnPosition();
        String COLUMN_PREV = columnName + '=' + prevId;
        String COLUMN_NEW = columnName + '=' + newId;

        if (checkIfLineContainsTable(currLine, INSERT_INTO, referringTableName) && checkIfLineContainsId(currLine, prevId)) {
            currLine = insertToReferringTable(currLine, columnName, columnPosition, prevId, newId);
        } else if (containIgnoreCase(currLine, COLUMN_PREV)) {
            if (checkIfLineContainsTable(currLine, UPDATE, referringTableName) || checkIfLineContainsTable(currLine, DELETE_FROM, referringTableName)) {
                currLine = replaceIgnoreCase(currLine, COLUMN_PREV, COLUMN_NEW);
            }
        }

        return currLine;
    }

    /**
     * check if current line contains the table name. (need to send project table specifically as the method does
     * not add the extension itself)
     *
     * @param currLine  - current line in the file
     * @param command   - action to be performed. like: INSERT TO
     * @param tableName - able being tested
     * @return true if the current line contains the table in it.
     */
    private Boolean checkIfLineContainsTable(String currLine, String command, String tableName) {
        if (currLine != null) {
            return containIgnoreCase(currLine, command + tableName + ' ');
        }
        return Boolean.FALSE;
    }


    /**
     * need a new method because of a very rare case when the table Id to replace is very short.
     * when this is the case, the id is included in other id's. for example - id =30 is included in 10000030.
     * this may cause problems in the calculation.
     *
     * @param currLine String- current line
     * @param id       - the previous id to look for
     * @return true if the ID exists.
     */
    private Boolean checkIfLineContainsId(String currLine, String id) {
        if (id == null || id.isEmpty()) {
            return Boolean.FALSE;
        }
        //if the id doesn't contain only numbers
        if (!Pattern.matches("\\d+", id)) {
            return Boolean.FALSE;
        }

        Pattern pattern = Pattern.compile("\\D" + id + "\\D"); //create regex for Id with no numbers around it
        Matcher matcher = pattern.matcher(currLine);
        return matcher.find();
    }

    /**
     * special case of insert to a referring table
     * the method works by testing the number of separating commas in the SQL statement (Line)
     * replace happens in the right place by taking into account the column relative position in the referring table
     *
     * @param line           current line
     * @param columnName     - FK column name
     * @param columnPosition
     * @param prevId         - prevID
     * @param newId          - newID
     * @return - the new Line after a replacement. if no changes made, the same line returns.
     */
    //TODO probably better implementation with StringBuilder
    private String insertToReferringTable(String line, String columnName, Long columnPosition, String prevId, String newId) {
        int insertPlace = (int) (long) columnPosition;
        if (insertPlace != (int) (long) Table.NO_VALUE) {
            String lineStart = "";
            String lineMiddle = "";
            String lineEnd = "";

            String[] lineParts = line.split(VALUES_PATTERN.pattern());

            if (lineParts.length > 1) {
                lineStart = lineParts[0] + "VALUES (";
                lineMiddle = "";
                lineEnd = lineParts[1];
            } else if ((lineParts = line.split(SELECT_MAX_PATTERN.pattern())).length > 1) {
                //for the rare and special case where the promotion doesn't contain "VALUES" but instead SELECT  MAX(ID) +1
                lineStart = lineParts[0] + "SELECT  MAX";
                lineEnd = lineParts[1];
            } else if ((lineParts = line.split(SELECT_PATTERN.pattern())).length > 1) {
                //for the rare and special case where the promotion doesn't contain "VALUES" but instead:  .....GRANTED_BY,REVOKED_BY) SELECT 200,1006033,1,0,0,NULL,NULL,NULL, MAX(ID)+1.... FROM T_ROLE_PRIVILEGE;
                lineStart = lineParts[0] + "SELECT ";
                lineEnd = lineParts[1];
            }

            while (insertPlace > 0) {
                String[] columns = lineEnd.split("[,]", 2);
                lineMiddle += (columns[0] + ',');
                lineEnd = columns[1];
                if (isSeparatingComma(columns[0])) {
                    --insertPlace;
                }
            }

            if (lineEnd.startsWith(prevId)) {
                lineEnd = lineEnd.replace(prevId, newId);
            }

            return lineStart + lineMiddle + lineEnd;
        }
        return null; //or line?
    }

    /**
     * method to test if the comma in the line is indeed separating comma.
     * <p/>
     * for now this method deals with:
     * 1. case of quote (like in resource code) lines like VALUES (1035045,'General Endorsement Created, Event Rule, or something else',5,1,
     * 2. case of convert date lines like VALUES (1009137,2, CONVERT(datetime, '20-07-2016 00:00:00', 103),
     *
     * @param linePart the current insert line part tested
     * @return true only if the current comma is a column separator
     */
    private Boolean isSeparatingComma(String linePart) {
        linePart = linePart.trim();
        if (linePart.startsWith("\'")) {
            columnWithCommasInIt.add("DESCRIPTION");
            linePart = linePart.substring(1); //to ignore the opening ' char
        }
        //QUOTE case: if we are in a quote, if quote ended now it's a separating comma. if not, it's part of the quote.
        if (columnWithCommasInIt.contains("DESCRIPTION")) {
            if (linePart.contains("\'")) {
                columnWithCommasInIt.remove("DESCRIPTION");
            } else {
                return Boolean.FALSE;
            }
        }
        if (linePart.startsWith("CONVERT(")) {
            columnWithCommasInIt.add("CONVERT");
        }
        //CONVERT case
        if (columnWithCommasInIt.contains("CONVERT")) {
            if (linePart.endsWith(")")) {
                columnWithCommasInIt.remove("CONVERT");
            } else {
                return Boolean.FALSE;
            }
        }

        if (columnWithCommasInIt.isEmpty()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * helper method to check contains when case is ignored
     *
     * @param container - source String
     * @param value     - tested string
     * @return - true if the source contains the value
     */
    private Boolean containIgnoreCase(String container, String value) {
        return container.toUpperCase().contains(value.toUpperCase());
    }

    /**
     * helper method to replace in a String when case is ignored
     * when changing the entire string to upper case we damage Description etc. so we need to change only the relevant parts.
     * If the line after change equals to the line before change, it means no chnage is done- so change the string to upper case and swap.
     *
     * @param container - source String
     * @param prevStr   - old value to replace
     * @param newStr    - new value to substitute
     * @return - the new String after replacement
     */
    private String replaceIgnoreCase(String container, String prevStr, String newStr) {
        String lineWithChanges = container.replace(prevStr, newStr);
        return lineWithChanges.equals(container) ? container.toUpperCase().replace(prevStr, newStr) : lineWithChanges;
    }

    /**
     * if the file is changed for the first time, log it and update the map.
     */
    private void logFileChangedFirstTime() {
        //null means it's first time. don't think a FALSE check is needed
        if (mapOfChangedFiles.get(currentFileBeingScanned) == null || mapOfChangedFiles.get(currentFileBeingScanned).equals(Boolean.FALSE)) {
            GUIHandler.logToGUIandLogger(logger, "info", "\nchanged file: " + currentFileBeingScanned);
            mapOfChangedFiles.put(currentFileBeingScanned, Boolean.TRUE);
        }
    }

    /**
     * @param lineNumber  - current line number
     * @param lineChanged - the changed line
     */
    private void logLineChanged(int lineNumber, String lineChanged) {
        GUIHandler.logToGUIandLogger(logger, "info", ("line " + lineNumber + ": " + lineChanged));
    }
}
