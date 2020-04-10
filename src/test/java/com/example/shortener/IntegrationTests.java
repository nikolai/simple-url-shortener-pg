package com.example.shortener;

import com.example.shortener.messages.CreateRedirectRequest;
import com.example.shortener.messages.CreateRedirectResponse;
import org.junit.Assert;
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
import org.testcontainers.utility.ResourceReaper;

import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
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

    int hostRandomPort = ThreadLocalRandom.current().nextInt(10000, 60000);

    @Rule
    public GenericContainer app = new FixedHostPortGenericContainer("me/shortener")
            .withFixedExposedPort(hostRandomPort, 8080)
            .withEnv("spring_datasource_url", "jdbc:postgresql://" + MYDB_HOSTNAME + ":5432/postgres")
            .withEnv("spring_datasource_username", postgresContainer.getUsername())
            .withEnv("spring_datasource_password", postgresContainer.getPassword())
            .withEnv("server_port", "8080")
            .withEnv("short_url_context", "http://localhost:" + hostRandomPort)
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/heartbeat"));


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
    public void test_create_n_redirect() {
        CreateRedirectRequest createReq = new CreateRedirectRequest(HTTP_YANDEX_RU);

        HttpEntity<CreateRedirectRequest> request = new HttpEntity<>(createReq);
        String uri = "http://localhost:" + app.getMappedPort(8080) + "/create";
        CreateRedirectResponse response = restTemplate.postForObject(uri, request, CreateRedirectResponse.class);

        assertThat(response, notNullValue());
        assertThat(response.getShortUrl(), notNullValue());

        ResponseEntity<Object> httpResp = restTemplate2.exchange(response.getShortUrl(), HttpMethod.GET, null, Object.class);
        assertThat(httpResp.getStatusCodeValue(), is(HttpServletResponse.SC_MOVED_PERMANENTLY));
        assertThat(httpResp.getHeaders().getLocation() + "", is("http://yandex.ru"));
    }

    @Test
    public void test_disconnecting_db() {
        test_create_n_redirect();

        postgresContainer.getDockerClient().stopContainerCmd(postgresContainer.getContainerId()).exec();
        try {
            test_create_n_redirect();
            Assert.fail("request should fail while db is down");
        } catch (Exception e) {
            System.out.println("ok for failing this time: " + e);
        }
        postgresContainer.getDockerClient().startContainerCmd(postgresContainer.getContainerId()).exec();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        test_create_n_redirect();
    }

}
