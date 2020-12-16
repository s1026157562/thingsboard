/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class WidgetTypeController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetTypeById(@PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            return checkWidgetTypeId(widgetTypeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType", method = RequestMethod.POST)
    @ResponseBody
    public WidgetType saveWidgetType(@RequestBody WidgetType widgetType) throws ThingsboardException {
        try {
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                widgetType.setTenantId(TenantId.SYS_TENANT_ID);
            } else {
                widgetType.setTenantId(getCurrentUser().getTenantId());
            }

            checkEntity(widgetType.getId(), widgetType, Resource.WIDGET_TYPE);
            WidgetType savedWidgetType = widgetTypeService.saveWidgetType(widgetType);

            sendNotificationMsgToEdgeService(getTenantId(), savedWidgetType.getId(), EntityType.WIDGET_TYPE,
                    widgetType.getId() == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED);

            return checkNotNull(savedWidgetType);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetType/{widgetTypeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteWidgetType(@PathVariable("widgetTypeId") String strWidgetTypeId) throws ThingsboardException {
        checkParameter("widgetTypeId", strWidgetTypeId);
        try {
            WidgetTypeId widgetTypeId = new WidgetTypeId(toUUID(strWidgetTypeId));
            checkWidgetTypeId(widgetTypeId, Operation.DELETE);
            widgetTypeService.deleteWidgetType(getCurrentUser().getTenantId(), widgetTypeId);

            sendNotificationMsgToEdgeService(getTenantId(), widgetTypeId, EntityType.WIDGET_TYPE, EdgeEventActionType.DELETED);

        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/widgetTypes", params = {"isSystem", "bundleAlias"}, method = RequestMethod.GET)
    @ResponseBody
    public List<WidgetType> getBundleWidgetTypes(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = TenantId.SYS_TENANT_ID;
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            return checkNotNull(widgetTypeService.findWidgetTypesByTenantIdAndBundleAlias(tenantId, bundleAlias));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/widgetType", params = {"isSystem", "bundleAlias", "alias"}, method = RequestMethod.GET)
    @ResponseBody
    public WidgetType getWidgetType(
            @RequestParam boolean isSystem,
            @RequestParam String bundleAlias,
            @RequestParam String alias) throws ThingsboardException {
        try {
            TenantId tenantId;
            if (isSystem) {
                tenantId = new TenantId(ModelConstants.NULL_UUID);
            } else {
                tenantId = getCurrentUser().getTenantId();
            }
            WidgetType widgetType = widgetTypeService.findWidgetTypeByTenantIdBundleAliasAndAlias(tenantId, bundleAlias, alias);
            checkNotNull(widgetType);
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, Operation.READ, widgetType.getId(), widgetType);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
