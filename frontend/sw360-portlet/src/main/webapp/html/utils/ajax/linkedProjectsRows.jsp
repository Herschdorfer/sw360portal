<%--
  ~ Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>

<%@include file="/html/init.jsp" %>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>" scope="request"/>
<jsp:useBean id="parent_branch_id" class="java.lang.String" scope="request"/>

<%--linked projects and their linked projects--%>
<core_rt:forEach items="${projectList}" var="projectLink" varStatus="loop">
    <%--first element in the list is the root linked project --%>
    <core_rt:if test="${loop.index==0}"><core_rt:set var="new_root_id" value="${projectLink.nodeId}"/></core_rt:if>
    <core_rt:if test="${loop.index!=0}">
        <tr data-tt-id="${projectLink.nodeId}" data-tt-branch="true"
            <%--attach children of the new root node to the existing node being expanded--%>
            <core_rt:if test="${projectLink.parentNodeId == new_root_id}">data-tt-parent-id="${parent_branch_id}"</core_rt:if>
            <core_rt:if test="${projectLink.parentNodeId != new_root_id}">data-tt-parent-id="${projectLink.parentNodeId}"</core_rt:if>
        >
            <td>
                <a href="<sw360:DisplayProjectLink projectId="${projectLink.id}" bare="true" />"><sw360:out
                        value="${projectLink.name} ${projectLink.version}" maxChar="60"/></a>
            </td>
            <td>
                <sw360:DisplayEnum value="${projectLink.relation}"/>
            </td>
            <td>
                <sw360:DisplayEnum value="${projectLink.projectType}"/>
            </td>
            <td>
                <sw360:DisplayEnum value="${projectLink.clearingState}"/>
            </td>
            <td>
            </td>
        </tr>
    </core_rt:if>
    <%--linked releases of linked projects--%>
    <core_rt:forEach items="${projectLink.linkedReleases}" var="releaseLink" varStatus="releaseloop">
        <tr data-tt-id="${releaseLink.nodeId}" data-tt-branch="false"
            <core_rt:if test="${loop.index==0 and not empty parent_branch_id}">data-tt-parent-id="${parent_branch_id}"</core_rt:if>
            <core_rt:if test="${loop.index!=0}">data-tt-parent-id="${projectLink.nodeId}"</core_rt:if>
        >
            <td>
                <a href="<sw360:DisplayReleaseLink releaseId="${releaseLink.id}" bare="true" />">
                    <sw360:out value="${releaseLink.longName}" maxChar="60"/>
                </a>
            </td>
            <td>
                <sw360:DisplayEnum value="${releaseLink.releaseRelationship}"/>
            </td>
            <td>
                <sw360:DisplayEnum value="${releaseLink.componentType}"/>
            </td>
            <td>
                <sw360:DisplayEnum value="${releaseLink.clearingState}"/>
            </td>
            <td>
                <core_rt:if test="${releaseLink.setLicenseIds}">
                    <tags:DisplayLicenseCollection licenseIds="${releaseLink.licenseIds}"
                                                   scopeGroupId="${pageContext.getAttribute('scopeGroupId')}"/>
                </core_rt:if>
            </td>
        </tr>
    </core_rt:forEach>
</core_rt:forEach>
