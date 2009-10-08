<?xml version="1.0" encoding="utf-8" ?>
<%@ page language="java" contentType="text/xml" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<rss version="2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
   <channel>
      <title><c:out value="${rssFeed.title}"/></title>
      <link><c:out value="${rssFeed.baseUrl}"/></link>
      <description><fmt:message key="dashboard.template.title"/> <c:out value="${rssFeed.title}"/></description>
      <language>en-us</language>
      <pubDate><c:out value="${rssFeed.pubDate}"/></pubDate>

      <lastBuildDate><c:out value="${rssFeed.buildDate}"/></lastBuildDate>
      <docs><fmt:message key="common.url.help"/></docs>
      <generator><fmt:message key="about.Title"/></generator>
      <managingEditor><fmt:message key="about.MoreInfo.LinkSales"/></managingEditor>
      <webMaster><fmt:message key="about.MoreInfo.LinkSupport"/></webMaster>
    <c:forEach items="${rssFeed.items}" var="item">
      <item>
         <title><c:out value="${item.title}"/></title>
         <link><![CDATA[<c:out value="${item.link}" escapeXml="false"/>]]></link>
         <description><![CDATA[<c:out value="${item.description}" escapeXml="false"/>]]></description>
         <pubDate><c:out value="${item.pubDate}"/></pubDate>
         <guid><![CDATA[<c:out value="${item.guid}"/>]]></guid>
      </item>
    </c:forEach>
   </channel>
</rss>
