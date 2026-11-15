ncLEAN CRM — podgląd przypomnień i podsumowania

Rozpakuj ZIP i otwórz plik nclean-contextual-reminders.html w przeglądarce.
To samodzielny plik z grafikami, CSS i JS, bez instalacji i bez połączenia z bazą.

Sprawdź:
1. Dzisiaj: pikselowe koło, miesiąc do dnia, tydzień, dzień i własne daty.
2. Otwórz wizytę z listy → Przypomnienia → + Przypomnienie.
3. Wybierz minuty/godziny/dni albo konkretną datę i zapisz przypomnienia.
4. Więcej → Notatki i cele → dodaj/edytuj wpis, podaj termin i godzinę,
   dodaj przypomnienia i zapisz wpis. Otwórz ponownie, aby sprawdzić zapis.
5. Menu „…” podglądu: szerokość telefonu/komputera oraz eksport próby JSON.

Wyłącznie syntetyczne dane. Zapis DEMO jest w pamięci karty; odświeżenie resetuje
próbę. Eksport JSON zachowuje ćwiczenie, ale nie jest backupem bazy ani zdjęć.
Nie wpisuj tu danych prawdziwych klientów. Dni poza przechwyconym zestawem DEMO
mają informację o braku danych. Pełna aplikacja odczytuje dane z PostgreSQL.

Zmiany wykonano również w prawdziwym Spring Boot CRM. Baza PostgreSQL,
przypomnienia i historia pozostają po restarcie oraz aktualizacji przez Flyway.
Powiadomienia obecnie są wewnątrz otwartej aplikacji. Android, push i SMS
wymagają jeszcze osobnej integracji; żaden SMS nie jest tu wysyłany.

Zweryfikowano Chromium 360/390/768/1440 px oraz dwa zagnieżdżone sandboxy.
Nie jest to uruchomiona publicznie usługa CRM ani podgląd serwera Codexa.
