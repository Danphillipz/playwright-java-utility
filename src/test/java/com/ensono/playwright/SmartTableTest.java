package com.ensono.playwright;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import com.ensono.utility.Validate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SmartTableTest {

    private enum Tables {
        ALTERNATIVE_PAGINATION("https://datatables.net/examples/basic_init/alt_pagination.html"),
        SCROLL_XY("https://datatables.net/examples/basic_init/scroll_xy.html"),
        INPUT_FORMS("https://datatables.net/examples/api/form.html");

        final String url;
        Tables(String url) {
            this.url = url;
        }

    }
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    private SmartTable table;

    @BeforeAll
    protected static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeEach
    public void beforeEachTest() {
        context = browser.newContext();
        page = context.newPage();
        page.setDefaultNavigationTimeout(60000);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    public SmartTable getTable(Tables table){
        page.navigate(table.url);
        var smartTable = SmartElement.find(page, "id=example").asTable("thead >> th", "tbody >> tr", "td").with(new SmartTable.Navigator(SmartElement.find(page, "id=example_paginate"))
                .withPreviousPage("a:has-text(\"Previous\")")
                .withNextPage("a:has-text(\"Next\")")
                .withPageNumberButtons("span >> a", "class", "current"));
        if (table == Tables.ALTERNATIVE_PAGINATION) {
            smartTable.navigate().withFirstPage("a:has-text(\"First\")")
                    .withLastPage("a:has-text(\"Last\")");
        }
        return this.table = smartTable;
    }

    @Test
    public void testBadHeaderLocator() {
        page.navigate(Tables.ALTERNATIVE_PAGINATION.url);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> SmartTable.find(SmartElement.find(page, "id=example"), "th", "tbody >> tr", "td").getRow(0));
    }

    @Test
    public void testFindRowPage1WithoutNavigator() {
        Assertions.assertEquals("New York", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Caesar Vance")).getCellValue("Office"));
    }

    @Test
    public void testRowNotFoundWithoutNavigator() {
        Assertions.assertTrue(Assertions.assertThrows(NullPointerException.class, () -> getTable(Tables.ALTERNATIVE_PAGINATION).with(null).findRow(Map.of("Name", "Jena Gaines"))).getMessage().contains("No row of data found with the following values: {Name=Jena Gaines}"));
    }

    @Test
    public void testRowFoundWithNavigator() {
        Assertions.assertEquals("Office Manager", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Jena Gaines")).getCellValue("Position"));
    }

    @Test
    public void testFindRowScrollPages() {
        //Should be multiple pages in
        Assertions.assertEquals("$234,500", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Olivia Liang", "Age", "64")).getCellValue("Salary"));
        //Should be at the start of the list
        Assertions.assertEquals("Software Engineer", table.findRow(Map.of("Name", "Brenden Wagner")).getCellValue("Position"));
    }

    @Test
    public void testInvalidColumnHeader() {
        Assertions.assertThrows(Error.class, () -> getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("InvalidHeader", "Jena Gaines")));
        Assertions.assertThrows(NullPointerException.class, () -> table.findRow(Map.of("Name", "Jena Gaines")).getCellValue("Invalid Header"));
    }

    @Test
    public void testAllDataExtracted() {
        Assertions.assertEquals(57, getTable(Tables.ALTERNATIVE_PAGINATION).extractData().size());
    }

    @Test
    public void testFullRowDataExtracted() {
        Assertions.assertTrue(Validate.that().valuesArePresentInMap(Map.of(
                        "Position", "Personnel Lead",
                        "Office", "Edinburgh",
                        "Age", "35",
                        "Start date", "2012-09-26",
                        "Salary", "$217,500"),
                getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Dai Rios")).getValueMap(), Validate.Method.EQUALS));
    }

    @Test
    public void testAllNavigationButtons() {
        Assertions.assertEquals(1, getTable(Tables.ALTERNATIVE_PAGINATION).navigate().getCurrentPageNumber());
        table.navigate().toNextPage();
        table.navigate().toNextPage();
        table.navigate().toNextPage();
        Assertions.assertEquals(4, table.navigate().getCurrentPageNumber());
        table.navigate().toPreviousPage();
        Assertions.assertEquals(3, table.navigate().getCurrentPageNumber());
        table.navigate().toFirstPage();
        Assertions.assertEquals(1, table.navigate().getCurrentPageNumber());
        table.navigate().toLastPage();
        Assertions.assertEquals(6, table.navigate().getCurrentPageNumber());
        Assertions.assertEquals(3, table.navigate().toPage(3).getCurrentPageNumber());
        Assertions.assertEquals(5, table.navigate().toPage(5).getCurrentPageNumber());
        Assertions.assertEquals("Required page 0 but cannot navigate past page 1", Assertions.assertThrows(IndexOutOfBoundsException.class, () -> table.navigate().toPage(0)).getMessage());
        Assertions.assertEquals("Required page 15 but cannot navigate past page 6", Assertions.assertThrows(IndexOutOfBoundsException.class, () -> table.navigate().toPage(15)).getMessage());
        table.navigate().withPreviousPage(null).toFirstPage();
        Assertions.assertEquals(1, table.navigate().getCurrentPageNumber());
    }

    @Test
    public void testHandlingScrollXY() {
        getTable(Tables.SCROLL_XY).navigate().withFirstPage(null).withLastPage(null);
        Assertions.assertEquals("Edinburgh", table.findRow(Map.of("First name", "Cedric", "E-mail", "c.kelly@datatables.net")).getCellValue("Office"));
        Assertions.assertEquals("z.serrano@datatables.net", table.findRow(Map.of("First name", "Zorita", "Last name", "Serrano")).getCellValue("E-mail"));
    }

    @Test
    public void testGetColumnAndSort() {
        getTable(Tables.ALTERNATIVE_PAGINATION).getColumn("Position").dblclick();
        Validate.that().listInAlphabeticalOrder(table.getListOfValues("Name"), true).assertFail();
        table.getColumn("Name").click();
        Validate.that().listInAlphabeticalOrder(table.getListOfValues("Name"), true).assertPass();
        table.getColumn("Name").click();
        Validate.that().listInAlphabeticalOrder(table.getListOfValues("Name"), false).assertPass();
    }

    @Test
    public void testExtractingWithDataEntryFields() {
        getTable(Tables.INPUT_FORMS).with(null).getListOfValues("Position").forEach(value -> Assertions.assertFalse(value.isBlank()));
        table.getListOfValues("Age").forEach(value -> Assertions.assertFalse(value.isBlank()));
        Validate.that().valuesArePresentInMaps(Arrays.asList(
                Map.of("Office", "Tokyo"),
                Map.of("Office", "London"),
                Map.of("Office", "San Francisco"),
                Map.of("Office", "London"),
                Map.of("Office", "San Francisco"),
                Map.of("Office", "New York"),
                Map.of("Office", "London"),
                Map.of("Office", "New York"),
                Map.of("Office", "New York"),
                Map.of("Office", "Edinburgh")), table.extractData("Office"), Validate.Method.EQUALS).assertPass();
    }

    @Test
    public void testEfficientDataValidation() {
        List<Map<String, String>> expectedData = List.of(
                Map.of("Name", "Brielle Williamson", "Position", "Integration Specialist", "Office", "New York", "Age", "61", "Start date", "2012-12-02", "Salary", "$372,000"),
                Map.of("Name", "Garrett Winters", "Position", "Accountant", "Office", "Tokyo", "Age", "63", "Start date", "2011-07-25", "Salary", "$170,750"));
        getTable(Tables.ALTERNATIVE_PAGINATION).validateTable(expectedData, Validate.Method.EQUALS).assertPass();
        Assertions.assertEquals(2, table.navigate().getCurrentPageNumber());
    }

    @Test
    public void testDataNotFound() {
        List<Map<String, String>> expectedData = List.of(
                Map.of("Name", "Brielle Williamson", "Position", "Integration Specialist", "Office", "New York", "Age", "61", "Start date", "2012-12-02", "Salary", "$372,000"),
                Map.of("Name", "Steve Jobs", "Position", "Accountant", "Office", "Tokyo", "Age", "63", "Start date", "2011-07-25", "Salary", "$170,750"));
        String reason = getTable(Tables.ALTERNATIVE_PAGINATION).validateTable(expectedData, Validate.Method.EQUALS).assertFail().getReason();
        expectedData.get(1).values().forEach(value -> Validate.that().compare(value, reason, Validate.Method.CONTAINS).assertPass());
        expectedData.get(0).values().forEach(value -> Validate.that().compare(value, reason, Validate.Method.CONTAINS).assertFail());
    }

    @Test
    public void testDataValidationThroughContains() {
        List<Map<String, String>> expectedData = List.of(
                Map.of("Name", "Gavin", "Position", "Developer", "Office", "Edinburgh", "Age", "42"),
                Map.of("Name", "Gavin", "Position", "Leader", "Office", "San Francisco", "Age", "22"));
        getTable(Tables.ALTERNATIVE_PAGINATION).validateTable(expectedData, Validate.Method.EQUALS).assertFail();
        table.validateTable(expectedData, Validate.Method.CONTAINS).assertPass();
    }

    @Test
    public void testDataValidationThroughCaseInsensitive() {
        List<Map<String, String>> expectedData = List.of(
                Map.of("Name", "brielle williamson", "Position", "integration specialist", "Office", "new york"));
        getTable(Tables.ALTERNATIVE_PAGINATION).validateTable(expectedData, Validate.Method.EQUALS).assertFail();
        table.validateTable(expectedData, Validate.Method.EQUALS_CASE_INSENSITIVE).assertPass();
    }

    @Test
    public void testDataEntry() {
        getTable(Tables.INPUT_FORMS).navigate().withFirstPage(null).withLastPage(null);
        Map<String, String> originalData = Map.of("Name", "Finn Camacho", "Age", "47", "Position", "Support Engineer", "Office", "San Francisco"),
                newData = Map.of("Age", "22", "Position", "QA Consultant", "Office", "London");
        table.findRow(originalData).enterData(newData);
        table.findRow(newData);
    }

}