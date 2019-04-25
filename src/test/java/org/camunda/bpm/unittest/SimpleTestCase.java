/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
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
package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.cmd.SetTaskVariablesCmd;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.test.Deployment;
import org.slf4j.Logger;

import java.util.Collections;

/**
 * @author Daniel Meyer
 * @author Martin Schimak
 */
public class SimpleTestCase extends PluggableProcessEngineTestCase {

  private static Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  static ControllableThread activeThread;

  class SetTaskVariablesThread extends ControllableThread {

    OptimisticLockingException optimisticLockingException;
    Exception exception;

    protected Object variableValue;
    protected String taskId;
    protected String variableName;

    public SetTaskVariablesThread(String taskId, String variableName, Object variableValue) {
      this.taskId = taskId;
      this.variableName = variableName;
      this.variableValue = variableValue;
    }

    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    public void run() {
      try {
        processEngineConfiguration
          .getCommandExecutorTxRequired()
          .execute(new ControlledCommand(activeThread, new SetTaskVariablesCmd(taskId, Collections.singletonMap(variableName, variableValue), false)));

      } catch (OptimisticLockingException e) {
        this.optimisticLockingException = e;
      } catch (Exception e) {
        this.exception = e;
      }
      LOG.debug(getName()+" ends");
    }
  }

  @Deployment(resources="testProcess.bpmn")
  public void testConcurrentVariableCreate() {

    runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("varName1", "someValue"));

    String variableName = "varName";
    String taskId = taskService.createTaskQuery().singleResult().getId();

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread1.startAndWaitUntilControlIsReturned();

    // this should fail with integrity constraint violation
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    assertNull(thread1.exception);
    assertNull(thread1.optimisticLockingException);

    thread2.proceedAndWaitTillDone();
    assertNull(thread2.exception);
    assertNotNull(thread2.optimisticLockingException);

    // should not fail with FK violation because one of the variables is not deleted.
    taskService.complete(taskId);
  }

}
