/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 4/12/2017
 * Time: 9:55 AM
 */
package com.sapiens;

/**
 * The about page for the app
 */
public class AboutPageText {

    public static final String TEXT_FOR_ABOUT = "\nFor any questions about this tool, or bugs found, please contact saar.schweid@sapiens.com \n\n " +
            "-----------------------------------------------------------------------------------------------------------------------------------\n" +
            "Running instructions to NumeratorFix:\n" +
            "\n" +
            "1. Select a properties file (See format example below)\n" +
            "2. Select the tables list file (see format example below)\n" +
            "3. Select the root folder for scanning. Example: \"C:/work/hscx/Release Documents/Hscx/Releases\"\n" +
            "4. Run the application.\n" +
            "\nNote:\nYou have to select the properties file on every run of the application\n\n" +
            "A log with the results will be saved in the path supplied in the properties file (RESULT_LOG_PATH)\n" +
            "It is also possible to open the log directly when the application is finished.\n" +
            "\n" +
            "-----------------------------------------------------------------------------------------------------------------------------------\n" +
            "\n" +
            "What is covered in the application:\n" +
            "\n" +
            "1. Referring table is changed based on the correct column position.\n" +
            "2. Different column name for the primary key in the original table\n" +
            "3. Non separating commas in the middle of an insert statement to a referring table (like in description)\n" +
            "4. Multiple swaps in same line for a referring table (many FK to the same table)\n" +
            "5. Dealing with lower case statements, or partly lower statements.\n" +
            "6. None vs one vs many referring tables\n" +
            "7. Project tables extensions\n" +
            "8. PK is not on the first column\n" +
            "9. ID value is very short (for example id with two digits)\n" +
            "\n" +
            "Limitations (for future development):\n\n" +
            "1. Multiple line statements\n" +
            "2. Creating the tables list independently\n" +
            "    2.1 Finding the problematic IDs between DBs\n" +
            "    2.2 Setting new IDs for the previous IDs found\n" +
            "    2.3 Creating and saving a tables list file.\n" +
            "\n" +
            "-----------------------------------------------------------------------------------------------------------------------------------\n\n" +
            "The properties file needs to look like this (See HSCX for example):\n" +
            "\n" +
            "*********************************************************************************************************\n" +
            "# DB details\n" +
            "JDBC_DRIVER = net.sourceforge.jtds.jdbc.Driver\n" +
            "DB_URL = insert DB URL from properties file here\n" +
            "DB_USER = user\n" +
            "DB_PASSWORD = pass\n" +
            "DB_TIMEOUT = 1000\n" +
            "\n" +
            "# Project name\n" +
            "PROJECT_NAME = HSCX\n" +
            "\n" +
            "# ****THE FOLLOWING IS OPTIONAL****\n" +
            "\n" +
            "#scan only DML files that are named like NAME_OF_DML\n" +
            "SCAN_ONLY_DML = true\n" +
            "NAME_OF_DML = DMLChanges\n" +
            "\n\n" +
            "#It's not mandatory, but best if you specify destination for the log results to be saved\n" +
            "RESULT_LOG_PATH = C:/Users/saars/Desktop\n" +
            "*********************************************************************************************************\n" +
            "\n" +
            "-----------------------------------------------------------------------------------------------------------------------------------\n\n" +
            "The tables list should look like this:\n" +
            "\n" +
            "Table_name\n" +
            "old_id to new_id\n" +
            "\n" +
            "Example:\n" +
            "\n" +
            "*********************************************************************************************************\n" +
            "T_PRODUCT_LINE\n" +
            "8888888 to 9999999\n" +
            "\n" +
            "T_CLAUSE_HEADER\n" +
            "9999999 to 8888888\n" +
            "7777777 to 5555555\n" +
            "6666666 to 4444444\n" +
            "*********************************************************************************************************";
}
