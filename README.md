# playwright-java-utility
A set of utility classes to make interacting with web applications simpler through playwright-java

<h3>SmartElement</h3>
A custom implementation of the Playwright Locator with additional capabilities for interacting with web elements

<h3>SmartTable</h3>
A utility class used to interact with web tables through a common approach
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

<h5>Example: Find how old 'Ashton Cox' is</h5>
<a>Get the table by it's id 'example' and construct a SmartTable by defining how the table headers, rows and cells are located. Then use the find row function with a map of the data you need to find within a row, on the returned row, call the getCellValue function for the 'Age' column</a><br>
<sub>
<a>var table = SmartElement.find(page, "id=example");</a><br>
<a>//SmartTable.find( SmartElement object, header locator, row locator, cell locator)</a><br>
<a>return SmartTable.find(table, "thead >> th", "tbody >> tr", "td").findRow(Map.of("Name", "Ashton Cox")).getCellValue("Age");</a>
</sub>
<h5>Example: Get all data from all pages</h5>
<a>In addition to creating a SmartTable, define a Navigator which defines the locator for the table navigation bar and the locators for it's buttons (i.e next, previous, last, first). Upon calling the extract data function, this will automatically use the navigator to extract data across all pages of the table</a><br>
<sub>
<a>var table = SmartElement.find(page, "id=example");</a><br>
<a>return SmartTable.find(table, "thead >> th", "tbody >> tr", "td")<a><br>
.with(new SmartTable.Navigator(SmartElement.find(page, "id=example_paginate"))<br>
                .withPreviousPage("a:has-text(\"Previous\")")<br>
                .withNextPage("a:has-text(\"Next\")"))<br>
.extractData();
</sub>
