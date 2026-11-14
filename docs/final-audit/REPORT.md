# Audyt i naprawa kandydata ncLEAN CRM do pilotażu

14.11.2026, Europe/Warsaw. Baza zmian: `87cb61f`, gałąź
`audit/final-pilot-hardening`. [Prompt](PROMPT.md), [wyniki](results.json),
[instrukcja administratora](../../deploy/ops/RUNBOOK.md).

## Werdykt i zakres

Naprawiono znane blokery adresowania `/crm`, izolacji PWA, przekierowania po
logowaniu, niepełnych danych dawnych wizyt oraz przygotowano narzędzia ochrony
zapisu podczas aktualizacji. Rzeczywisty Spring Boot i PostgreSQL były uruchamiane
w izolowanych próbach. Nie wykonywano wdrożenia na nclean.pl ani połączenia z CMS.

To kandydat do kontrolowanego pilotażu, nie gwarancja braku wszystkich przyszłych
błędów. Raport rozróżnia zweryfikowany kod od nieznanej infrastruktury i testów,
których to środowisko nie może zastąpić.

Przegląd obejmował wszystkie moduły: kalendarz/obszary, klientów, cennik/chemię,
realizacje/zdjęcia, kontakty i wyszukiwanie, zamknięcie dnia, podsumowanie,
paliwo/agregat, koszty/sprzęt, notatki, ncLEANKA, animacje, bezpieczeństwo,
architekturę, migracje, konfigurację i procedury operacyjne. Użyto istniejących
modeli i aplikacji. Nie dodano Reacta, SMS, GPS ani drugiego CRM.

## Ustalenia i zastosowane poprawki

| ID | Priorytet | Wykryte zachowanie | Naprawa / dowód |
| --- | --- | --- | --- |
| F01 | Wysoki | Linki, zasoby i JS wychodziły z `/crm/` do publicznego CMS | Thymeleaf URL expressions, względne CSS i wspólny `routes.js`; normalizacja istniejącego klienta sesji, linków dynamicznych i JSON |
| F02 | Wysoki | Worker rejestrował root i usuwał obce cache domeny | Rejestracja i manifest względem kontekstu; cache z namespace i ścieżką instalacji; sprzątanie wyłącznie własnych wersji, bez prywatnych odpowiedzi |
| F03 | Wysoki | Wejście bez sesji na login pobierało API postępu; login potrafił wracać do surowego JSON | Brak odczytu postępu na stronie dostępu i jawny powrót do kalendarza; rzeczywista próba HTTPS/login/logout |
| F04 | Wysoki | Dawnej realizacji nie można było zapisać bez wymyślonego imienia/telefonu | Jawny wpis historyczny, V16, klient nullable wyłącznie w historii, snapshot kontaktu, brak fikcyjnej kartoteki; zwykłe umawianie nadal wymaga danych |
| F05 | Średni | Dopisanie dawnych realizacji mogłoby wywołać dawny obowiązek zamknięcia i odznaki | Wpis historyczny DONE, bez wywołania osiągnięcia; pominięcie w liczniku i wyszukiwaniu zaległego dnia; jawne braki pomiarów pozostają brakami |
| F06 | Wysoki | Sam nowy atrybut booking mógł zmienić fingerprint dawnych zamknięć | Kanoniczny fingerprint pomija nowy techniczny atrybut; test przejścia dawnych schematów i zachowania zamknięcia |
| F07 | Wysoki | Brak kompletnego narzędzia aktualizacji z obowiązkowym backupem | `crm_ops.py`: tożsamość bazy, blokada, kontrola JAR/SHA256, stop/backup/przełączenie/start; brak usuwania danych lub sekretów |
| F08 | Wysoki | Sama obecność pliku dump nie potwierdzała odtworzenia | Sprawdzenie formatu i SHA256; restore wyłącznie do nowej bazy; próba z binarnymi zdjęciami i rachunkiem, cenami i notatkami |
| F09 | Wysoki | Automatyczny rollback po migracji mógł uruchomić niezgodny stary kod | Po błędzie nowego startu usługa pozostaje zatrzymana; kopia/wydania zostają do analizy. Błąd backupu restartuje stare wydanie przed migracją |
| F10 | Średni | Część pomocniczych linków kosztów i podsumowania działała tylko przy root | URL expressions także dla warunkowych linków z danymi; testy istniejących scenariuszy i przegląd szablonów |
| F11 | Średni | Dekoracja kalendarza zabierała miejsce liście dnia | Zachowana grafika i styl, mniejsza dekoracja kalendarza i czytelniejszy tekst kart |
| F12 | Średni | Pasek zapisu zasłaniał część dotykowego przycisku pomocy adresu | Osobne obszary formularza: przewijane pola i nieruchoma stopka, bez nakładania; ponowione axe i test formularza |
| F13 | Średni | Podgląd HTML nie przenosił stanu ćwiczenia po odświeżeniu | Jawny eksport/import danych próbnych JSON, z opisem zakresu i walidacją; nadal brak połączenia demonstracji z bazą produkcyjną |

## Dane i migracje

- Migracje V1–V15 nie zmienione, żadna istniejąca rezerwacja nie jest kasowana.
- V16 dodaje `historical=false` do dotychczasowych rekordów i pozwala na brak
  kartoteki/adresu wyłącznie dla jawnej historii. Ceny i zdjęcia pozostają takie same.
- Nieznane finalne kwoty, chemia i czas nie są wypełniane zerami ani prognozami.
  Cena w formularzu jest wyceną; końcową można osobno potwierdzić w wykonaniu.
- Rezerwacje, ceny, zdjęcia i rachunki są w PostgreSQL. Dane nie zależą od katalogu
  konkretnego JAR-a. Aktualizacja aplikacji nie usuwa wolumenu bazy.
- Backupy i wszystkie dane syntetycznych prób są poza Git. Paczka nie zawiera
  sekretów, konfiguracji lokalnych, dumpów ani danych klientów.

## Weryfikacja

- Maven verify: 50 testów jednostkowych/MVC/architektury i 101 integracyjnych
  PostgreSQL, bez failures/errors/skipped. Zachowano testy granic modułów.
- Nowe regresje: zwykłe vs historyczne umawianie, idempotencja, brak fikcyjnego
  klienta, brak dawnych przypomnień i osiągnięć (również po dopisaniu historii do zamkniętego dnia), zdjęcie po restarcie, rzeczywisty
  servlet context `/crm`, zasoby/JSON/session/CSRF i migracje dawnych kopii.
- Node: adresy `/`, `/crm/`, zagnieżdżony kontekst i obce URL-e; Cache Storage
  CMS i drugiej instalacji nie są usuwane przez worker.
- Chromium: pełne dotychczasowe przepływy pracy, 8 zleceń, kilka kategorii,
  korekta ceny/adresu, 503/ponowienie, konflikt kart, notatki, zdjęcia/czas,
  koszty, zamknięcie dnia i restart, cztery szerokości 360/390/768/1440.
- HTTPS nginx + publiczny stub + realny CRM `/crm`: 15 ekranów przy każdej
  szerokości, logowanie, historyczny zapis bez kartoteki, upload i odczyt zdjęcia,
  eksport, CSRF403, cookie, wylogowanie, scope/cache PWA. Zero żądań CRM poza
  `/crm/` i brak błędów JS. Certyfikat testowy, nie certyfikat produkcyjny.
- Operacje: prawdziwy upgrade V15→V16, restart Java i odtworzenie do nowej bazy;
  porównanie snapshotów i skrótów obrazów, ochrona przed złą sumą, istniejącym
  celem, błędem backupu i błędem readiness. Sterowanie systemd w próbie zastępował
  adapter uruchamiający prawdziwy proces Java; usługi nie instalowano na serwerze.
- Dostępność: automatyczny axe-core WCAG2/2.1/2.2 AA i best-practice, wyniki
  szczegółowe w results.json / accessibility.json. To nie zastępuje oceny manualnej.
- Samodzielny HTML: rzeczywiste wyrenderowane widoki z syntetycznych scenariuszy,
  klikanie, formularze, usuwanie/cofnięcie, ustawienia i animacje, cztery szerokości
  oraz zagnieżdżony sandbox czatu. DEMO jawnie symuluje zapis; nie jest bazą CRM.

## Obsługa nowych poprawek

W kalendarzu `+ Zlecenie` → `Wpisuję dawną realizację` → wcześniejszy dzień.
Możesz pozostawić pusty kontakt i adres. Zapis powstaje jako wykonana historyczna
wizyta; zdjęcia i znane szczegóły uzupełnisz w jej widoku. Bez zaznaczenia historii
obowiązuje dotychczasowe umawianie klienta. Nie łączymy osób po samym telefonie.

W podglądzie opcje `⋯` pozwalają eksportować/wczytać próbę JSON. Eksport obejmuje
zlecenia próbne, notatki, oznaczenia dni i koszty, bez zdjęć i szkiców. Nie jest
backupem PostgreSQL i nie służy do gromadzenia rzeczywistych danych klientów.

## Pozostaje do odbioru administratora

1. Aktualny NcleanCMS i faktyczny deploy: dokładne repo, system/proxy/porty,
   certyfikat, role, firewall/VPN i dostępna pojemność. W sesji brak dostępu do
   repo xazi091/NcleanCMS, a archiwalny audyt nie potwierdza aktualnej produkcji.
2. Zastosowanie konfiguracji i grantów na stagingu właściwego serwera; test
   publicznego CMS, fizyczny Android, galeria/aparat i zachowanie klawiatury.
3. Backup poza serwerem, harmonogram/retencja/szyfrowanie/alerty i odpowiedzialny
   operator. Dostarczono narzędzia i instrukcję, nie skonfigurowano usług hostingu.
4. Aktualny skan CVE zależności: nie ma potwierdzenia dostępu do bieżącego
   advisory API w ograniczonej sieci. Nie ogłaszamy zależności wolnymi od podatności.
5. Testy obciążenia na reprezentatywnej liczbie zdjęć/zleceń i pojemności serwera.
   Bieżące testy funkcjonalne nie są testem dużego ruchu produkcyjnego.
6. Stopniowy podział fasady CrmRecords i typowanie projekcji zgodnie z planem
   architektury. Aktualny etap nie usuwa całego istniejącego długu technicznego.

Brak Pull Requesta, scalenia i publikacji produkcyjnej. Dostępność panelu podglądu
platformy raportujemy osobno od sprawdzenia działania aplikacji i pliku HTML.
