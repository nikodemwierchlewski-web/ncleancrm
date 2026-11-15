# Dzisiaj, kalendarz, tankowania i przypomnienia

Wdrożono zaakceptowany zakres w istniejącym CRM na gałęzi
`feat/today-planning-fuel-reminders`. Commit kodu: `bc7c9bf`, baza: `d7398af`.
Bez PR, scalania, wdrożenia ani połączenia z produkcją nclean.pl.

## Obsługa

- **Dzisiaj**: główny cel z krokiem do odhaczenia, przychód i zysk za miesiąc
  lub tydzień, wykres słupkowy, lista wizyt oraz przypomnienia i przypięte wpisy.
  Skrót z liczbą zleceń przewija do planu. Osobnego podglądu obszarów tutaj nie ma.
- **Nawiguj**: przy każdej wizycie z adresem jest osobny przycisk z autorską
  grafiką 8-bit. Otwiera link Google Maps z adresem tej wizyty. Nie pobiera GPS.
  Brak adresu prowadzi do szczegółów w celu uzupełnienia. Przycisk nie uruchamia
  jednocześnie otwarcia karty zlecenia.
- **Kalendarz**: miesiąc w siedmiu kolumnach na telefonie i komputerze. Na
  telefonie dotknięcie dnia otwiera dolny panel; na komputerze aktualizuje panel
  obok miesiąca. Zachowano obszary, kolory, szczegóły, edycję i dodawanie wizyt.
  Nie ma domyślnej mobilnej listy dnia ani paska tygodnia powielających Dzisiaj.
- **Tankowanie**: data/godzina, przebieg, litry i cena. Kwota liczy się na bieżąco
  i ponownie na serwerze. Rachunek, rodzaj paliwa, przeznaczenie i pełny bak są
  w szczegółach. Historia oraz dotychczasowe odczyty samochodu/agregatu zostają.
- **Notatki i cele**: „Przypnij do Dzisiaj” działa niezależnie od dotychczasowego
  przypięcia do podsumowania. Główny cel ma własną kartę; aktywne przypomnienie
  zastępuje drugą widoczną kartę przypiętego wpisu. Przypomnienie głównego celu
  znajduje się wewnątrz jego karty.

Daty, godziny, kwoty i odczyty używają czytelnych cyfr, bez problematycznego
pixelowego glifu „5”. Ilustracje, obramowania i krótkie efekty pozostają 8-bit.
Ruch respektuje ustawienia systemowe, przełącznik aplikacji i widoczność karty.

## Finanse i paliwo

Przychód obejmuje wykonane, nieusunięte zlecenia. Korzysta z kwoty końcowej;
przy historycznym braku potwierdzonej kwoty pozostaje wycena i oznaczenie wyniku
jako orientacyjnego. Zaplanowane i anulowane wizyty nie zwiększają przychodu.
Wpłaty są osobnym rozliczeniem i nie zmieniają przychodu z wykonanej usługi.

Wynik to przychód minus zapisane koszty chemii, zużycia paliwa, marketingu,
koszty stałe i pozostałe koszty operacyjne. Nie jest wynikiem netto po podatkach.
Koszty miesięczne są rozdzielane na dni metodą zachowującą dokładną sumę miesiąca.
Brakujące ceny i koszty powodują widoczne oznaczenie „Wynik orientacyjny”.
Wykres wykorzystuje tę samą kalkulację co istniejące podsumowanie; wartości
słupków sumują się dokładnie do przychodu i wyniku okresu. Odczyty są pobierane
zbiorczo, bez osobnej kalkulacji/bazy zapytań dla każdego słupka.

Tankowanie dokumentuje zakup paliwa. Nie jest automatycznie dodatkowym kosztem
tej samej jazdy. Sama ilość zatankowana i pojedynczy licznik nie wyznaczają
rzeczywistego dziennego zużycia. Zachowano wcześniejsze pomiary i porównania
pełny–pełny. Odczyty agregatu są opcjonalne, nie dzielimy wspólnego rachunku
między urządzenia bez danych.

Kwota tankowania to litry × cena, zaokrąglona HALF_UP do groszy. Obsługiwany
jest przecinek. Opcjonalna kwota rachunku może skorygować wynik o maksymalnie
2 grosze; dawne niepełne rachunki nadal można edytować bez wymyślania pomiarów.
Zachowano cenę wpisaną za litr, walidację sąsiednich przebiegów, token zapisu,
wersję, historię zmian i dotychczasowe bezpieczne usuwanie/przywracanie.

## Przypomnienia

Harmonogramy: jednorazowo, codziennie, wybrane dni tygodnia i co 1–365 dni.
Obowiązuje Europe/Warsaw. „Zrobione na dziś” potwierdza konkretne wystąpienie;
nie odhacza celu ani nie kończy całego cyklicznego wpisu. Dostępne jest odłożenie
o 15 minut, godzinę lub trzy godziny. Trwałe zakończenie i archiwizacja wpisu
wyłączają jego przypomnienia.

Pokazywane jest ostatnie należne wystąpienie, bez serii zaległych animacji.
Potwierdzenia i pierwsze pokazanie są zapisane w PostgreSQL; dwie karty nie
przyznają sobie ponownie animacji. Zmiana samej treści nie resetuje harmonogramu.
Zmiana harmonogramu otrzymuje osobną rewizję; stare żądanie nie odkłada nowego
wystąpienia. Godziny przy zmianie czasu mają jednoznaczną reprezentację.

To przypomnienia wewnątrz aplikacji, odświeżane po otwarciu, powrocie do karty
oraz co minutę podczas używania. Nie dodano SMS, e-maili ani systemowego push.
Potwierdzenie wyświetlenia ma semantykę najwyżej raz: przerwane połączenie po
potwierdzeniu może pominąć samą animację, ale nie usuwa przypomnienia.

## Weryfikacja

| Sprawdzenie | Rzeczywisty wynik |
|---|---|
| Maven Wrapper / Java 17 | 57 testów jednostkowych, MVC i architektury + 113 PostgreSQL/Testcontainers; 0 błędów i 0 pominiętych |
| Migracje | V1–V18, upgrade zachowuje stare przypięcia, główny cel i kroki; nie tworzy przypomnień za użytkownika |
| Finanse | Zgodność sum słupków i podsumowań dziennych/okresowych, ujemny wynik, brakujące koszty, marketing jednorazowy/stały i brak podwójnego paliwa |
| Harmonogramy | Jednorazowe, dni tygodnia, interwały, granica dnia, DST wiosna/jesień, odłożenie, dwie karty, rewizje, archiwizacja i zachowanie historii |
| Chromium: 360/390/768/1440 px | Osiem wizyt, adres/nawigacja, cel i deduplikacja, miesiąc/tydzień, miesięczna siatka, dolny panel i fokus, powrót ze szczegółów, dwa obszary i zapis |
| Formularze | Błąd 503 i ponowienie bez kasowania danych, dwa tankowania, przecinek i zaokrąglenie; nieaktywne pola nie blokują zapisu po zmianie opcji |
| Zmiana szerokości w trakcie edycji | Brak niechcianego otwarcia mobilnego panelu po zamknięciu kolejnego formularza |
| Rzeczywisty restart | Osiem zleceń, cel/przypięcia, wykonane/odłożone przypomnienia oraz dwa tankowania pozostają zapisane |
| axe | 69 skanów, 0 naruszeń, także panel dnia, edytor celu/harmonogramu i szczegóły tankowania |
| HTTPS `/crm` | 60 kombinacji ekran/szerokość oraz rzeczywiste zapisy celu, przypomnienia, przypięcia, paliwa, historycznej wizyty i zdjęć; OWNER/CSRF oraz PWA zachowane |
| Regresje | Klienci, cennik, zdjęcia z błędem/ponowieniem i usuwaniem, koszty, cele, dokumentacja dnia i dane historyczne |
| HTML DEMO | Cztery szerokości, miesiąc/tydzień, kalendarz/panel, tankowanie/edycja/anulowanie/przywrócenie, przypinanie i przypomnienie, cel, ograniczenie ruchu i zagnieżdżony sandbox; 0 błędów JS |
| Eksport/import DEMO | Tankowanie, przypięcie i wykonanie przypomnienia wracają po przeładowaniu; błędny import zachowuje dotychczasową próbę |

To testy Chromium o podanych szerokościach, nie próba na fizycznym Androidzie.
Automatyczny axe nie zastępuje pełnej oceny z czytnikiem ekranu. Nie testowano
obciążenia serwera produkcyjnego ani nie wykonywano wdrożenia.

## Dane, architektura i aktualizacja

Nowa migracja `V18__today_notes_and_reminders.sql` dodaje niezależne przypięcie
oraz harmonogramy i historię wystąpień. Nie zmieniano wykonanych V1–V17, nie
kasowano zleceń, zdjęć, starych cen, rachunków ani notatek. Do aktualizacji użyj
istniejącej [procedury backup/update/restore](../../deploy/ops/RUNBOOK.md).
Archiwalna paczka administratora V1–V16 nie zawiera tej aktualizacji.

TodayController komponuje istniejące modele. Kalkulacja finansowa pozostaje
w DashboardService, paliwo w FuelJournalService, a harmonogram i jego testowalna
logika dat w module notes. Kontrolery nie korzystają z JDBC. Testy architektury
pozostają aktywne. Adaptery HTML w `preview/support` nie trafiają do aplikacji
Spring Boot i nie zmieniają jej logowania ani dostępu do danych.

## Podgląd

[Klikalny HTML](../../preview/nclean-today-workspace.html) ·
[ZIP](../../preview/nclean-today-workspace.zip).

Plik powstał z 69 widoków prawdziwej aplikacji i bieżących modułów interfejsu,
z danymi syntetycznymi. Odświeżenie resetuje stan; menu `⋯` pozwala zachować
ćwiczenie w JSON, również tankowania i stan przypomnień, bez zdjęć i szkiców.
Nie wpisuj danych klientów. Wykresy pokazują uchwycony scenariusz i dostępne
okresy; DEMO nie przelicza pełnego rozliczenia po każdej zmianie przykładowych
zleceń. Prawdziwa aplikacja korzysta z PostgreSQL i przelicza wyniki na bieżąco.
W DEMO nawigacja opisuje otwarcie map; w CRM jest rzeczywistym linkiem do map.

W tej sesji nie ma narzędzia przekierowania portu ani sterowania pulpitem.
Nie potwierdzam działania panelu podglądu Codexa. HTML sprawdzono przez lokalny
HTTP i w dwóch ramkach sandbox bez `allow-same-origin`; nie podaję chmurowego
localhost jako adresu dostępnego na urządzeniu użytkownika.

[Metadane i sumy](build.json) · [axe](accessibility.json) · [DEMO](preview-results.json) ·
[Telefon — Dzisiaj](today-390.png) · [Komputer](today-1440.png) ·
[Kalendarz](calendar-month-390.png) · [Tankowanie](quick-fuel-390.png).

Powtarzalne testy: `./mvnw verify`, `node scripts/test-web-boundaries.cjs`,
`scripts/verify-browser.py --owner-file <prywatny-plik.json> --pixel-tycoon` oraz
`scripts/verify-today-preview.py` z lokalnym URL syntetycznego HTML. Sekrety i logi
startowe pozostają poza repozytorium.
