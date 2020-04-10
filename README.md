# Simple url-shortener service
Simple realization of UrlShortener service (only add URL and redirect requests).
Runtime needs PostgreSQL database accessed by default at localhost:5432 (this can be reconfigured in properties) 

## Running
0. Start PostgreSQL: docker run --name pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:12
1. Run Spring Boot application `./mvnw spring-boot:run`
2. Create short url. You need file [local JSON request file](./create-req.json) for this step. 
Send request `curl -X POST -d "@create-req.json" -H "Content-Type: application/json" http://localhost:8080/create`
This will receive short URL in a form of: `{"shortUrl":"http://localhost:8080/hnh"}`
3. Send request `curl -v http://localhost:8080/hnh` and you will get HTTP 301 redirection to the URL defined in `longUrl` field of create-req.json file.

## Packing application in Docker
1. Run in root project folder
 mvn package -DskipTests && docker build -t me/shortener .
2. Check availability: 
docker run --rm --name app -p8080:8080 -e spring_datasource_url=jdbc:postgresql://<your_ip>:5432/postgres me/shortener

## Integration tests
Run tests in IDEA
src\test\java\com\example\shortener\IntegrationTests.java