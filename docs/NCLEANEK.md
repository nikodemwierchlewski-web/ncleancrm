# ncLEANEK — pomoc w codziennej pracy

ncLEANEK jest pomocnikiem ncLEAN CRM. Zachowano obecną grafikę, a nazwy „Tycoon” i „Pixel Tycoon” usunięto z ekranów. Techniczne identyfikatory plików, API i tabel pozostają zgodne z wcześniejszą aplikacją, żeby zachować historię i kompatybilność.

## Jak korzystać

- **Ludzik na ekranie:** jedno kliknięcie lub dotknięcie od razu uruchamia podpowiedzi aktualnego ekranu. Możesz przeciągnąć ncLEANKA; podnoszenie za rękę i lądowanie są dekoracją. W panelu użyj Opcje, żeby zobaczyć przypomnienia lub zmienić położenie.
- **Znak „?” przy opcji:** na komputerze najedź lub ustaw fokus, żeby przeczytać podpowiedź. Kliknięcie przypina pomoc. Na telefonie dotknij znaku. Escape zamyka pomoc i wraca do przycisku; otwarty formularz zostaje.
- **Samouczek po kliknięciu ludzika:** krótki samouczek wskazuje widoczne elementy. Wstecz/Dalej/Gotowe lub Zakończ. Ukryte kroki są pomijane. Pomoc nie wysyła formularza, nie zmienia jego pól i nie wykonuje operacji za właściciela.
- **Pomoc wewnątrz formularza:** po przypięciu wskazówki możesz uruchomić samouczek bieżącego formularza, także w otwartym panelu zlecenia, kosztu lub notatki.
- **Ustawienia → Wygląd i aplikacja:** ukryj/przywróć ncLEANKA, otwórz pomoc, zresetuj propozycje samouczka lub użyj dotychczasowego ograniczenia animacji.

Postać chodzi wyłącznie w scenie bazy. Dekoracje nie przejmują kliknięć. Ruch zatrzymuje się poza ekranem, w tle i po włączeniu ograniczenia animacji. Systemowe prefers-reduced-motion ma pierwszeństwo. Ukrycie pomocnika usuwa również jego chodzącą postać.

## Przypomnienia

Znacznik przy przycisku oznacza dostępne przypomnienie. Treść i odnośnik zobaczysz po otwarciu pomocy. Nie otwieramy automatycznych okien podczas wpisywania.

Źródłem braków jest istniejący `DayReviewService`: status, kwota końcowa, rzeczywista chemia, zdjęcia/wyjątki i notatka. Należność nie jest drugim warunkiem dokumentacji.

Dzisiejszy dzień jest proponowany do uzupełnienia od **18:00 Europe/Warsaw** albo wcześniej, gdy wszystkie wizyty mają już rozstrzygnięty status. W przeciwnym razie pomoc może wskazać ostatni wcześniejszy niezamknięty dzień. Pusty dzień nie tworzy własnego przypomnienia, a przyszły dzień nie jest zaległością. Zapisane zamknięcie jest sprawdzane tą samą sygnaturą danych co normalny przegląd dnia; korekta może ponownie otworzyć braki.

„Później” wycisza przypomnienia na 20 minut w tej sesji. „Wycisz na dziś” działa do końca bieżącego dnia według Warszawy. Dane są odświeżane po zapisie, zmianie dnia, powrocie do karty i co 2 minuty w widocznej karcie. To pomoc w otwartej aplikacji, bez SMS/push i bez pracy w tle po zamknięciu przeglądarki.

## Dane i technologia

Wspólny katalog treści i mały moduł JS korzystają z dotychczasowych szablonów i animacji. Endpoint GET `/help/context` jest chroniony OWNER i nie jest buforowany. Odczyt nie zapisuje danych ani osiągnięć. Nie dodano migracji, zewnętrznego AI ani telemetrii.

W localStorage są jedynie preferencje: ukrycie pomocnika, identyfikatory samouczków i data wyciszenia. sessionStorage zawiera czas krótkiego odroczenia. Nie zapisujemy tam kontaktów, zdjęć, treści formularzy ani odpowiedzi API.

Pomoc używa niemodalnego panelu/Popover API z wariantem zastępczym. Nie blokuje nawigacji ani zapisu. Formularze nadal korzystają z dotychczasowej sesji, CSRF, transakcji, wersji i tokenów idempotencji.

## Walidacja

Pełne Maven Wrapper `verify`: **36 unit/MVC + 85 PostgreSQL/Testcontainers = 121**, zero błędów, porażek i pominięć. Nowe testy obejmują porę przypomnienia, puste/przyszłe/anulowane/zamknięte dni, rzeczywisty wcześniejszy brak, właściwy ID, brak zapisu przez pomoc, odtworzenie po restarcie i ochronę OWNER/no-store.

Dotychczasowy scenariusz rzeczywistej aplikacji przeszedł: rezerwacja, klient powracający, ceny historyczne, zdjęcia i chemia, notatki/cele, koszty, zamknięcie dnia oraz restart. Dodatkowe wyniki interfejsu i dostępności są w `docs/audit/ncleanek-*.json`.

Nie badano fizycznego telefonu, klawiatury Android/iOS, pełnego czytnika ekranu ani starszych przeglądarek bez Popover API. Symulacja małego viewportu nie jest testem rzeczywistej klawiatury ekranowej. Automatyczny axe nie zastępuje ręcznego odbioru dostępności; pozycje incomplete są raportowane jawnie.

## Podgląd

Aktualna paczka: `preview/ncleanek-floating-demo.zip`. Wypakuj ZIP i otwórz index.html w aktualnym Chrome lub Edge. To syntetyczna demonstracja 62 widoków istniejącej aplikacji, z symulowanym zapisem w pamięci. Preferencje pomocnika zachowują się między ekranami tej demonstracji; odświeżenie resetuje sesję. Nie wpisuj prawdziwych danych klientów.

Wbudowany panel Codexa w tej sesji nie udostępnia przeglądarki użytkownikowi. HTML sprawdzamy oddzielnie w Chromium, również w ramce sandbox. Nie wdrażamy produkcji ani nie łączymy bazy nclean.pl.

Wynik przeglądarki: 11 scenariuszy pomocnika i 60 kombinacji tras/szerokości, bez błędów JS. Zmierzono zmianę transformacji chodzącej postaci, pauzę poza ekranem i powrót ruchu; ograniczenie systemowe i symulowane ukrycie karty zatrzymują ruch. Dodatkowo samouczek notatki zachował tytuł i pozwolił zapisać dokładnie jeden wpis. Krótki viewport 390×430 mieści panel pomocy.

axe-core 4.10.3: 16 kombinacji przy 360/390/768/1440 px, zero zgłoszonych naruszeń. We wszystkich 16 są pozycje incomplete wymagające ręcznej oceny; nie uznano ich za zaliczone. [Raport dostępności](audit/ncleanek-accessibility.json) i [próby interfejsu](audit/ncleanek-browser.json).

## Dostarczenie

[Pobierz zweryfikowany ZIP](https://github.com/nikodemwierchlewski-web/ncleancrm/raw/refs/heads/feature/ncleanek-guide/preview/ncleanek-demo.zip). Pobrano 674409 B, porównano SHA-256 ZIP/HTML z build.json i uruchomiono pobrany HTML w Chromium. Pomoc, samouczek, próbny zapis, Klienci/Ustawienia i preferencje między ekranami przeszły także w zagnieżdżonym sandboxie. [Dowód pobrania](audit/ncleanek-delivery.json).

Gałąź: `feature/ncleanek-guide`, baza: `release/tycoon-0.1`. Utworzenie draft PR przez gh zostało odrzucone: `Post "https://api.github.com/graphql": Forbidden`. Nie powstał numer PR. [Opis PR](NCLEANEK-PR.md), [porównanie zmian](https://github.com/nikodemwierchlewski-web/ncleancrm/compare/release/tycoon-0.1...feature/ncleanek-guide?expand=1). Nie scalano ani nie wdrażano aplikacji.


## Przesuwanie ncLEANKA

Mysz, dotyk i pióro korzystają ze wspólnego gestu. Domyślnie ncLEANEK stoi w prawym dolnym rogu, nad mobilną nawigacją. Można przesuwać go tylko lewo/prawo, bez ruchu w pionie. Ruch poniżej 8 px pozostaje kliknięciem; po przekroczeniu progu ludzik przyjmuje pozę podnoszenia. Pole dotykowe ma zawsze 44×60 px. Przeciągnięcie ani anulowanie gestu nie otwierają pomocy. Nowe, świadome tapnięcie działa od razu. Przy systemowym ograniczeniu ruchu poza jest statyczna, ale przesuwanie działa.

Położenie jest zapisywane jako dwie liczby 0–1 w preferencji `nclean.puppet.position.v1`, bez danych klientów. Wykorzystujemy tylko współrzędną poziomą; pionowe położenie wynika z dolnej krawędzi ekranu. Nie ma wyszukiwania wolnego miejsca, przyciągania ani automatycznych zmian rozmiaru. Przewijanie i otwarcie formularza nie przestawiają ludzika. W kolizji z przyciskiem zapisu lub aktywnym polem postać blednie i przepuszcza kliknięcia, zachowując położenie. Po zmianie rozmiaru ekranu zachowana jest preferencja pozioma.

Strzałki lewo/prawo przesuwają postać, Home przywraca prawy dolny róg, Enter/Spacja otwierają pomoc. Reset i przeniesienie na drugą stronę są też w Opcjach oraz Ustawienia → Wygląd i aplikacja. Nie ma nowego modułu biznesowego, migracji ani zmian w OWNER/CSRF.

Aktualne zachowanie i testy: [NCLEANEK-BOTTOM-RAIL-RESULTS.md](NCLEANEK-BOTTOM-RAIL-RESULTS.md). Starsze raporty opisują poprzedni wariant pomocnika.

Przypomnienie „Dokończ dzień” pojawia się bez otwierania okna, gdy istniejący endpoint `/help/context` zgłasza zaległość. Prowadzi do pierwszej wizyty z brakami lub do zamknięcia uzupełnionego dnia. × odkłada je na 20 minut; Opcje pozwalają wyciszyć je na dziś. Nie pokazujemy chmurki w otwartym formularzu ani podczas pomocy lub przeciągania. Przypomnienia działają tylko w otwartej aplikacji.
