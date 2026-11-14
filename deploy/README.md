# Docelowe wdrożenie: https://nclean.pl/crm/

**Bez subdomeny. Obecny kod nie jest jeszcze gotowy pod `/crm/`.**
[Audyt zgodności i wynik prawdziwej próby HTTPS](SAME-DOMAIN-AUDIT.md).
Samo dodanie `context-path` lub reguły nginx nie wystarcza: część zasobów,
linków, żądań JavaScript i PWA nadal korzysta z głównego `/`.

## Układ docelowy

- `https://nclean.pl/` i dotychczasowe adresy publiczne: istniejący CMS.
- `https://nclean.pl/crm/`: osobny proces CRM, dostępny przez ten sam serwer WWW.
- Java 17/CRM nasłuchuje na loopbacku `127.0.0.1:8081`.
- Oddzielna baza PostgreSQL, role, sekrety, kopie i konto systemowe `nclean-crm`.
- Własna sesja `NCLEAN_CRM_SESSION`, Secure/HttpOnly/SameSite=Strict,
  ze ścieżką `/crm`; logowanie OWNER i CSRF pozostają włączone.
- Przed przełączeniem zachować istniejącą konfigurację CMS, certyfikat i routing.
  Nie zastępować `location /`, nie współdzielić bazy ani kont właściciela.

Aktualnego systemu/proxy/portów ani konfiguracji produkcyjnego nclean.pl nie
potwierdzono. Przykłady dotyczą nginx na tym samym Linuxie co CRM. Dla Apache
operator musi przygotować i przetestować równoważną konfigurację.

## Pliki do przeglądu

- `nginx-crm-path.http.conf.example`: format logów bez query string i limit
  logowania; kontekst `http`.
- `nginx-crm-path.locations.conf.example`: trzy lokalizacje do istniejącego
  vhosta HTTPS `nclean.pl`. `^~ /crm/` chroni routing zasobów przed regułami CMS.
- `nginx-crm-path-proxy.conf.example`: fragment proxy, docelowo
  `/etc/nginx/snippets/nclean-crm-path-proxy.conf`. Zachowuje prefix w URI.
- `nclean-crm.service.example`: konto systemowe, restart i ograniczenia procesu.
  Po naprawie ścieżek wymaga dopisania do ExecStart
  `--server.servlet.context-path=/crm` oraz
  `--server.tomcat.remoteip.port-header=X-Forwarded-Port`.
- `secrets.properties.example`: szablon, nigdy wypełniony plik w Git.

Starsze `nginx-crm.conf.example` i `nginx-crm-proxy.conf.example` dokumentują
archiwalny wariant subdomenowy. **Nie używać ich jako docelowego wdrożenia.**
Nowych przykładów również nie należy instalować przed zaliczeniem bramki poniżej.

## Bramka wdrożeniowa

1. Dostosować wszystkie adresy serwera, Thymeleaf, JS, CSS i PWA do prefixu;
   zachować również lokalne uruchomienie w `/`. Nie obchodzić tego masowym
   przekazywaniem głównych `/login`, `/css`, `/js`, `/sw.js` do CRM.
2. Przetestować pod jednym HTTPS: publiczne `/` i oferta, `/crm/`, logowanie,
   OWNER/CSRF, wszystkie moduły, obrazy, rachunki, pliki, usuwanie/przywracanie,
   wylogowanie, sesję i restart. Brak żądań CRM poza `/crm/`.
3. Zawęzić manifest i service worker do `/crm/`; sprzątanie Cache Storage może
   usuwać tylko cache CRM. Cache publicznej strony musi przeżyć aktualizację CRM.
4. Potwierdzić rzeczywisty serwer WWW, certyfikat, firewall/VPN i wolne porty.
   Port Java i PostgreSQL mają pozostać prywatne. Operator ustala, czy `/crm/`
   będzie dostępny tylko przez VPN, czy przez Internet z normalnym logowaniem.
5. Wybrać sprawdzony commit pełnej implementacji. `main` był starszy o 51 commitów
   od badanego `e9fab47`; samo wdrożenie main nie zawiera obecnego CRM.
   Przygotowanie wydania/scalenie wymaga oddzielnego przeglądu, bez automatycznego
   zastępowania produkcji w tym audycie.
6. Na izolowanym stagingu wykonać `./mvnw -B -ntp verify` (Windows: `mvnw.cmd`),
   próbę na fizycznym telefonie i kopię/odtworzenie aktualnego schematu V15 wraz
   ze zdjęciami, wpłatami i usuniętymi wpisami. Uruchomić aktualny skan zależności.
7. Ustalić kopie poza serwerem, retencję, miejsce na zdjęcia i logi, monitoring
   dostępności/dysku/certyfikatu oraz operatora odpowiedzialnego za odtworzenie.

## Proces, baza i wycofanie

Wydanie przechowywać niezmiennie w `/opt/nclean-crm/releases/<SHA>/nclean-crm.jar`;
`current` wskazuje wybraną wersję. Sprawdzić SHA-256 po przesłaniu.
Sekrety dostarczyć poza checkoutem w `/etc/nclean-crm/secrets.properties`,
`root:nclean-crm`, tryb0640. Login i BCrypt wygenerować lokalnie. Nie podawać haseł
w argumentach procesu i nie uruchamiać aplikacji jako superużytkownika PostgreSQL.
Rola migracji i rola aplikacji muszą mieć uzgodnione, sprawdzone granty tabel
oraz sekwencji. Lokalny Compose jest środowiskiem developmentu.

Przed zmianą wykonać kopię i sprawdzić jej odtworzenie do osobnej bazy. Zachować
wcześniejszy JAR i konfigurację proxy. Nie zakładać zgodności starego JAR-a z nowym
schematem; uwzględnić zapisy po kopii. Nie używać Flyway clean ani usuwać wolumenu.
Zdjęcia są w PostgreSQL; limity 40×8 MiB/zlecenie wymagają planu pojemności.

To dokumentacja i lokalny audyt. Nie zmieniono serwera, CMS, DNS, certyfikatów
ani bazy produkcyjnej. Nie utworzono Pull Requesta i nie scalono gałęzi.
