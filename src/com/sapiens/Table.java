/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 8/17/2016
 * Time: 2:35 PM
 */
package com.sapiens;


import java.util.ArrayList;
import java.util.List;

/**
 * basic table Abstract class (for Original and FK use)
 */
public abstract class Table {

    //Constants
    public static final String TYPE_REGULAR = "TYPE_REGULAR";
    public static final String TYPE_FOREIGN_KEY = "TYPE_FOREIGN_KEY";
    public static final String NOT_INIT = "NOT_INIT";
    public static final Long NO_VALUE = -1L;

    //Table fields
    protected String tableName;
    protected String tableType;
    protected Long prevId;
    protected Long newId;
    protected List<TableColumns> relevantColumns = new ArrayList<>();

    /**
     * Default Constructor
     */
    protected Table() {
        this("NO_NAME", Table.NO_VALUE, Table.NO_VALUE);
    }

    /**
     * @param tableName name
     * @param prevId    prev Id that needs to be changed
     * @param newId     what to change to
     */
    protected Table(String tableName, Long prevId, Long newId) {
        this(tableName, Table.NOT_INIT, prevId, newId);
    }

    /**
     * @param tableName table name
     * @param tableType table type (referring or original)
     * @param prevId    previous Id that needs to be replaced
     * @param newId     new Id value
     */
    protected Table(String tableName, String tableType, Long prevId, Long newId) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.prevId = prevId;
        this.newId = newId;
    }

    /**
     * getter to this instance of table name
     *
     * @return table name
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * setter to this instance of table name
     *
     * @param tableName table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * getter to this instance of table type (original or referring)
     *
     * @return table type
     */
    public String getTableType() {
        return this.tableType;
    }

    /**
     * setter to this instance of table type (original or referring)
     *
     * @param tableType table type
     */
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    /**
     * get the id that needs to be replaced
     *
     * @return id to change
     */
    public Long getPrevId() {
        return this.prevId;
    }

    /**
     * set the id that needs to be replaced
     *
     * @param prevId id to change
     */
    public void setPrevId(Long prevId) {
        this.prevId = prevId;
    }

    /**
     * gets the new id that needs to be put
     *
     * @return new id
     */
    public Long getNewId() {
        return this.newId;
    }

    /**
     * sets the new id that needs to be put
     *
     * @param newId new id
     */
    public void setNewId(Long newId) {
        this.newId = newId;
    }

    /**
     * @return list of the columns to this table instance
     */
    public List<TableColumns> getRelevantColumns() {
        return relevantColumns;
    }

    /**
     * add a column to the column list of this table
     *
     * @param columnName     column to be added
     * @param columnPosition position of the column in the table (starting from 0)
     */
    public void addColumnToColumnList(String columnName, Long columnPosition) {
        TableColumns tableColumn = new TableColumns(columnName, columnPosition);
        this.relevantColumns.add(tableColumn);
    }

    /**
     * inner class to hold the columns of the table instance
     */
    class TableColumns {

        private String columnName = NOT_INIT;
        private Long columnPosition = NO_VALUE;

        TableColumns() {
        }

        /**
         * Constructor with given column name and position
         *
         * @param columnName     - name
         * @param columnPosition - position
         */
        TableColumns(String columnName, Long columnPosition) {
            this.columnName = columnName;
            this.columnPosition = columnPosition;
        }

        /**
         * @return Column name
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * @return Column position starting from 0 or Table.NO_VALUE
         */
        public Long getColumnPosition() {
            return columnPosition;
        }
    }
}
