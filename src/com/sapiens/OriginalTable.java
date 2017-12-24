/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 8/10/2016
 * Time: 5:07 PM
 */
package com.sapiens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * class to hold main tables
 */
public class OriginalTable extends Table {

    private List<Table> fkTables = new ArrayList<>(); //tables that point to THIS table
    private static Map<String, List<Table>> referringTablesForTableMap = new HashMap<>();

    //instance specific pattern so each table + ID combination is created once no matter how many lines are scanned.
    private Pattern insertPattern = null;
    private Pattern updatePattern = null;
    private Pattern deletePattern = null;
    private Pattern valuesPattern = null;
    private Pattern wherePkPattern = null;

    /**
     * Constructor to create Original Table by name, PK and ID's
     * Table will also contain a list of referring tables
     *
     * @param tableName      String
     * @param pkColumnName   column of FK
     * @param columnPosition the position of the column
     * @param prevId         prev ID
     * @param newId          new ID
     */
    public OriginalTable(String tableName, String pkColumnName, Long columnPosition, Long prevId, Long newId) {
        super(tableName, Table.TYPE_REGULAR, prevId, newId);
        this.addColumnToColumnList(pkColumnName, columnPosition);
        this.fkTables = getOrCreateReferringTablesList(this);
        //TODO add some things to the compile, DOT, LITERAL, what's needed
        this.insertPattern = Pattern.compile(NumeratorFix.INSERT_INTO + tableName + NumeratorFix.VALUES_REGEX + prevId);
        String WherePkIsPrevId = NumeratorFix.WHERE_REGEX + pkColumnName + NumeratorFix.EQUAL_REGEX + prevId;
        this.updatePattern = Pattern.compile(NumeratorFix.UPDATE + tableName + WherePkIsPrevId);
        this.deletePattern = Pattern.compile(NumeratorFix.DELETE_FROM + tableName + WherePkIsPrevId);
        this.valuesPattern = Pattern.compile(NumeratorFix.VALUES_REGEX + prevId);
        this.wherePkPattern = Pattern.compile(tableName + WherePkIsPrevId);
    }

    /**
     * method designed to reduce trips to DB. hold the list for each table in a map and
     * for each instance of this table type just fill in the id's needed.
     *
     * @param originalTable the instance of this Regular Table
     * @return - List of referring Tables for this Table updated to this instance
     */
    private static List<Table> getOrCreateReferringTablesList(OriginalTable originalTable) {

        String tableName = originalTable.getTableName();
        Long prevId = originalTable.getPrevId();
        Long newId = originalTable.getNewId();
        List<Table> referringTablesFromMap = referringTablesForTableMap.get(tableName);
        List<Table> referringTablesToReturn = new ArrayList<>();

        if (referringTablesFromMap == null) {
            referringTablesToReturn = SQLUtil.buildReferringTableListFromTable(originalTable);
            referringTablesForTableMap.put(tableName, referringTablesToReturn);
        }
        //updating the existing list to hold current Regular Table instance data (prev, new ids)
        else {
            for (Table referring : referringTablesFromMap) {
                ReferringTable tableToAdd = new ReferringTable((ReferringTable) referring);
                tableToAdd.setPrevId(prevId);
                tableToAdd.setNewId(newId);
                referringTablesToReturn.add(tableToAdd);
            }
        }

        return referringTablesToReturn;
    }

    /**
     * get the referring tables list
     *
     * @return List of referring tables to this table
     */
    public List<Table> getFkTables() {
        return this.fkTables != null ? this.fkTables : new ArrayList<Table>(0);
    }

    /**
     * @return Pattern
     */
    public Pattern getInsertPattern() {
        return insertPattern;
    }

    /**
     * @return Pattern
     */
    public Pattern getUpdatePattern() {
        return updatePattern;
    }

    /**
     * @return Pattern
     */
    public Pattern getDeletePattern() {
        return deletePattern;
    }

    /**
     * @return Pattern
     */
    public Pattern getValuesPattern() {
        return valuesPattern;
    }

    /**
     * @return Pattern
     */
    public Pattern getWherePkPattern() {
        return wherePkPattern;
    }
}
