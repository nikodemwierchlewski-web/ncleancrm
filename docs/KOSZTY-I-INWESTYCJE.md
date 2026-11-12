# Koszty i inwestycje

Zmiany są w prawdziwej aplikacji Spring Boot/Thymeleaf, na gałęzi `feat/costs-investments`, od `feat/notes-goals` (`bbb2592`). Ta baza zawiera wcześniejsze, niescalone funkcje CRM.

## Obsługa

1. **Ustawienia → Koszty i inwestycje**. Wybierz miesiąc lub rok i zakładkę: Przegląd, Samochód, Sprzęt i inwestycje, Chemia, Stałe i marketing albo Wszystkie wpisy.
2. **+ Wydatek**: wybierz rodzaj, datę, łączną kwotę i opis. Pozostałe pola są opcjonalne lub pojawiają się odpowiednio do rodzaju wydatku. Ilość nie mnoży ponownie kwoty.
3. **Samochód** pokazuje dotychczasowe tankowania samochodu razem z naprawami, serwisem, częściami i pozostałymi rachunkami. Miesięczna/roczna suma nie obejmuje paliwa agregatu ani wspólnego zapasu. Przebieg przy naprawie jest informacją historyczną: nie zmienia dziennego licznika i kalkulacji zużycia.
4. **Paliwo** w formularzu kieruje do istniejącego dziennika `/fuel`. Nie powstaje drugi zapis tankowania. Nie trzeba ponownie wpisywać starych tankowań.
5. **Zakup sprzętu** tworzy jego kartotekę albo łączy wpis z istniejącą. W kartotece są model, numer seryjny, gwarancja, sprzedawca, notatka, historia zakupów/serwisu i sumy całej historii. Dezaktywacja zachowuje historię i blokuje nowe powiązania; można ponownie aktywować sprzęt.
6. **Zakup chemii** wymaga wskazania środka z istniejącego katalogu. Zakupy i rzeczywiste pomiary zużycia są pokazane osobno. Zakup nie zmienia cen katalogowych ani historycznych wycen zleceń. Nie jest to magazyn; ilość oznacza opakowania, nie pomiar zużycia.
7. **Rachunek już ujęty w kosztach miesięcznych**: wskaż skonfigurowany miesiąc i rodzaj. Kwota rachunku pozostaje w historii zakupów, ale wynik operacyjny nadal korzysta tylko z konfiguracji miesiąca. Rachunek może mieć inną datę/kwotę niż konfiguracja; nie zmienia jej automatycznie.
8. Kliknij tytuł wpisu, aby poprawić kwotę, datę i dane. **Anuluj błędny wpis** wymaga powodu. Anulowanie wyłącza kwotę z sum, zachowując rekord, powód, audyt i rachunek. Włącz „Pokaż też anulowane”, żeby zobaczyć historię.

Zdjęcie rachunku można wybrać z galerii lub aparatu: JPEG/PNG do 8 MB i 20 MP. Plik jest przechowywany prywatnie w PostgreSQL i dostępny tylko dla OWNER. Zmiana zdjęcia nie usuwa poprzedniego załącznika udokumentowanego w audycie. Nie dodano OCR ani obsługi PDF.

## Co oznaczają liczby

**Zakupy i rachunki**: aktywne tankowania i nowe wydatki według daty zakupu. Każdy wpis występuje raz, niezależnie od źródła. To rejestr dokumentów zakupowych, nie księgowość ani ewidencja przepływów gotówki.

**W tym inwestycje**: zakupy sprzętu, będące częścią powyższej sumy. Nie obniżają bieżącego wyniku operacyjnego; nie dodano amortyzacji.

**Koszty realizacji i utrzymania**: istniejąca kalkulacja dashboardu dla całego wybranego okresu — zużycie chemii/paliwa, rozdzielone koszty miesięczne oraz aktywne koszty jednorazowe. Przy niepełnych danych widoczna jest informacja o brakach. Nie należy dodawać tych trzech liczb do siebie.

- Zakup paliwa/chemii nie jest drugim kosztem ich zużycia.
- Rachunek powiązany z konfiguracją miesiąca nie obniża wyniku drugi raz. Nie można usunąć z konfiguracji rodzaju kosztu, do którego nadal jest przypisany aktywny rachunek; najpierw trzeba poprawić powiązanie lub anulować rachunek.
- Jednorazowy marketing trafia do kategorii Marketing, inne koszty jednorazowe do „Utrzymanie i inne”. Są naliczane raz, według daty wydatku.
- Inwestycje dashboard pokazuje osobno. Wpłaty klientów i historyczne ceny zleceń pozostają osobnymi źródłami.

Nie ma automatycznego uzgadniania rachunków z zużyciem. Ekran chemii pokazuje do 200 potwierdzonych pomiarów i do 200 wpisów bez chemii; brak pomiaru nie oznacza zera. Lista zakupów ma strony po 50 wpisów, filtr opisu/sprzedawcy/notatki i okres miesiąc/rok. Kartoteka sprzętu pokazuje całą historię, niezależnie od okresu wybranego w centrum kosztów.

## Dane i bezpieczeństwo

Flyway **V13** dodaje `equipment_item`, `cost_entry`, `cost_audit` oraz oznaczenie anulowania tankowania. V1–V12 pozostają niezmienione. Widok łączy istniejące `fuel_purchase` i nowe koszty; nie kopiuje tankowań. Wykonano migracje także na bazie z wcześniejszymi danymi w ramach testów integracyjnych.

OWNER i CSRF pozostają wymagane. Token zapisu zapobiega powieleniu wydatku i sprzętu przy ponowieniu. Wersja chroni przed nadpisaniem z innej karty. Błąd zapisu pozostawia pola i przesłany rachunek w formularzu do ponowienia. Operacje współdzielą blokadę z dziennikiem paliwa i sprzątaniem załączników; ten sam rachunek nie może być powiązany z dwoma wpisami.

Audyt zawiera stan przed i po korekcie oraz użytkownika. Kopia całej bazy PostgreSQL obejmuje nowe tabele, audyt i obrazy rachunków; należy nadal stosować instrukcję kopii/odtworzenia z audytu gotowości. Nie zastosowano zmian na produkcji i nie łączono się z nclean.pl.

## Weryfikacja — 12.11.2026

**Maven Wrapper `verify`: 94 testy — 26 jednostkowych/MVC oraz 68 integracyjnych PostgreSQL/Testcontainers. Zero błędów, niepowodzeń i pominiętych.** Raporty: `target/surefire-reports`, `target/failsafe-reports`.

Nowe scenariusze obejmują sumy miesiąca/roku, brak podwójnego naliczania paliwa/chemii/kosztów miesięcznych, jednorazowy marketing, zmianę daty/kwoty, token powtórzenia, konflikt wersji, anulowanie, walidację, prywatne rachunki, historię sprzętu i trwałość po restarcie. Sprawdzono OWNER i CSRF dla nowych endpointów.

Prawdziwa aplikacja w Chromium z odizolowanym, tymczasowym schematem PostgreSQL: tankowanie, naprawa z przebiegiem i zdjęciem, zakup sprzętu, serwis, zakup chemii bez zmiany katalogu, rachunek do miesięcznego marketingu, marketing jednorazowy, edycja, anulowanie, dezaktywacja/aktywacja sprzętu, powrót do wybranego okresu, kontrolowany HTTP503 i ponowienie, dwie karty, odczyt historii/zdjęcia po restarcie. Widoki i formularz sprawdzono przy **360, 390, 768, 1440 px**, bez poziomego przewijania. Zapis pozostaje widoczny także przy widoku 390 × 430 px. Nie jest to test fizycznej klawiatury telefonu.

Podgląd HTML jest eksportem rzeczywistych szablonów z danymi syntetycznymi. Symuluje zapis w pamięci sesji; nie zastępuje logowania i trwałego zapisu w aplikacji.
