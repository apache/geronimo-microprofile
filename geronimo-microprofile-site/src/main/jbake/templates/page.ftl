<#include "header.ftl">

	<#include "menu.ftl">

	<div class="page-header">
		<h4><#escape x as x?xml>${content.title}</#escape></h4>
	</div>

	<p>${content.body}</p>

	<hr />

<#include "footer.ftl">