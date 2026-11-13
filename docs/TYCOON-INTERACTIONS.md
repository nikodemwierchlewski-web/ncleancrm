# Cleaning Tycoon — reakcje na pracę użytkownika

Eksperymentalna gałąź `experiment/tycoon-interactions`, oparta na `experiment/cleaning-tycoon-progress`. Normalny CRM zachowuje klasyczny wygląd. Uruchomienie na Windows: `./scripts/Start-Local.ps1 -PixelTycoon`; powrót przez uruchomienie bez przełącznika. Nie zmieniono schematu bazy, migracji ani historycznych danych.

## Co można zobaczyć

- Krótkie wciśnięcie przycisku, wyróżnienie klikniętej karty i przesuwający się znacznik zakładki.
- Wybór dnia: podświetlenie, krótkie wejście listy i ruch samochodu w istniejącej scenie. Przejście między miesiącami ma kierunek zgodny z nawigacją.
- Formularze: płynne wejście i wyjście, zachowany fokus, reakcja wybranej kategorii, wiersza usługi, licznika pozycji i sumy. Wybrane usługi z innych kategorii pozostają w wycenie.
- W trakcie zapisu: tekst „Zapisywanie…” i mała ikonka ekstraktora przy obsługiwanym komunikacie formularza. Nie symuluje czasu ładowania.
- Udane dodanie: kalendarzyk z potwierdzeniem na scenie i wyróżnienie dokładnie zapisanego zlecenia. Edycja wyróżnia zmieniony wpis.
- Potwierdzenie wykonania: krótki ruch pracownika i końcówki ekstraktora, bąbelki i błysk. Zamknięcie dnia: krótki błysk bazy. Gdy na stronie nie ma widocznej sceny, pojawia się mały, nieinteraktywny komunikat ze sceną na około 2 sekundy. Otworzenie formularza usuwa ten komunikat. Odznaka czeka na jego zakończenie.
- Krok celu reaguje po udanym zapisie i odświeżeniu karty. Paski przechodzą do wartości odczytanych z serwera. Odznaka ma krótkie podskoczenie grafiki i kilka wygaszanych pikselowych iskierek.

Scena jest dekoracją; nie przedstawia GPS ani rzeczywistego ruchu pracowników. Efekty nie zmieniają cen, finansów, pomiarów, statusów ani osiągnięć. Nie dodano punktów za kliknięcia, dźwięków czy zapętlonego konfetti.

## Potwierdzenie zapisu

Wspólny moduł `tycoon-motion.js` obsługuje prezentację. Zapisy AJAX przekazują efekt dopiero po udanej odpowiedzi. Formularz kalendarza wymaga HTTP sukcesu, JSON i `saved:true`. API dodawania i edycji zachowuje pole `saved` i zwraca dodatkowo `id` oraz `motion`, żeby wyróżnić właściwy wpis. Błędy walidacji i konfliktu wersji nie zawierają sygnału sukcesu.

Przy przejściu do następnej strony zapamiętuje się w `sessionStorage` tylko rodzaj efektu, identyfikator wpisu i czas — bez danych kontaktowych, kwot czy usług. Efekt jest jednorazowy, z ważnością 10 sekund. Podobnie przechowywany jest kierunek miesiąca i poprzednia zakładka. Zablokowanie pamięci przeglądarki wyłącza tę dekorację przejścia, nie zapis CRM.

Zwykłe formularze HTML korzystają z jednorazowego Spring FlashMap po udanej operacji. Fragment Thymeleaf wystawia sygnał wyłącznie w motywie testowym. Samodzielne wpisanie `?saved=1` nie uruchamia sceny potwierdzenia. Native pending jest czyszczony po powrocie strony z pamięci historii przeglądarki.

Nie dodano nowego zapisu osiągnięć ani migracji; wykorzystano istniejącą historię V14. OWNER i CSRF pozostają wymagane.

## Ruch i dostępność

Przycisk: 100 ms, panele: 150–180 ms, krótki akcent: do 220 ms, scena: do 650 ms. Kliknięcia nie czekają na zakończenie efektu. Kolejny efekt tego samego elementu zastępuje poprzedni. Nie ma kolejki kolejnych przejazdów ani opóźnienia operacji biznesowej. Zmiana szerokości aktualizuje położenie znaczników.

Efekty używają CSS i Web Animations API. Własne SVG i fonty są lokalne. Zatrzymanie ruchu w ustawieniach, preferencja systemowa, ukrycie karty i wyjście sceny poza ekran zatrzymują także krótkie efekty JavaScript. Ustawienie ograniczenia ruchu ma pierwszeństwo. Bez obsługi animacji pozostają zwykłe stany interfejsu.

## Weryfikacja

13 listopada 2026:

- Maven Wrapper `clean verify`: **27 jednostkowych/MVC + 78 integracyjnych PostgreSQL/Testcontainers = 105**, bez błędów, porażek i pominięć.
- Nowe testy integracyjne: poprawny identyfikator i rodzaj sygnału po zapisie, ten sam ID po ponowieniu tokenu, brak sygnału po konflikcie, jednorazowy flash po wykonaniu oraz brak flasha przy błędzie lub ręcznie podanym `saved=1`.
- Prawdziwa aplikacja w Chromium na osobnym schemacie Compose: osiem wizyt, dodawanie,503 i ponowienie, dokładne wyróżnienie wpisu, szybka zmiana dni, przejście miesiąca, dwie kategorie, licznik pozycji, fokus modala, zdjęcia/chemia/czas, cele/kroki, odznaki, zamknięcie dnia i trwałość po restarcie.
- Szerokości 360, 390, 768 i 1440 px: bez poziomego przewijania. Ograniczenie ruchu po odświeżeniu, ustawienie systemowe i zatrzymanie w tle. Powtarzalny skrypt: `scripts/verify-browser.py --owner-file <prywatny-json> --pixel-tycoon`. Weryfikuje też odrzucenie HTTP200 z `saved:false`, zachowanie pól i brak fałszywego sukcesu.
- Podgląd HTML: nawigacja i formularze, odznaki/postęp, ruch między widokami, dokładny ID po przykładowym zapisie, błędna odpowiedź i ponowienie, wygaszenie efektów po serii kliknięć, tryb klasyczny, cztery szerokości oraz zagnieżdżona ramka z nieprzezroczystym originem. Brak błędów JS w zakończonych próbach.

Pierwsze nowe scenariusze przeglądarkowe wymagały otwarcia mobilnego przeglądu miesiąca, ustawienia mobilnej szerokości dla paska tygodnia oraz dopasowania pełnego URL w atrapach odpowiedzi. Poprawiono scenariusze i wykonano je ponownie. W kodzie dopracowano animowanie nowej karty celu po odświeżeniu, znacznik nawigacji i walidację potwierdzenia odpowiedzi.

## Podgląd do pobrania

[Instrukcja i ZIP](../preview/README.md). Paczka zawiera 60 widoków wyrenderowanych przez prawdziwą aplikację i te same moduły UI, z adapterami symulującymi zapis. Dane są syntetyczne; zmiany w pliku HTML pozostają w pamięci karty. Pamięć krótkich efektów przejścia jest również symulowana wewnątrz podglądu. PostgreSQL i transakcje sprawdzono osobno.

Ta sesja nie deklaruje natywnego pulpitu ani przekierowania portów Codexa. Lokalny serwer służy testom i nie jest publicznym adresem użytkownika. Dostęp dostarczamy przez pobieralny ZIP, nie ścieżkę `/workspace`.

Wyniki i sumy: [tycoon-interactions-evidence.json](audit/tycoon-interactions-evidence.json). Paczkę pobrano z GitHuba: HTTP200, application/zip, zgodność SHA-256 z przetestowanym HTML i metadanymi.

Gałąź została wysłana. Próba utworzenia draft PR przez `gh pr create` zakończyła się `POST https://api.github.com/graphql: Forbidden` (403). [Gotowy opis PR](TYCOON-INTERACTIONS-PR.md) i [porównanie do poprzedniego eksperymentu](https://github.com/nikodemwierchlewski-web/ncleancrm/compare/experiment/cleaning-tycoon-progress...experiment/tycoon-interactions?expand=1).
