# Audyt: ncLEAN CRM pod nclean.pl/crm/ — 14.11.2026

**Decyzja: technicznie możliwe bez subdomeny; aktualny kandydat NIE jest gotowy
na wdrożenie pod `/crm/`.** Wymagane jest dostosowanie adresów i izolacji PWA.
Nie wdrażano aplikacji na nclean.pl i nie połączono produkcyjnej bazy.

Badany kod: `feature/ncleanek-floating`, commit `e9fab47`. Ten audyt aktualizuje
dokumentację i przykłady konfiguracji; nie zmienia działania aplikacji ani jej
migracji. Przykłady nginx są materiałem do przeglądu po naprawie adresów.

## Co ze zdjęcia pozostaje aktualne

| Element | Stan potwierdzony w bieżącym repozytorium |
| --- | --- |
| Java/Spring | Java 17, Spring Boot 3.5.16, Spring MVC |
| Baza | PostgreSQL 17; lokalny Docker Compose i oddzielne bazy Testcontainers |
| Dostęp do danych | JPA/Hibernate oraz JdbcTemplate; większość modułów CRM zapisuje przez JdbcTemplate |
| Migracje | Flyway V1–V15; Hibernate waliduje schemat, nie zastępuje migracji |
| Frontend | Thymeleaf, JavaScript i CSS, bez Reacta |
| Logowanie | Spring Security, OWNER, sesja i hash BCrypt; CSRF pozostaje włączone |
| Budowanie | Maven Wrapper, także `mvnw.cmd` dla Windows |
| Cennik i kategorie | W PostgreSQL, edytowane przez aplikację; początkowe 6 kategorii/17 usług w migracji SQL V3. Opis „katalog w JSON” jest nieaktualny |
| Podgląd HTML | Zapis symulowany w pamięci otwartego pliku; odświeżenie resetuje dane. localStorage prawdziwej aplikacji służy preferencjom, nie bazie klientów |
| Wygląd | Stały wygląd graficzny ncLEAN; Montserrat i pixelowa typografia. Opis „klasyczny domyślny / eksperyment” był nieaktualny |

Właściwe wdrożenie uruchamia proces Java i osobną bazę PostgreSQL. Pobrany HTML
jest demonstracją interfejsu, nie wydaniem serwerowym.

## Docelowy układ, zgodny z wymaganiem właściciela

| Adres publiczny | Obsługa |
| --- | --- |
| `https://nclean.pl/` i istniejące publiczne ścieżki | Dotychczasowy CMS |
| `https://nclean.pl/crm/` i jego ścieżki | Oddzielny proces CRM przez istniejący reverse proxy |
| `127.0.0.1:8081` | Prywatny port procesu Java na tym samym hoście |
| Prywatny PostgreSQL CRM | Oddzielna baza/role/sekrety/kopie; bez współdzielenia tabel CMS |

Nie wymaga to subdomeny, połączenia obu projektów w jeden JAR, współdzielonego
logowania ani zmiany publicznego `/`. Potrzebna jest rzeczywista obsługa prefixu
w aplikacji oraz reguła `/crm/` w istniejącym serwerze HTTPS.

## Co potwierdzono w uruchomionym środowisku

Własny, odizolowany PostgreSQL, ten sam skompilowany CRM, profile `local,prod`,
`server.servlet.context-path=/crm`, nginx 1.28 i lokalny HTTPS. Publiczna strona
była kontrolowanym stubem — nie był to prawdziwy CMS ani serwer nclean.pl.
Certyfikat był lokalny i samopodpisany; jego akceptacja w przeglądarce dotyczyła
wyłącznie próby. To nie sprawdzenie certyfikatu produkcyjnego.

- Publiczne `/` i `/oferta` pozostały obsługiwane przez stub CMS (HTTP 200).
- `/crm` → `/crm/` (308); anonimowe `/crm/calendar` → `/crm/login` (302).
- Właściciel zalogował się; kalendarz został wyrenderowany po stronie serwera.
- Autoryzowany `/crm/help/context`: 200. Zapis bez CSRF: 403.
- Sesja `NCLEAN_CRM_SESSION`: Path `/crm`, Secure, HttpOnly, SameSite=Strict.
- Backendowy `/crm/js/removal.js`: poprawny zasób 200.
- W widoku kalendarza pozostało **30 odwołań do głównego `/`** (zasoby i linki).
  Przykłady: `/css/crm.css`, `/js/records.js`, `/calendar`, `/orders`.
  Zamiast CRM trafiały do stubu publicznej strony, wywołując w tej próbie
  **22 błędy JS**. To wynik błędnego routingu w próbie `/crm`, nie 22 niezależne
  błędy normalnego działania pod `/`.
- Manifest nadal deklaruje id/scope `/` i start_url `/calendar`; rejestracja
  workera w app.js wskazuje `/sw.js`, a jego offline `/offline.html`.
- Po ręcznym zarejestrowaniu obecnego workera pod poprawnym prefiksem usunął on
  **obcy cache kontrolny publicznej części strony**: obecny przed aktywacją,
  nieobecny po aktywacji. Powodem jest kasowanie wszystkich nazw poza własną.
  Zawężenie samego zakresu URL workera nie izoluje globalnego Cache Storage.

Na niestandardowym porcie HTTPS próby konieczne było przekazanie
`X-Forwarded-Port` i odczyt przez zaufany loopback Tomcata; inaczej powrót po
logowaniu zmieniał port, a CSP blokowało formularz. Po poprawieniu konfiguracji
próby logowanie przeszło **bez wyłączania CSP lub innych zabezpieczeń**.
To dodatkowy element odbioru konfiguracji proxy, szczególnie na stagingu.

Dowód: [same-domain-path-runtime.json](../docs/audit/same-domain-path-runtime.json).

## Blokery i niezbędne poprawki

1. **Adresy aplikacji.** Thymeleaf `@{...}` dla zasobów i linków, wspólna obsługa
   bazowej ścieżki w JS, względne adresy CSS, bez ręcznie wpisanego `/crm`
   w każdym module. Dopasować także sprawdzanie `location.pathname`, URL-e
   zwracane w JSON, obrazy, eksporty, przekierowania i formularze dynamiczne.
   Sam `<base>`, `context-path`, `X-Forwarded-Prefix` lub proxy_pass nie naprawia
   literalnych adresów zaczynających się od `/`.
2. **PWA i cache.** Manifest/start/ikony/scope i rejestracja workera muszą pozostać
   w `/crm/`. Worker pobiera właściwy plik offline i czyści wyłącznie swój
   namespace cache. Aktualizacja CRM nie może usuwać cache CMS ani przejmować
   publicznej strony. Nie przekierowywać głównych `/sw.js`, `/css` i `/js` do CRM.
3. **Źródło wydania.** Po pobraniu aktualnego main z GitHuba: `7d26eb0`, kandydat
   `e9fab47` był 51 commitów dalej, bez brakujących commitów main. Pełny CRM
   nie jest jeszcze w main. Nie wdrażać main z założeniem, że zawiera te funkcje.
4. **Rzeczywisty serwer.** System, nginx/Apache, reguły istniejącego CMS, porty,
   domena/certyfikat, sposób startu, dostęp VPN i role PostgreSQL nie zostały
   potwierdzone. Potrzebny odbiór stagingu na faktycznym hostingu, kopia/
   odtworzenie V15, monitorowanie i sposób wycofania wydania.

## Granice audytu

Ponowne `mvnw verify`: **38 unit/MVC + 93 PostgreSQL/Testcontainers = 131**,
zero failures/errors/skipped, `BUILD SUCCESS`. Potwierdza to funkcje i bazę
w dotychczasowym trybie uruchomienia, nie gotowość wszystkich modułów pod `/crm`.
Nowa próba ścieżki ma wynik negatywny, opisany wyżej.

`nginx -t` przeszedł dla nowych trzech przykładów konfiguracji `/crm` przy
zachowaniu publicznego `/` w kontrolowanym vhoście. Sprawdzenie składni nie
jest wdrożeniem ani potwierdzeniem poprawności obecnego frontendu.

Publiczny odczyt `https://nclean.pl/` został zablokowany przez proxy środowiska:
`CONNECT tunnel failed, response 403`. Nie należy przypisywać tego błędu stronie
nclean.pl. Nie potwierdzono aktualnego repo/konfiguracji CMS; dostępny był tylko
archiwalny raport przesłanego kodu z 28.10.2026. Brak aktualnego skanu CVE,
fizycznego telefonu i odbioru docelowego hostingu nie jest oznaczony jako zaliczony.

## Kolejny etap i kryterium zgody na wdrożenie

Dostosować prefix bez zmiany reguł biznesowych i danych. Testować równolegle `/`
oraz `/crm/`, następnie jeden HTTPS z publicznym stubem, a na końcu staging
rzeczywistego CMS. Zalogować się, dodać/edytować/usunąć/przywrócić zlecenie,
zmienić cennik i obszary, przesłać zdjęcie/rachunek, zamknąć dzień, wyeksportować,
zrestartować i wylogować. Network nie może pokazywać żądań CRM poza `/crm/`;
cache publicznej strony oraz dane po restarcie pozostają. Wtedy można ocenić
konkretne wydanie do uruchomienia na `https://nclean.pl/crm/`.
