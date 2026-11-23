package pl.stadiumscoreboard

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ApplyExtension(SpringExtension::class)
class HealthEndpointSpec : FunSpec() {

    @Autowired
    private lateinit var context: WebApplicationContext

    init {
        test("GET /actuator/health zwraca status UP") {
            val mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

            mockMvc.get("/actuator/health").andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
        }
    }
}
