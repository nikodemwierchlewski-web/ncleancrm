# Audyt architektury i wykonany etap naprawy

Data: 14.11.2026. Punkt odniesienia: `b5b646b` (pełna implementacja istniejących
funkcji). Gałąź: `refactor/modular-crm-foundation`. Bez Pull Requesta i bez
połączenia z produkcją. [Prompt zastosowany w tym etapie](REFACTOR-PROMPT.md).

## Ocena

Aplikację można rozwijać jako modularny monolit. Przepisywanie całości lub
mikroserwisy zwiększyłyby teraz złożoność. Przed zmianą granice były zbyt słabe:
kontroler zapisywał SQL, domeny używały dashboardu jako biblioteki narzędziowej,
a koordynator usuwania znał reguły i tabele siedmiu rodzajów danych.

Zastosowano pierwszy etap rozdzielenia i automatyczne zabezpieczenia przed
powrotem tych problemów. To fundament rozbudowy, nie stwierdzenie, że cały dług
techniczny został usunięty.

## Ustalenia i zmiany

| Priorytet | Ustalenie przed zmianą | Zastosowana poprawka / dalsza decyzja |
| --- | --- | --- |
| Wysoki | `ExportController` wykonywał SQL bez filtra `removed_at`, mimo odwracalnego usuwania zleceń | `ExportQueries`, zapytanie bez usuniętych rezerwacji, regresja eksportu i przywrócenia |
| Wysoki | Edycja profilu i źródła klienta była rozdzielona między usługę i transakcyjny kontroler z SQL | Jeden `CustomerProfileCommand`, transakcja w usłudze, test pełnego rollbacku i zachowania źródła |
| Wysoki | Cykle kodu biznesowego `dashboard ↔ costs` i `dashboard ↔ fuel` | `BusinessDates` / `DecimalInput`; dashboard pozostaje odbiorcą, bez zwrotnych zależności |
| Wysoki | Centralny `RemovalService` znał 7 tabel i reguły kosztów, chemii oraz rezerwacji | Rejestr domenowych handlerów, typowane polecenie/wynik, wspólny protokół blokad, audytu i wersji |
| Średni | Słownik klienta należał do ogólnego `context`, a używał go zapis rezerwacji | Własność `records.CustomerAcquisitionSource`, zachowane adaptery zgodności |
| Średni | `FuelController` interpretował `java.sql.Timestamp` | Konwersja w projekcji `FuelJournalService.purchaseForEdit` |
| Wysoki | Brak testów granic architektury | ArchUnit w Maven/CI: kontrolery bez dostępu do bazy i transakcji, shared niezależny, brak cykli logiki, usługi bez kontrolerów, niezależny koordynator usuwania i zakaz używania dashboardu jako biblioteki |
| Średni | `CrmRecords` łączy klientów, cennik i rezerwacje; wiele usług łączy SQL, walidację i raporty | Pozostaje jawny dług; kolejne wydzielenia przez zgodną fasadę, po jednym przypadku użycia |
| Średni | Część usług nadal rzuca `ResponseStatusException`, wiążąc reguły ze Spring HTTP | Docelowo wyjątki aplikacyjne i mapowanie w adapterach; nowy kontrakt usuwania ma własny wyjątek bez HTTP |
| Średni | Wiele wyników to `Map<String,Object>`, SQL działa ponad granicami pakietów | Nowe kontrakty są typowane; kolejne modele i własność tabel wymagają osobnych etapów |
| Średni | Gęsto zapisane klasy i duży przekrojowy test integracyjny | Nowe klasy czytelnie sformatowane; istniejące formatować i testy dzielić stopniowo, bez masowego przepisywania |
| Średni | Wiele skryptów ekranowych, różne obsługi fetch/błędu/CSRF i ścieżek | Plan wspólnego klienta HTTP i stanu ekranu; frontend zachowany, regresja na rzeczywistej aplikacji |
| Bloker wdrożenia | Ścieżki i PWA nie są gotowe do współistnienia z nclean.pl pod `/crm/` | Nadal otwarty osobny etap; [szczegóły](../../deploy/SAME-DOMAIN-AUDIT.md) |

## Dowody zależności

ArchUnit przeanalizował skompilowany kod przed/po, bez listy wyjątków dla
istniejących cykli. Przed: 53 klasy/typy logiki, wykryte 2 cykle; po: 74 typy,
0 cykli. Liczba obejmuje rekordy i typy zagnieżdżone, nie tylko pliki źródłowe.
Przyrost obejmuje jawne kontrakty i 7 implementacji usuwania; nie nowe funkcje CRM.
Pełny wynik: [dependency-evidence.json](dependency-evidence.json).

Graf wyłącza adaptery HTTP i konfigurację aplikacji (`ui`, `security`), ponieważ
składanie ekranu może używać kilku modułów. Osobne reguły nadal sprawdzają wszystkie
kontrolery i zabraniają zależności logiki od kontrolerów. Nie jest to dowód izolacji
tabel ani odrębnych baz — SQL i mapowe projekcje pozostają opisanym długiem.

Inwentaryzacja po etapie: 59 plików Java, 23 skrypty JS, 43 szablony HTML.
149 wierszy Java nadal przekracza 300 znaków. Największe skrypty dotyczą ncLEANKA,
animacji i formularzy. Nie zwiększamy rozmiaru pojedynczej klasy przez dokładanie
kolejnych niezwiązanych przypadków użycia. Nie wprowadzono nowego frameworka
frontendowego ani nowych migracji.

## Weryfikacja

Wyniki końcowe są zapisane w [validation.json](validation.json). Testy obejmują:

- Granice architektury, rejestrację nowego handlera bez zmiany koordynatora,
  wykrycie duplikatu i walidację polecenia; daty i liczby.
- Rzeczywisty PostgreSQL: atomowy profil i źródło klienta; brak częściowego nowego
  klienta przy błędzie; wycofanie usunięcia przy wymuszonym błędzie audytu;
  zakaz handlera bez transakcji; JSON, ponowienie, 404, CSRF i konflikt wersji.
- Eksport bez usuniętych zleceń i identyczny snapshot po przywróceniu.
- Dotychczasowe scenariusze V1–V15, historia cen, zamknięcie dnia, zdjęcia,
  chemia, paliwo, koszty, notatki i trwałość po restarcie.
- Chromium headless na prawdziwym Spring Boot + izolowanym schemacie PostgreSQL:
  360, 390, 768 i 1440 px; 8 zleceń, usunięcie/cofnięcie, nowy/powracający klient,
  dwie kategorie, cena i adres wizyty, błąd 503/ponowienie i konflikt dwóch kart,
  zamknięcie dnia, zdjęcia/czas, notatki, ograniczenie animacji i restart.

Pierwszy nowy test eksportu miał błędne założenie w danych testowych: szukał nazwy
usługi w CSV, który eksportuje nazwę klienta. Poprawiono asercję na rzeczywisty
kontrakt eksportu, bez osłabiania sprawdzenia ukrywania i przywrócenia wpisu.
Pierwszy przebieg ArchUnit wskazał też interpretację JDBC w kontrolerze paliwa;
przeniesiono ją do projekcji usługi. Mapowanie wyjątku JPA na HTTP pozostaje
legalne; EntityManager, repozytoria i JDBC w kontrolerach są zakazane.

Nie przeprowadzono testu penetracyjnego, obciążenia produkcyjnego ani wdrożenia.
Przeglądarka headless potwierdza scenariusze aplikacji, nie dostępność przycisku
podglądu w interfejsie platformy. Nie utworzono kolejnego demonstracyjnego CRM.

## Zasady następnych zmian

[Architektura i kolejność rozdzielania](ARCHITECTURE.md) oraz
[CONTRIBUTING.md](../../CONTRIBUTING.md) stanowią instrukcję dalszej rozbudowy.
CI uruchamia `verify` dla PR, main i gałęzi `refactor/**`, a także ręcznie.
Bezpieczeństwo OWNER/CSRF, URL-e, szablony, wygląd, PostgreSQL i migracje pozostają
zachowane. Żadne dane produkcyjne, hasła ani zdjęcia klientów nie były używane.
