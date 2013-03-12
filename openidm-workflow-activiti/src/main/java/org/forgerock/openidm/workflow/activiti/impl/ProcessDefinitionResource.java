/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.workflow.activiti.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.form.DefaultStartFormHandler;
import org.activiti.engine.impl.form.FormPropertyHandler;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.workflow.activiti.impl.mixin.ProcessDefinitionMixIn;

/**
 * Resource implementation of ProcessDefinition related Activiti operations
 * @author orsolyamebold
 */
public class ProcessDefinitionResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;
    private TaskDefinitionResource taskDefinitionResource;

    static {
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(ProcessDefinitionEntity.class, ProcessDefinitionMixIn.class);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public ProcessDefinitionResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        taskDefinitionResource = new TaskDefinitionResource(processEngine);
    }
    
    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        taskDefinitionResource = new TaskDefinitionResource(processEngine);
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ActionCollection on ProcessDefinitionResource not supported."));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("ActionInstance on ProcessDefinitionResource not supported."));
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("CreateInstance on ProcessDefinitionResource not supported."));
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("DeleteInstance on ProcessDefinitionResource not supported."));
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("PatchInstance on ProcessDefinitionResource not supported."));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            if ("taskdefinition".equals(((RouterContext) context).getUriTemplateVariables().get("subobj"))) {
                taskDefinitionResource.queryCollection(context, request, handler);
            } else if (ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                List<ProcessDefinition> definitionList = processEngine.getRepositoryService().createProcessDefinitionQuery().list();
                if (definitionList != null && definitionList.size() > 0) {
                    for (ProcessDefinition processDefinition : definitionList) {
                        Map value = mapper.convertValue(processDefinition, HashMap.class);
                        Resource r = new Resource(processDefinition.getId(), null, new JsonValue(value));
                        handler.handleResource(r);
                    }
                }
                handler.handleResult(new QueryResult());
            } else if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                ProcessDefinitionQuery query = processEngine.getRepositoryService().createProcessDefinitionQuery();
                setProcessDefinitionParams(query, request);
                List<ProcessDefinition> list = query.list();
                for (ProcessDefinition processDefinition : list) {
                    Map value = mapper.convertValue(processDefinition, HashMap.class);
                    Resource r = new Resource(processDefinition.getId(), null, new JsonValue(value));
                    handler.handleResource(r);
                }
                handler.handleResult(new QueryResult());
            } else {
                handler.handleError(new BadRequestException("Unknown query-id"));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            if ("taskdefinition".equals(((RouterContext) context).getUriTemplateVariables().get("subobj"))) {
                taskDefinitionResource.readInstance(context, ((RouterContext) context).getUriTemplateVariables().get("subobjid"), request, handler);
            } else {
                ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl) processEngine.getRepositoryService()).getDeployedProcessDefinition(resourceId);
                Map value = mapper.convertValue(def, HashMap.class);
                Resource r = new Resource(def.getId(), null, new JsonValue(value));
                FormService formService = processEngine.getFormService();
                StartFormData startFormData = formService.getStartFormData(def.getId());
                if (def.hasStartFormKey()) {
                    r.getContent().add(ActivitiConstants.ACTIVITI_FORMRESOURCEKEY, startFormData.getFormKey());
                }
                DefaultStartFormHandler startFormHandler = (DefaultStartFormHandler) def.getStartFormHandler();
                List<Map> propertyList = new ArrayList<Map>();
                addFormHandlerData(propertyList, startFormHandler.getFormPropertyHandlers());
                r.getContent().add(ActivitiConstants.FORMPROPERTIES, propertyList);
                handler.handleResult(r);
            }
        } catch (ActivitiException e) {
            handler.handleError(new NotFoundException());
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("UpdateInstance on ProcessDefinitionResource not supported."));
    }

    /**
     * Process the query parameters of the request and set it on the
     * ProcessDefinitionQuery
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setProcessDefinitionParams(ProcessDefinitionQuery query, QueryRequest request) {
        String deploymentId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_DEPLOYMENTID);
        query = deploymentId == null ? query : query.deploymentId(deploymentId);
        String category = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CATEGORY);
        query = category == null ? query : query.processDefinitionCategory(category);
        String categoryLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_CATEGORY + ActivitiConstants.LIKE);
        query = categoryLike == null ? query : query.processDefinitionCategoryLike(categoryLike);
        String processDefinitionId = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ID);
        query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
        String processDefinitionKey = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_KEY);
        query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
        String processDefinitionKeyLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_KEY + ActivitiConstants.LIKE);
        query = processDefinitionKeyLike == null ? query : query.processDefinitionKeyLike(processDefinitionKeyLike);
        String processDefinitionName = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_NAME);
        query = processDefinitionName == null ? query : query.processDefinitionName(processDefinitionName);
        String processDefinitionNameLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_NAME + ActivitiConstants.LIKE);
        query = processDefinitionNameLike == null ? query : query.processDefinitionNameLike(processDefinitionNameLike);
        String processDefinitionResourceName = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME);
        query = processDefinitionResourceName == null ? query : query.processDefinitionResourceName(processDefinitionResourceName);
        String processDefinitionResourceNameLike = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_PROCESSDEFINITIONRESOURCENAME + ActivitiConstants.LIKE);
        query = processDefinitionResourceNameLike == null ? query : query.processDefinitionResourceNameLike(processDefinitionResourceNameLike);
        String processDefinitionVersion = ActivitiUtil.getParamFromRequest(request, ActivitiConstants.ACTIVITI_VERSION);
        query = processDefinitionVersion == null ? query : query.processDefinitionVersion(Integer.getInteger(processDefinitionVersion));
    }

    /**
     * Add FormProperty related data to the map of task properties
     *
     * @param propertyList map containing the result
     * @param handlers list of handlers to process
     */
    private void addFormHandlerData(List<Map> propertyList, List<FormPropertyHandler> handlers) {
        for (FormPropertyHandler h : handlers) {
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put(ActivitiConstants.ID, h.getId());
            entry.put(ActivitiConstants.FORMPROPERTY_DEFAULTEXPRESSION, h.getDefaultExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLEEXPRESSION, h.getVariableExpression());
            entry.put(ActivitiConstants.FORMPROPERTY_VARIABLENAME, h.getVariableName());
            entry.put(ActivitiConstants.ACTIVITI_NAME, h.getName());
            Map<String, Object> type = new HashMap<String, Object>(3);
            if (h.getType() != null) {
                type.put(ActivitiConstants.ACTIVITI_NAME, h.getType().getName());
                type.put(ActivitiConstants.ENUM_VALUES, h.getType().getInformation("values"));
                type.put(ActivitiConstants.DATE_PATTERN, h.getType().getInformation("datePattern"));
            }
            entry.put(ActivitiConstants.FORMPROPERTY_TYPE, type);
            entry.put(ActivitiConstants.FORMPROPERTY_READABLE, h.isReadable());
            entry.put(ActivitiConstants.FORMPROPERTY_REQUIRED, h.isRequired());
            entry.put(ActivitiConstants.FORMPROPERTY_WRITABLE, h.isWritable());
            propertyList.add(entry);
        }
    }
}