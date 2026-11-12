# CRM na własnym serwerze z VPN — przygotowanie wdrożenia

To konfiguracja do przeglądu, nie wykonane wdrożenie. System operacyjny, adres VPN, usługi PostgreSQL/nginx, DNS i połączenie do serwera użytkownika nie są dostępne w tej sesji. Przykład zakłada Linux z systemd i nginx na tym samym hoście co CRM. Dla innego układu trzeba jawnie zmienić reguły zaufania proxy; nie rozszerzać ich na dowolne adresy.

## Rozdzielenie od strony

- `nclean.pl`: dotychczasowy CMS, bez zmian w tej pracy.
- `crm.nclean.pl`: osobny vhost, proces i konto systemowe `nclean-crm`.
- CRM nasłuchuje wyłącznie na `127.0.0.1:8081`; PostgreSQL nie ma publicznego portu.
- Dostęp do vhosta CRM początkowo wyłącznie przez VPN. Przykład nginx wiąże się z loopbackiem `127.0.0.1:8443`, aby skopiowanie go nie wystawiło aplikacji. Po sprawdzeniu firewalla operator zastępuje bind faktycznym adresem interfejsu VPN i portem443.
- Osobna baza, role, sekrety, kopie i sesja CRM. Nie współdzielić tabel, kont administratora, kluczy ani cookies domenowych z CMS. Samo użycie Java 17/PostgreSQL po obu stronach nie oznacza wspólnej bazy.
- Pierwsze połączenie strony z CRM może być zwykłym odnośnikiem do logowania. Przekazywanie zgłoszeń przez API jest następnym, oddzielnie projektowanym etapem; tutaj nie dodano API publicznego, CORS ani obejścia logowania.

## Przed uruchomieniem

1. Potwierdzić aktualny kod CMS i jego mapowanie portów, użytkownika systemowego, konfigurację proxy i bazy. Raport `AUDYT-NcleanCMS.md` dotyczy archiwum z 28.10.2026, nie bieżącej produkcji.
2. Podłączyć serwer przez VPN, potwierdzić Linux/Java 17, zasoby, wolne porty i odizolowaną testową bazę. Nie używać adresów ani poświadczeń bazy CMS.
3. W czystym checkoutcie kandydata uruchomić `./mvnw -B -ntp clean verify`. Testcontainers wymaga Dockera; testy nie korzystają z produkcji.
4. Przygotować bazę CRM i role. Rola aplikacji powinna mieć prawa odczytu/zapisu tabel i sekwencji, rola migracji DDL; schemat i domyślne granty muszą zapewnić aplikacji prawa do tabel tworzonych przez migratora. Nie uruchamiać aplikacji jako superużytkownik PostgreSQL. Rozdzielenie ról na docelowym serwerze wymaga sprawdzenia; w lokalnym Compose `crm_dev` jest właścicielem wyłącznie swojej bazy.
5. Utworzyć osobny użytkownik systemowy. JAR kopiować jako niezmienny artefakt wydania do `/opt/nclean-crm/releases/<SHA>/nclean-crm.jar`; `/opt/nclean-crm/current` wskazuje zatwierdzone wydanie. Sprawdzić sumę SHA256 po przesłaniu.
6. Na serwerze, poza checkoutem, utworzyć `/etc/nclean-crm/secrets.properties` według szablonu: właściciel `root:nclean-crm`, tryb0640. Własny login i hash BCrypt wygenerować lokalnie; nie przesyłać haseł przez czat i nie umieszczać ich w argumentach procesu. Wypełnione pliki nie trafiają do Git.
7. Zapewnić poprawny certyfikat dla subdomeny. Certyfikat z próby audytowej jest jednodniowy i lokalny — nie stanowi certyfikatu wdrożeniowego.
8. Przejrzeć jednostkę systemd i pliki nginx, dopasować ścieżki. `nginx-crm.conf.example` wchodzi do kontekstu `http`, a fragment proxy do `/etc/nginx/snippets/nclean-crm-proxy.conf`. Sprawdzić `nginx -t` i `systemd-analyze verify`; dopiero potem operator instaluje/uruchamia usługę.

Profil `prod` wymaga Secure cookie, przyjmuje nagłówki proxy jedynie od loopbacka i działa za HTTPS. nginx zastępuje nagłówki przekazane przez klienta, ogranicza logowanie do 5 żądań/min/IP z krótkim burst, ogranicza rozmiar żądania do 9 MiB i pomija query string w access logu. Limit obejmuje GET i POST `/login`; przy wspólnym wyjściowym IP VPN trzeba ocenić wygodę tego ustawienia.

## Próba odbiorcza na stagingu

- Poprawny HTTPS/certyfikat, przekierowanie logowania, sesja Secure/HttpOnly/SameSite=Strict, brak Domain cookies współdzielonego z CMS.
- Anonimowy użytkownik nie odczytuje CRM ani zdjęć; brak tokenu CSRF blokuje zapis; wielokrotne próby logowania otrzymują429.
- Nowy i powracający klient, zachowanie ręcznego adresu, dwie kategorie usług, zmiana ceny, zapis i edycja.
- Osiem zleceń, kilka obszarów, kolory i historia dezaktywacji przy 360/390/768/1440px.
- Wykonanie, rzeczywista chemia, zdjęcia, dwie notatki, wpłata częściowa, zamknięcie dnia i korekta po zamknięciu.
- Rzeczywisty telefon i jego klawiatura/aparat. Obsługa HEIC nie jest zaimplementowana; JPEG/PNG mają limit8 MiB i20 MP.
- Licznik/agregat i tankowanie nie dublują kosztu zużycia paliwa. Pusty odczyt i niepotwierdzona cena nie stają się zerem.
- Restart i pełna próba kopii/odtworzenia na osobnej bazie, wraz ze zdjęciami, wpłatami i zamknięciem dnia.

## Kopie, obserwacja i wycofanie

- Przed pierwszym użyciem ustalić harmonogram, retencję, zaszyfrowane miejsce kopii poza serwerem i osobę sprawdzającą ich wykonanie. Kopia zawiera dane klientów i prywatne zdjęcia. Lokalny dump i pomyślne odtworzenie nie zastępują kopii poza serwerem.
- Monitorować dostępność logowania, błędy5xx, miejsce PostgreSQL/backupu, ważność certyfikatu i stan procesu. Nie wysyłać danych klientów do zewnętrznego monitoringu bez osobnej decyzji.
- Zdjęcia są BLOB-ami w PostgreSQL: limit 40 zdjęć/zlecenie i8 MiB/plik oznacza do 320MiB na zlecenie. Zaplanować miejsce i czas wykonywania kopii według rzeczywistego wykorzystania.
- Przed zmianą wydania wykonać kopię i potwierdzić jej odtworzenie. Nie wykonywać Flyway clean, nie edytować wykonanych migracji i nie usuwać wolumenu.
- Przy wycofaniu nie zakładać, że stary JAR obsłuży nowy schemat. Sprawdzić zgodność albo odtworzyć kopię do osobnej bazy i uruchomić zgodne wydanie na jej adresie. Nie nadpisywać jedynej aktualnej bazy; uwzględnić zapisy wykonane po kopii.

Konfiguracja CI została dodana, ale wymaga opublikowania gałęzi i wykonania przez GitHub Actions. Ustawić wymagany wynik `verify` przed scalaniem. W tej sesji przeszły lokalne testy; nie deklarujemy wykonania workflow na GitHubie ani wdrożenia na serwerze użytkownika.
