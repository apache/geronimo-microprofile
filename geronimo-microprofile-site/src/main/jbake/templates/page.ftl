<#include "header.ftl">

	<#include "menu.ftl">

	<div class="page-header">
		<h4><#escape x as x?xml>${content.title}</#escape></h4>
	</div>

	<p><em>${content.date}</em></p>

	<p>${content.body}</p>

	<hr />

<#include "footer.ftl">