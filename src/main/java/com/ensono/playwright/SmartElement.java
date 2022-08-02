package com.ensono.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.ensono.utility.Validate;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Custom implementation of the {@link Locator} interface with additional capabilities over the standard playwright implementation
 * @author dphillips
 */
public class SmartElement implements Locator {

    private final Locator locator;
    private SmartElement(Page page, String locator) {
        this.locator = page.locator(locator);
    }

    private SmartElement(Locator locator) { this.locator = locator; }

    /**
     * Find an element on the page and build a {@link SmartElement} object
     * @param page {@link Page} to find the element on
     * @param locator Locator for the element (e.g "//a[text()='%s']")
     * @param format Formats the locator (e.g "test" = //a[text()='test'])
     * @return {@link SmartElement}
     */
    public static SmartElement find(Page page, String locator, String... format) {
        return new SmartElement(page, String.format(locator, format));
    }

    /**
     * Builds a {@link SmartElement} object from an existing {@link Locator}
     * @param locator Locator to build the SmartElement from
     * @return {@link SmartElement}
     */
    public static SmartElement fromLocator(Locator locator){
        return new SmartElement(Optional.ofNullable(locator).orElseThrow(() -> new NullPointerException("Cannot create SmartElement from null locator")));
    }

    /**
     * Creates a {@link SmartTable} object from this {@link SmartElement}
     * @param headersLocator Locator for finding the headers within the main table element (e.g "thead >> th")
     * @param rowLocator    Locator for finding all rows within the main table element (e.g "tbody >> tr")
     * @param cellLocator   Locator for finding each cell within a row (e.g "td")
     * @return
     */
    public SmartTable asTable(String headersLocator, String rowLocator, String cellLocator){
        return SmartTable.find(this, headersLocator, rowLocator, cellLocator);
    }

    private SmartElement cast(Locator locator) {
        return (SmartElement) locator;
    }

    /**
     * Check if the locator is valid by querying {@link #count()} > 0 (indicating matches were found)
     * @return true if 1 or more elements were found matching the locator
     */
    public boolean isValid() {
        return locator.count() > 0;
    }

    public void selectOptionByValue(String values) {
        selectOption(values);
    }

    public void selectOptionByLabel(String value) {
        selectOption(new SelectOption().setLabel(value));
    }

    /**
     * Gets a SmartElement matching the defined {@link #locator} which contains the required value within the specified attribute
     * @param attribute e.g "class"
     * @param requiredValue e.g "selected"
     * @param validationMethod {@link Validate.Method}
     * @return {@link #nth(int)} {@link SmartElement} which passes the required validation method
     * @throws NoSuchElementException if no attribute found containing the required value
     */
    public SmartElement withAttribute(String attribute, String requiredValue, Validate.Method validationMethod) {
        return cast(nth(IntStream.range(0, count()).filter(
                i -> Validate.that().compare(requiredValue, nth(i).getAttribute(attribute), validationMethod).passed()).findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format("Unable to find an element with the value '%s' in the '%s' attribute for this locator", requiredValue, attribute)))));
    }
    public SmartElement waitForLoadState(LoadState state) {
        page().waitForLoadState(state);
        return this;
    }

    /**
     * Check to see whether a child can be found with the specified locator
     * @param childLocator locator for the child which is passed into {@link #locator(String)}
     * @return result of {@link #isValid()} for the child
     */
    public boolean hasChild(String childLocator){
        return cast(locator(childLocator)).isValid();
    }

    /**
     * Given a number of possible locators, will check each one until it finds the first locator which returns true when calling {@link #hasChild(String)}
     * @param locators Array of locators to check for existence of a child element
     * @return {@link Optional} empty if no child found, otherwise can use {@link Optional#get()} to extract the matching child locator
     */
    public Optional<String> getChild(String...locators){
        return Arrays.stream(locators).filter(this::hasChild).findFirst();
    }

    /**
     * Checks to see if there is an input element as a child of this SmartElement via {@link #getChild(String...)}
     * <br>Input types checked for<ol><li>input</li><li>textarea</li><li>select</li></ol>
     * @return {@link Optional} empty if no input element found, otherwise can use {@link Optional#get()} to extract the input element
     */
    public Optional<SmartElement> innerInput() {
        return getChild("//input", "//textarea", "//select").map(s -> cast(locator(s)));
    }

    /**
     * Get the tag name of this element, e.g. h1, p, select, input, textarea, div etc
     * @return tag name
     */
    public String getTagName() {
        return String.valueOf(evaluate("e => e.tagName"));
    }

    //All interface methods below
    @Override
    public List<String> allInnerTexts() {
        return locator.allInnerTexts();
    }

    @Override
    public List<String> allTextContents() {
        return locator.allTextContents();
    }

    @Override
    public BoundingBox boundingBox(BoundingBoxOptions boundingBoxOptions) {
        return locator.boundingBox(boundingBoxOptions);
    }

    @Override
    public void check(CheckOptions checkOptions) {
        locator.check(checkOptions);
    }

    @Override
    public void click(ClickOptions clickOptions) {
        locator.click(clickOptions);
    }

    @Override
    public int count() {
        return locator.count();
    }

    @Override
    public void dblclick(DblclickOptions dblclickOptions) {
        locator.dblclick();
    }

    @Override
    public void dispatchEvent(String s, Object o, DispatchEventOptions dispatchEventOptions) {
        locator.dispatchEvent(s, o, dispatchEventOptions);
    }

    @Override
    public void dragTo(Locator locator, DragToOptions dragToOptions) {
    locator.dragTo(locator, dragToOptions);
    }

    @Override
    public ElementHandle elementHandle(ElementHandleOptions elementHandleOptions) {
        return locator.elementHandle(elementHandleOptions);
    }

    @Override
    public List<ElementHandle> elementHandles() {
        return locator.elementHandles();
    }

    @Override
    public Object evaluate(String s, Object o, EvaluateOptions evaluateOptions) {
        return locator.evaluate(s,o,evaluateOptions);
    }

    @Override
    public Object evaluateAll(String s, Object o) {
        return locator.evaluateAll(s,o);
    }

    @Override
    public JSHandle evaluateHandle(String s, Object o, EvaluateHandleOptions evaluateHandleOptions) {
        return locator.evaluateHandle(s,o,evaluateHandleOptions);
    }

    @Override
    public void fill(String s, FillOptions fillOptions) {
        locator.fill(s,fillOptions);
    }

    @Override
    public Locator filter(FilterOptions filterOptions) {
        return fromLocator(locator.filter(filterOptions));
    }

    @Override
    public Locator first() {
        return fromLocator(locator.first());
    }

    @Override
    public void focus(FocusOptions focusOptions) {
        locator.focus(focusOptions);
    }

    @Override
    public FrameLocator frameLocator(String s) {
        return locator.frameLocator(s);
    }

    @Override
    public String getAttribute(String s, GetAttributeOptions getAttributeOptions) {
        return locator.getAttribute(s, getAttributeOptions);
    }

    @Override
    public void highlight() {
        locator.highlight();
    }

    @Override
    public void hover(HoverOptions hoverOptions) {
        locator.hover(hoverOptions);
    }

    @Override
    public String innerHTML(InnerHTMLOptions innerHTMLOptions) {
        return locator.innerHTML(innerHTMLOptions);
    }

    @Override
    public String innerText(InnerTextOptions innerTextOptions) {
        return locator.innerText(innerTextOptions);
    }

    @Override
    public String inputValue(InputValueOptions inputValueOptions) {
        return locator.inputValue(inputValueOptions);
    }

    /**
     * Inputs the given value into the element depending on its tag ({@link #getTagName()})
     * <ol><li>SELECT - Calls {@link #selectOptionByLabel(String)}</li>
     * <li>Default - Call {@link #fill(String)}</li></ol>
     * @param value Value to enter
     */
    public void inputValue(String value){
        if ("SELECT".equals(getTagName())) {
            selectOptionByLabel(value);
        } else {
            fill(value);
        }
    }

    @Override
    public boolean isChecked(IsCheckedOptions isCheckedOptions) {
        return locator.isChecked(isCheckedOptions);
    }

    @Override
    public boolean isDisabled(IsDisabledOptions isDisabledOptions) {
        String disabled = getAttribute("disabled");
        if (disabled != null && (disabled.isBlank() || disabled.contains("true"))) {
            return true;
        }else if ((disabled = getAttribute("class")) != null && (disabled.contains("disabled"))) {
            return true;
        }
        return locator.isDisabled(isDisabledOptions);
    }

    public boolean isParentsOrSelfDisabled() {
        return isParentsOrSelfDisabled(null);
    }

    public boolean isParentsOrSelfDisabled(IsDisabledOptions isDisabledOptions) {
        if(isDisabled(isDisabledOptions)) {
            return true;
        }
        SmartElement parent = fromLocator(locator.locator("xpath=.."));
        return parent.isValid() && parent.isParentsOrSelfDisabled(isDisabledOptions);
    }

    @Override
    public boolean isEditable(IsEditableOptions isEditableOptions) {
        return locator.isEditable(isEditableOptions);
    }

    @Override
    public boolean isEnabled(IsEnabledOptions isEnabledOptions) {
        return locator.isEnabled(isEnabledOptions) && !isDisabled();
    }

    @Override
    public boolean isHidden(IsHiddenOptions isHiddenOptions) {
        return locator.isHidden(isHiddenOptions);
    }

    @Override
    public boolean isVisible(IsVisibleOptions isVisibleOptions) {
        return locator.isVisible(isVisibleOptions);
    }

    @Override
    public Locator last() {
        return fromLocator(locator.last());
    }

    @Override
    public Locator locator(String s, LocatorOptions locatorOptions) {
        return fromLocator(locator.locator(Optional.ofNullable(s).orElseThrow(() -> new NullPointerException("Cannot locate with null locator")), locatorOptions));
    }

    public Locator locator(String s, String... format){
        return fromLocator(locator.locator(String.format(Optional.ofNullable(s).orElseThrow(() -> new NullPointerException("Cannot locate with null locator")), format)));
    }

    @Override
    public Locator nth(int i) {
        return fromLocator(locator.nth(i));
    }

    @Override
    public Page page() {
        return locator.page();
    }

    @Override
    public void press(String s, PressOptions pressOptions) {
        locator.press(s, pressOptions);
    }

    @Override
    public byte[] screenshot(ScreenshotOptions screenshotOptions) {
        return locator.screenshot(screenshotOptions);
    }

    @Override
    public void scrollIntoViewIfNeeded(ScrollIntoViewIfNeededOptions scrollIntoViewIfNeededOptions) {
        locator.scrollIntoViewIfNeeded(scrollIntoViewIfNeededOptions);
    }

    @Override
    public List<String> selectOption(String s, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(s, selectOptionOptions);
    }

    @Override
    public List<String> selectOption(ElementHandle elementHandle, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(elementHandle, selectOptionOptions);
    }

    @Override
    public List<String> selectOption(String[] strings, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(strings, selectOptionOptions);
    }

    @Override
    public List<String> selectOption(SelectOption selectOption, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(selectOption, selectOptionOptions);
    }

    @Override
    public List<String> selectOption(ElementHandle[] elementHandles, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(elementHandles, selectOptionOptions);
    }

    @Override
    public List<String> selectOption(SelectOption[] selectOptions, SelectOptionOptions selectOptionOptions) {
        return locator.selectOption(selectOptions, selectOptionOptions);
    }

    @Override
    public void selectText(SelectTextOptions selectTextOptions) {
        locator.selectText(selectTextOptions);
    }

    @Override
    public void setChecked(boolean b, SetCheckedOptions setCheckedOptions) {
        locator.setChecked(b, setCheckedOptions);
    }

    @Override
    public void setInputFiles(Path path, SetInputFilesOptions setInputFilesOptions) {
        locator.setInputFiles(path, setInputFilesOptions);
    }

    @Override
    public void setInputFiles(Path[] paths, SetInputFilesOptions setInputFilesOptions) {
        locator.setInputFiles(paths, setInputFilesOptions);
    }

    @Override
    public void setInputFiles(FilePayload filePayload, SetInputFilesOptions setInputFilesOptions) {
        locator.setInputFiles(filePayload, setInputFilesOptions);
    }

    @Override
    public void setInputFiles(FilePayload[] filePayloads, SetInputFilesOptions setInputFilesOptions) {
        locator.setInputFiles(filePayloads, setInputFilesOptions);
    }

    @Override
    public void tap(TapOptions tapOptions) {
        locator.tap(tapOptions);
    }

    @Override
    public String textContent(TextContentOptions textContentOptions) {
        //Wait for idle network to ensure text has rendered
        return waitForLoadState(LoadState.NETWORKIDLE).locator.textContent(textContentOptions);
    }

    @Override
    public void type(String s, TypeOptions typeOptions) {
        locator.type(s, typeOptions);
    }

    @Override
    public void uncheck(UncheckOptions uncheckOptions) {
        locator.uncheck(uncheckOptions);
    }

    @Override
    public void waitFor(WaitForOptions waitForOptions) {
        locator.waitFor(waitForOptions);
    }

}
