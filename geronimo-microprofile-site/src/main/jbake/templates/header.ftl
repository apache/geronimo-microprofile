<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <title><#if (content.title)??><#escape x as x?xml>${content.title}</#escape><#else>JBake</#if></title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Apache Geronimo Microprofile Site">
    <meta name="author" content="Apache">
    <meta name="keywords" content="Apache Geronimo Microprofile">
    <meta name="generator" content="JBake">

    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/font-awesome.min.css" rel="stylesheet">
    <link href="css/prettify.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<#if (content.rootpath)??>${content.rootpath}</#if>css/geronimo.css"></link>
  </head>
  <body cz-shortcut-listen="true">
