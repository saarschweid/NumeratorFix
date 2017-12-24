/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 8/10/2016
 * Time: 5:17 PM
 */
package com.sapiens;

/**
 * class to hold referring tables to original. these tables hold a foreign key(s) to original table
 */
public class ReferringTable extends Table {

    protected String originalTableName;

    /**
     * this Constructor is used by the Original Table it refers to. The idea is to keep all referring tables for
     * an original table in a map, so that trips to DB are minimized. to create current instance of referring table,
     * we use this Constructor in order to duplicate all the non-changing data, and change just the id's
     *
     * @param table - another referring table.
     */
    public ReferringTable(ReferringTable table) {
        super();
        this.originalTableName = table.getOriginalTableName();
        this.tableName = table.getTableName();
        this.tableType = Table.TYPE_FOREIGN_KEY;
        this.relevantColumns = table.getRelevantColumns();
    }

    /**
     * this constructor is used by the SQLUtil method to build the referring tables list.
     *
     * @param originalTable original table to point to
     */
    public ReferringTable(Table originalTable) {
        this.originalTableName = originalTable.getTableName();
        this.setTableType(Table.TYPE_FOREIGN_KEY);
        this.setPrevId(originalTable.getPrevId());
        this.setNewId(originalTable.getNewId());
    }

    /**
     * @return Original table's name
     */
    public String getOriginalTableName() {
        return originalTableName;
    }
}
