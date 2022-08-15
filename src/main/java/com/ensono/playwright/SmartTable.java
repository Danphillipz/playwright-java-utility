package com.ensono.playwright;

import com.ensono.utility.TimeLimit;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import com.ensono.utility.Validate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class used to interact with tables on web pages
 *
 * @author Daniel Phillips
 */
public class SmartTable {

    private enum Type {
        STANDARD, INPUT_VALUES
    }

    private final SmartElement table;
    private final String headersLocator, rowLocator, cellLocator;
    private Navigator navigator;
    private final List<String> headers;
    private final Type type;

    private SmartTable(SmartElement table, String headersLocator, String rowLocator, String cellLocator) {
        this.table = table;
        this.headersLocator = headersLocator;
        this.rowLocator = rowLocator;
        this.cellLocator = cellLocator;
        headers = table.locator(headersLocator).allTextContents();
        // Determine whether the table contains input type elements, influencing how
        // methods interact with the table
        type = table.innerInput().isPresent() ? Type.INPUT_VALUES : Type.STANDARD;
    }

    /**
     * Creates a SmartTable object which enables interaction with a web table. See
     * the included table for an example
     * <table id="example">
     * <thead>
     * <tr>
     * <th>Name</th>
     * <th>Position</th>
     * <th>Office</th>
     * <th>Age</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td>Airi Satou</td>
     * <td>Accountant</td>
     * <td>Tokyo</td>
     * <td>33</td>
     * </tr>
     * <tr>
     * <td>Angelica Ramos</td>
     * <td>Chief Executive Officer (CEO)</td>
     * <td>London</td>
     * <td>47</td>
     * </tr>
     * <tr>
     * <td>Ashton Cox</td>
     * <td>Junior Technical Author</td>
     * <td>San Francisco</td>
     * <td>66</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param table          The main SmartElement object for the entire table, i.e.
     *                       the root table element (e.g id="example" || "table")
     * @param headersLocator Locator for finding the headers within the main table
     *                       element (e.g "thead >> th")
     * @param rowLocator     Locator for finding all rows within the main table
     *                       element (e.g "tbody >> tr")
     * @param cellLocator    Locator for finding each cell within a row (e.g "td")
     * @return A SmartTable object
     */
    public static SmartTable find(SmartElement table, String headersLocator, String rowLocator, String cellLocator) {
        return new SmartTable(table.waitForLoadState(LoadState.NETWORKIDLE), headersLocator, rowLocator, cellLocator);
    }

    /**
     * Can be chained with {@link #find(SmartElement, String, String, String)}
     * during initialisation to add table navigation capabilities to the table
     *
     * @param navigator {@link Navigator}
     * @return {@link SmartTable}
     */
    public SmartTable with(Navigator navigator) {
        this.navigator = navigator;
        return this;
    }

    /**
     * Gets the entire table as it's {@link SmartElement} object
     *
     * @return {@link SmartElement} - {@link #table}
     */
    public SmartElement asSmartElement() {
        return table;
    }

    /**
     * Used to navigate the tables pages via the declared {@link #navigator}
     *
     * @return {@link Navigator}
     */
    public Navigator navigate() {
        return navigator;
    }

    public boolean navigationSet() {
        return navigator != null;
    }

    /**
     * Finds a row within the table which matches the given set of data.
     * <br>
     * If navigation is set through {@link #with(Navigator)} this will first go to
     * the first page of the table via {@link Navigator#toFirstPageIfSet()}
     * <br>
     * Data will be extracted from page 1, and then will cycle through each page via
     * {@link Navigator#toNextPageIfSet()}
     *
     * @param requiredValues Map of required values where Key = Column header, Value
     *                       = required value within the cell
     *                       <br>
     *                       e.g If wanting to find a row which contains the value
     *                       "Test" within the "Example" column you could use
     *                       Map.of("Example", "Test")
     * @return {@link SmartTableRow}
     */
    public SmartTableRow findRow(Map<String, String> requiredValues) {
        Validate.that().valuesArePresentInList(List.of(requiredValues.keySet().toArray()), headers).assertPass();
        if (navigationSet())
            navigate().toFirstPageIfSet();
        Optional<SmartTableRow> row = findRowOnPage(requiredValues);
        while (row.isEmpty() && navigationSet() && navigate().toNextPageIfSet()) {
            row = findRowOnPage(requiredValues);
        }
        return row.orElseThrow(
                () -> new NullPointerException("No row of data found with the following values: " + requiredValues));
    }

    /**
     * Finds a row on the current page which contains all the values within the
     * required headers
     * <br>
     * <b>Does not</b> cycle all pages to data when searching, only the current page
     *
     * @param requiredValues Data to search for on the current page
     * @return {@link Optional} - Use {@link Optional#isPresent()} to see if a row
     * was found
     */
    public Optional<SmartTableRow> findRowOnPage(Map<String, String> requiredValues) {
        return getRows().stream().filter(
                        row -> Validate.that().valuesArePresentInMap(requiredValues, row.getValueMap(), Validate.Method.EQUALS))
                .findFirst();
    }

    /**
     * Returns the locator for all rows within the Table
     *
     * @return {@link SmartElement}
     */
    public SmartElement rows() {
        return (SmartElement) table.locator(rowLocator);
    }

    /**
     * Extracts all data from within the table by calling
     * {@link #extractDataOnPage(String...)} for each page of table data
     * <br>
     * Will only cycle page data if {@link #navigator} has been set through
     * {@link #with(Navigator)}
     * <br>
     * If no headers are specified, all data will be extracted, otherwise only the
     * specified headers of data will be extracted
     *
     * @param columnHeaders Headers of the columns to extract data from (multiple
     *                      parameters can be specified), if none, extract all data
     * @return Builds a list of Maps by calling
     * {@link #extractDataOnPage(String...)} for each page of table data
     */
    public List<Map<String, String>> extractData(String... columnHeaders) {
        if (navigationSet())
            navigate().toFirstPageIfSet();
        List<Map<String, String>> tableData = extractDataOnPage(columnHeaders);
        while (navigationSet() && navigate().toNextPageIfSet()) {
            tableData.addAll(extractDataOnPage(columnHeaders));
        }
        return tableData;
    }

    /**
     * Gets a list of all values within a specific column for each page of table
     * data
     * <br>
     * Will only cycle page data if {@link #navigator} has been set through
     * {@link #with(Navigator)}
     *
     * @param columnHeader Columns to extract data from
     * @return Builds a list by calling {@link #getListOfValuesOnPage(String)} for
     * each page of table data
     */
    public List<String> getListOfValues(String columnHeader) {
        if (navigationSet())
            navigate().toFirstPageIfSet();
        List<String> tableData = getListOfValuesOnPage(columnHeader);
        while (navigationSet() && navigate().toNextPageIfSet()) {
            tableData.addAll(getListOfValuesOnPage(columnHeader));
        }
        return tableData;
    }

    /**
     * Extracts all data from within the tables current page by building a list of
     * maps for each row.
     * <br>
     * Key = Column header, Value = value extracted from the cell in that column
     * <br>
     * If no headers are specified, all data will be extracted, otherwise only the
     * specified headers of data will be extracted
     *
     * @param columnHeaders Headers of the columns to extract data from
     * @return Builds a list of Maps by calling
     * {@link SmartTableRow#getValueMap(String...)} on each row
     */
    public List<Map<String, String>> extractDataOnPage(String... columnHeaders) {
        return getRows().stream().map(row -> row.getValueMap(columnHeaders)).collect(Collectors.toList());
    }

    /**
     * Gets a list of all values within the specified column by calling
     * {@link SmartTableRow#getCellValue(String)} on each row
     * <br>
     * <b>Does not</b> cycle all pages of data when searching, only the current page
     *
     * @param columnHeader Header of the column to extract data from
     * @return List of all values within the column
     */
    private List<String> getListOfValuesOnPage(String columnHeader) {
        return getRows().stream().map(row -> row.getCellValue(columnHeader)).collect(Collectors.toList());
    }

    /**
     * Gets a list of all rows within the table as {@link SmartTableRow} objects
     *
     * @return List of {@link SmartTableRow}
     */
    public List<SmartTableRow> getRows() {
        return IntStream.range(0, rows().count()).boxed().map(this::getRow).collect(Collectors.toList());
    }

    /**
     * Gets a {@link SmartTableRow} object for the row at the given index
     *
     * @param index rows number, where 0 = first row
     * @return {@link SmartTableRow}
     */
    public SmartTableRow getRow(int index) {
        return new SmartTableRow(rows().nth(index));
    }

    /**
     * Gets the locator for the column header cells
     *
     * @return {@link SmartElement} of the column header cells
     */
    public SmartElement getColumns() {
        return (SmartElement) table.locator(headersLocator);
    }

    /**
     * Gets the column cell with the specified header
     *
     * @param header Name of the column to get
     * @return SmartElement for the header cell
     */
    public SmartElement getColumn(String header) {
        return getColumn(getColumnIndex(header));
    }

    /**
     * Gets the column cell at the specified index
     *
     * @param index Index of the column to get (0 = Starting index)
     * @return SmartElement for the header cell
     */
    public SmartElement getColumn(int index) {
        return (SmartElement) getColumns().nth(index);
    }

    /**
     * Checks to see if the table contains the required data
     *
     * @param expectedData Each map in the list equates to a required row of data,
     *                     where K = column and V = required value
     * @param method       {@link com.ensono.utility.Validate.Method}
     * @return {@link com.ensono.utility.Validate.ValidationResult}
     */
    public Validate.ValidationResult validateTable(List<Map<String, String>> expectedData, Validate.Method method) {
        LinkedList<Map<String, String>> remainingData = new LinkedList<>(expectedData);
        if (navigationSet())
            navigate().toFirstPageIfSet();
        remainingData = validatePage(remainingData, method);
        while (!remainingData.isEmpty() && navigationSet() && navigate().toNextPageIfSet()) {
            remainingData = validatePage(remainingData, method);
        }
        return remainingData.isEmpty() ? Validate.ValidationResult.pass()
                : Validate.ValidationResult.fail("Matches not found for the following data: %s",
                remainingData.toString());
    }

    /**
     * Checks to see if the tables current page contains any of the required data
     *
     * @param required Each map in the list equates to a required row of data, where
     *                 K = column and V = required value
     * @param method   {@link com.ensono.utility.Validate.Method}
     * @return List of rows which has not matched any data on the page
     */
    private LinkedList<Map<String, String>> validatePage(LinkedList<Map<String, String>> required,
                                                         Validate.Method method) {
        rowCheck:
        for (int i = 0; i < rows().count(); i++) {
            var rowData = getRow(i).getValueMap();
            for (int j = required.size() - 1; j >= 0; j--) {
                if (Validate.that().valuesArePresentInMap(required.get(j), rowData, method)) {
                    required.remove(j);
                    continue rowCheck;
                }
            }
        }
        return required;
    }

    /**
     * Gets the index for a given column header if it is present within
     * {@link #headers}
     *
     * @param header Header of the column to retrieve the index for
     * @return index of the column, if present, else throws
     * @throws NullPointerException if no column exists for this table, as specified
     *                              by {@link #headers}
     */
    public int getColumnIndex(String header) {
        int index = headers.indexOf(header);
        if (index == -1)
            throw new NullPointerException(String.format("No column with the header '%s' exists", header));
        return index;
    }

    /**
     * An object representation for a row within a web table. Provides utility
     * methods for common interactions
     */
    protected class SmartTableRow {

        Locator element;

        /**
         * Creates a new SmartTableRow
         *
         * @param element The {@link Locator} for this row
         */
        public SmartTableRow(Locator element) {
            this.element = element;
            int cellCount = getCells().count();
            if (cellCount != headers.size()) {
                throw new IndexOutOfBoundsException(String.format(
                        "%d headers identified, but %d cells of data extracted, please verify locators are accurate.%sInner HTML for row where issue found: %s",
                        headers.size(), cellCount, System.lineSeparator(), element.innerHTML()));
            }
        }

        /**
         * Gets the {@link SmartElement} object for the required column in the row
         *
         * @param header Header of the column to get a cell object for
         * @return {@link SmartElement} object for the required cell
         */
        public SmartElement getCell(String header) {
            return (SmartElement) getCells().nth(getColumnIndex(header));
        }

        /**
         * Gets the value within the column of this row
         * <br>
         * 1: {@link Type#STANDARD} By calling {@link #getCell(String)} and
         * {@link Locator#textContent(Locator.TextContentOptions)})
         * <br>
         * 2: {@link Type#INPUT_VALUES} By checking to see if the cell is an input cell
         * through {@link SmartElement#innerInput()} otherwise is treated as standard
         *
         * @param header Header of the column in the row to get the text content
         * @return text content of the specified cell in the row
         */
        public String getCellValue(String header) {
            var cell = getCell(header);
            switch (type) {
                case STANDARD:
                    return cell.textContent();
                case INPUT_VALUES:
                    var inputCell = cell.innerInput();
                    return inputCell.isPresent() ? inputCell.get().inputValue() : cell.innerText();
                default:
                    throw new Error("Unknown table type");
            }

        }

        /**
         * Gets the required cell by calling {@link SmartTableRow#getCell(String)} and
         * then calls {@link Locator#click()} on a child with a href attribute
         *
         * @param header Header of the column which contains a clickable link (a child
         *               element with a href attribute)
         */
        public void selectLink(String header) {
            getCell(header).locator("//*[@href]").click();
        }

        /**
         * Gets all cell contents by calling {@link SmartTableRow#getValues()} and
         * builds a map where K = Column header / V = Cell contents
         * <br>
         * Where only specific headers of data are required,
         * {@link #getCellValue(String)} is used instead
         *
         * @param columnsToExtract The headers of data to build a map for, if none
         *                         specified, a map is built for every column of data
         * @return Map K = Column header / V = Cell contents
         */
        public Map<String, String> getValueMap(String... columnsToExtract) {
            if (columnsToExtract.length == 0) {
                var values = getValues();
                return IntStream.range(0, headers.size()).boxed()
                        .collect(Collectors.toMap(headers::get, values::get));
            }
            return Arrays.stream(columnsToExtract).collect(Collectors.toMap(c -> c, this::getCellValue));
        }

        /**
         * Get all text contents for the current row
         * <br>
         * For performance, one of two approaches are taken:
         * <br>
         * 1: {@link Type#STANDARD} - By calling
         * {@link #getCells()}{@link Locator#allTextContents()}
         * <br>
         * 2: {@link Type#INPUT_VALUES} - Build a list by calling
         * {@link #getCellValue(String)} for each column
         *
         * @return A list of Strings for each cell value
         */
        public List<String> getValues() {
            switch (type) {
                case STANDARD:
                    return getCells().allTextContents();
                case INPUT_VALUES:
                    return headers.stream().map(this::getCellValue).collect(Collectors.toList());
                default:
                    throw new Error("Unrecognised table type");
            }
        }

        /**
         * Gets the locator for cells within the row
         *
         * @return {@link SmartElement}
         */
        public SmartElement getCells() {
            return (SmartElement) element.locator(cellLocator);
        }

        /**
         * For each key/value (where K = Column Header/ V = Value to enter) this will
         * call {@link #enterData(String, String)}
         *
         * @param data - Data to enter into the row
         */
        public void enterData(Map<String, String> data) {
            data.keySet().forEach(header -> enterData(header, data.get(header)));
        }

        /**
         * Enters the value into the required column by getting the input element within
         * the cell ({@link SmartElement#innerInput()} and then calls
         * {@link SmartElement#inputValue(String)}
         *
         * @param header - Header of the column to enter the value into
         * @param value  - Value to enter into the headers
         * @throws PlaywrightException if column is not an editable cell
         */
        public void enterData(String header, String value) {
            getCell(header).innerInput()
                    .orElseThrow(() -> new PlaywrightException("Element is not an editable element")).inputValue(value);
        }
    }

    /**
     * A utility class used to navigate tables through selecting first pages,
     * next/previous pages etc.
     */
    public static class Navigator {

        private final SmartElement navigationBar;
        private SmartElement previousPageLocator, nextPageLocator, firstPageLocator, lastPageLocator,
                pageNumberButtonsLocator;
        private String currentPageNumberAttribute, currentPageNumberRequiredValue;
        private Duration timeoutLimit;

        /**
         * Create a new table navigation bar, where the {@link SmartElement} should be a
         * reference to the entire navigation bar
         * <br>
         * (i.e. inside this element is all buttons to navigate the table)
         *
         * @param navigationBarLocator - The parent element for all navigation elements
         */
        public Navigator(SmartElement navigationBarLocator) {
            this.navigationBar = navigationBarLocator;
            this.timeoutLimit = Duration.ofMinutes(2);
        }

        private boolean isSet(Locator locator) {
            return locator != null;
        }

        /**
         * Sets the locator for the previous page button, which should be relative to
         * the primary {@link #navigationBar}
         *
         * @param previousPageLocator i.e a:has-text("Previous"), which would search for
         *                            a child element of {@link #navigationBar} matching
         *                            this locator
         * @return {@link Navigator}
         */
        public Navigator withPreviousPage(String previousPageLocator) {
            this.previousPageLocator = previousPageLocator == null ? null
                    : SmartElement.fromLocator(navigationBar.locator(previousPageLocator));
            return this;
        }

        /**
         * Sets the locator for the next page button, which should be relative to the
         * primary {@link #navigationBar}
         *
         * @param nextPageLocator i.e a:has-text("Next"), which would search for a child
         *                        element of {@link #navigationBar} matching this
         *                        locator
         * @return {@link Navigator}
         */
        public Navigator withNextPage(String nextPageLocator) {
            this.nextPageLocator = nextPageLocator == null ? null
                    : SmartElement.fromLocator(navigationBar.locator(nextPageLocator));
            return this;
        }

        /**
         * Sets the locator for the first page button, which should be relative to the
         * primary {@link #navigationBar}
         *
         * @param firstPageLocator i.e a:has-text("First"), which would search for a
         *                         child element of {@link #navigationBar} matching this
         *                         locator
         * @return {@link Navigator}
         */
        public Navigator withFirstPage(String firstPageLocator) {
            this.firstPageLocator = firstPageLocator == null ? null
                    : SmartElement.fromLocator(navigationBar.locator(firstPageLocator));
            return this;
        }

        /**
         * Sets the locator for the last page button, which should be relative to the
         * primary {@link #navigationBar}
         *
         * @param lastPageLocator i.e a:has-text("Last"), which would search for a child
         *                        element of {@link #navigationBar} matching this
         *                        locator
         * @return {@link Navigator}
         */
        public Navigator withLastPage(String lastPageLocator) {
            this.lastPageLocator = lastPageLocator == null ? null
                    : SmartElement.fromLocator(navigationBar.locator(lastPageLocator));
            return this;
        }

        /**
         * Sets the locator for the page number buttons, which should be relative to the
         * primary {@link #navigationBar}
         *
         * @param pageNumberButtonsLocator       i.e //a[@class="paginate_button"],
         *                                       which would be for all page number
         *                                       buttons which are a child element of
         *                                       {@link #navigationBar}
         * @param currentPageNumberAttribute     the attribute which should be retrieved
         *                                       to check which page number button is
         *                                       currently selected, i.e "class"
         * @param currentPageNumberRequiredValue the value within the retrieved
         *                                       attribute which indicates the page
         *                                       number button is selected, i.e
         *                                       "current"
         * @return {@link Navigator}
         */
        public Navigator withPageNumberButtons(String pageNumberButtonsLocator, String currentPageNumberAttribute,
                                               String currentPageNumberRequiredValue) {
            this.pageNumberButtonsLocator = pageNumberButtonsLocator == null ? null
                    : SmartElement.fromLocator(navigationBar.locator(pageNumberButtonsLocator));
            this.currentPageNumberAttribute = currentPageNumberAttribute;
            this.currentPageNumberRequiredValue = currentPageNumberRequiredValue;
            return this;
        }

        /**
         * Navigates to the previous page
         *
         * @return true if navigation was performed
         * <br>
         * false would be returned if the element is disabled
         * {@link SmartElement#isParentsOrSelfDisabled()} indicating navigation
         * can no longer be performed
         * @throws NullPointerException if {@link #previousPageLocator} has not been set
         */
        public boolean toPreviousPage() {
            if (!Optional.ofNullable(previousPageLocator)
                    .orElseThrow(() -> new NullPointerException("Previous page locator has not been set"))
                    .isParentsOrSelfDisabled()) {
                previousPageLocator.click();
                return true;
            }
            return false;
        }

        /**
         * Attempts to navigate to the previous page if {@link #previousPageLocator} has
         * been set
         *
         * @return true if set and {@link #toPreviousPage()} returns true
         */
        public boolean toPreviousPageIfSet() {
            return isSet(previousPageLocator) && toPreviousPage();
        }

        /**
         * Navigates to the next page
         *
         * @return true if navigation was performed
         * <br>
         * false would be returned if the element is disabled
         * {@link SmartElement#isParentsOrSelfDisabled()} indicating navigation
         * can no longer be performed
         * @throws NullPointerException if {@link #nextPageLocator} has not been set
         */
        public boolean toNextPage() {
            if (!Optional.ofNullable(nextPageLocator)
                    .orElseThrow(() -> new NullPointerException("Next page locator has not been set"))
                    .isParentsOrSelfDisabled()) {
                nextPageLocator.click();
                return true;
            }
            return false;
        }

        /**
         * Attempts to navigate to the next page if {@link #nextPageLocator} has been
         * set
         *
         * @return true if set and {@link #toNextPage()} returns true
         */
        public boolean toNextPageIfSet() {
            return isSet(nextPageLocator) && toNextPage();
        }

        /**
         * Navigates to the first page within the table
         * <br>
         * If {@link #firstPageLocator} has been set through
         * {@link #withFirstPage(String)} this element will be clicked
         * otherwise attempt to cycle through all previous pages via
         * {@link #toPreviousPage()}
         *
         * @return {@link Navigator}
         * @throws NullPointerException if neither the {@link #firstPageLocator} or
         *                              {@link #previousPageLocator} has been set
         */
        public Navigator toFirstPage() {
            if (isSet(firstPageLocator)) {
                firstPageLocator.click();
            } else if (isSet(previousPageLocator)) {
                TimeLimit limit = new TimeLimit(timeoutLimit);
                while (toPreviousPage() && limit.timeLeftElseThrow());
            } else {
                throw new NullPointerException("Neither the 'First' or 'Previous' page locators have been set");
            }
            return this;
        }

        /**
         * Attempts to navigate to the first page only if {@link #firstPageLocator} or
         * {@link #previousPageLocator} has been set
         *
         * @return true if navigation was performed, false if not
         */
        public boolean toFirstPageIfSet() {
            if (isSet(Optional.ofNullable(firstPageLocator).orElse(previousPageLocator))) {
                toFirstPage();
                return true;
            }
            return false;

        }

        /**
         * Navigates to the last page within the table
         * <br>
         * If {@link #lastPageLocator} has been set through
         * {@link #withLastPage(String)} this element will be clicked
         * otherwise attempt to cycle through all next pages via {@link #toNextPage()}
         *
         * @return {@link Navigator}
         * @throws NullPointerException if neither the {@link #lastPageLocator} or
         *                              {@link #nextPageLocator} has been set
         */
        public Navigator toLastPage() {
            if (isSet(lastPageLocator)) {
                lastPageLocator.click();
            } else if (isSet(nextPageLocator)) {
                TimeLimit limit = new TimeLimit(timeoutLimit);
                while (toNextPage() && limit.timeLeftElseThrow());
            } else {
                throw new NullPointerException("Neither the 'Last' or 'Next' page locators have been set");
            }
            return this;
        }

        /**
         * Attempts to navigate to the last page only if {@link #lastPageLocator} or
         * {@link #nextPageLocator} has been set
         *
         * @return true if navigation was performed, false if not
         */
        public boolean toLastPageIfSet() {
            if (isSet(Optional.ofNullable(lastPageLocator).orElse(nextPageLocator))) {
                toLastPage();
                return true;
            }
            return false;
        }

        /**
         * Gets the page number button which is currently selected and retrieves that
         * page number
         *
         * @return the currently selected page number
         * @throws NullPointerException if
         *                              {@link #withPageNumberButtons(String, String, String)}
         *                              has not been set
         */
        public int getCurrentPageNumber() {
            return Integer.parseInt(Optional.ofNullable(pageNumberButtonsLocator)
                    .orElseThrow(() -> new NullPointerException(
                            "Current page number can only be retrieved if a locator has been set for page number buttons"))
                    .withAttribute(currentPageNumberAttribute, currentPageNumberRequiredValue, Validate.Method.CONTAINS)
                    .textContent());
        }

        /**
         * Navigates to the specified page number
         *
         * @param page page number to navigate to
         * @return {@link Navigator}
         */
        public Navigator toPage(int page) {
            while (page != getCurrentPageNumber()) {
                if (page < getCurrentPageNumber() ? !toPreviousPage() : !toNextPage()) {
                    throw new IndexOutOfBoundsException(String
                            .format("Required page %d but cannot navigate past page %d", page, getCurrentPageNumber()));
                }
            }
            return this;
        }

        /**
         * Sets the default timeout limit for this navigator, i.e the amount of time to wait before throwing an exception if navigation is taking too long between pages
         * @param timeoutLimit The duration of time to wait by default
         */
        public void withTimeoutLimit(Duration timeoutLimit) {
            this.timeoutLimit = timeoutLimit;
        }
    }
}
