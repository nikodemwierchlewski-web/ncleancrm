# Kalendarz i szybkie umawianie

Kalendarz jest ekranem startowym. Na komputerze miesiąc ma 7 kolumn, a po prawej stale widać plan wybranego dnia. Kliknięcie dnia zmienia ten plan bez otwierania okna. W kratce mieszczą się dwa krótkie wpisy; **+N kolejnych** wybiera dzień i pokazuje jego pełną listę po prawej.

Na telefonie i tablecie do 900 px nagłówek zawiera datę i **+ Zlecenie**. Pod nim są pasek tygodnia, obszary i karty zleceń. Dotknięcie daty rozwija miesiąc; wybór dnia ponownie go zwija. Przycisk dodawania pozostaje dostępny podczas przewijania. Wybrana data jest zachowana w adresie strony i po zapisie.

Karta dnia pokazuje godzinę, usługi, klienta i adres. Wykonane i anulowane zlecenia mają spokojniejszy wygląd oraz opis statusu. Kliknij kartę, aby zobaczyć pozycje i kwotę, zadzwonić przez odnośnik telefonu albo zmienić termin i status.

## Jedno krótkie zlecenie

1. Wybierz dzień i **+ Zlecenie**.
2. Wybierz kategorię przyciskiem. Zwiększ ilość usługi przyciskiem + albo wpisz wartość. Możesz przejść do innej kategorii: wcześniejsze ilości pozostają wybrane i trafiają do zapisu. Podsumowanie obejmuje wszystkie kategorie; × usuwa pojedynczą pozycję.
3. W polu **Telefon lub klient** wpisz numer albo imię. Wybierz podpowiedź istniejącego klienta lub uzupełnij dane nowej osoby. Wpisany numer jest podpowiadany w polu telefonu, bez nadpisywania ręcznych poprawek. Samo dopasowanie numeru nie łączy kontaktów.
4. Dla wybranego klienta adres domyślny uzupełnia się automatycznie. Możesz poprawić adres tej wizyty, a przez **Edytuj kontakt wizyty** również imię i telefon. Te poprawki nie przepisują kartoteki ani wcześniejszych rezerwacji. **Zmień klienta** ponownie otwiera wyszukiwanie.
5. Sprawdź datę, godzinę i sumę, a następnie zapisz. Na telefonie suma i zapis pozostają dostępne podczas przewijania formularza.

Błąd zapisu pozostawia formularz, wybrane usługi oraz kontakt i adres. Ponowny zapis jest blokowany podczas wysyłania. Token żądania zapobiega powtórnemu utworzeniu tej samej rezerwacji. Zmiana cennika nie przelicza wcześniejszych zleceń.

Nowe opcje opisuje [PLAN-CENNIK-CHEMIA.md](PLAN-CENNIK-CHEMIA.md): ceny uzgodnione, prognoza chemii i dziedziczenie tygodniowego planu obszarów.

## Obszary i ustawienia

**Obszary dnia** otwierają panel przypisania i pokazują źródło oznaczenia. Plan tygodniowy jest domyślny; indywidualny wyjątek (również pusty) ma pierwszeństwo. Przywrócenie planu usuwa wyjątek. Możesz zaznaczyć kilka obszarów albo wyczyścić oznaczenia. Dzień ma delikatne tło, paski kolorów i nazwy; na telefonie kolory są również w pasku tygodnia. Obszar jest opcjonalnym ręcznym oznaczeniem, nie dobiera terminów.

Główna nawigacja jest taka sama na wszystkich ekranach: **Kalendarz · Klienci · Ustawienia**. W ustawieniach są **Cennik** i **Obszary pracy**. Dezaktywacja obszaru zachowuje wcześniejsze przypisania. Istniejące adresy `/orders`, `/settings/services` i `/settings/areas` nadal działają; historia klienta nadal prowadzi do jego zleceń.

## Zapis i wymagania

Interakcje wymagają JavaScript. Prawdziwa aplikacja zapisuje do istniejącego PostgreSQL; nie używa localStorage do danych CRM. Migracje V1–V3 pozostają niezmienione. Restart aplikacji lub kontenera z zachowanym wolumenem nie usuwa danych.

Odczyt i zapis wymagają konta OWNER. Formularze używają CSRF. Aplikacja nie łączy się z produkcyjną bazą nclean.pl. Uruchomienie na Windows: [WINDOWS.md](WINDOWS.md).
