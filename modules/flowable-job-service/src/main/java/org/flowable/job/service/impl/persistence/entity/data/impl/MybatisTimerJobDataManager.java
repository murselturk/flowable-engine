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
package org.flowable.job.service.impl.persistence.entity.data.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.ListQueryParameterObject;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.JobByCorrelationIdMatcher;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.TimerJobsByExecutionIdMatcher;
import org.flowable.job.service.impl.persistence.entity.data.impl.cachematcher.TimerJobsByScopeIdAndSubScopeIdMatcher;

/**
 * @author Tijs Rademakers
 * @author Vasile Dirla
 * @author Joram Barrez
 */
public class MybatisTimerJobDataManager extends AbstractDataManager<TimerJobEntity> implements TimerJobDataManager {

    protected JobServiceConfiguration jobServiceConfiguration;
    
    protected CachedEntityMatcher<TimerJobEntity> timerJobsByExecutionIdMatcher = new TimerJobsByExecutionIdMatcher();

    protected CachedEntityMatcher<TimerJobEntity> timerJobsByScopeIdAndSubScopeIdMatcher = new TimerJobsByScopeIdAndSubScopeIdMatcher();

    protected SingleCachedEntityMatcher<TimerJobEntity> timerJobByCorrelationId = new JobByCorrelationIdMatcher<>();

    public MybatisTimerJobDataManager(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
    }
    
    @Override
    public Class<? extends TimerJobEntity> getManagedEntityClass() {
        return TimerJobEntityImpl.class;
    }

    @Override
    public TimerJobEntity create() {
        return new TimerJobEntityImpl();
    }

    @Override
    public TimerJobEntity findJobByCorrelationId(String correlationId) {
        return getEntity("selectTimerJobByCorrelationId", correlationId, timerJobByCorrelationId, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
        String query = "selectTimerJobByQueryCriteria";
        return getDbSqlSession().selectList(query, jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return (Long) getDbSqlSession().selectOne("selectTimerJobCountByQueryCriteria", jobQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findExpiredJobs(List<String> enabledCategories, Page page) {
        Map<String, Object> params = new HashMap<>();
        params.put("jobExecutionScope", jobServiceConfiguration.getJobExecutionScope());
        Date now = jobServiceConfiguration.getClock().getCurrentTime();
        params.put("now", now);
        if (enabledCategories != null && enabledCategories.size() > 0) {
            params.put("enabledCategories", enabledCategories);
        }
        
        ListQueryParameterObject listQueryParameterObject = new ListQueryParameterObject(params, page.getFirstResult(), page.getMaxResults());
        listQueryParameterObject.setIgnoreOrderBy();
        return getDbSqlSession().selectList("selectExpiredTimerJobs", listQueryParameterObject);
    }

    @Override
    public void resetExpiredJob(String jobId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("id", jobId);
        params.put("now", jobServiceConfiguration.getClock().getCurrentTime());
        getDbSqlSession().directUpdate("resetExpiredTimerJob", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsToExecute(List<String> enabledCategories, Page page) {
        Map<String, Object> params = new HashMap<>(2);
        String jobExecutionScope = jobServiceConfiguration.getJobExecutionScope();
        params.put("jobExecutionScope", jobExecutionScope);
        
        Date now = jobServiceConfiguration.getClock().getCurrentTime();
        params.put("now", now);
        
        if (enabledCategories != null && enabledCategories.size() > 0) {
            params.put("enabledCategories", enabledCategories);
        }
        
        ListQueryParameterObject listQueryParameterObject = new ListQueryParameterObject(params, page.getFirstResult(), page.getMaxResults());
        listQueryParameterObject.setIgnoreOrderBy();
        return getDbSqlSession().selectList("selectTimerJobsToExecute", listQueryParameterObject);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionId", processDefinitionId);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionId", params);

    }

    @Override
    public List<TimerJobEntity> findJobsByExecutionId(final String executionId) {
        DbSqlSession dbSqlSession = getDbSqlSession();
        
        // If the execution has been inserted in the same command execution as this query, there can't be any in the database
        if (isEntityInserted(dbSqlSession, "execution", executionId)) {
            return getListFromCache(timerJobsByExecutionIdMatcher, executionId);
        }
        
        return getList(dbSqlSession, "selectTimerJobsByExecutionId", executionId, timerJobsByExecutionIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByProcessInstanceId(final String processInstanceId) {
        return getDbSqlSession().selectList("selectTimerJobsByProcessInstanceId", processInstanceId);
    }

    @Override
    public List<TimerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("scopeId", scopeId);
        paramMap.put("subScopeId", subScopeId);
        return getList(getDbSqlSession(), "selectTimerJobsByScopeIdAndSubScopeId", paramMap, timerJobsByScopeIdAndSubScopeIdMatcher, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        Map<String, String> params = new HashMap<>(2);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionKey", processDefinitionKey);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyNoTenantId", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        Map<String, String> params = new HashMap<>(3);
        params.put("handlerType", jobHandlerType);
        params.put("processDefinitionKey", processDefinitionKey);
        params.put("tenantId", tenantId);
        return getDbSqlSession().selectList("selectTimerJobByTypeAndProcessDefinitionKeyAndTenantId", params);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("tenantId", newTenantId);
        getDbSqlSession().directUpdate("updateTimerJobTenantIdForDeployment", params);
    }

    @Override
    public void bulkUpdateJobLockWithoutRevisionCheck(List<TimerJobEntity> timerJobEntities, String lockOwner, Date lockExpirationTime) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("lockOwner", lockOwner);
        params.put("lockExpirationTime", lockExpirationTime);

        bulkUpdateEntities("updateTimerJobLocks", params, "timerJobs", timerJobEntities);
    }

    @Override
    public void bulkDeleteWithoutRevision(List<TimerJobEntity> timerJobEntities) {
        bulkDeleteEntities("deleteTimerJobs", timerJobEntities);
    }

    @Override
    protected IdGenerator getIdGenerator() {
        return jobServiceConfiguration.getIdGenerator();
    }
}
