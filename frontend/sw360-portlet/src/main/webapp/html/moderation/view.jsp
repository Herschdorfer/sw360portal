<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>

<portlet:resourceURL var="deleteModerationRequestAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.DELETE_MODERATION_REQUEST%>'/>
</portlet:resourceURL>

<jsp:useBean id="moderationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="closedModerationRequests" type="java.util.List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>"
             scope="request"/>
<jsp:useBean id="isUserAtLeastClearingAdmin" class="java.lang.String" scope="request" />

<div id="header"></div>
<p class="pageHeader">
    <span class="pageHeaderBigSpan">Moderations</span> <span class="pageHeaderSmallSpan" title="Count of open/closed moderation requests">(${moderationRequests.size()}/${closedModerationRequests.size()})</span>
</p>


<div id="content">
    <div class="container-fluid">
        <div id="myTab" class="row-fluid">
            <ul class="nav nav-tabs span2">
                <div id="searchInput">
                    <table>
                        <thead>
                        <tr>
                            <th class="infoheading">
                                Quick Filter
                            </th>
                        </tr>
                        </thead>
                        <tbody style="background-color: #f8f7f7; border: none;">
                        <tr>
                            <td>
                                <input type="text" class="searchbar"
                                       id="keywordsearchinput" value="" onkeyup="useSearch('keywordsearchinput')">
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
                <br/>
                <li class="active"><a href="#tab-Open">Open</a></li>
                <li><a href="#tab-Closed">Closed</a></li>
            </ul>
            <div class="tab-content span10">
                <div id="tab-Open" class="tab-pane">
                    <table id="moderationsTable" cellpadding="0" cellspacing="0" border="0" class="display">
                        <tfoot>
                        <tr>
                            <th colspan="5"></th>
                        </tr>
                        </tfoot>
                    </table>
                </div>
                <div id="tab-Closed">
                    <table id="closedModerationsTable" cellpadding="0" cellspacing="0" border="0" class="display">
                        <tfoot>
                        <tr>
                            <th colspan="5"></th>
                        </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/jquery.validate.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-validation/1.15.1/additional-methods.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/webjars/datatables/1.10.7/js/jquery.dataTables.min.js"></script>

<script>
    var tabView;
    var Y = YUI().use(
        'aui-tabview',
        function (Y) {
            tabView = new Y.TabView(
                {
                    srcNode: '#myTab',
                    stacked: true,
                    type: 'tab'
                }
            ).render();
        }
    );

    var moderationsDataTable;
    var closedModerationsDataTable;

    //This can not be document ready function as liferay definitions need to be loaded first
    $(window).load(function () {
        moderationsDataTable = createModerationsTable("#moderationsTable", prepareModerationsData());
        closedModerationsDataTable = createModerationsTable("#closedModerationsTable", prepareClosedModerationsData());
    });

    function useSearch(searchFieldId) {
        var searchText = $('#'+searchFieldId).val();
        moderationsDataTable.search(searchText).draw();
        closedModerationsDataTable.search(searchText).draw();
    }

    function prepareModerationsData() {
        var result = [];
        <core_rt:forEach items="${moderationRequests}" var="moderation">
            result.push({
                "DT_RowId": "${moderation.id}",
                "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                "1": '<sw360:DisplayUserEmail email="${moderation.requestingUser}" bare="true"/>',
                "2": '<sw360:DisplayUserEmailCollection value="${moderation.moderators}" bare="true"/>',
                "3": '<sw360:DisplayEnum value="${moderation.moderationState}"/>',
                "4": 'TODO'
            });
        </core_rt:forEach>
        return result;
    }

    function prepareClosedModerationsData() {
        var result = [];
        <core_rt:forEach items="${closedModerationRequests}" var="moderation">
            result.push({
                "DT_RowId": "${moderation.id}",
                "0": "<sw360:DisplayModerationRequestLink moderationRequest="${moderation}"/>",
                "1": '<sw360:DisplayUserEmail email="${moderation.requestingUser}" bare="true"/>',
                "2": '<sw360:DisplayUserEmailCollection value="${moderation.moderators}" bare="true"/>',
                "3": '<sw360:DisplayEnum value="${moderation.moderationState}"/>',
                <core_rt:if test="${isUserAtLeastClearingAdmin == 'Yes'}">
                "4": "<img src='<%=request.getContextPath()%>/images/Trash.png' onclick=\"deleteModerationRequest('${moderation.id}','<b>${moderation.documentName}</b>')\"  alt='Delete' title='Delete'>"
                </core_rt:if>
                <core_rt:if test="${isUserAtLeastClearingAdmin != 'Yes'}">
                "4": "READY"
                </core_rt:if>

            });
        </core_rt:forEach>
        return result;

    }

    function createModerationsTable(tableId, tableData) {
        var tbl = $(tableId).DataTable({
            pagingType: "simple_numbers",
            dom: "lrtip",
            data: tableData,
            columns: [
                {"title": "Document Name"},
                {"title": "Requesting User"},
                {"title": "Moderators"},
                {"title": "State"},
                {"title": "Actions"}
            ]
        });

        return tbl;
    }

    function deleteModerationRequest(id, docName) {

        function deleteModerationRequestInternal() {
            jQuery.ajax({
                type: 'POST',
                url: '<%=deleteModerationRequestAjaxURL%>',
                cache: false,
                data: {
                    <portlet:namespace/>moderationId: id
                },
                success: function (data) {
                    if (data.result == 'SUCCESS') {
                        closedModerationsDataTable.row('#' + id).remove().draw(false);
                    }
                    else {
                        $.alert("I could not delete the moderation request!");
                    }
                },
                error: function () {
                    $.alert("I could not delete the moderation request!");
                }
            });
        }

        deleteConfirmed("Do you really want to delete the moderation request for " + docName + " ?", deleteModerationRequestInternal);
    }
</script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/jquery-ui/1.12.1/jquery-ui.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/webjars/github-com-craftpip-jquery-confirm/3.0.1/jquery-confirm.min.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/sw360.css">
