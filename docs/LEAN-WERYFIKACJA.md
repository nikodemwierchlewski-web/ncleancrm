# Weryfikacja uproszczonego CRM

Sprawdzono prawdziwą aplikację Spring Boot z PostgreSQL Compose na Linux/Chromium. Użyto wyłącznie syntetycznych klientów i zleceń. Po teście usunięto utworzone rekordy i przywrócono wcześniejsze przypisania obszarów. Nie łączono się z produkcją.

## Testy automatyczne

Maven Wrapper, JDK 17: `./mvnw verify` — **24 testy: 12 MVC/security i 12 Testcontainers/PostgreSQL; 0 niepowodzeń, 0 błędów, 0 pominiętych**.

Nowa integracja zapisuje usługi z różnych kategorii w jednej rezerwacji, sprawdza ilości, sumę, kontakt i adres wizyty, niezmienioną kartotekę, ponowiony token oraz trwałość po restarcie aplikacji. Pozostałe testy obejmują migracje V1–V3, CSRF, OWNER, obszary i dezaktywację, historyczne ceny oraz konflikt wersji. Test MVC dodatkowo sprawdza dostęp do wspólnych ustawień i odnośniki do cennika/obszarów.

## Scenariusze w przeglądarce

- 8 zleceń: dwie krótkie pozycje i „+6 kolejnych” w miesiącu, pełna uporządkowana lista dnia, ulica, szczegóły, telefon, zmiana terminu i statusu.
- Komputer: plan dnia widoczny od razu po prawej, zmiana dnia bez modala.
- Nowe zlecenie: usługi z dwóch kategorii, podsumowanie i rzeczywisty zapis obu pozycji w PostgreSQL.
- Stały klient: podpowiedź, adres domyślny, korekta adresu wyłącznie dla wizyty.
- Nowy klient: ponowne wykorzystanie wpisanego telefonu, brak nadpisania ręcznie wpisanego imienia przez opóźnioną odpowiedź wyszukiwania.
- Kontrolowany HTTP 400: formularz zachowuje wybrane pozycje, klienta i adres; ponowienie zapisuje jedno zlecenie.
- Ten sam numer u dwóch osób: klient zostaje wybrany dopiero po kliknięciu konkretnej podpowiedzi.
- Błędna ilość w ukrytej kategorii: walidacja odsłania właściwą usługę, nie kasując pozostałych pól.
- Kilka obszarów: przypisanie, nazwa/HEX, dezaktywacja i zachowanie historii.
- Wspólna nawigacja i brak poziomego przewijania na kalendarzu, kartotece, cenniku, ustawieniach obszarów i historii zleceń.
- Restart aplikacji: zlecenia i przypisania nadal widoczne po ponownym zalogowaniu.

Rozmiary okna Chromium: **360, 390, 768 i 1440 px**, wysokość 844 px. Pierwsza karta dnia była widoczna bez przewijania; jej górna krawędź znajdowała się odpowiednio na 348, 348, 336 i około 304 px. Na telefonie sprawdzono przyklejony przycisk dodawania oraz sumę/zapis podczas przewijania formularza. Dodatkowo sprawdzono obniżone okno 390 × 520 px. To przybliżenie przestrzeni po otwarciu klawiatury, nie test fizycznej klawiatury iOS/Android. Nie stwierdzono błędów JavaScript ani żądań do zewnętrznych serwisów.

## Krótka obsługa i ograniczenia

Instrukcja: [KALENDARZ.md](KALENDARZ.md). Instalacja: [WINDOWS.md](WINDOWS.md). Wykonanych migracji nie zmieniono; aplikacja korzysta z istniejących modeli i tabel.

W tej sesji platforma nie udostępniła narzędzia wspólnego pulpitu; próba otwarcia Desktop zakończyła się komunikatem o niedostępnym narzędziu. Automatyczne testy Chromium nie oznaczają dostępnego panelu interaktywnego dla użytkownika. Ewentualny plik HTML jest odizolowaną demonstracją z danymi przykładowymi i nie zapisuje do PostgreSQL.
