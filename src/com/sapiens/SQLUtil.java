/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 8/21/2016
 * Time: 3:13 PM
 */
package com.sapiens;

import org.apache.log4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO to avoid many travels to DB- use each query with thought. eg: get list of all column PK names at once, all max ID at once...
 * TODO another idea is to call all tables in one query instead of each one, and maybe there is a way to open connection and close once.
 * TODO check if to use METADATA object instead of regular connections. I can keep an instance of metaData and use it for other tasks.
 * <p/>
 * Helper class to perform all DB related actions such as retrieving referring tables, column names, etc.
 */
public class SQLUtil {

    //logger
    static final Logger logger = Logger.getLogger(SQLUtil.class.getName());

    //Database credentials
    static String JDBC_DRIVER = "";
    static String DB_URL = "";
    static String USER = "";
    static String PASS = "";
    static int TIMEOUT = 0;

    //Private variables
    private static Map<String, TablePrimaryKey> tablePrimaryKeyMap = new HashMap<>();

    //Constants
    private static final String TABLE_EXISTS_MESSAGE = "project table exists";
    private static final String TABLE_DOES_NOT_EXIST_MESSAGE = "project table does not exist";

    /**
     * @return all tables in filesystem that needs to be changed. this includes original tables and referring tables.
     */
    public static List<Table> getTablesList() {
        logger.debug("collecting tables list...");
        List<Table> returnTables = new ArrayList<>();

        List<OriginalTable> originalTables = getTableListFromConfigFile();
        returnTables.addAll(originalTables);
        GUIHandler.logToGUIandLogger(logger, "debug", "adding all tables to a complete tables list...");
        for (OriginalTable table : originalTables) {
            returnTables.addAll(table.getFkTables());
        }

        return returnTables;
    }

    /**
     * create a map of all data relevant to pk columns of original tables.
     * key = table name
     * Value = data relevant to this table (pk column name, pk column position)
     */
    public static void initTablePrimaryKeyList() {
        GUIHandler.logToGUIandLogger(logger, "debug", "collecting data regarding primary keys of tables in the system...");
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("net.sourceforge.jtds.jdbcx.JtdsDataSource"); //register driver
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(buildGetPrimaryKeyDataQuery());
            //add an object containing the relevant data to the Map of TableName -> data about the table.
            while (resultSet.next()) {
                String tableName = resultSet.getString(1);
                String columnName = resultSet.getString(2);
                Long columnPosition = resultSet.getLong(3);
                TablePrimaryKey tablePrimaryKey = new TablePrimaryKey(tableName, columnName, columnPosition);
                tablePrimaryKeyMap.put(tableName, tablePrimaryKey);
            }
            resultSet.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            logger.error(e);
        } finally {
            //close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }
    }

    /**
     * method to build an original tables list from config file.
     * file should be in this format: "TABLE NAME" in a single line and the following lines: "prevId TO newId"
     *
     * @return a list of original tables generated from a config file
     */
    public static List<OriginalTable> getTableListFromConfigFile() {
        GUIHandler.logToGUIandLogger(logger, "debug", "fetching original tables list from config file...");
        File file = new File(NumeratorFix.ORIGINAL_TABLES_FILE);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);

        } catch (FileNotFoundException e) {
            logger.error("File not found: currentFile = [" + NumeratorFix.ORIGINAL_TABLES_FILE + ']', e);
        }

        BufferedReader br = new BufferedReader(fileReader);
        List<OriginalTable> originalTablesToReturn = new ArrayList<>();
        String currLineInFile;
        String currentTableName = Table.NOT_INIT;
        String pkColumnName = Table.NOT_INIT;
        Long pkPosition = Table.NO_VALUE;

        try {
            while ((currLineInFile = br.readLine()) != null) {
                currLineInFile = currLineInFile.trim().toUpperCase();
                if (!currLineInFile.equals("") && currLineInFile.toUpperCase().charAt(0) >= 'A' && currLineInFile.toUpperCase().charAt(0) <= 'Z') {
                    currentTableName = currLineInFile;
                    TablePrimaryKey tablePrimaryKey = tablePrimaryKeyMap.get(currentTableName);
                    if (tablePrimaryKey != null) {
                        pkColumnName = tablePrimaryKey.getPrimaryColumnName();
                        pkPosition = tablePrimaryKey.getPrimaryColumnPosition();
                    }

                } else if (!currLineInFile.isEmpty()) {
                    String[] ids = currLineInFile.split("TO", 2);
                    Long prevId = Long.valueOf(ids[0].trim()).longValue();
                    Long newId = Long.valueOf(ids[1].trim()).longValue();
                    OriginalTable originalTable = new OriginalTable(currentTableName, pkColumnName, pkPosition, prevId, newId);
                    originalTablesToReturn.add(originalTable);
                    //for each table check if a project extension exists, and if yes- add it to the list
                    if (checkIfProjectExtensionExists(currentTableName)) {
                        String projectTableName = currentTableName + NumeratorFix.PROJECT_EXTENSION;
                        TablePrimaryKey projectTablePrimaryKey = tablePrimaryKeyMap.get(projectTableName);
                        if (projectTablePrimaryKey != null) {
                            pkColumnName = projectTablePrimaryKey.getPrimaryColumnName();
                            pkPosition = projectTablePrimaryKey.getPrimaryColumnPosition();
                        }
                        OriginalTable projectTable = new OriginalTable(projectTableName, pkColumnName, pkPosition, prevId, newId);
                        originalTablesToReturn.add(projectTable);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Problem in reading file: currentFile = [" + NumeratorFix.ORIGINAL_TABLES_FILE + ']', e);
        }

        return originalTablesToReturn;
    }

    /**
     * method to verify that a core table has an extension on project.
     * uses SQL query that prints the answer, and then reads the result
     *
     * @param tableName project table
     * @return true if the project table exists, false if not
     */
    private static Boolean checkIfProjectExtensionExists(String tableName) {
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("net.sourceforge.jtds.jdbcx.JtdsDataSource"); //register driver
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            stmt.execute(buildIsProjectTableExistQuery(tableName));
            SQLWarning sqlWarning = stmt.getWarnings();

            //check what is the message printed from SQL
            while (sqlWarning != null) {
                if (sqlWarning.getMessage().equals(TABLE_EXISTS_MESSAGE)) {
                    return Boolean.TRUE;
                }
                if (sqlWarning.getMessage().equals(TABLE_DOES_NOT_EXIST_MESSAGE)) {
                    return Boolean.FALSE;
                }
                sqlWarning.getNextWarning();
            }
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            logger.error("error in executing the query buildIsProjectTableExistQuery: " + e);
        } finally {
            //close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                logger.error("exception in closing statement at checkIfProjectExtensionExists: " + e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("exception in closing connection at checkIfProjectExtensionExists: " + e);
            }
        }
        return Boolean.FALSE;
    }

    /**
     * the method creates a list of all tables the point to it, including the column(s) of each table and relative place
     *
     * @param originalTable original table- the table to which other tables are pointing
     * @return a list of all pointing tables
     */
    public static List<Table> buildReferringTableListFromTable(Table originalTable) {

        GUIHandler.logToGUIandLogger(logger, "debug", "getting referring tables for " + originalTable.getTableName());
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        List<Table> referringTables = new ArrayList<>();

        try {
            Class.forName("net.sourceforge.jtds.jdbcx.JtdsDataSource");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            preparedStatement = conn.prepareStatement("exec sp_fkeys ?");
            preparedStatement.setEscapeProcessing(true);
            preparedStatement.setQueryTimeout(TIMEOUT);
            preparedStatement.setString(1, originalTable.getTableName());

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String referringTableName = resultSet.getString(7);
                String columnName = resultSet.getString(8);
                Boolean isMultipleReferral = Boolean.FALSE;

                for (Table table : referringTables) {
                    //more then one pointing column to original table in the same referring table
                    if (table.getTableName().equals(referringTableName)) {
                        referringTables.remove(table);
                        table.addColumnToColumnList(columnName, getColumnPosition(referringTableName, columnName));
                        referringTables.add(table);
                        isMultipleReferral = Boolean.TRUE;
                    }
                }

                if (isMultipleReferral.equals(Boolean.FALSE) || referringTables.isEmpty()) {
                    ReferringTable referringTable = new ReferringTable(originalTable);
                    referringTable.setTableName(referringTableName); //get name of referring table
                    referringTable.addColumnToColumnList(columnName, getColumnPosition(referringTableName, columnName));
                    referringTables.add(referringTable);
                }
            }

            resultSet.close();
            preparedStatement.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {/*something*/
        } finally {
            //close resources
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                logger.error("problem in creating referring tables (statement)" + e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("problem in creating referring tables (connection)" + e);
            }
        }

        return referringTables;
    }

    /**
     * return the column position of a given table
     *
     * @param tableName  String
     * @param columnName String
     * @return column position STARTING FROM 0 or NO_VALUE (-1) in case of error
     */
    public static Long getColumnPosition(String tableName, String columnName) {
        Connection conn = null;
        Statement stmt = null;

        try {
            Class.forName("net.sourceforge.jtds.jdbcx.JtdsDataSource"); //register driver
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(buildColumnPositionQuery(tableName));

            while (resultSet.next()) {
                if (resultSet.getString(1).equals(columnName)) {
                    return resultSet.getLong(2) - 1L;
                }
            }

            resultSet.close();
            stmt.close();
            conn.close();

        } catch (ClassNotFoundException | SQLException e) {
            logger.error(e);
        } finally {
            //close resources
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        return Table.NO_VALUE;
    }

    /**
     * @param tableName String
     * @return query for checking column position
     */
    private static String buildColumnPositionQuery(String tableName) {
        return "SELECT name, column_id from sys.columns WHERE OBJECT_NAME(object_id) = '" + tableName + "' order by column_id";
    }

    /**
     * @return list of relevant data. TableName, ColumnName, ColumnPosition.
     */
    private static String buildGetPrimaryKeyDataQuery() {
        return "select t.name as TableName, tc.name as ColumnName, ic.key_ordinal as KeyOrderNr\n" +
                "from sys.schemas s \n" +
                "inner join sys.tables t   on s.schema_id=t.schema_id\n" +
                "inner join sys.indexes i  on t.object_id=i.object_id\n" +
                "inner join sys.index_columns ic on i.object_id=ic.object_id   and i.index_id=ic.index_id\n" +
                "inner join sys.columns tc on ic.object_id=tc.object_id \n     and ic.column_id=tc.column_id\n" +
                "where i.is_primary_key=1 order by t.name, ic.key_ordinal ;";
    }

    /**
     * method to check if a project extension exists for the input table
     * The method inserts a message to the statement object, and then we query that object to see if we
     * do or don't have an extension.
     *
     * @param table - original table name
     * @return String
     */
    private static String buildIsProjectTableExistQuery(String table) {
        return "if OBJECT_ID('dbo." + table + "') is not null print '" + TABLE_EXISTS_MESSAGE + "' else print '" + TABLE_DOES_NOT_EXIST_MESSAGE + "'";
    }
}
