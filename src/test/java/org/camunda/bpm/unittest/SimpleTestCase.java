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

import java.util.List;

import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.cmd.ExecuteJobsCmd;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.jobexecutor.ExecuteJobHelper;
import org.camunda.bpm.engine.impl.jobexecutor.JobFailureCollector;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.test.Deployment;
import org.slf4j.Logger;

/**
 * @author Daniel Meyer
 * @author Martin Schimak
 */
public class SimpleTestCase
		extends PluggableProcessEngineTestCase {

	private static Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

	static ControllableThread activeThread;

	class ExecuteJobControlThread
			extends ControllableThread {

		OptimisticLockingException optimisticLockingException;
		Exception exception;

		protected String jobId;

		public ExecuteJobControlThread(String jobId) {
			this.jobId = jobId;
		}

		@Override
		public synchronized void startAndWaitUntilControlIsReturned() {
			activeThread = this;
			super.startAndWaitUntilControlIsReturned();
		}

		@Override
		public void run() {
			try {
				CommandExecutor commandExecutor = SimpleTestCase.this.processEngineConfiguration.getCommandExecutorTxRequired();

				JobFailureCollector jobFailureCollector = new JobFailureCollector(this.jobId);
				ExecuteJobsCmd executeJobsCmd = new ExecuteJobsCmd(this.jobId, jobFailureCollector);

				ExecuteJobHelper.executeJob(this.jobId, commandExecutor, jobFailureCollector, new ControlledCommand<>(
						activeThread, executeJobsCmd));
			} catch (OptimisticLockingException e) {
				e.printStackTrace();
				this.optimisticLockingException = e;
			} catch (Exception e) {
				e.printStackTrace();
				this.exception = e;
			}
			LOG.debug(this.getName() + " ends");
		}
	}

	@Deployment(resources = "testProcess.bpmn")
	public void testConcurrentVariableCreate() {
		this.runtimeService.startProcessInstanceByKey("testProcess");

		List<Job> list = getProcessEngine().getManagementService().createJobQuery().list();
		assertEquals(2, list.size());

		ExecuteJobControlThread thread1 = new ExecuteJobControlThread(list.get(0).getId());
		thread1.startAndWaitUntilControlIsReturned();

		ExecuteJobControlThread thread2 = new ExecuteJobControlThread(list.get(1).getId());
		thread2.startAndWaitUntilControlIsReturned();

		thread1.proceedAndWaitTillDone();
		assertNull(thread1.exception);
		assertNull(thread1.optimisticLockingException);

		thread2.proceedAndWaitTillDone();
		assertNotNull(thread2.optimisticLockingException);
	}

}
