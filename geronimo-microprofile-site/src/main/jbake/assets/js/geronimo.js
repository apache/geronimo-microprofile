$(function() {
  // ensure dependencies blocks are multi-dependencies friendly
  function isSimpleTag(content, tagName) {
    var endTag = '</' + tagName + '>';
    return content.indexOf('<' + tagName + '>') === 0 && content.indexOf(endTag) === content.length - endTag.length;
  }
  function toADocCode(content, highlighting) {
    return '<div class="listingblock dependency-sample-code"><div class="content"><pre class="prettyprint">' +
      '<code class="language-' + highlighting + ' hljs" data-lang="' + highlighting + '">' + content + '</code></pre></div></div>';
  }
  function extractFromXml(content, tagName) {
    var startTag = '<' + tagName + '>';
    var start = content.indexOf(startTag);
    var end = content.indexOf('</' + tagName + '>', start + 1);
    if (start < 0 || end <= start) {
      return false;
    }
    return content.substring(start + startTag.length, end);
  }
  function parseGav(dep) {
    var artifactId = extractFromXml(dep, 'artifactId');
    var groupId = extractFromXml(dep, 'groupId');
    var version = extractFromXml(dep, 'version');
    var scope = extractFromXml(dep, 'scope');
    return {
      success: artifactId && groupId && version,
      groupId: groupId,
      artifactId: artifactId,
      version: version,
      scope: scope || 'compile'
    };
  }
  var codeCounter = 0;
  $('code.language-xml').each(function () {
    var code = $(this);
    var content = code.text().trim();
    if (isSimpleTag(content, 'dependency')) {
      var highlightjsParent = code.parent();
      if (!highlightjsParent || !highlightjsParent.hasClass('prettyprint')) {
        return;
      }
      var contentParent = highlightjsParent.parent();
      if (!contentParent || !contentParent.hasClass('content')) {
        return;
      }
      var listingblockParent = contentParent.parent();
      if (!listingblockParent || !listingblockParent.hasClass('listingblock')) {
        return;
      }
      var gav = parseGav(content);
      if (!gav.success) {
        return;
      }
      listingblockParent.html('<ul class="nav nav-tabs">'+
        '<li class="show active"><a data-toggle="tab" href="#__generated_code_tab_maven_' + codeCounter + '">Maven</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_gradle_' + codeCounter + '">Gradle</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_sbt_' + codeCounter + '">SBT</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_ivy_' + codeCounter + '">Ivy</a></li>' +
        '<li><a data-toggle="tab" href="#__generated_code_tab_grapes_' + codeCounter + '">Grapes</a></li>' +
      '</ul>' +
      '<div class="tab-content dependency-sample">' +
        '<div id="__generated_code_tab_maven_' + codeCounter + '" class="tab-pane fade in show active">' +
          toADocCode($('<div/>').text(content).html(), 'xml') +
        '</div>' +
        '<div id="__generated_code_tab_gradle_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode(gav.scope + ' ' + '"' + gav.groupId + ':' + gav.artifactId + ':' + gav.version + '"', 'java') +
        '</div>' +
        '<div id="__generated_code_tab_sbt_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode('libraryDependencies += "' + gav.groupId + '" % "' + gav.artifactId + '" % "' + gav.version + '" % ' + gav.scope, 'text') +
        '</div>' +
        '<div id="__generated_code_tab_ivy_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode($('<div/>').text('<dependency org="' + gav.groupId + '" name="' + gav.artifactId + '" rev="' + gav.version + '" />').html(), 'xml') +
        '</div>' +
        '<div id="__generated_code_tab_grapes_' + codeCounter + '" class="tab-pane fade">' +
          toADocCode('@Grapes(\n    @Grab(group=\'' + gav.groupId + '\', module=\'' + gav.artifactId + '\', version=\'' + gav.version + '\')\n)', 'java') +
        '</div>' +
      '</div>');
      codeCounter++;
    }
  });
  prettyPrint();
});
