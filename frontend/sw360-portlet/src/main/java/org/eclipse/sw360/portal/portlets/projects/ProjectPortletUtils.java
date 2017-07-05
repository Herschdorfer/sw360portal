/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projects;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
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
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityRatingForProject;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import java.util.*;

import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_PROJECT_GROUP_FILTER;
import static org.eclipse.sw360.portal.common.PortletUtils.getUserExpandoBridge;

/**
 * Component portlet implementation
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ProjectPortletUtils {

    private static final Logger log = Logger.getLogger(ProjectPortletUtils.class);

    private ProjectPortletUtils() {
        // Utility class with only static functions
    }

    public static void updateProjectFromRequest(PortletRequest request, Project project) {
        for (Project._Fields field : Project._Fields.values()) {
            switch (field) {
                case LINKED_PROJECTS:
                    if (!project.isSetLinkedProjects()) {
                        project.setLinkedProjects(new HashMap<>());
                    }
                    updateLinkedProjectsFromRequest(request, project.linkedProjects);
                    break;
                case RELEASE_ID_TO_USAGE:
                    if (!project.isSetReleaseIdToUsage()) {
                        project.setReleaseIdToUsage(new HashMap<>());
                    }
                    updateLinkedReleasesFromRequest(request, project.releaseIdToUsage);
                    break;

                case ATTACHMENTS:
                    project.setAttachments(PortletUtils.updateAttachmentsFromRequest(request, project.getAttachments()));
                    break;

                case ROLES:
                    project.setRoles(PortletUtils.getCustomMapFromRequest(request));
                default:
                    setFieldValue(request, project, field);
            }
        }
    }

    private static void updateLinkedReleasesFromRequest(PortletRequest request, Map<String, ProjectReleaseRelationship> releaseUsage) {
        releaseUsage.clear();
        String[] ids = request.getParameterValues(Project._Fields.RELEASE_ID_TO_USAGE.toString() + ReleaseLink._Fields.ID.toString());
        String[] relations = request.getParameterValues(Project._Fields.RELEASE_ID_TO_USAGE.toString() + ProjectReleaseRelationship._Fields.RELEASE_RELATION.toString());
        String[] mainlStates = request.getParameterValues(Project._Fields.RELEASE_ID_TO_USAGE.toString() + ProjectReleaseRelationship._Fields.MAINLINE_STATE.toString());
        if (ids != null && relations != null && mainlStates != null && ids.length == relations.length && ids.length == mainlStates.length) {
            for (int k = 0; k < ids.length; ++k) {
                ReleaseRelationship relation = ReleaseRelationship.findByValue(Integer.parseInt(relations[k]));
                MainlineState mainlState = MainlineState.findByValue(Integer.parseInt(mainlStates[k]));
                releaseUsage.put(ids[k], new ProjectReleaseRelationship(relation, mainlState));
            }
        }
    }

    private static void updateLinkedProjectsFromRequest(PortletRequest request, Map<String, ProjectRelationship> linkedProjects) {
        linkedProjects.clear();
        String[] ids = request.getParameterValues(Project._Fields.LINKED_PROJECTS.toString() + ProjectLink._Fields.ID.toString());
        String[] relations = request.getParameterValues(Project._Fields.LINKED_PROJECTS.toString() + ProjectLink._Fields.RELATION.toString());
        if (ids != null && relations != null && ids.length == relations.length)
            for (int k = 0; k < ids.length; ++k) {
                linkedProjects.put(ids[k], ProjectRelationship.findByValue(Integer.parseInt(relations[k])));
            }
    }

    private static void setFieldValue(PortletRequest request, Project project, Project._Fields field) {
        PortletUtils.setFieldValue(request, project, field, Project.metaDataMap.get(field), "");
    }

    public static ProjectVulnerabilityRating updateProjectVulnerabilityRatingFromRequest(Optional<ProjectVulnerabilityRating> projectVulnerabilityRatings, ResourceRequest request){
        String projectId = request.getParameter(PortalConstants.PROJECT_ID);
        ProjectVulnerabilityRating projectVulnerabilityRating = projectVulnerabilityRatings.orElse(
                new ProjectVulnerabilityRating()
                        .setProjectId(projectId)
                        .setVulnerabilityIdToReleaseIdToStatus(new HashMap<>()));

        String vulnerabilityId = request.getParameter(PortalConstants.VULNERABILITY_ID);
        String releaseId = request.getParameter(PortalConstants.RELEASE_ID);
        if(! projectVulnerabilityRating.isSetVulnerabilityIdToReleaseIdToStatus()){
            projectVulnerabilityRating.setVulnerabilityIdToReleaseIdToStatus(new HashMap<>());
        }

        Map<String, Map <String, List<VulnerabilityCheckStatus>>>  vulnerabilityIdToReleaseIdToStatus = projectVulnerabilityRating.getVulnerabilityIdToReleaseIdToStatus();
        if(! vulnerabilityIdToReleaseIdToStatus.containsKey(vulnerabilityId)){
            vulnerabilityIdToReleaseIdToStatus.put(vulnerabilityId, new HashMap<>());
        }
        if(! vulnerabilityIdToReleaseIdToStatus.get(vulnerabilityId).containsKey(releaseId)){
            vulnerabilityIdToReleaseIdToStatus.get(vulnerabilityId).put(releaseId, new ArrayList<>());
        }

        List<VulnerabilityCheckStatus> vulnerabilityCheckStatusHistory = vulnerabilityIdToReleaseIdToStatus.get(vulnerabilityId).get(releaseId);
        VulnerabilityCheckStatus vulnerabilityCheckStatus = newVulnerabilityCheckStatusFromRequest(request);
        vulnerabilityCheckStatusHistory.add(vulnerabilityCheckStatus);

        return projectVulnerabilityRating;
    }

    private static VulnerabilityCheckStatus newVulnerabilityCheckStatusFromRequest(ResourceRequest request){
        VulnerabilityRatingForProject vulnerabilityRatingForProject = VulnerabilityRatingForProject.findByValue(
                        Integer.parseInt(request.getParameter(PortalConstants.VULNERABILITY_RATING_VALUE)));

        VulnerabilityCheckStatus vulnerabilityCheckStatus = new VulnerabilityCheckStatus()
                .setCheckedBy(UserCacheHolder.getUserFromRequest(request).getEmail())
                .setCheckedOn(SW360Utils.getCreatedOn())
                .setComment(request.getParameter(PortalConstants.VULNERABILITY_RATING_COMMENT))
                .setVulnerabilityRating(vulnerabilityRatingForProject);
        return vulnerabilityCheckStatus;
    }

    public static void saveStickyProjectGroup(PortletRequest request, User user, String groupFilterValue) {
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            exp.setAttribute(CUSTOM_FIELD_PROJECT_GROUP_FILTER, groupFilterValue);
        } catch (PortalException | SystemException e) {
            log.warn("Could not save sticky project group to custom field", e);
        }
    }

    public static String loadStickyProjectGroup(PortletRequest request, User user){
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            return (String) exp.getAttribute(CUSTOM_FIELD_PROJECT_GROUP_FILTER);
        } catch (PortalException | SystemException e) {
            log.error("Could not load sticky project group from custom field", e);
            return null;
        }
    }

    public static Map<String, Set<String>> getSelectedReleaseAndAttachmentIdsFromRequest(ResourceRequest request) {
        Map<String, Set<String>> releaseIdToAttachmentIds = new HashMap<>();
        String[] checkboxes = request.getParameterValues(PortalConstants.LICENSE_INFO_RELEASE_TO_ATTACHMENT);
        Arrays.stream(checkboxes).forEach(s -> {
            String[] split = s.split(":");
            if (split.length==2){
                String releaseId = split[0];
                String attachmentId = split[1];
                if (!releaseIdToAttachmentIds.containsKey(releaseId)){
                    releaseIdToAttachmentIds.put(releaseId, new HashSet<>());
                }
                releaseIdToAttachmentIds.get(releaseId).add(attachmentId);
            }
        });
        return releaseIdToAttachmentIds;
    }
}
