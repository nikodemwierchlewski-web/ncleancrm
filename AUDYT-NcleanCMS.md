# Audyt źródeł NcleanCMS

Data: 28.10.2026. Źródło: przekazane przez użytkownika `NcleanCMS-master.zip`, katalog główny `NcleanCMS-master/`.

SHA-256 archiwum: `80217C0DE765204B8AA1D67EF9D42C9AF0F8BF678D5C560E97DBD80C7500421E`.

To audyt statyczny przesłanego snapshotu. Archiwum nie zawiera metadanych `.git`; nie można przypisać SHA commita ani potwierdzić zgodności z aktualną produkcją. Nie uruchamiano CMS, skryptów, kontenerów, migracji ani wysyłki. Wartości sekretów nie są częścią raportu. Ścieżki i numery linii poniżej odnoszą się do plików w archiwum.

## Faktyczny stos

| Warstwa | Ustalenie | Dowód |
| --- | --- | --- |
| Backend | Java 17, Spring Boot 3.2.3, Maven | `pom.xml`: parent i java.version |
| WWW | Spring MVC + Thymeleaf, renderowanie po stronie serwera, dodatkowe endpointy REST | starter-web i starter-thymeleaf; controllers; templates |
| Frontend | JavaScript, Bootstrap ^5.3.3, Popper ^2.11.8, Sass ^1.71.0; brak Reacta w deklaracjach projektu | `package.json`, static/js, scss |
| Dane | PostgreSQL, Spring Data JPA/Hibernate; dodatkowo zadeklarowany Spring Data JDBC | `pom.xml`, `application.properties:3–6`, repozytoria dziedziczące JpaRepository |
| Dostęp | Spring Security, logowanie formularzem, konta i uprawnienia w bazie, BCrypt | WebSecurityConfig, MyUserDetailsService, Account/Authority, AccountService |
| Pozostałe | Spring Mail, Apache POI 5.2.3 do Excela, Guava 32.1.3-jre, jsoup 1.18.1, Jackson XML; Lombok | `pom.xml` |
| Zadania | Włączone async i scheduling, domyślna strefa Europe/Warsaw | `NcleanBlogAppApplication.java:10–18` |
| Budowanie obrazu | Node 20 dla SCSS, Maven/JDK 21 dla budowania, końcowy obraz Java 17.0.1; testy pomijane w Dockerfile | `Dockerfile:1–19` |

Nie ma potrzeby tworzenia React SPA, żeby zachować stos ncLEAN. Node służy tu do przygotowania CSS; obecność `package.json` nie oznacza aplikacji React.

## Struktura i odpowiedzialności

```text
pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
package.json, Dockerfile, docker-compose.yaml
src/main/java/com/example/Ncleanblogapp/
  NcleanBlogAppApplication.java
  config/        konfiguracja WWW, Security i inicjalizacja danych
  controllers/   16 klas: strona, blog, panel CMS, oferty, konta, mailing, Excel
  services/      17 klas: logika domenowa, pliki, poczta, logowanie
  repositories/ 12 interfejsów dostępu do bazy
  models/        27 klas bezpośrednio w katalogu; dodatkowo podkatalog Enums
  dto/           obiekty danych dla widoków/API
  event/, eventListeners/, events/   zdarzenia poczty i uwierzytelniania
  errors/, exceptions/, exceptionsRestController/
  excel/         eksporty
src/main/resources/
  application.properties
  templates/     nclean.html, blog, panel, formularze i fragmenty Thymeleaf
  static/        CSS, JS, obrazy i fonty
  scss/          źródła stylów
src/test/.../NcleanBlogAppApplicationTests.java
uploads/
```

Przepływ jest typowy dla MVC: kontroler → serwis → repozytorium JPA → PostgreSQL, następnie model i szablon Thymeleaf albo odpowiedź REST. To CMS z blogiem, ofertami, formularzami i mailingiem, a nie gotowy operacyjny CRM. Modele newslettera nie zastępują klientów i historii realizacji usług. Część kodu obsługuje także drugi serwis; nie przenosić jej automatycznie do CRM.

## Baza i konfiguracja uruchomienia

- `application.properties:2`: port 9000. Linie 3–6: generowanie DDL i `ddl-auto=update`, jawny dialekt PostgreSQL. Nie znaleziono migracji Flyway/Liquibase ani odpowiednich zależności.
- Archiwum zawiera jeden `application.properties`; nie znaleziono osobnych profili dev/test/prod. W nim nie ma `spring.datasource.url/username/password`. Faktycznego adresu, poświadczeń, wersji serwera i połączenia produkcyjnego nie ustalono.
- Cały `docker-compose.yaml` jest zakomentowany. Opisuje PostgreSQL i aplikację, w tym zmienne Spring datasource, lecz nie jest działającą deklaracją usług w tym snapshotcie. Obraz bazy w przykładzie nie ma przypiętej wersji.
- Archiwum zawiera `.env`. Nie skopiowano go do szkieletu ani pakietu wynikowego. Sama obecność `.env` nie dowodzi jego wczytywania przez Spring. Nie ustalono rzeczywistego mechanizmu dostarczenia datasource na serwerze.
- `spring.main.allow-circular-references=true` dopuszcza cykle beanów. Security ma poziom DEBUG, SMTP ma konfigurację debugowania. Nie przenosić tego jako ustawień domyślnych nowej aplikacji.
- Testy w snapshotcie ograniczają się do jednego `contextLoads`; nie potwierdzają zachowania kontroli dostępu, migracji ani operacji biznesowych.

## Ustalenia bezpieczeństwa

Priorytety dotyczą kodu z archiwum. Nie wykonywano prób wykorzystania problemów, logowania ani testów na stronie. CORS nie zastępuje uwierzytelnienia endpointów.

### P0 — wpisane dane uwierzytelniające

`src/main/resources/application.properties` zawiera niepuste literalne dane SMTP, w tym hasło (`spring.mail.password`). `config/SeedData.java:77–100` przy braku określonego konta tworzy je z wpisanym w kod hasłem i nadaje ROLE_USER; `AccountService.java:25–28` hashuje je przez BCrypt przed zapisem. Hashowanie w bazie nie usuwa ryzyka znajomości hasła z kodu.

Nie wiadomo, czy dane SMTP albo konto są aktualne na produkcji. Jeżeli były kiedykolwiek używane, traktować je jako ujawnione odbiorcom źródeł: osobno zaplanować rotację hasła SMTP i hasła konta, sprawdzenie sesji oraz usunięcie sekretów z bieżących plików i historii. Sama zmiana SeedData nie zmienia hasła już istniejącego konta. Żadnej rotacji tutaj nie wykonano.

`.dockerignore` i `.gitignore` nie wykluczają `.env`. Dockerfile kopiuje cały kontekst do etapów budowania, więc plik może trafić do tych etapów i ich cache; nie oznacza to automatycznie, że znajduje się w końcowym obrazie. Hasło w `application.properties` jest dodatkowo zasobem aplikacji. W CRM wszystkie sekrety mają pochodzić z konfiguracji środowiska.

### P1 — mailing uruchamiany publicznie

`WebSecurityConfig.java:81` dopuszcza POST `/narko-mailing/send**` przez `permitAll`; ten sam obszar jest wyłączony z CSRF w linii 149. `MultimailController.java:25–28` przyjmuje żądanie i wywołuje serwis, bez widocznej kontroli użytkownika. `MultiMailingService.java:100–124` pobiera odbiorców z bazy, kolejkuje wysyłkę, używa tytułu i treści z żądania oraz zapisuje adresy odbiorców w logu.

W tym kodzie anonimowy klient może uruchomić wysyłkę, jeśli endpoint jest osiągalny i poczta działa. Wymaga osobnej poprawki CMS: uwierzytelnienie, odpowiednia rola, CSRF dla panelu, limity i audyt bez adresów. Nie kopiować tego przepływu do przyszłych SMS.

### P1 — reguła końcowa dopuszcza pozostałe żądania

`WebSecurityConfig.java:119` kończy konfigurację przez `anyRequest().permitAll()`. Wymienione wcześniej adresy panelu mają ochronę ról USER/ADMIN, ale pominięta trasa nie otrzymuje automatycznie ochrony. Kontrolery zawierają `@PreAuthorize`, lecz w źródłach nie znaleziono `@EnableMethodSecurity` ani `@EnableGlobalMethodSecurity`. Nie można polegać na tych adnotacjach jako aktywnej dodatkowej barierze w tym snapshotcie.

W `pom.xml` jest też starter Spring Data REST, a repozytoria są publicznymi interfejsami JPA. Istnieje ryzyko niezamierzonej publikacji zasobów repozytoriów; szczególnie kont i danych mailingowych. Zasięg ekspozycji i pola trzeba potwierdzić lokalnym testem z syntetycznymi danymi. Nie twierdzimy, że wykonano odczyt takich danych z produkcji. Nowy CRM nie ma zależności Data REST i domyślnie wymaga roli OWNER.

### P1 — operacje usuwania przez GET

`PostController.java:308` i `NcleanAdditionalPagesController.java:175` mapują usuwanie przez GET. Są to operacje panelu, ale GET nie powinien zmieniać danych i nie jest standardowo chroniony tokenem CSRF. Zastąpić je formularzami POST/DELETE, zachować kontrolę uprawnień i dodać testy. W CRM zapis oznaczeń dnia już używa POST.

### P2 — pliki i treść HTML wymagają osobnego przeglądu

`FileService.java:32–34,40–47,83–92,100–111` buduje ścieżki z nazw plików klienta. W tych metodach nie ma sprawdzenia po normalizacji, że wynik pozostaje wewnątrz katalogu uploadów, ani kontroli rzeczywistego typu i wielkości pliku. Trzeba sprawdzić wszystkie wywołania i ograniczenia na poziomie żądania, zanim przesądzi się zdalną wykorzystywalność. CRM początkowo nie potrzebuje uploadów.

Kilka szablonów używa `th:utext` do HTML. Samo to nie dowodzi XSS; przed ponownym użyciem należy sprawdzić, kto zapisuje treść i czy przechodzi właściwą sanitację. Zwykłe notatki klienta w CRM wyświetlać przez escapowany tekst.

### P2 — limit prób logowania i proxy

Jest rzeczywisty mechanizm limitu: `LoginAttemptService.java:17,25,44–58` ustala próg 10 i cache z czasem wygasania jednego dnia. Jednak odczyt IP ufa `X-Forwarded-For`, a listener zliczający błędy ma inną logikę wyboru adresu (`AuthenticationEventListener.java:28–40`). Skuteczność zależy od zaufanego proxy i wymaga testów. Cache jest lokalny dla procesu i nie zapewnia wspólnego limitu wielu instancjom.

### Co już jest w kodzie

- BCrypt do haseł, konta i role z bazy, sprawdzanie flagi `account.isEnabled()` podczas logowania.
- Logowanie formularzem pod `/login`, wylogowanie z unieważnieniem sesji.
- CSRF włączone z repozytorium sesyjnym, ale z wyłączeniami dla kilku endpointów pocztowych i obsługi danych newslettera.
- Maksymalnie jedna sesja według konfiguracji, polityka tworzenia sesji ALWAYS. Efektywnego zachowania sesji nie testowano.
- CORS ograniczony do dwóch domen, lecz wartości zawierają końcowy `/`; konfigurację porównać z rzeczywistym nagłówkiem Origin i potrzebami integracji. Nie stosować CORS jako mechanizmu ochrony publicznego mailingu.
- Nie potwierdzono wymuszania HTTPS i flag cookie w konfiguracji serwera; mogą być ustawiane poza tym repozytorium.

## Wnioski dla osobnego CRM

Zachować rodzinę technologii: Java 17, Maven, Spring Boot, Spring MVC, Thymeleaf, Security, JPA/Hibernate i PostgreSQL. Nie kopiować literalnie historycznych wersji runtime, inicjalizacji kont, konfiguracji SMTP, publicznych reguł ani automatycznych zmian schematu `update`.

Szkielet CRM został dopasowany do Java 17 i repozytoriów JPA. Zachowuje odrębną bazę, Flyway oraz `ddl-auto=validate`, wyłączone Open Session in View, obowiązkowe logowanie OWNER i CSRF. Wersja Boot w szkielecie jest osobnym wyborem do potwierdzenia przez build, nie wersją wykrytą w CMS. Brak integracji z CMS i produkcją.

Plan funkcji i kolejność implementacji są w `PLAN-CRM.md`. Pierwszy następny krok: uruchomić build oraz testy MVC i PostgreSQL w osobnym środowisku. Audyt jest statyczny; Java, Maven i Docker nie są dostępne jako polecenia w tym środowisku. Nie potwierdzono kompilacji, renderowania ani migracji.
