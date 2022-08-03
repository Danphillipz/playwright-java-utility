# playwright-java-utility
A set of utility classes to make interacting with web applications simpler through playwright-java

<h3>SmartElement</h3>
<span>A custom implementation of the Playwright Locator with additional capabilities for interacting with web elements</span><br>
<b>Example: Select 'Third Value' </b><br>
<blockquote>SmartElement.find(page, "select").selectOptionByLabel("Third Value");</blockquote>
<h3>SmartTable</h3>
<span>A utility class used to interact with web tables through a common approach</span>
<table id="example">
  <thead>
    <tr><th>Name</th><th>Position</th><th>Office</th><th>Age</th></tr>
 	</thead>
 	<tbody>
 		 <tr><td>Airi Satou</td><td>Accountant</td><td>Tokyo</td><td>33</td></tr>
     <tr><td>Angelica Ramos</td><td>Chief Executive Officer (CEO)</td><td>London</td><td>47</td></tr>
     <tr><td>Ashton Cox</td><td>Junior Technical Author</td><td>San Francisco</td><td>66</td></tr>
  </tbody>
</table>

<b>Example: Find how old 'Ashton Cox' is</b><br>
<span>Get the table by its id 'example' and construct a SmartTable by defining how the table headers, rows and cells are located. Then use the find row function with a map of the data you need to find within a row, on the returned row, call the getCellValue function for the 'Age' column</span><br>
<blockquote>
<span>return SmartElement.find(page, "id=example").asTable("thead >> th", "tbody >> tr", "td").findRow(Map.of("Name", "Ashton Cox")).getCellValue("Age");</span>
</blockquote>
<b>Example: Get all data from all pages</b><br>
<span>In addition to creating a SmartTable, define a Navigator which defines the locator for the table navigation bar and the locators for it's buttons (i.e next, previous, last, first). Upon calling the extract data function, this will automatically use the navigator to extract data across all pages of the table</span><br>
<blockquote>
<span>var table = SmartElement.find(page, "id=example");</span><br>
<span>return SmartTable.find(table, "thead >> th", "tbody >> tr", "td").with(new SmartTable.Navigator(SmartElement.find(page, "id=example_paginate")).withPreviousPage("a:has-text(\"Previous\")").withNextPage("a:has-text(\"Next\")")).extractData();</span>
</blockquote>

<h3>Validate</h3>
A utility class which allows for various types of validation, with results returned in a ValidationResult
<br><b>Example: Check if 'A', 'B', 'C' is in alphabetical order</b><br>
<blockquote>Validate.that().listInAlphabeticalOrder(List.of("A", "B", "C"), true).assertPass();</blockquote>
