/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.form.FormData;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ActivateProcessInstanceCmd;
import org.flowable.engine.impl.cmd.AddEventConsumerCommand;
import org.flowable.engine.impl.cmd.AddEventListenerCommand;
import org.flowable.engine.impl.cmd.AddIdentityLinkForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.AddMultiInstanceExecutionCmd;
import org.flowable.engine.impl.cmd.ChangeActivityStateCmd;
import org.flowable.engine.impl.cmd.CompleteAdhocSubProcessCmd;
import org.flowable.engine.impl.cmd.DeleteIdentityLinkForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.DeleteMultiInstanceExecutionCmd;
import org.flowable.engine.impl.cmd.DeleteProcessInstanceCmd;
import org.flowable.engine.impl.cmd.DeleteProcessInstanceStartEventSubscriptionCmd;
import org.flowable.engine.impl.cmd.DeleteProcessInstancesByIdCmd;
import org.flowable.engine.impl.cmd.DispatchEventCommand;
import org.flowable.engine.impl.cmd.EvaluateConditionalEventsCmd;
import org.flowable.engine.impl.cmd.ExecuteActivityForAdhocSubProcessCmd;
import org.flowable.engine.impl.cmd.FindActiveActivityIdsCmd;
import org.flowable.engine.impl.cmd.GetActiveAdhocSubProcessesCmd;
import org.flowable.engine.impl.cmd.GetDataObjectCmd;
import org.flowable.engine.impl.cmd.GetDataObjectsCmd;
import org.flowable.engine.impl.cmd.GetEnabledActivitiesForAdhocSubProcessCmd;
import org.flowable.engine.impl.cmd.GetEntityLinkChildrenForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetEntityLinkChildrenForTaskCmd;
import org.flowable.engine.impl.cmd.GetEntityLinkChildrenWithSameRootAsProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetEntityLinkParentsForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetEntityLinkParentsForTaskCmd;
import org.flowable.engine.impl.cmd.GetExecutionVariableCmd;
import org.flowable.engine.impl.cmd.GetExecutionVariableInstanceCmd;
import org.flowable.engine.impl.cmd.GetExecutionVariableInstancesCmd;
import org.flowable.engine.impl.cmd.GetExecutionVariablesCmd;
import org.flowable.engine.impl.cmd.GetExecutionsVariablesCmd;
import org.flowable.engine.impl.cmd.GetIdentityLinksForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetProcessInstanceEventsCmd;
import org.flowable.engine.impl.cmd.GetStartFormCmd;
import org.flowable.engine.impl.cmd.GetStartFormModelCmd;
import org.flowable.engine.impl.cmd.HasExecutionVariableCmd;
import org.flowable.engine.impl.cmd.MessageEventReceivedCmd;
import org.flowable.engine.impl.cmd.ModifyProcessInstanceStartEventSubscriptionCmd;
import org.flowable.engine.impl.cmd.RegisterProcessInstanceStartEventSubscriptionCmd;
import org.flowable.engine.impl.cmd.RemoveEventConsumerCommand;
import org.flowable.engine.impl.cmd.RemoveEventListenerCommand;
import org.flowable.engine.impl.cmd.RemoveExecutionVariablesCmd;
import org.flowable.engine.impl.cmd.RemoveProcessInstanceAssigneeCmd;
import org.flowable.engine.impl.cmd.RemoveProcessInstanceOwnerCmd;
import org.flowable.engine.impl.cmd.SetAsyncExecutionVariablesCmd;
import org.flowable.engine.impl.cmd.SetExecutionVariablesCmd;
import org.flowable.engine.impl.cmd.SetProcessInstanceAssigneeCmd;
import org.flowable.engine.impl.cmd.SetProcessInstanceBusinessKeyCmd;
import org.flowable.engine.impl.cmd.SetProcessInstanceBusinessStatusCmd;
import org.flowable.engine.impl.cmd.SetProcessInstanceNameCmd;
import org.flowable.engine.impl.cmd.SetProcessInstanceOwnerCmd;
import org.flowable.engine.impl.cmd.SignalEventReceivedCmd;
import org.flowable.engine.impl.cmd.StartProcessInstanceAsyncCmd;
import org.flowable.engine.impl.cmd.StartProcessInstanceByMessageCmd;
import org.flowable.engine.impl.cmd.StartProcessInstanceCmd;
import org.flowable.engine.impl.cmd.SuspendProcessInstanceCmd;
import org.flowable.engine.impl.cmd.TriggerCmd;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceStartEventSubscriptionBuilderImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceStartEventSubscriptionDeletionBuilderImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceStartEventSubscriptionModificationBuilderImpl;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.NativeExecutionQuery;
import org.flowable.engine.runtime.NativeProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstanceStartEventSubscriptionBuilder;
import org.flowable.engine.runtime.ProcessInstanceStartEventSubscriptionDeletionBuilder;
import org.flowable.engine.runtime.ProcessInstanceStartEventSubscriptionModificationBuilder;
import org.flowable.engine.task.Event;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.runtime.NativeVariableInstanceQuery;
import org.flowable.variable.api.runtime.VariableInstanceQuery;
import org.flowable.variable.service.impl.NativeVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.VariableInstanceQueryImpl;

/**
 * @author Tom Baeyens
 * @author Daniel Meyer
 */
public class RuntimeServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements RuntimeService {

    public RuntimeServiceImpl(ProcessEngineConfigurationImpl configuration) {
        super(configuration);
    }

    @Override
    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, null, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey, String businessKey) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, businessKey, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey, Map<String, Object> variables) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, null, variables));
    }

    @Override
    public ProcessInstance startProcessInstanceByKey(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, businessKey, variables));
    }

    @Override
    public ProcessInstance startProcessInstanceByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, null, null, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByKeyAndTenantId(String processDefinitionKey, String businessKey, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, businessKey, null, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByKeyAndTenantId(String processDefinitionKey, Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, null, variables, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByKeyAndTenantId(String processDefinitionKey, String businessKey, Map<String, Object> variables, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processDefinitionKey, null, businessKey, variables, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceById(String processDefinitionId) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(null, processDefinitionId, null, null));
    }

    @Override
    public ProcessInstance startProcessInstanceById(String processDefinitionId, String businessKey) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(null, processDefinitionId, businessKey, null));
    }

    @Override
    public ProcessInstance startProcessInstanceById(String processDefinitionId, Map<String, Object> variables) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(null, processDefinitionId, null, variables));
    }

    @Override
    public ProcessInstance startProcessInstanceById(String processDefinitionId, String businessKey, Map<String, Object> variables) {
        return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(null, processDefinitionId, businessKey, variables));
    }

    @Override
    public ProcessInstance startProcessInstanceWithForm(String processDefinitionId, String outcome, Map<String, Object> variables, String processInstanceName) {
        ProcessInstanceBuilder processInstanceBuilder = createProcessInstanceBuilder()
            .processDefinitionId(processDefinitionId)
            .outcome(outcome)
            .startFormVariables(variables)
            .name(processInstanceName);
        return processInstanceBuilder.start();
    }

    @Override
    public FormInfo getStartFormModel(String processDefinitionId, String processInstanceId) {
        return commandExecutor.execute(new GetStartFormModelCmd(processDefinitionId, processInstanceId));
    }

    @Override
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        commandExecutor.execute(new DeleteProcessInstanceCmd(processInstanceId, deleteReason));
    }

    @Override
    public void bulkDeleteProcessInstances(Collection<String> processInstanceIds, String deleteReason) {
        commandExecutor.execute(new DeleteProcessInstancesByIdCmd(processInstanceIds, deleteReason));
    }

    @Override
    public ExecutionQuery createExecutionQuery() {
        return new ExecutionQueryImpl(commandExecutor, configuration);
    }

    @Override
    public NativeExecutionQuery createNativeExecutionQuery() {
        return new NativeExecutionQueryImpl(commandExecutor);
    }

    @Override
    public NativeProcessInstanceQuery createNativeProcessInstanceQuery() {
        return new NativeProcessInstanceQueryImpl(commandExecutor);
    }

    @Override
    public NativeActivityInstanceQueryImpl createNativeActivityInstanceQuery() {
        return new NativeActivityInstanceQueryImpl(commandExecutor);
    }

    @Override
    public EventSubscriptionQuery createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(commandExecutor, configuration.getEventSubscriptionServiceConfiguration());
    }

    @Override
    public void updateBusinessKey(String processInstanceId, String businessKey) {
        commandExecutor.execute(new SetProcessInstanceBusinessKeyCmd(processInstanceId, businessKey));
    }
    
    @Override
    public void updateBusinessStatus(String processInstanceId, String businessStatus) {
        commandExecutor.execute(new SetProcessInstanceBusinessStatusCmd(processInstanceId, businessStatus));
    }

    @Override
    public Map<String, Object> getVariables(String executionId) {
        return commandExecutor.execute(new GetExecutionVariablesCmd(executionId, null, false));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String executionId) {
        return commandExecutor.execute(new GetExecutionVariableInstancesCmd(executionId, null, false));
    }

    @Override
    public List<VariableInstance> getVariableInstancesByExecutionIds(Set<String> executionIds) {
        return commandExecutor.execute(new GetExecutionsVariablesCmd(executionIds));
    }

    @Override
    public Map<String, Object> getVariablesLocal(String executionId) {
        return commandExecutor.execute(new GetExecutionVariablesCmd(executionId, null, true));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String executionId) {
        return commandExecutor.execute(new GetExecutionVariableInstancesCmd(executionId, null, true));
    }

    @Override
    public Map<String, Object> getVariables(String executionId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetExecutionVariablesCmd(executionId, variableNames, false));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String executionId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetExecutionVariableInstancesCmd(executionId, variableNames, false));
    }

    @Override
    public Map<String, Object> getVariablesLocal(String executionId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetExecutionVariablesCmd(executionId, variableNames, true));
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String executionId, Collection<String> variableNames) {
        return commandExecutor.execute(new GetExecutionVariableInstancesCmd(executionId, variableNames, true));
    }

    @Override
    public Object getVariable(String executionId, String variableName) {
        return commandExecutor.execute(new GetExecutionVariableCmd(executionId, variableName, false));
    }

    @Override
    public VariableInstance getVariableInstance(String executionId, String variableName) {
        return commandExecutor.execute(new GetExecutionVariableInstanceCmd(executionId, variableName, false));
    }

    @Override
    public <T> T getVariable(String executionId, String variableName, Class<T> variableClass) {
        return variableClass.cast(getVariable(executionId, variableName));
    }

    @Override
    public boolean hasVariable(String executionId, String variableName) {
        return commandExecutor.execute(new HasExecutionVariableCmd(executionId, variableName, false));
    }

    @Override
    public Object getVariableLocal(String executionId, String variableName) {
        return commandExecutor.execute(new GetExecutionVariableCmd(executionId, variableName, true));
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String executionId, String variableName) {
        return commandExecutor.execute(new GetExecutionVariableInstanceCmd(executionId, variableName, true));
    }

    @Override
    public <T> T getVariableLocal(String executionId, String variableName, Class<T> variableClass) {
        return variableClass.cast(getVariableLocal(executionId, variableName));
    }

    @Override
    public boolean hasVariableLocal(String executionId, String variableName) {
        return commandExecutor.execute(new HasExecutionVariableCmd(executionId, variableName, true));
    }

    @Override
    public void setVariable(String executionId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetExecutionVariablesCmd(executionId, variables, false));
    }

    @Override
    public void setVariableLocal(String executionId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetExecutionVariablesCmd(executionId, variables, true));
    }

    @Override
    public void setVariables(String executionId, Map<String, ?> variables) {
        commandExecutor.execute(new SetExecutionVariablesCmd(executionId, variables, false));
    }

    @Override
    public void setVariablesLocal(String executionId, Map<String, ?> variables) {
        commandExecutor.execute(new SetExecutionVariablesCmd(executionId, variables, true));
    }
    
    @Override
    public void setVariableAsync(String executionId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetAsyncExecutionVariablesCmd(executionId, variables, false));
    }

    @Override
    public void setVariableLocalAsync(String executionId, String variableName, Object value) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put(variableName, value);
        commandExecutor.execute(new SetAsyncExecutionVariablesCmd(executionId, variables, true));
    }

    @Override
    public void setVariablesAsync(String executionId, Map<String, ?> variables) {
        commandExecutor.execute(new SetAsyncExecutionVariablesCmd(executionId, variables, false));
    }

    @Override
    public void setVariablesLocalAsync(String executionId, Map<String, ?> variables) {
        commandExecutor.execute(new SetAsyncExecutionVariablesCmd(executionId, variables, true));
    }

    @Override
    public void removeVariable(String executionId, String variableName) {
        Collection<String> variableNames = new ArrayList<>(1);
        variableNames.add(variableName);
        commandExecutor.execute(new RemoveExecutionVariablesCmd(executionId, variableNames, false));
    }

    @Override
    public void removeVariableLocal(String executionId, String variableName) {
        Collection<String> variableNames = new ArrayList<>();
        variableNames.add(variableName);
        commandExecutor.execute(new RemoveExecutionVariablesCmd(executionId, variableNames, true));
    }

    @Override
    public void removeVariables(String executionId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveExecutionVariablesCmd(executionId, variableNames, false));
    }

    @Override
    public void removeVariablesLocal(String executionId, Collection<String> variableNames) {
        commandExecutor.execute(new RemoveExecutionVariablesCmd(executionId, variableNames, true));
    }
    
    @Override
    public VariableInstanceQuery createVariableInstanceQuery() {
        return new VariableInstanceQueryImpl(commandExecutor, configuration.getVariableServiceConfiguration());
    }

    @Override
    public NativeVariableInstanceQuery createNativeVariableInstanceQuery() {
        return new NativeVariableInstanceQueryImpl(commandExecutor, configuration.getVariableServiceConfiguration());
    }

    @Override
    public Map<String, DataObject> getDataObjects(String executionId) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, null, false));
    }

    @Override
    public Map<String, DataObject> getDataObjects(String executionId, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, null, false, locale, withLocalizationFallback));
    }

    @Override
    public Map<String, DataObject> getDataObjectsLocal(String executionId) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, null, true));
    }

    @Override
    public Map<String, DataObject> getDataObjectsLocal(String executionId, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, null, true, locale, withLocalizationFallback));
    }

    @Override
    public Map<String, DataObject> getDataObjects(String executionId, Collection<String> dataObjectNames) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, dataObjectNames, false));
    }

    @Override
    public Map<String, DataObject> getDataObjects(String executionId, Collection<String> dataObjectNames, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, dataObjectNames, false, locale, withLocalizationFallback));
    }

    @Override
    public Map<String, DataObject> getDataObjectsLocal(String executionId, Collection<String> dataObjects) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, dataObjects, true));
    }

    @Override
    public Map<String, DataObject> getDataObjectsLocal(String executionId, Collection<String> dataObjectNames, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectsCmd(executionId, dataObjectNames, true, locale, withLocalizationFallback));
    }

    @Override
    public DataObject getDataObject(String executionId, String dataObject) {
        return commandExecutor.execute(new GetDataObjectCmd(executionId, dataObject, false));
    }

    @Override
    public DataObject getDataObject(String executionId, String dataObjectName, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectCmd(executionId, dataObjectName, false, locale, withLocalizationFallback));
    }

    @Override
    public DataObject getDataObjectLocal(String executionId, String dataObjectName) {
        return commandExecutor.execute(new GetDataObjectCmd(executionId, dataObjectName, true));
    }

    @Override
    public DataObject getDataObjectLocal(String executionId, String dataObjectName, String locale, boolean withLocalizationFallback) {
        return commandExecutor.execute(new GetDataObjectCmd(executionId, dataObjectName, true, locale, withLocalizationFallback));
    }

    public void signal(String executionId) {
        commandExecutor.execute(new TriggerCmd(executionId, null));
    }

    @Override
    public void trigger(String executionId) {
        commandExecutor.execute(new TriggerCmd(executionId, null));
    }

    @Override
    public void triggerAsync(String executionId) {
        commandExecutor.execute(new TriggerCmd(executionId, null, true));
    }

    public void signal(String executionId, Map<String, Object> processVariables) {
        commandExecutor.execute(new TriggerCmd(executionId, processVariables));
    }

    @Override
    public void trigger(String executionId, Map<String, Object> processVariables) {
        commandExecutor.execute(new TriggerCmd(executionId, processVariables));
    }

    @Override
    public void triggerAsync(String executionId, Map<String, Object> processVariables) {
        commandExecutor.execute(new TriggerCmd(executionId, processVariables, true));
    }

    @Override
    public void trigger(String executionId, Map<String, Object> processVariables, Map<String, Object> transientVariables) {
        commandExecutor.execute(new TriggerCmd(executionId, processVariables, transientVariables));
    }
    
    @Override
    public void evaluateConditionalEvents(String processInstanceId) {
        commandExecutor.execute(new EvaluateConditionalEventsCmd(processInstanceId, null));
    }

    @Override
    public void evaluateConditionalEvents(String processInstanceId, Map<String, Object> processVariables) {
        commandExecutor.execute(new EvaluateConditionalEventsCmd(processInstanceId, processVariables));
    }

    @Override
    public void setOwner(String processInstanceId, String userId) {
        commandExecutor.execute(new SetProcessInstanceOwnerCmd(processInstanceId, userId));
    }

    @Override
    public void removeOwner(String processInstanceId) {
        commandExecutor.execute(new RemoveProcessInstanceOwnerCmd(processInstanceId));
    }

    @Override
    public void setAssignee(String processInstanceId, String userId) {
        commandExecutor.execute(new SetProcessInstanceAssigneeCmd(processInstanceId, userId));
    }

    @Override
    public void removeAssignee(String processInstanceId) {
        commandExecutor.execute(new RemoveProcessInstanceAssigneeCmd(processInstanceId));
    }

    @Override
    public void addUserIdentityLink(String processInstanceId, String userId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkForProcessInstanceCmd(processInstanceId, userId, null, identityLinkType));
    }

    @Override
    public void addGroupIdentityLink(String processInstanceId, String groupId, String identityLinkType) {
        commandExecutor.execute(new AddIdentityLinkForProcessInstanceCmd(processInstanceId, null, groupId, identityLinkType));
    }

    @Override
    public void addParticipantUser(String processInstanceId, String userId) {
        commandExecutor.execute(new AddIdentityLinkForProcessInstanceCmd(processInstanceId, userId, null, IdentityLinkType.PARTICIPANT));
    }

    @Override
    public void addParticipantGroup(String processInstanceId, String groupId) {
        commandExecutor.execute(new AddIdentityLinkForProcessInstanceCmd(processInstanceId, null, groupId, IdentityLinkType.PARTICIPANT));
    }

    @Override
    public void deleteParticipantUser(String processInstanceId, String userId) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessInstanceCmd(processInstanceId, userId, null, IdentityLinkType.PARTICIPANT));
    }

    @Override
    public void deleteParticipantGroup(String processInstanceId, String groupId) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessInstanceCmd(processInstanceId, null, groupId, IdentityLinkType.PARTICIPANT));
    }

    @Override
    public void deleteUserIdentityLink(String processInstanceId, String userId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessInstanceCmd(processInstanceId, userId, null, identityLinkType));
    }

    @Override
    public void deleteGroupIdentityLink(String processInstanceId, String groupId, String identityLinkType) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessInstanceCmd(processInstanceId, null, groupId, identityLinkType));
    }

    @Override
    public List<IdentityLink> getIdentityLinksForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetIdentityLinksForProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkChildrenForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetEntityLinkChildrenForProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkChildrenWithSameRootAsProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetEntityLinkChildrenWithSameRootAsProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkChildrenForTask(String taskId) {
        return commandExecutor.execute(new GetEntityLinkChildrenForTaskCmd(taskId));
    }

    @Override
    public List<EntityLink> getEntityLinkParentsForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetEntityLinkParentsForProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<EntityLink> getEntityLinkParentsForTask(String taskId) {
        return commandExecutor.execute(new GetEntityLinkParentsForTaskCmd(taskId));
    }

    @Override
    public ProcessInstanceQuery createProcessInstanceQuery() {
        return new ProcessInstanceQueryImpl(commandExecutor, configuration);
    }

    @Override
    public ActivityInstanceQueryImpl createActivityInstanceQuery() {
        return new ActivityInstanceQueryImpl(commandExecutor);
    }

    @Override
    public List<String> getActiveActivityIds(String executionId) {
        return commandExecutor.execute(new FindActiveActivityIdsCmd(executionId));
    }

    public FormData getFormInstanceById(String processDefinitionId) {
        return commandExecutor.execute(new GetStartFormCmd(processDefinitionId));
    }

    @Override
    public void suspendProcessInstanceById(String processInstanceId) {
        commandExecutor.execute(new SuspendProcessInstanceCmd(processInstanceId));
    }

    @Override
    public void activateProcessInstanceById(String processInstanceId) {
        commandExecutor.execute(new ActivateProcessInstanceCmd(processInstanceId));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessage(String messageName) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, null, null, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessageAndTenantId(String messageName, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, null, null, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessage(String messageName, String businessKey) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, businessKey, null, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessageAndTenantId(String messageName, String businessKey, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, businessKey, null, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessage(String messageName, Map<String, Object> processVariables) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, null, processVariables, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessageAndTenantId(String messageName, Map<String, Object> processVariables, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, null, processVariables, tenantId));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessage(String messageName, String businessKey, Map<String, Object> processVariables) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, businessKey, processVariables, null));
    }

    @Override
    public ProcessInstance startProcessInstanceByMessageAndTenantId(String messageName, String businessKey, Map<String, Object> processVariables, String tenantId) {
        return commandExecutor.execute(new StartProcessInstanceByMessageCmd(messageName, businessKey, processVariables, tenantId));
    }

    @Override
    public void signalEventReceived(String signalName) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, null, null));
    }

    @Override
    public void signalEventReceivedWithTenantId(String signalName, String tenantId) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, null, tenantId));
    }

    @Override
    public void signalEventReceivedAsync(String signalName) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, true, null));
    }

    @Override
    public void signalEventReceivedAsyncWithTenantId(String signalName, String tenantId) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, true, tenantId));
    }

    @Override
    public void signalEventReceived(String signalName, Map<String, Object> processVariables) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, processVariables, null));
    }

    @Override
    public void signalEventReceivedWithTenantId(String signalName, Map<String, Object> processVariables, String tenantId) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, null, processVariables, tenantId));
    }

    @Override
    public void signalEventReceived(String signalName, String executionId) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, executionId, null, null));
    }

    @Override
    public void signalEventReceived(String signalName, String executionId, Map<String, Object> processVariables) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, executionId, processVariables, null));
    }

    @Override
    public void signalEventReceivedAsync(String signalName, String executionId) {
        commandExecutor.execute(new SignalEventReceivedCmd(signalName, executionId, true, null));
    }

    @Override
    public void messageEventReceived(String messageName, String executionId) {
        commandExecutor.execute(new MessageEventReceivedCmd(messageName, executionId, null));
    }

    @Override
    public void messageEventReceived(String messageName, String executionId, Map<String, Object> processVariables) {
        commandExecutor.execute(new MessageEventReceivedCmd(messageName, executionId, processVariables));
    }

    @Override
    public void messageEventReceivedAsync(String messageName, String executionId) {
        commandExecutor.execute(new MessageEventReceivedCmd(messageName, executionId, true));
    }

    @Override
    public void addEventListener(FlowableEventListener listenerToAdd) {
        commandExecutor.execute(new AddEventListenerCommand(listenerToAdd));
    }

    @Override
    public void addEventListener(FlowableEventListener listenerToAdd, FlowableEngineEventType... types) {
        commandExecutor.execute(new AddEventListenerCommand(listenerToAdd, types));
    }

    @Override
    public void removeEventListener(FlowableEventListener listenerToRemove) {
        commandExecutor.execute(new RemoveEventListenerCommand(listenerToRemove));
    }

    @Override
    public void dispatchEvent(FlowableEvent event) {
        commandExecutor.execute(new DispatchEventCommand(event));
    }
    
    @Override
    public void addEventRegistryConsumer(EventRegistryEventConsumer eventConsumer) {
        commandExecutor.execute(new AddEventConsumerCommand(eventConsumer));
    }
    
    @Override
    public void removeEventRegistryConsumer(EventRegistryEventConsumer eventConsumer) {
        commandExecutor.execute(new RemoveEventConsumerCommand(eventConsumer));
    }

    @Override
    public ProcessInstanceStartEventSubscriptionBuilder createProcessInstanceStartEventSubscriptionBuilder() {
        return new ProcessInstanceStartEventSubscriptionBuilderImpl(this);
    }

    @Override
    public ProcessInstanceStartEventSubscriptionModificationBuilder createProcessInstanceStartEventSubscriptionModificationBuilder() {
        return new ProcessInstanceStartEventSubscriptionModificationBuilderImpl(this);
    }

    @Override
    public ProcessInstanceStartEventSubscriptionDeletionBuilder createProcessInstanceStartEventSubscriptionDeletionBuilder() {
        return new ProcessInstanceStartEventSubscriptionDeletionBuilderImpl(this);
    }

    @Override
    public void setProcessInstanceName(String processInstanceId, String name) {
        commandExecutor.execute(new SetProcessInstanceNameCmd(processInstanceId, name));
    }

    @Override
    public List<Event> getProcessInstanceEvents(String processInstanceId) {
        return commandExecutor.execute(new GetProcessInstanceEventsCmd(processInstanceId));
    }

    @Override
    public List<Execution> getAdhocSubProcessExecutions(String processInstanceId) {
        return commandExecutor.execute(new GetActiveAdhocSubProcessesCmd(processInstanceId));
    }

    @Override
    public List<FlowNode> getEnabledActivitiesFromAdhocSubProcess(String executionId) {
        return commandExecutor.execute(new GetEnabledActivitiesForAdhocSubProcessCmd(executionId));
    }

    @Override
    public Execution executeActivityInAdhocSubProcess(String executionId, String activityId) {
        return commandExecutor.execute(new ExecuteActivityForAdhocSubProcessCmd(executionId, activityId));
    }

    @Override
    public void completeAdhocSubProcess(String executionId) {
        commandExecutor.execute(new CompleteAdhocSubProcessCmd(executionId));
    }

    @Override
    public ProcessInstanceBuilder createProcessInstanceBuilder() {
        return new ProcessInstanceBuilderImpl(this);
    }

    @Override
    public ChangeActivityStateBuilder createChangeActivityStateBuilder() {
        return new ChangeActivityStateBuilderImpl(this);
    }

    @Override
    public Execution addMultiInstanceExecution(String activityId, String parentExecutionId, Map<String, Object> executionVariables) {
        return commandExecutor.execute(new AddMultiInstanceExecutionCmd(activityId, parentExecutionId, executionVariables));
    }

    @Override
    public void deleteMultiInstanceExecution(String executionId, boolean executionIsCompleted) {
        commandExecutor.execute(new DeleteMultiInstanceExecutionCmd(executionId, executionIsCompleted));
    }

    public ProcessInstance startProcessInstance(ProcessInstanceBuilderImpl processInstanceBuilder) {
        if (processInstanceBuilder.getProcessDefinitionId() != null || processInstanceBuilder.getProcessDefinitionKey() != null) {
            return commandExecutor.execute(new StartProcessInstanceCmd<ProcessInstance>(processInstanceBuilder));
        } else if (processInstanceBuilder.getMessageName() != null) {
            return commandExecutor.execute(new StartProcessInstanceByMessageCmd(processInstanceBuilder));
        } else {
            throw new FlowableIllegalArgumentException("No processDefinitionId, processDefinitionKey nor messageName provided");
        }
    }

    public ProcessInstance startProcessInstanceAsync(ProcessInstanceBuilderImpl processInstanceBuilder) {
        if (processInstanceBuilder.getProcessDefinitionId() != null || processInstanceBuilder.getProcessDefinitionKey() != null) {
            return (ProcessInstance) commandExecutor.execute(new StartProcessInstanceAsyncCmd(processInstanceBuilder));
        } else {
            throw new FlowableIllegalArgumentException("No processDefinitionId, processDefinitionKey provided");
        }
    }

    public EventSubscription registerProcessInstanceStartEventSubscription(ProcessInstanceStartEventSubscriptionBuilderImpl builder) {
        return commandExecutor.execute(new RegisterProcessInstanceStartEventSubscriptionCmd(builder));
    }

    public void migrateProcessInstanceStartEventSubscriptionsToProcessDefinitionVersion(ProcessInstanceStartEventSubscriptionModificationBuilderImpl builder) {
        commandExecutor.execute(new ModifyProcessInstanceStartEventSubscriptionCmd(builder));
    }

    public void deleteProcessInstanceStartEventSubscriptions(ProcessInstanceStartEventSubscriptionDeletionBuilderImpl builder) {
        commandExecutor.execute(new DeleteProcessInstanceStartEventSubscriptionCmd(builder));
    }

    public void changeActivityState(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        commandExecutor.execute(new ChangeActivityStateCmd(changeActivityStateBuilder));
    }
}
