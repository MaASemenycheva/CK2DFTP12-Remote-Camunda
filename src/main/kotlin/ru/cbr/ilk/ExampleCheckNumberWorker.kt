package ru.cbr.ilk

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription
import org.camunda.bpm.client.task.ExternalTask
import org.camunda.bpm.client.task.ExternalTaskHandler
import org.camunda.bpm.client.task.ExternalTaskService
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.logging.Logger


@Configuration
@ExternalTaskSubscription("check-number")
class ExampleCheckNumberWorker : ExternalTaskHandler {
    override fun execute(externalTask: ExternalTask, externalTaskService: ExternalTaskService) {
        // Get a process variable
        val someProcessVariable = externalTask.getVariable<Any>("someProcessVariable") as String
        var isNumber = false
        try {
            someProcessVariable.toLong()
            isNumber = true
        } catch (e: NumberFormatException) {
            LOGGER.info("$someProcessVariable is not numeric")
        }
        LOGGER.info("Returning validate=$isNumber")

        // Complete the task
        externalTaskService.complete(externalTask, Collections.singletonMap<String, Any>("isNumber", isNumber))
    }

    companion object {
        private val LOGGER = Logger.getLogger(ExampleCheckNumberWorker::class.java.name)
    }
}