package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ServiceTaskImplA
		implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("ServiceTaskImplA.execute()");
		execution.setVariable("myProcessVariable", "I' am Service-Task A");
	}

}
