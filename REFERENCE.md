PROJECT BRIEFING: Java CLI Weather App — Task Allocation & Engineering Standards

Target JDK: Java 25 LTS
Build Tool: Apache Maven

ENGINEERING PROTOCOLS

1. Branching Strategy:
   - Direct commits to 'main' are restricted.
   - All work must be conducted on designated feature branches.
   - Sync local branches prior to PR submission: git pull origin main

2. Credential Security:
   - Do not commit real API keys or modify 'config.properties.example' with secrets.
   - Configure local credentials in 'src/main/resources/config.properties' (ignored by Git).

3. Quality Gate:
   - All code must pass local verification before opening a Pull Request:
     mvn clean test
   - Zero compiler warnings and zero failing tests are required for merge approval.

RESPONSIBILITY 

ROLE 1: Network & API Client Engineer
- Branch: feat/api-client
- Scope:
  * src/main/java/com/princeblue/weather/config/AppConfig.java
  * src/main/java/com/princeblue/weather/client/ApiClient.java
  * src/main/java/com/princeblue/weather/client/OpenWeatherClient.java
  * src/test/java/com/princeblue/weather/client/OpenWeatherClientTest.java
- Deliverables:
  * Implement AppConfig to load api.key via classpath.
  * Implement OpenWeatherClient using java.net.http.HttpClient.
  * Map HTTP status codes (200, 401, 404, 5xx) to domain exceptions.
  * Implement unit tests with simulated HTTP responses.

ROLE 2: Data Modeling & JSON Parsing Specialist
- Branch: feat/models-and-mapper
- Scope:
  * src/main/java/com/princeblue/weather/model/Weather.java
  * src/main/java/com/princeblue/weather/model/Location.java
  * src/main/java/com/princeblue/weather/service/WeatherMapper.java
  * src/main/java/com/princeblue/weather/exception/*
  * src/test/java/com/princeblue/weather/service/WeatherMapperTest.java
- Deliverables:
  * Define immutable domain models (Weather, Location).
  * Implement exception hierarchy constructors.
  * Implement WeatherMapper using Gson to deserialize external payloads.
  * Implement unit test suite verifying parser robustness against edge cases.

ROLE 3: Core Service & Business Logic Developer
- Branch: feat/weather-service
- Scope:
  * src/main/java/com/princeblue/weather/service/WeatherService.java
  * src/test/java/com/princeblue/weather/service/WeatherServiceTest.java
- Deliverables:
  * Implement WeatherService utilizing constructor-based dependency injection.
  * Enforce input sanitization (validate non-blank city parameters).
  * Orchestrate retrieval: ApiClient -> WeatherMapper -> Weather domain model.
  * Implement unit tests using Mockito to mock client interactions.

ROLE 4: CLI Interface & Application Coordinator
- Branch: feat/console-ui
- Scope:
  * src/main/java/com/princeblue/weather/ui/ConsoleUI.java
  * src/main/java/com/princeblue/weather/Main.java
- Deliverables:
  * Implement interactive console interface, output formatting, and input loop.
  * Intercept domain exceptions to present structured, user-friendly terminal feedback.
  * Assemble composition root in Main.java (dependency wiring and runtime bootstrap).

INTEGRATION & MERGE SEQUENCE

To resolve cross-package dependencies without blocking:
1. Stage 1: Merge 'feat/models-and-mapper' (Core data structures & exceptions)
2. Stage 2: Merge 'feat/api-client' (HTTP transport layer)
3. Stage 3: Merge 'feat/weather-service' (Business logic & orchestration)
4. Stage 4: Merge 'feat/console-ui' (Application entry point & interface)