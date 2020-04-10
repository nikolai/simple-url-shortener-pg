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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class IntegrationTests {


    public static final String HTTP_RAMBLER_RU = "http://rambler.ru";
    private RestTemplate restTemplate = new RestTemplate();
    private RestTemplate restTemplateNoRedirect = new RestTemplate(new SimpleClientHttpRequestFactory() {
        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            connection.setInstanceFollowRedirects(false);
        }
    });

    public static final String MYDB_HOSTNAME = "mydb";
    Network network = Network.newNetwork();

    @Rule
    public PostgreSQLContainer postgreSQLContainer = (PostgreSQLContainer) new PostgreSQLContainer("postgres:12")
            .withNetwork(network)
            .withNetworkAliases(MYDB_HOSTNAME);

    int hostRandomPort = ThreadLocalRandom.current().nextInt(10000, 60000);

    @Rule
    public GenericContainer app = new FixedHostPortGenericContainer("me/shortener")
            .withFixedExposedPort(hostRandomPort, 8080)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("spring_datasource_url", "jdbc:postgresql://"+MYDB_HOSTNAME+":5432/postgres")
            .withEnv("spring_datasource_username", postgreSQLContainer.getUsername())
            .withEnv("spring_datasource_password", postgreSQLContainer.getPassword())
            .withEnv("short_url_context", "http://localhost:" + hostRandomPort)
            .waitingFor(Wait.forHttp("/heartbeat"))
            .withLogConsumer(new ToStringConsumer() {
                @Override
                public void accept(OutputFrame outputFrame) {
                    if (outputFrame != null && outputFrame.getBytes() != null)
                        System.out.println(new String(outputFrame.getBytes(), Charset.forName("UTF-8")));
                }
            })
            ;

    @Test
    public void test_create_and_redirect() {
        CreateRedirectRequest createRequest = new CreateRedirectRequest(HTTP_RAMBLER_RU);

        String url = "http://localhost:" + app.getMappedPort(8080) + "/create";
        HttpEntity<CreateRedirectRequest> httpEntity = new HttpEntity<>(createRequest);
        CreateRedirectResponse createResponse = restTemplate.postForObject(url, httpEntity, CreateRedirectResponse.class);

        assertThat(createResponse, notNullValue());
        assertThat(createResponse.getShortUrl(), notNullValue());


        ResponseEntity<Object> httpResp = restTemplateNoRedirect.exchange(createResponse.getShortUrl(), HttpMethod.GET, null, Object.class);
        assertThat(httpResp.getStatusCodeValue(), is(301));
        assertThat(httpResp.getHeaders().getLocation() + "", is(HTTP_RAMBLER_RU));
    }

    @Test
    public void test_db_disconnect() {
        test_create_and_redirect();

        postgreSQLContainer.getDockerClient().stopContainerCmd(postgreSQLContainer.getContainerId()).exec();

        try {
            test_create_and_redirect();
            Assert.fail("Сервис должен быть недоступен при недоступности БД");
        } catch (Exception e) {
            // ok
        }
        postgreSQLContainer.getDockerClient().startContainerCmd(postgreSQLContainer.getContainerId()).exec();

        test_create_and_redirect();
    }

//    @Test
    public void test_db_available() throws SQLException {
        String jdbcUrl = postgreSQLContainer.getJdbcUrl();
        String username = postgreSQLContainer.getUsername();
        String password = postgreSQLContainer.getPassword();

        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);

        ResultSet resultSet = connection.createStatement().executeQuery("SELECT 154");

        resultSet.next();
        int result = resultSet.getInt(1);
        Assert.assertEquals(154, result);
    }



}
