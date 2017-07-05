/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.google.common.collect.Sets;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.cvesearch.UpdateType;
import org.eclipse.sw360.datahandler.thrift.cvesearch.VulnerabilityUpdateStatus;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.portlet.PortletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static java.lang.Integer.parseInt;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_COMPONENTS_VIEW_SIZE;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_PROJECT_GROUP_FILTER;

/**
 * Portlet helpers
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class PortletUtils {

    private static final Logger log = Logger.getLogger(PortletUtils.class);

    private PortletUtils() {
        // Utility class with only static functions
    }

    public static ComponentType getComponentTypefromString(String enumNumber) {
        return ComponentType.findByValue(parseInt(enumNumber));
    }

    public static ClearingState getClearingStatefromString(String enumNumber) {
        return ClearingState.findByValue(parseInt(enumNumber));
    }

    public static RepositoryType getRepositoryTypefromString(String enumNumber) {
        return RepositoryType.findByValue(parseInt(enumNumber));
    }

    public static MainlineState getMainlineStatefromString(String enumNumber) {
        return MainlineState.findByValue(parseInt(enumNumber));
    }

    public static ModerationState getModerationStatusfromString(String enumNumber) {
        return ModerationState.findByValue(parseInt(enumNumber));
    }

    public static AttachmentType getAttachmentTypefromString(String enumNumber) {
        return AttachmentType.findByValue(parseInt(enumNumber));
    }
    public static CheckStatus getCheckStatusfromString(String enumNumber) {
        return CheckStatus.findByValue(parseInt(enumNumber));
    }

    public static ProjectState getProjectStateFromString(String enumNumber) {
        return ProjectState.findByValue(parseInt(enumNumber));
    }

    public static ProjectClearingState getProjectClearingStateFromString(String enumNumber) {
        return ProjectClearingState.findByValue(parseInt(enumNumber));
    }

    public static ProjectType getProjectTypeFromString(String enumNumber) {
        return ProjectType.findByValue(parseInt(enumNumber));
    }
    public static Visibility getVisibilityFromString(String enumNumber) {
        return  Visibility.findByValue(parseInt(enumNumber));
    }

    public static UserGroup getUserGroupFromString(String enumNumber) {
        return  UserGroup.findByValue(parseInt(enumNumber));
    }
    public static ECCStatus getEccStatusFromString(String enumNumber) {
        return  ECCStatus.findByValue(parseInt(enumNumber));
    }

    public static <U extends TFieldIdEnum, T extends TBase<T, U>> void setFieldValue(PortletRequest request, T instance, U field, FieldMetaData fieldMetaData, String prefix) {

        String value = request.getParameter(prefix + field.toString());
        if (value != null) {
            switch (fieldMetaData.valueMetaData.type) {

                case org.apache.thrift.protocol.TType.SET:
                    instance.setFieldValue(field, CommonUtils.splitToSet(value));
                    break;
                case org.apache.thrift.protocol.TType.ENUM:
                    if (!"".equals(value))
                        instance.setFieldValue(field, enumFromString(value, field));
                    break;
                case org.apache.thrift.protocol.TType.I32:
                    if (!"".equals(value))
                        instance.setFieldValue(field, Integer.parseInt(value));
                    break;
                case org.apache.thrift.protocol.TType.BOOL:
                    if (!"".equals(value))
                        instance.setFieldValue(field, true);
                    break;
                default:
                    instance.setFieldValue(field, value);
            }
        }
    }

    public static <U extends TFieldIdEnum> Object enumFromString(String value, U field) {
        if (field == Release._Fields.CLEARING_STATE)
            return getClearingStatefromString(value);
        else if (field == Component._Fields.COMPONENT_TYPE)
            return getComponentTypefromString(value);
        else if (field == Repository._Fields.REPOSITORYTYPE)
            return getRepositoryTypefromString(value);
        else if (field == Release._Fields.MAINLINE_STATE)
            return getMainlineStatefromString(value);
        else if (field == Project._Fields.STATE)
            return getProjectStateFromString(value);
        else if (field == Project._Fields.CLEARING_STATE)
            return getProjectClearingStateFromString(value);
        else if (field == Project._Fields.PROJECT_TYPE)
            return getProjectTypeFromString(value);
        else if (field == Project._Fields.VISBILITY)
            return getVisibilityFromString(value);
        else if (field == User._Fields.USER_GROUP)
            return getUserGroupFromString(value);
        else if (field == EccInformation._Fields.ECC_STATUS)
            return getEccStatusFromString(value);
        else {
            log.error("Missing case in enumFromString, unknown field was " + field.toString());
            return null;
        }
    }


    public static Set<Attachment> updateAttachmentsFromRequest(PortletRequest request, Set<Attachment> documentAttachments) {
        if (documentAttachments == null) {
            documentAttachments = new HashSet<>();
        }

        User user = UserCacheHolder.getUserFromRequest(request);
        String[] fileNames = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.FILENAME);
        String[] ids = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_CONTENT_ID.toString());
        String[] createdComments = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CREATED_COMMENT.toString());
        String[] checkedComments = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECKED_COMMENT.toString());
        String[] checkStatuses = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECK_STATUS.toString());
        String[] atypes = request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_TYPE.toString());

        if(ids == null || ids.length == 0) {
            return new HashSet<>();
        } else if (CommonUtils.oneIsNull(atypes, createdComments, checkedComments, fileNames)) {
            log.error("We have a problem with null arrays");
        } else if (
                atypes.length != createdComments.length ||
                atypes.length != ids.length ||
                atypes.length != checkedComments.length ||
                atypes.length != fileNames.length) {
            log.error("We have a problem length != other.length ");
        } else {
            Map<String, Attachment> documentAttachmentMap = documentAttachments.stream().collect(Collectors.toMap(Attachment::getAttachmentContentId, Function.identity()));
            Map<String, Attachment> documentAttachmentsInRequestMap = new HashMap<>();
            int length = atypes.length;

            for (int i = 0; i < length; ++i) {

                String id = ids[i];
                Attachment attachment;
                if (documentAttachmentMap.containsKey(id)) {
                    attachment = documentAttachmentMap.get(id);
                    documentAttachmentsInRequestMap.put(id,attachment);
                } else {
                    //the sha1 checksum is not computed here, but in the backend, when updating the component in the database
                    attachment = CommonUtils.getNewAttachment(user, id, fileNames[i]);
                    documentAttachments.add(attachment);
                }
                attachment.setCreatedComment(createdComments[i]);
                attachment.setAttachmentType(getAttachmentTypefromString(atypes[i]));
                if(!checkedComments[i].equals(attachment.checkedComment) || attachment.checkStatus != getCheckStatusfromString(checkStatuses[i])){
                    attachment.setCheckedOn(SW360Utils.getCreatedOn());
                    attachment.setCheckedBy(UserCacheHolder.getUserFromRequest(request).getEmail());
                    attachment.setCheckedTeam(UserCacheHolder.getUserFromRequest(request).getDepartment());
                    attachment.setCheckStatus(getCheckStatusfromString(checkStatuses[i]));
                    attachment.setCheckedComment(checkedComments[i]);
                }
            }
            Set<String> removedAttachmentIds = Sets.difference(documentAttachmentMap.keySet(),documentAttachmentsInRequestMap.keySet());
            documentAttachments.removeIf(attachment -> removedAttachmentIds.contains(attachment.getAttachmentContentId()));
        }
        return documentAttachments;
    }


    public static Release cloneRelease(String emailFromRequest, Release release) {

        Release newRelease = release.deepCopy();

        //new DB object
        newRelease.unsetId();
        newRelease.unsetRevision();

        //new Owner
        newRelease.setCreatedBy(emailFromRequest);
        newRelease.setCreatedOn(SW360Utils.getCreatedOn());

        //release specifics
        newRelease.unsetCpeid();
        newRelease.unsetAttachments();
        newRelease.unsetClearingInformation();

        return newRelease;
    }

    public static Project cloneProject(String emailFromRequest, String department, Project project) {

        Project newProject = project.deepCopy();

        //new DB object
        newProject.unsetId();
        newProject.unsetRevision();

        //new Owner
        newProject.setCreatedBy(emailFromRequest);
        newProject.setCreatedOn(SW360Utils.getCreatedOn());
        newProject.setBusinessUnit(department);

        //project specifics
        newProject.unsetAttachments();
        newProject.setClearingState(ProjectClearingState.OPEN);

        return newProject;
    }

    public static JSONObject importStatusToJSON(VulnerabilityUpdateStatus updateStatus) {
        JSONObject responseData = JSONFactoryUtil.createJSONObject();
        if(! updateStatus.isSetStatusToVulnerabilityIds() || ! updateStatus.isSetRequestStatus()){
            responseData.put(PortalConstants.REQUEST_STATUS, PortalConstants.RESPONSE__IMPORT_GENERAL_FAILURE);
            return responseData;
        }
        if (updateStatus.getRequestStatus().equals(RequestStatus.FAILURE)
                && nullToEmptyList(updateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED)).size() == 0) {
            responseData.put(PortalConstants.REQUEST_STATUS, PortalConstants.RESPONSE__IMPORT_GENERAL_FAILURE);
            return responseData;
        }
        responseData.put(PortalConstants.REQUEST_STATUS, updateStatus.getRequestStatus().toString());
        JSONArray jsonFailedIds = JSONFactoryUtil.createJSONArray();
        JSONArray jsonNewIds = JSONFactoryUtil.createJSONArray();
        JSONArray jsonUpdatedIds = JSONFactoryUtil.createJSONArray();

        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.FAILED).forEach(id -> jsonFailedIds.put(id));
        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.NEW).forEach(id -> jsonNewIds.put(id));
        updateStatus.getStatusToVulnerabilityIds().get(UpdateType.UPDATED).forEach(id -> jsonUpdatedIds.put(id));

        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__FAILED_IDS, jsonFailedIds);
        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__NEW_IDS, jsonNewIds);
        responseData.put(PortalConstants.UPDATE_VULNERABILITIES__UPDATED_IDS, jsonUpdatedIds);
        return responseData;
    }

    private static Map<String, Integer> addToMatchedByHistogram(Map<String, Integer> matchedByHistogram, String matchedBy){
        if (matchedByHistogram.containsKey(matchedBy)){
            matchedByHistogram.put(matchedBy, matchedByHistogram.get(matchedBy) + 1);
        }else{
            matchedByHistogram.put(matchedBy, 1);
        }
        return matchedByHistogram;
    }

    public static Map<String, Integer> addToMatchedByHistogram(Map<String,Integer> matchedByHistogram, VulnerabilityDTO vul){
        final String isMatchedByUnknown = "UNKNOWN";

        if (vul.isSetMatchedBy()) {
            return addToMatchedByHistogram(matchedByHistogram, vul.getMatchedBy());
        } else {
            return addToMatchedByHistogram(matchedByHistogram, isMatchedByUnknown);
        }
    }

    public static VerificationState getVerificationState(VulnerabilityDTO vul){
        if(vul.isSetReleaseVulnerabilityRelation()){
            if(vul.getReleaseVulnerabilityRelation().isSetVerificationStateInfo()){
                List<VerificationStateInfo> history = vul.getReleaseVulnerabilityRelation().getVerificationStateInfo();
                if(history.size()>0) {
                    return history.get(history.size() - 1).getVerificationState();
                }
            }
        }
        return VerificationState.NOT_CHECKED;
    }

    public static Map<String,Set<String>> getCustomMapFromRequest(PortletRequest request) {
        Map<String, Set<String>> customMap = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        List<String> keyAndValueParameterIds = Collections.list(parameterNames).stream()
                .filter(p -> p.startsWith(PortalConstants.CUSTOM_MAP_KEY))
                .map(s -> s.replace(PortalConstants.CUSTOM_MAP_KEY, ""))
                .collect(Collectors.toList());
        for(String parameterId : keyAndValueParameterIds) {
            String key = request.getParameter(PortalConstants.CUSTOM_MAP_KEY +parameterId);
            if(isNullEmptyOrWhitespace(key)){
                log.error("Empty map key found");
            } else {
                String value = request.getParameter(PortalConstants.CUSTOM_MAP_VALUE + parameterId);
                if(!customMap.containsKey(key)){
                    customMap.put(key, new HashSet<>());
                }
                customMap.get(key).add(value);
            }
        }
        return customMap;
    }

    private static com.liferay.portal.model.User getLiferayUser(PortletRequest request, User user) throws PortalException, SystemException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        long companyId = themeDisplay.getCompanyId();
        return UserLocalServiceUtil.getUserByEmailAddress(companyId, user.email);
    }

    static void ensureUserCustomFieldExists(com.liferay.portal.model.User liferayUser, PortletRequest request, String customFieldName, int customFieldType) throws PortalException, SystemException {
        ExpandoBridge exp = liferayUser.getExpandoBridge();
        if (!exp.hasAttribute(customFieldName)){
            exp.addAttribute(customFieldName, customFieldType, false);
            long companyId = liferayUser.getCompanyId();

            ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(companyId, exp.getClassName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, customFieldName);

            String[] roleNames = new String[]{RoleConstants.USER, RoleConstants.POWER_USER};
            for (String roleName: roleNames) {
                Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
                if (role != null && column != null) {
                    ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId,
                            ExpandoColumn.class.getName(),
                            ResourceConstants.SCOPE_INDIVIDUAL,
                            String.valueOf(column.getColumnId()),
                            role.getRoleId(),
                            new String[]{ActionKeys.VIEW, ActionKeys.UPDATE});
                }
            }
        }
    }

    public static ExpandoBridge getUserExpandoBridge(PortletRequest request, User user) throws PortalException, SystemException {
        com.liferay.portal.model.User liferayUser = getLiferayUser(request, user);
        ensureUserCustomFieldExists(liferayUser, request, CUSTOM_FIELD_PROJECT_GROUP_FILTER, ExpandoColumnConstants.STRING);
        ensureUserCustomFieldExists(liferayUser, request, CUSTOM_FIELD_COMPONENTS_VIEW_SIZE, ExpandoColumnConstants.INTEGER);
        return liferayUser.getExpandoBridge();
    }
}
