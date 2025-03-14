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
package org.flowable.common.engine.api.variable;

import java.util.Set;

/**
 * @author Joram Barrez
 */
public interface VariableContainer {

    /**
     * @return an empty (null object) variable container.
     */
    public static VariableContainer empty(){
        return EmptyVariableContainer.INSTANCE;
    }

    boolean hasVariable(String variableName);
    
    Object getVariable(String variableName);
    
    void setVariable(String variableName, Object variableValue);
    
    void setTransientVariable(String variableName, Object variableValue);

    String getTenantId();

    Set<String> getVariableNames();

}
