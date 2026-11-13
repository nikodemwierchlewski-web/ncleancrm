Codzienne operacje były ukryte w Ustawieniach, Podsumowanie wymagało długiego przewijania, a Dzisiaj i zamknięcie dnia miały różne reguły braków. Ten PR rozdziela miejsca pracy i konfigurację oraz skupia dokumentowanie zlecenia w jednym współdzielonym formularzu.

- Nawigacja Dzisiaj / Kalendarz / Klienci / Więcej, kalendarz nadal startowy; Ustawienia zawierają konfigurację. Zachowano istniejące adresy i endpointy.
- Dzisiaj pokazuje następny termin i braki z DayReviewService przed listą. Podsumowanie ma trzy kwoty i jedną akcję, a szczegóły są rozwijane. W próbce 390 px domyślna wysokość spadła z około 3734 do 1150 px; główna akcja mieści się do 544 px. Porównanie jest orientacyjne, nie badaniem czasu użytkowników.
- Work/completion-form jest wspólny dla wizyty i kolejki dnia; zachowano pomiary, zdjęcia, wyjątki, notatki i opcjonalny podział ceny/czasu. Skróty chemii prowadzą do wykonania. Pola tworzenia/edycji klienta są współdzielone. Stary ręczny model paliwa jest nadal dostępny w korektach zaawansowanych.
- Wspólny spokojniejszy układ obu motywów. Klasyczny jest domyślny także w HTML, Tycoon opcjonalny. Bez nowych modułów biznesowych, migracji, zmiany historii/cen lub dostępu do produkcji. OWNER i CSRF zachowane.

Weryfikacja: 27 jednostkowych/MVC + 80 integracyjnych PostgreSQL/Testcontainers =107, bez błędów/pominięć. Chromium na osobnych schematach lokalnych, oba motywy i 360/390/768/1440 px: osiem wizyt, nowy/powracający klient, dwie kategorie/cena/adres wizyty, zmiana cennika zachowuje historyczną cenę, wspólne wykonanie, chemia/zdjęcia/czas,503/ponowienie, konflikt kart, anulowanie/przełożenie, cele, zamknięcie i restart. Dodano testy IA, uprawnień menu, zgodności braków i atomowej walidacji podziału kwot.

Pobieralny ZIP w preview zawiera 62 wyrenderowane widoki z tymi samymi modułami UI i jawną symulacją zapisu na danych syntetycznych. Sprawdzono nawigację, formularze, zapis wykonania/powrót do wizyty, oba motywy, cztery szerokości i zagnieżdżoną ramkę. Pobrano paczkę z GitHuba: HTTP 200, zgodność SHA-256.

Obsługa/audyt/przed-po: docs/LEAN-WORKSPACE.md. Wyniki i sumy migracji: docs/audit/lean-workspace-evidence.json. Tożsamość paczki: preview/build.json. Bazą jest niescalony pełny CRM experiment/tycoon-interactions. Nie wdrożono na produkcję ani nie scalono do main.
