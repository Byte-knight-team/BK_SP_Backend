# BK_SP_Backend

Backend services for the BK Software Project (restaurant system).

Overview
- This repository contains the backend service module `resturarent-system`, a Spring Boot application that implements the restaurant system API and business logic.

Repository layout
- `resturarent-system/` — main Spring Boot service (contains `pom.xml`, `mvnw`, and source under `src/`).
- `docs/` — design notes and backend explanation documents.
- `util/` — utility classes used by the service (for example `QrCodeGenerator.java`).

Prerequisites
- Java 11 or newer (use the JDK matching your environment).
- Git (to clone the repo).
- Maven is not required locally because the project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).

Quick start (development)
1. Open a terminal at the repository root.
2. Change into the service folder:

	 - Unix/macOS:

		 ```bash
		 cd resturarent-system
		 ./mvnw spring-boot:run
		 ```

	 - Windows (PowerShell / CMD):

		 ```powershell
		 cd resturarent-system
		 .\mvnw.cmd spring-boot:run
		 ```

3. The service starts on the port configured in `src/main/resources/application.properties` (or `target/classes/application.properties` after a build).

Build and run (production)
- Build JAR:

	```bash
	cd resturarent-system
	./mvnw -DskipTests package
	```

- Run the produced JAR:

	```bash
	java -jar target/*.jar
	```

Configuration
- Application configuration lives in `src/main/resources/application.properties` (checked into source in development). When running a packaged artifact the file is available under `target/classes`.
- Common overrides:
	- `SPRING_PROFILES_ACTIVE` — select an active Spring profile.
	- Database connection settings — update the `spring.datasource.*` properties as needed.

Testing
- Run unit and integration tests with:

	```bash
	cd resturarent-system
	./mvnw test
	```
 PR.

Maintainers
- For questions about the backend, contact the repository owner or the backend team listed in the project documentation.

License

---

