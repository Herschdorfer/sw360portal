/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.io.IOException;
import java.util.List;

import static org.apache.log4j.Logger.getLogger;

/**
 * Small homepage portlet
 *
 * @author cedric.bodet@tngtech.com
 * @author gerrit.grenzebach@tngtech.com
 */
public class RecentComponentPortlet extends Sw360Portlet {

    private static final Logger log = getLogger(RecentComponentPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        List<Component> components=null;
        User user = UserCacheHolder.getUserFromRequest(request);

        try {
            components = thriftClients.makeComponentClient().getRecentComponentsSummary(5, user);
        } catch (TException e) {
            log.error("Could not fetch recent components from backend", e);
        }

        request.setAttribute("components",  CommonUtils.nullToEmptyList(components));

        super.doView(request, response);
    }

}
