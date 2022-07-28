package ensono;

import com.microsoft.playwright.*;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Map;
import static ensono.SmartElement.find;

public class SmartTableTest {

    private enum Tables {
        ALTERNATIVE_PAGINATION("https://datatables.net/examples/basic_init/alt_pagination.html"),
        SCROLL_XY("https://datatables.net/examples/basic_init/scroll_xy.html"),
        INPUT_FORMS("https://datatables.net/examples/api/form.html");

        String url;
        Tables(String url) {
            this.url = url;
        }

    }
    private static Playwright playwright;
    private static Browser browser;

    // New instance for each test method.
    private BrowserContext context;
    private Page page;
    SmartTable table;

    @BeforeAll
    protected static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
    }

    @BeforeEach
    public void beforeEachTest() {
        context = browser.newContext();
        page = context.newPage();
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
        var smartTable = SmartTable.find(find(page, "id=example"), "thead >> th", "tbody >> tr", "td").with(new SmartTable.Navigator(find(page, "id=example_paginate"))
                .withPreviousPage("a:has-text(\"Previous\")")
                .withNextPage("a:has-text(\"Next\")")
                .withPageNumberButtons("span >> a", "class", "current"));
        switch (table) {

            case ALTERNATIVE_PAGINATION:
                smartTable.navigate().withFirstPage("a:has-text(\"First\")")
                        .withLastPage("a:has-text(\"Last\")");
            default:
                return this.table = smartTable;
        }
    }

    @Test
    public void testFindRowPage1WithoutNavigator() {
        Assert.assertEquals("New York", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Caesar Vance")).getCellValue("Office"));
    }

    @Test
    public void testRowNotFoundWithoutNavigator() {
        Assert.assertTrue(Assertions.assertThrows(NullPointerException.class, () -> getTable(Tables.ALTERNATIVE_PAGINATION).with(null).findRow(Map.of("Name", "Jena Gaines"))).getMessage().contains("No row of data found with the following values: {Name=Jena Gaines}"));
    }

    @Test
    public void testRowFoundWithNavigator() {
        Assert.assertEquals("Office Manager", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Jena Gaines")).getCellValue("Position"));
    }

    @Test
    public void testFindRowScrollPages() {
        //Should be multiple pages in
        Assert.assertEquals("$234,500", getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Olivia Liang", "Age", "64")).getCellValue("Salary"));
        //Should be at the start of the list
        Assert.assertEquals("Software Engineer", table.findRow(Map.of("Name", "Brenden Wagner")).getCellValue("Position"));
    }

    @Test
    public void testInvalidColumnHeader() {
        Assertions.assertThrows(Error.class, () -> getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("InvalidHeader", "Jena Gaines")));
        Assertions.assertThrows(NullPointerException.class, () -> table.findRow(Map.of("Name", "Jena Gaines")).getCellValue("Invalid Header"));
    }

    @Test
    public void testAllDataExtracted() {
        Assert.assertEquals(57, getTable(Tables.ALTERNATIVE_PAGINATION).extractData().size());
    }

    @Test
    public void testFullRowDataExtracted() {
        Assert.assertTrue(Validate.that().valuesArePresentInMap(Map.of(
                        "Position", "Personnel Lead",
                        "Office", "Edinburgh",
                        "Age", "35",
                        "Start date", "2012-09-26",
                        "Salary", "$217,500"),
                getTable(Tables.ALTERNATIVE_PAGINATION).findRow(Map.of("Name", "Dai Rios")).getValueMap(), Validate.Method.EQUALS));
    }

    @Test
    public void testAllNavigationButtons() {
        Assert.assertEquals(1, getTable(Tables.ALTERNATIVE_PAGINATION).navigate().getCurrentPageNumber());
        table.navigate().toNextPage();
        table.navigate().toNextPage();
        table.navigate().toNextPage();
        Assert.assertEquals(4, table.navigate().getCurrentPageNumber());
        table.navigate().toPreviousPage();
        Assert.assertEquals(3, table.navigate().getCurrentPageNumber());
        table.navigate().toFirstPage();
        Assert.assertEquals(1, table.navigate().getCurrentPageNumber());
        table.navigate().toLastPage();
        Assert.assertEquals(6, table.navigate().getCurrentPageNumber());
        Assert.assertEquals(3, table.navigate().toPage(3).getCurrentPageNumber());
        Assert.assertEquals(5, table.navigate().toPage(5).getCurrentPageNumber());
        Assert.assertEquals("Required page 0 but cannot navigate past page 1", Assertions.assertThrows(IndexOutOfBoundsException.class, () -> table.navigate().toPage(0)).getMessage());
        Assert.assertEquals("Required page 15 but cannot navigate past page 6", Assertions.assertThrows(IndexOutOfBoundsException.class, () -> table.navigate().toPage(15)).getMessage());
        table.navigate().withPreviousPage(null).toFirstPage();
        Assert.assertEquals(1, table.navigate().getCurrentPageNumber());
    }

    @Test
    public void testHandlingScrollXY() {
        getTable(Tables.SCROLL_XY).navigate().withFirstPage(null).withLastPage(null);
        Assert.assertEquals("Edinburgh", table.findRow(Map.of("First name", "Cedric", "E-mail", "c.kelly@datatables.net")).getCellValue("Office"));
        Assert.assertEquals("z.serrano@datatables.net", table.findRow(Map.of("First name", "Zorita", "Last name", "Serrano")).getCellValue("E-mail"));
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
        getTable(Tables.INPUT_FORMS).with(null).getListOfValues("Position").forEach(value -> Assert.assertFalse(value.isBlank()));
        table.getListOfValues("Age").forEach(value -> Assert.assertFalse(value.isBlank()));
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
    public void testDataEntry() {
        getTable(Tables.INPUT_FORMS).navigate().withFirstPage(null).withLastPage(null);
        Map<String, String> originalData = Map.of("Name", "Finn Camacho", "Age", "47", "Position", "Support Engineer", "Office", "San Francisco"),
                newData = Map.of("Age", "22", "Position", "QA Consultant", "Office", "London");
        table.findRow(originalData).enterData(newData);
        table.findRow(newData);
    }

}