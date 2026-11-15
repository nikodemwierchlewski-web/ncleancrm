ncLEAN — podgląd po audycie codziennej pracy

Rozpakuj ZIP i otwórz nclean-daily-audit.html w przeglądarce.
DEMO zawiera przykładowe dane i te same moduły interfejsu co poprawiona aplikacja.
Nie wymaga hasła; nie łączy się z bazą CRM. Nie wpisuj danych prawdziwych klientów.

Sprawdź w Notatkach i celach oraz Kosztach:
1. Zacznij wpis, zamknij edytor i otwórz go ponownie.
2. Szkic pozostaje w formularzu; „Odrzuć szkic” jawnie przywraca zapisany stan.
3. Termin, przypięcia i przypomnienia nadal są rozwijane.
4. Po skutecznym zapisie nowy formularz zaczyna się od nowego wpisu.

Szkice są pamięcią bieżącej strony. Zamknięcie karty lub przeładowanie może je
usunąć. Trwały zapis prawdziwej aplikacji następuje po „Zapisz” do PostgreSQL.
W HTML stan jest symulowany w karcie; eksport JSON próby nie jest backupem
PostgreSQL ani zdjęć. Dane finansowe i natywne operacje są scenariuszem DEMO.

Naprawy timera, płatności, wygasłej sesji, utraty odpowiedzi serwera i kopii
bazy sprawdzono osobno w prawdziwym Spring Boot z PostgreSQL. HTML nie zastępuje
tego sprawdzenia. Android, SMS i powiadomienia push nie są wdrożone.

Menu „…”: szerokość telefonu/komputera oraz eksport/import ćwiczenia JSON.
