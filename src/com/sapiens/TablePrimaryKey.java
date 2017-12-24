/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 9/15/2016
 * Time: 3:32 PM
 */
package com.sapiens;

/**
 * an object to hold all relevant data to primary keys of original tables.
 * the idea is that we cannot assume that the PK is located on the first column of the table.
 * <p/>
 * if any more data is needed from DB, it will be added as fields here.
 */
public class TablePrimaryKey {

    private String tableName;
    private String primaryColumnName;
    private Long primaryColumnPosition;

    /**
     * @param tableName             the table name to which this Object belongs
     * @param primaryColumnName     PK column name
     * @param primaryColumnPosition PK position in the table starting from 0
     */
    TablePrimaryKey(String tableName, String primaryColumnName, Long primaryColumnPosition) {
        this.tableName = tableName;
        this.primaryColumnName = primaryColumnName;
        this.primaryColumnPosition = primaryColumnPosition;
    }

    /**
     * @return this table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return the PK column name
     */
    public String getPrimaryColumnName() {
        return primaryColumnName;
    }

    /**
     * @return the PK column position
     */
    public Long getPrimaryColumnPosition() {
        return primaryColumnPosition;
    }
}
