package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ServiceTaskImplB
		implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("ServiceTaskImplB.execute()");
		execution.setVariable("myProcessVariable", "I' am Service-Task B");
	}

}
