package ru.cbr.ilk

import org.camunda.community.rest.client.api.ProcessDefinitionApi
import org.camunda.community.rest.client.dto.HistoricProcessInstanceDto
import org.camunda.community.rest.client.dto.HistoricProcessInstanceQueryDto
import org.camunda.community.rest.client.dto.StartProcessInstanceDto
import org.camunda.community.rest.client.dto.VariableValueDto
import org.camunda.community.rest.client.invoker.ApiException
import org.camunda.community.rest.client.springboot.CamundaHistoryApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange


@RestController
class ExampleRestEndpoint {
    @Autowired
    private val processDefinitionApi: ProcessDefinitionApi? = null

    @Autowired
    private val camundaHistoryApi: CamundaHistoryApi? = null
    @PutMapping("/start")
    @Throws(ApiException::class)
    fun startProcess(exchange: ServerWebExchange?): ResponseEntity<String> {
        // TODO: Get from REST request / URL parameter
        val someProcessVariable = "test"

        // prepare variables to pass on to process
        val variables: MutableMap<String, VariableValueDto> = HashMap()
        variables[ProcessConstants.VAR_NAME_SOME_VARIABLE] =
            VariableValueDto().value(someProcessVariable).type("string")

        // start process instance
        val processInstance = processDefinitionApi!!.startProcessInstanceByKey(
            ProcessConstants.PROCESS_KEY,
            StartProcessInstanceDto().variables(variables)
        )

        // And just return something for the sake of the example
        return ResponseEntity
            .status(HttpStatus.OK)
            .body("Started process instance with id: " + processInstance.id)
    }

    @get:Throws(ApiException::class)
    @get:GetMapping("/info")
    val activeProcessInstances: ResponseEntity<List<HistoricProcessInstanceDto>>
        get() {
            val processInstances = camundaHistoryApi!!.historicProcessInstanceApi().queryHistoricProcessInstances(
                null,
                null,
                HistoricProcessInstanceQueryDto().active(true)
            )
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(processInstances)
        }
}
