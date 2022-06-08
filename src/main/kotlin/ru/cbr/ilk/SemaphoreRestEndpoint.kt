package ru.cbr.ilk

import org.camunda.bpm.client.ExternalTaskClient
import org.camunda.bpm.client.task.ExternalTask
import org.camunda.bpm.client.task.ExternalTaskService
import org.camunda.bpm.client.topic.TopicSubscription
import org.camunda.community.rest.client.api.ProcessDefinitionApi
import org.camunda.community.rest.client.dto.StartProcessInstanceDto
import org.camunda.community.rest.client.dto.VariableValueDto
import org.camunda.community.rest.client.invoker.ApiException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


@RestController
class ExampleSemaphoreRestEndpoint {
    @Autowired
    private val processDefinitionApi: ProcessDefinitionApi? = null

    @Autowired
    private val externalTaskClient: ExternalTaskClient? = null
    @PutMapping("/startWithResponse")
    @Throws(ApiException::class, InterruptedException::class)
    fun startProcessWithResponse(exchange: ServerWebExchange?): ResponseEntity<String> {
        // TODO: Get from REST request / URL parameter
        val someProcessVariable = "test"

        // prepare variables to pass on to process
        val variables: MutableMap<String, VariableValueDto> = HashMap()
        variables[ProcessConstants.VAR_NAME_SOME_VARIABLE] =
            VariableValueDto().value(someProcessVariable).type("string")

        // create a unique ID to wait for completion later and add it to the process
        val uuid = UUID.randomUUID().toString()
        variables[ProcessConstants.VAR_NAME_SEMAPHORE_CHECK] =
            VariableValueDto().value(uuid).type("string")

        // start process instance
        val processInstance = processDefinitionApi!!.startProcessInstanceByKey(
            ProcessConstants.PROCESS_KEY_SEMAPHORE_EXTENSION,
            StartProcessInstanceDto().variables(variables)
        )

        // now let's wait for the process to reach
        return try {
            val semaphore = newSemaphore(uuid)
            val finished = semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)
            if (!finished) {
                // we don't know what happened - it took too long. Let's respond by HTTP 202
                return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body("Started process instance with id: " + processInstance.id)
            }
            // yeah, we have our results :-)
            val isNumber = semaphorResults[uuid]!![ProcessConstants.VAR_NAME_IS_NUMBER] as Boolean
            // And just return something for the sake of the example
            ResponseEntity
                .status(HttpStatus.OK)
                .body("Started process instance with id: " + processInstance.id + ". Is number? " + isNumber)
        } finally {
            cleanupSemaphore(uuid)
        }
    }

    private fun newSemaphore(uuid: String): Semaphore {
        val semaphore = Semaphore(0)
        semaphors[uuid] = semaphore
        val topicSubscription = externalTaskClient!!.subscribe("SEMAPHORE_$uuid")
            .handler { externalTask: ExternalTask, externalTaskService: ExternalTaskService? ->
                // remember results
                semaphorResults[uuid] = externalTask.allVariables
                // and release semaphore
                semaphors[uuid]!!.release()
            }.open()
        semaphorSubscriptions[uuid] = topicSubscription
        return semaphore
    }

    private fun cleanupSemaphore(uuid: String) {
        semaphors.remove(uuid)
        semaphorSubscriptions[uuid]!!.close()
        semaphorSubscriptions.remove(uuid)
        semaphorResults.remove(uuid)
    }

    companion object {
        var semaphors: MutableMap<String, Semaphore> = HashMap()
        var semaphorSubscriptions: MutableMap<String, TopicSubscription> = HashMap()
        var semaphorResults: MutableMap<String, Map<*, *>> = HashMap()
    }
}
