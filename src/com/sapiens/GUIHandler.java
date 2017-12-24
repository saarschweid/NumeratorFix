/**
 * Copyright 2005 I.D.I. Technologies Ltd.
 * User: saars
 * Date: 3/19/2017
 * Time: 2:30 PM
 */
package com.sapiens;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

/**
 * general class to deal with all GUI of the application
 */
public class GUIHandler {

    static final Logger logger = Logger.getLogger(GUIHandler.class.getName());

    //CONSTANTS FOR UX
    private static String SELECT_PROPERTIES_FILE = "Load Properties file";
    private static String SELECT_TABLES_LIST = "Tables list";
    private static String SELECT_ROOT_FOLDER = "Root Folder";
    private static String RUN = "Run";
    private static String OPEN_LOG = "Open Results Log";
    public static FileNameExtensionFilter propertiesFilter = new FileNameExtensionFilter("Properties file", "properties");
    private FileNameExtensionFilter tablesFilter = new FileNameExtensionFilter("text files for tables", "txt");

    //Titles
    private final static String FIRST_TAB = "Application";
    private final static String SECOND_TAB = "Help";
    private final static String SCREEN_TITLE = "Numerator Fix - Developed by Saar Schweid " + (char) 169;

    //Messages to the user
    private final static String SELECT_PROPERTIES = "Please select a properties file. For more information see the " + SECOND_TAB + " tab";
    private final static String STARTING_MESSAGE = "Please select a tables list to continue";
    private final static String AFTER_LIST_SELECTION_MESSAGE = "Please select a root folder for scanning";
    private final static String AFTER_ROOT_SELECTION_MESSAGE = "Hit run to start the scan!";
    private final static String AFTER_RUN_CLICK_MESSAGE = "Scanning in progress...";

    //File System
    public static String HOME_DIRECTORY = "C:\\Users\\saars\\Desktop\\";
    public static String LOG4J_LOG_PATH = "./NumeratorFixLog.out";
    public static String NAME_FOR_USER_LOG = "\\NumeratorFixLog.txt";

    //SWING
    private static JFrame mainFrame = null;
    private static JLabel statusLabel = null;
    private static JPanel controlPanel = null;
    private static JTextArea textAreaForLogging = null;
    private static JTextArea textAreaForInfo = null;
    private static JProgressBar progressBar = null;
    private JLabel imageLabel = null;
    private JPanel statusPanel = null;
    private JButton run = new JButton(RUN);
    private JButton selectTables = new JButton(SELECT_TABLES_LIST);
    private JButton selectRoot = new JButton(SELECT_ROOT_FOLDER);
    private JButton openLogButton = new JButton(OPEN_LOG);

    //Internal use
    private Boolean filesSelected = Boolean.FALSE;
    private Boolean rootSelected = Boolean.FALSE;
    private Boolean propertiesSelected = Boolean.FALSE;
    private Boolean logCreatedForThisRun = Boolean.FALSE;
    private String pathToLogFileFromUser = "";

    public GUIHandler() {
        prepareGUI();
    }

    /**
     * @param logInstance Logger
     * @param level       logging level
     * @param message     line to print
     */
    public static void logToGUIandLogger(Logger logInstance, String level, String message) {
        switch (level) {
            case "info":
                logInstance.info(message);
                break;
            case "debug":
                logInstance.debug(message);
                break;
            case "error":
                logInstance.error(message);
                break;
        }
        textAreaForLogging.append('\n' + message);
        textAreaForLogging.setCaretPosition(textAreaForLogging.getDocument().getLength());
        textAreaForLogging.update(textAreaForLogging.getGraphics());
    }

    /**
     * main method for GUI creation.
     */
    protected void prepareGUI() {
        final JFrame frame = new JFrame(SCREEN_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();

        JPanel mainPanel = createMainApplicationTab();
        JPanel helpPanel = createHelpTab();

        tabs.addTab(FIRST_TAB, mainPanel);
        tabs.addTab(SECOND_TAB, helpPanel);

        frame.add(tabs);
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }

    /**
     * create the main application tab
     *
     * @return JPanel
     */
    private JPanel createMainApplicationTab() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel headerPanel = createHeaderPanel();
        JPanel middlePanel = createTextPanel();
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }


    /**
     * method to create the about page tab
     *
     * @return Panel with the about text content
     */
    private JPanel createHelpTab() {
        JPanel aboutPanel = new JPanel();

        textAreaForInfo = new JTextArea(37, 60);
        textAreaForInfo.setMargin(new Insets(10, 20, 10, 10));
        setTextToInfoTab();
        JScrollPane scrollPaneForAbout = new JScrollPane(textAreaForInfo);
        scrollPaneForAbout.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        aboutPanel.add(scrollPaneForAbout);

        return aboutPanel;
    }

    /**
     * @return JPanel headerPanel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        statusPanel = createStatusPanel();
        JPanel buttonPanel = createButtonsPanel();
        headerPanel.add(statusPanel, BorderLayout.NORTH);
        headerPanel.add(buttonPanel, BorderLayout.SOUTH);
        return headerPanel;
    }

    /**
     * method to build the buttons panel
     *
     * @return the buttons panel
     */
    private JPanel createButtonsPanel() {

        JPanel buttonPanel = new JPanel();

        JButton selectProperties = new JButton(SELECT_PROPERTIES_FILE);

        selectProperties.setActionCommand(SELECT_PROPERTIES_FILE);
        selectTables.setActionCommand(SELECT_TABLES_LIST);
        selectRoot.setActionCommand(SELECT_ROOT_FOLDER);
        run.setActionCommand(RUN);

        selectProperties.addActionListener(new ButtonClickListener());
        selectTables.addActionListener(new ButtonClickListener());
        selectRoot.addActionListener(new ButtonClickListener());
        run.addActionListener(new ButtonClickListener());

        selectTables.setEnabled(Boolean.FALSE);
        selectRoot.setEnabled(Boolean.FALSE);
        run.setEnabled(Boolean.FALSE);
        run.setBackground(Color.LIGHT_GRAY); //(155,240,175)

        buttonPanel.add(selectProperties);
        buttonPanel.add(selectTables);
        buttonPanel.add(selectRoot);
        buttonPanel.add(Box.createRigidArea(new Dimension(308, 0)));
        buttonPanel.add(run);

        return buttonPanel;
    }

    /**
     * @return the text panel (log)
     */
    private JPanel createTextPanel() {
        JPanel textPanel = new JPanel();
        textAreaForLogging = new JTextArea(30, 62);
        textAreaForLogging.setMargin(new Insets(10, 10, 10, 10));
        DefaultCaret caret = (DefaultCaret) textAreaForLogging.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPaneForLog = new JScrollPane(textAreaForLogging);
        scrollPaneForLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        textPanel.add(scrollPaneForLog);

        return textPanel;
    }

    /**
     * @return status panel
     */
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Ariel", Font.BOLD, 14));
        statusLabel.setText(SELECT_PROPERTIES);
        statusLabel.setHorizontalAlignment(JLabel.LEFT);
        imageLabel = new JLabel();
        setTransparentIconToStatusPanel(statusPanel);
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 0));
        return statusPanel;
    }

    /**
     * set empty image so that the status won't move when the run starts
     *
     * @param statusPanel JPanel (before it was set)
     */
    private void setTransparentIconToStatusPanel(JPanel statusPanel) {
        Icon loadingIcon = new ImageIcon(NumeratorFix.class.getClassLoader().getResource("appBackground.png"));
        Image imageIcon = ((ImageIcon) loadingIcon).getImage().getScaledInstance(40, 40, Image.SCALE_DEFAULT);
        loadingIcon = new ImageIcon(imageIcon);
        imageLabel = new JLabel(loadingIcon);
        statusPanel.add(imageLabel, BorderLayout.WEST);
    }

    /**
     * set the loading icon to the screen
     */
    private void setLoadingIconToStatusPanel() {
        Icon loadingIcon = new ImageIcon(NumeratorFix.class.getClassLoader().getResource("gear_load_icon.gif"));
        Image imageIcon = ((ImageIcon) loadingIcon).getImage().getScaledInstance(40, 40, Image.SCALE_DEFAULT);
        loadingIcon = new ImageIcon(imageIcon);
        imageLabel.setIcon(loadingIcon);
    }

    /**
     * remove the loading icon from the GUI
     */
    public void stopLoadingIcon() {
        statusPanel.remove(imageLabel);
    }

    /**
     * @return bottom panel
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        openLogButton.setActionCommand(OPEN_LOG);
        openLogButton.setEnabled(false);
        openLogButton.addActionListener(new ButtonClickListener());
        progressBar = new JProgressBar();
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(openLogButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        return bottomPanel;
    }

    /**
     * @param value the progress of the progress bar
     */
    public void setProgressValue(int value) {
        progressBar.setValue(value);
    }

    /**
     * put the about page text.
     * for some reason opening a file doesn't work on exe, (only through intelij).
     * so I used a string instead.
     */
    private void setTextToInfoTab() {
        String text = "";
        text = AboutPageText.TEXT_FOR_ABOUT;
        textAreaForInfo.setText(text);
    }

    /**
     * uploads the properties file
     *
     * @return true if a file was loaded, false if there was no file selection
     */
    private Boolean uploadPropertiesFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(HOME_DIRECTORY));
        chooser.setFileFilter(propertiesFilter);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.showSaveDialog(null);
        File newProp = chooser.getSelectedFile();

        InputStream inputStream = null;

        if (newProp != null) {
            try {
                inputStream = new FileInputStream(newProp);
                NumeratorFix.inputStream = inputStream;
                return Boolean.TRUE;
            } catch (FileNotFoundException e) {
                logger.error("couldn't turn properties file into input stream");
            }
        }
        return Boolean.FALSE;
    }

    /**
     * enable the open log button after Run was finished
     */
    public void enableOpenLogButton() {
        openLogButton.setEnabled(true);
    }

    /**
     * method to set a new status on screen
     *
     * @param text text to be shown on status label
     */
    public void setTextToStatusLabel(String text) {
        statusLabel.setText(text);
    }

    /**
     * perform post run validations and settings
     */
    private void performPostRunActions() {
        run.setEnabled(Boolean.FALSE);
        selectTables.setEnabled(Boolean.FALSE);
        selectRoot.setEnabled(Boolean.FALSE);
        propertiesSelected = false;
        filesSelected = false;
        rootSelected = false;
        setLoadingIconToStatusPanel();
    }

    /**
     * *************** inner class to deal with all button clicks of the application *****************
     */
    private class ButtonClickListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();

            if (command.equals(SELECT_TABLES_LIST)) {
                handleFilesSelection(command);
                statusLabel.setText(AFTER_LIST_SELECTION_MESSAGE);
                enableRunButtonIfNeeded();
                selectRoot.setEnabled(Boolean.TRUE);
            } else if (command.equals(SELECT_ROOT_FOLDER)) {
                handleFilesSelection(command);
                statusLabel.setText(AFTER_ROOT_SELECTION_MESSAGE);
                enableRunButtonIfNeeded();
            } else if (command.equals(RUN)) {
                validateDataBeforeRun();
                statusLabel.setText(AFTER_RUN_CLICK_MESSAGE);
                new Thread() {
                    public void run() {
                        NumeratorFix numeratorFix = new NumeratorFix(GUIHandler.this);
                        numeratorFix.start();
                    }
                }.start();
                performPostRunActions();
            } else if (command.equals(OPEN_LOG)) {
                openLogFile();
            } else if (command.equals(SELECT_PROPERTIES_FILE)) {
                if (uploadPropertiesFile()) {
                    enableButtonsAfterPropertiesSelected();
                    statusLabel.setText(STARTING_MESSAGE);
                }
            }
        }

        /**
         * enable root folder and table selection buttons only after properties file was selected.
         */
        private void enableButtonsAfterPropertiesSelected() {
            selectTables.setEnabled(Boolean.TRUE);
            propertiesSelected = Boolean.TRUE;
        }

        /**
         * enable the run button after all data was entered.
         */
        private void enableRunButtonIfNeeded() {
            if (filesSelected && rootSelected && propertiesSelected) {
                run.setEnabled(Boolean.TRUE);
                run.setBackground(new Color(155, 240, 175));
            }
        }

        /**
         * method to deal with tables list selection and root folder selection
         *
         * @param command root folder / tables list
         */
        private void handleFilesSelection(String command) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(HOME_DIRECTORY));
            int fileChooser;
            if (command.equals(SELECT_TABLES_LIST)) {
                fileChooser = JFileChooser.FILES_ONLY;
                chooser.setFileFilter(tablesFilter);
            } else {
                fileChooser = JFileChooser.DIRECTORIES_ONLY;
            }
            chooser.setFileSelectionMode(fileChooser);
            chooser.showSaveDialog(null);
            File tablesFile = chooser.getSelectedFile();
            if (command.equals(SELECT_TABLES_LIST)) {
                NumeratorFix.ORIGINAL_TABLES_FILE = tablesFile.getAbsolutePath();
                filesSelected = Boolean.TRUE;
            } else {
                NumeratorFix.rootFolder = tablesFile;
                rootSelected = Boolean.TRUE;
            }
        }

        /**
         * method to open the log file after the run.
         * if the user provided a new path, open the file and save it in the given path.
         * if not, use the internal log created by Log4j
         */
        private void openLogFile() {

            String logPath = NumeratorFix.USER_RESULTS_LOG_PATH;
            checkUserLogPathExistAndCreateNewLog(logPath);
            String fileToOpen = logCreatedForThisRun ? pathToLogFileFromUser : LOG4J_LOG_PATH;

            Runtime runtime = Runtime.getRuntime();
            try {
                Process process = runtime.exec("notepad " + fileToOpen);
            } catch (IOException e) {
                logger.error("error opening result file", e);
            }
        }

        /**
         * creates a new log file in the path the user provided (in case it was provided)
         *
         * @param userLogPath the path for the log file, given by the user
         */
        private void checkUserLogPathExistAndCreateNewLog(String userLogPath) {

            //if user provided a path to a new log, create a new file in the given path
            if (userLogPath != null && !userLogPath.equals("") && !logCreatedForThisRun) {
                File applicationLogFile = new File(LOG4J_LOG_PATH);
                File userLogFile = new File(userLogPath + NAME_FOR_USER_LOG);

                try {
                    FileInputStream fis = new FileInputStream(applicationLogFile);
                    BufferedReader in = new BufferedReader(new InputStreamReader(fis));

                    FileWriter fstream = new FileWriter(userLogFile, true);
                    BufferedWriter out = new BufferedWriter(fstream);

                    String aLine = null;
                    while ((aLine = in.readLine()) != null) {
                        //Process each line and add output to Dest.txt file
                        out.write(aLine);
                        out.newLine();
                    }

                    pathToLogFileFromUser = userLogFile.getCanonicalPath();

                    in.close();
                    out.close();
                } catch (IOException e) {
                    logger.error("couldn't create custom user log ", e);
                }

                logCreatedForThisRun = Boolean.TRUE;
            }
        }

        /**
         * method to check that all the data entered is valid. (not in use for now).
         */
        private void validateDataBeforeRun() {
            //check that a file has been selected - if not alert that previous file will be used.
            //check that root folder has been selected - if not alert.
            logCreatedForThisRun = Boolean.FALSE;
            openLogButton.setEnabled(false);
            pathToLogFileFromUser = "";
        }
    }
}
