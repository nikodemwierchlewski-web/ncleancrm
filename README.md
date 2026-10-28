# ncLEAN CRM — osobny szkielet

Status: kod startowy do dalszej pracy, bez wdrożenia. Po audycie przekazanego ZIP dopasowano Java 17 oraz JPA/Hibernate do technologii CMS. CMS ma Spring Boot 3.2.3; osobny CRM deklaruje 3.5.16 i wymaga weryfikacji buildu. Raport: ../AUDYT-NcleanCMS.md. Nie potwierdzono kompilacji ani działania: w środowisku brak poleceń Java/Maven i dostępu do bazy testowej.

## Co zawiera

- Java 17, Maven, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Security.
- PostgreSQL i Spring Data JPA/Hibernate jak w CMS, migracja Flyway V1 oraz walidacja schematu. Wyłączone Open Session in View. Brak zależności Spring Data REST, więc repozytoria nie są automatycznie publikowane jako API.
- Logowanie właściciela z loginem i hashem BCrypt przekazanymi przez środowisko; bez domyślnego hasła i rejestracji publicznej. Jedno konto w pamięci jest rozwiązaniem początkowym.
- Widok miesiąca i trwały zapis jednego obszaru na dzień; zmiana i usuwanie oznaczenia, nazwa obok koloru, układ mobilny.
- Pięć przykładowych obszarów operacyjnych. To nie deklaracja aktualnego podziału administracyjnego. Nazwy i kolory mają docelowo być edytowalne, również na poziomie osiedli.
- CSRF włączone; formularze Thymeleaf używają `th:action`. Zapis i wylogowanie przez POST. Wszystkie funkcje wymagają roli OWNER.
- Testy MVC dostępu anonimowego, roli, CSRF, renderowania i zapisu. Baza nie jest objęta tymi testami.

## Uruchomienie lokalne

1. Zainstaluj JDK 17 i Maven 3.6.3+; sprawdź dostępność zadeklarowanego wydania Spring Boot przed pierwszym buildem.
2. Utwórz NOWĄ pustą bazę PostgreSQL `nclean_crm` i osobnego użytkownika, którego uprawnienia obejmują tylko tę bazę. Nie podłączaj bazy strony.
3. Ustaw zmienne procesu opisane w `.env.example`. Sam plik `.env` nie jest automatycznie wczytywany. Nie wstawiaj danych logowania w poleceniach zapisywanych w historii.
4. Wygeneruj lokalnie hash silnego hasła przez `BCryptPasswordEncoder(12).encode(...)`, np. w tymczasowym lokalnym narzędziu Java. Przekaż go jako `CRM_ADMIN_PASSWORD_HASH`; nie commituj hasha ani hasła.
5. W katalogu projektu uruchom `mvn test`, następnie `mvn spring-boot:run`.
6. Wejdź na http://127.0.0.1:8081. Migracja Flyway uruchomi się przy starcie i utworzy tabele w wskazanej bazie.

Brak wymaganych zmiennych ma przerwać start. Flyway nie chroni przed wskazaniem złej bazy: bezpieczeństwo zapewnia osobna baza i konto bez dostępu do CMS.

## Struktura

```text
src/main/java/pl/nclean/crm/
  CrmApplication.java
  security/SecurityConfiguration.java
  calendar/CalendarController.java
  calendar/CalendarService.java
  calendar/DistrictEntity.java, DayDistrictEntity.java
  calendar/DistrictRepository.java, DayDistrictRepository.java
src/main/resources/
  application.yml
  application-prod.yml
  db/migration/V1__calendar.sql
  templates/calendar/month.html
src/test/java/pl/nclean/crm/calendar/CalendarSecurityTest.java
```

To modularny monolit. Rozwijaj pakiety `customer`, `job`, `catalog`, `chemical`, `expense`, `report`, później `message` i `consent`. Kontrolery przyjmują formularze, serwisy realizują transakcje, dostęp do danych należy wydzielać do repozytoriów wraz ze wzrostem modułów.

## Ograniczenia przed wykorzystaniem z rzeczywistymi danymi

Brak CRUD klientów/zleceń/usług, edytora obszarów, audytu zmian i ochrony przed utratą równoczesnej edycji. Brak limitowania prób logowania, kont bazodanowych, odzyskiwania hasła, kopii zapasowych i testów integracyjnych PostgreSQL. Domyślnie dostęp tylko z localhost; profil prod ustawia Secure cookie, lecz nie stanowi kompletnej konfiguracji wdrożenia. Wymagane HTTPS, osobna domena/host i przegląd reverse proxy. Brak SMS, inteligentnych terminów, GPS i integracji z produkcją.

Szczegółowy plan oraz gotowe zadania: `../PLAN-CRM.md`.
