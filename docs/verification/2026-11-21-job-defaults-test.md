# TEST — standardy zleceń i użycie sprzętu

Punkt wyjścia: `nclean-v0.1-test.3` (`9eb40dd`), nowszy od FINAL `.1`.
Gałąź: `feat/job-equipment-usage`. Wydanie: `nclean-v0.1-test.4`.

- Notatka do nowego zlecenia jest wymagana także dla dawnej realizacji.
  Formularz pokazuje ją bez rozwijania sekcji; serwer odrzuca brak i same spacje
  bez częściowego zapisania klienta lub wizyty. Dawnych wpisów nie uzupełniamy.
- Imię jest opcjonalne; przy jego braku nazwą klienta zostaje telefon.
- Przy zapisie wykonania brak pomiaru chemii przyjmuje prognozę zapisaną
  w zleceniu wraz z historycznymi cenami. Standard jest oznaczony jako szacunek
  i nie zasila propozycji uczących się z rzeczywistych pomiarów.
- Bez własnego czasu oraz pomiaru ze zdjęć/timera przyjmujemy standard usługi.
  Nowe rezerwacje zachowują jego kopię. Przy pierwszym uzupełnieniu starszej
  wizyty zapisujemy zgodny jednostkowo obecny standard z jawnym źródłem.
- Liczniki sprzętu mają osobne standardy minut na jednostkę usługi, kopie
  w rezerwacji, korekty i jawne zero. Starsze wizyty wymagają przygotowania
  szacunku. Agregat ma niezależny czas, bez sumowania podłączonych urządzeń.
- Tylko wykonane, nieusunięte wizyty zwiększają szacowane użycie; ponowienie
  nie dodaje godzin drugi raz. Odczyt narastający obejmuje wcześniejsze dni
  i swój dzień, więc szacunek dodaje tylko późniejsze dni. Nie zmieniamy pomiaru.
- Jedyna nowa migracja: V23. V1–V22, zdjęcia, historyczne ceny, konfiguracja
  Dockera, `.local` i istniejące wolumeny pozostają zachowane.

Weryfikacja jest wykonywana wyłącznie na nowym, izolowanym PostgreSQL
z syntetycznymi danymi. Nie łączono się z produkcją ani bazą właściciela.

## Wyniki

- Java 17, `./mvnw verify`: 227 testów (74 jednostkowe/architektury/uprawnień,
  153 integracyjne PostgreSQL), 0 błędów, porażek i pominięć.
- Chromium na rzeczywistym Spring Boot: `/crm` — 24 widoki; `/` — 28 widoków,
  szerokości 360/390/768/1440 px. Dodatkowo formularz dodawania w wariancie `/`.
  W obu wariantach brak imienia, wymaganie notatki w HTML i na serwerze,
  zachowanie danych po błędzie, standardy i własne pomiary, godziny sprzętu,
  utrata odpowiedzi/ponowienie, offline, konflikt wersji, CSRF, klawiatura
  i ograniczenie animacji przeszły bez błędów JavaScript.
- Migracje z V12/V20/V21/V22, zamrożone ceny, zdjęcia, odczyty i przywracanie
  historii pozostają objęte testami. Sprawdzono też brak częściowego zapisu
  po odrzuceniu notatki oraz brak automatycznego nadpisania dawnych wpisów.

Nie wykonano wdrożenia. Instalacja PC wymaga backupu i przebudowy obrazu
z nowego wydania, z zachowaniem istniejącego podłączenia PostgreSQL,
zewnętrznego wolumenu `ncleancrm-pc-data` i `.local`.
