package com.example.shortener;

import com.example.shortener.messages.CreateRedirectRequest;
import com.example.shortener.messages.CreateRedirectResponse;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class IntegrationTests {
    private static final Logger log = Logger.getLogger(IntegrationTests.class.getName());

    public static final String HTTP_YANDEX_RU = "http://yandex.ru";
    public static final String MYDB_HOSTNAME = "mydb";
    private RestTemplate restTemplate = new RestTemplate();
    private RestTemplate restTemplate2 = new RestTemplate(
            new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
                    connection.setInstanceFollowRedirects(false);
                }
            }
    );
    Network network = Network.newNetwork();
    @Rule
    public PostgreSQLContainer postgresContainer = (PostgreSQLContainer) new PostgreSQLContainer("postgres:12")
            .withNetwork(network)
            .withNetworkAliases(MYDB_HOSTNAME);

    @Test
    public void test_a_db_available() throws SQLException {
        String jdbcUrl = postgresContainer.getJdbcUrl();
        String username = postgresContainer.getUsername();
        String password = postgresContainer.getPassword();
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
        ResultSet resultSet = conn.createStatement().executeQuery("SELECT 1");
        resultSet.next();
        int result = resultSet.getInt(1);
        assertEquals(1, result);
    }

    @Test
    public void test_create() {

        int hostRandomPort = 8888; //ThreadLocalRandom.current().nextInt(10000, 60000);

        GenericContainer app = new FixedHostPortGenericContainer("me/shortener")
                .withFixedExposedPort(hostRandomPort, 8080)
                .withEnv("spring_datasource_url", "jdbc:postgresql://"+ MYDB_HOSTNAME +":5432/postgres")
                .withEnv("spring_datasource_username", postgresContainer.getUsername())
                .withEnv("spring_datasource_password", postgresContainer.getPassword())
                .withEnv("server_port", "8080")
                .withEnv("short_url_context", "http://localhost:"+hostRandomPort)
                .withNetwork(network)
//                .withLogConsumer(new LogConsumer())
                .waitingFor(Wait.forHttp("/heartbeat"))
                .withExposedPorts(8080)
                ;

        app.start();

        CreateRedirectRequest req = new CreateRedirectRequest();
        req.setLongUrl(HTTP_YANDEX_RU);

        HttpEntity<CreateRedirectRequest> request = new HttpEntity<>(req);
        String uri = "http://localhost:" + app.getMappedPort(8080) + "/create";
        CreateRedirectResponse response = restTemplate.postForObject(uri, request, CreateRedirectResponse.class);

        assertThat(response.getShortUrl(), notNullValue());

        System.out.println(response.getShortUrl());


        ResponseEntity<Object> httpResp = restTemplate2.exchange(response.getShortUrl(), HttpMethod.GET, null, Object.class);
        int statusCode = httpResp.getStatusCodeValue();
        String location = httpResp.getHeaders().getLocation() == null ? "" : httpResp.getHeaders().getLocation().toString();
        assertThat(statusCode, is(HttpServletResponse.SC_MOVED_TEMPORARILY));
        assertThat(location, is("http://yandex.ru"));
    }

    private class LogConsumer extends ToStringConsumer {
        @Override
        public void accept(OutputFrame outputFrame) {
            if (outputFrame != null && outputFrame.getBytes() != null)
            log.info(new String(outputFrame.getBytes(), Charset.forName("UTF-8")));
        }
    }
}
