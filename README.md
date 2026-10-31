# ncLEAN CRM — lokalny kalendarz

Pierwsza wersja obsługuje logowanie właściciela, kalendarz miesiąca i kolorowe oznaczenie dzielnicy przy dniu. Przypisania są zapisywane w PostgreSQL i pozostają po restarcie aplikacji. To osobna aplikacja i baza, bez połączenia z produkcyjnym CMS nclean.pl.

Stos: Java 17, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Security, JPA/Hibernate, PostgreSQL 17 i Flyway. Maven Wrapper pobiera Maven 3.9.11 i sprawdza jego SHA-256. Nie ma Reacta ani osobnej aplikacji SPA.

## Windows — pierwsze uruchomienie

Zainstaluj JDK 17, ustaw `JAVA_HOME` i uruchom Docker Desktop z kontenerami Linux oraz Compose v2. W PowerShell, w katalogu repozytorium:

```powershell
.\scripts\Initialize-Local.ps1
.\scripts\Start-Local.ps1
```

Inicjalizator poprosi o własne hasło konta `local-owner`, wygeneruje hasła bazy i zapisze konfigurację tylko w ignorowanym katalogu `.local/`. Jawne hasło właściciela nie jest zapisywane; aplikacja otrzymuje hash BCrypt. Pliki nie są nadpisywane przy kolejnym uruchomieniu.

Po starcie otwórz http://127.0.0.1:8081 i zaloguj się. Pełna instrukcja, sprawdzenie zapisu po restarcie i rozwiązywanie problemów: [docs/WINDOWS.md](docs/WINDOWS.md). Skrypty PowerShell wymagają osobnej weryfikacji na rzeczywistym Windows; w tym środowisku sprawdzono inicjalizator Java, Compose i aplikację na Linux.

## Linux/macOS — pierwsze uruchomienie

Wymagane są JDK 17, ustawione `JAVA_HOME`, Docker i Compose v2. Z katalogu repozytorium, w interaktywnym terminalu:

```sh
./mvnw dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=.local/runtime-classpath.txt
"${JAVA_HOME}/bin/java" -cp "$(cat .local/runtime-classpath.txt)" scripts/InitializeLocal.java
docker compose up -d --wait
./mvnw spring-boot:run -Dspring-boot.run.profiles=local '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://127.0.0.1:55433/nclean_crm --spring.flyway.url=jdbc:postgresql://127.0.0.1:55433/nclean_crm'
```

Inicjalizator wymaga terminala, aby ukryć wpisywane hasło. Przy kolejnych uruchomieniach wystarczą dwa ostatnie polecenia.

## Lokalna baza i konfiguracja

- Compose uruchamia PostgreSQL wyłącznie pod `127.0.0.1:55433`, z nazwanym wolumenem `ncleancrm-local_postgres-data`.
- Aplikacja używa bazy `nclean_crm` i konta `crm_dev`, bez uprawnień superużytkownika, tworzenia baz i ról. Osobne konto `crm_bootstrap` służy do pierwszej inicjalizacji kontenera.
- Hasła bazy są przekazywane do Compose z lokalnych plików sekretów. Kontrola gotowości sprawdza rzeczywiste połączenie konta `crm_dev`, a nie tylko otwarty port.
- Profil `local` wymaga `.local/application-local.properties` i ustawia lokalny adres bazy. Nie kopiuj do tego środowiska poświadczeń CMS. Pliki `.env` nie są automatycznie wczytywane przez Spring; [env.example](env.example) dokumentuje zmienne dla ręcznej konfiguracji.
- Flyway uruchamia istniejącą migrację V1 przy pierwszym starcie. Tworzy `district`, `day_district` i pięć przykładowych obszarów. Hibernate tylko waliduje schemat (`ddl-auto=validate`); Flyway `clean` jest wyłączony.
- Logowanie wymaga loginu oraz poprawnego hasha BCrypt. Operacje zapisu i wylogowania wymagają POST z CSRF; odczyt kalendarza wymaga roli OWNER.

Zatrzymanie aplikacji przez `Ctrl+C` zachowuje dane. `docker compose down` usuwa kontener, ale zachowuje wolumen; `docker compose up -d --wait` korzysta z zapisanej bazy. **`docker compose down -v` usuwa dane** — nie używaj go do zwykłego restartu. Nie usuwaj również plików `.local/`, jeśli chcesz dalej korzystać z istniejącej bazy i tych samych haseł.

## Testy

```sh
./mvnw test          # 5 testów MVC, bez Dockera i połączenia z bazą
./mvnw clean verify  # dodatkowo 5 testów integracyjnych, wymaga działającego Dockera
```

W Windows użyj `.\mvnw.cmd`. Testcontainers automatycznie tworzy oddzielny PostgreSQL z losowymi poświadczeniami. Testy nie korzystają z lokalnych sekretów ani zewnętrznych adresów bazy. Brak działającego Dockera powoduje niepowodzenie etapu integracyjnego, zamiast pomijania testów.

Sprawdzane są: migracja V1 i dane początkowe, prawdziwe logowanie HTTP, renderowanie kalendarza, dodanie/zmiana/usunięcie przypisania dzielnicy, walidacja, CSRF oraz zachowanie zapisu po zamknięciu i ponownym uruchomieniu aplikacji z tą samą bazą. Wyniki XML znajdują się w `target/surefire-reports` i `target/failsafe-reports`.

## Struktura

```text
compose.yaml, pom.xml, mvnw, mvnw.cmd
docker/postgres/           inicjalizacja lokalnej bazy i konta aplikacji
scripts/                   inicjalizator sekretów i skrypty Windows
src/main/java/pl/nclean/crm/
  calendar/                kontroler, serwis, encje i repozytoria
  security/                logowanie i uprawnienia
src/main/resources/
  application.yml, application-local.yml, application-prod.yml
  db/migration/V1__calendar.sql
  templates/calendar/month.html
src/test/java/pl/nclean/crm/calendar/
  CalendarSecurityTest.java
  CalendarPostgresIT.java
```

## Zakres

Obszary są przykładowymi strefami operacyjnymi; nazwa jest wyświetlana razem z kolorem. Zmiana oznaczenia dnia nie rezerwuje terminu. Nie ma jeszcze zleceń, kartoteki klientów, SMS, finansów, GPS ani inteligentnego dobierania terminów.

To lokalne środowisko deweloperskie. Publiczne wdrożenie, HTTPS, kopie zapasowe i konta użytkowników w bazie pozostają osobnymi zadaniami. Profil `prod` nie jest kompletną konfiguracją wdrożenia. Dokumenty historyczne: [audyt CMS](AUDYT-NcleanCMS.md) i [plan CRM](PLAN-CRM.md).
