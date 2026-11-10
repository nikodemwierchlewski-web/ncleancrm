# Wyniki weryfikacji planu i prognoz

Maven Wrapper, JDK 17, `verify`: **32 testy — 14 MVC/security oraz 18 integracyjnych PostgreSQL Testcontainers; 0 błędów, 0 niepowodzeń, 0 pominiętych**.

Integracja sprawdza wersje tygodniowe i wyjątki (również puste), przywrócenie dziedziczenia, zachowanie historii i restart. Test migracji V1 zachowuje wcześniejsze przypisanie. Osobny test migracji V3 → V5 zachowuje ceny starych zleceń i nie dopisuje fikcyjnej prognozy.

Sprawdzono uzgodnioną cenę 220 przy sugestii 180, standard pomnożony przez ilość, ręczne prognozy/dodatkowy środek, przeliczenia l/ml i kg/g, rozróżnienie zera i braku prognozy, zapis bez standardu, odrzucenie błędnej ceny/ilości/jednostki i wycofanie zapisu przy błędzie. Ceny, nazwy i prognozy pozostają po edycji katalogu oraz restarcie. Operacje zapisu wymagają OWNER i CSRF, powtórzenie tokenu nie tworzy drugiej rezerwacji.

Chromium/Playwright uruchomiono przeciw prawdziwej aplikacji z lokalnym PostgreSQL Compose w osobnym tymczasowym schemacie. Schemat został usunięty po teście; nie modyfikowano dotychczasowych danych ani produkcji. Scenariusze:

- Plan „poniedziałek — Krzyki, wtorek — Fabryczna”, pusty wyjątek i przywrócenie planu przez formularze.
- Dodanie kategorii i usługi w niej; dodanie opakowań 1 l, 500 ml i 2 kg; standard płukanki 50 ml i presprayu 30 ml, koszt 4,90 zł.
- Cena 180 → 220 zł, ilość 2 → 3; automatyczne zużycie reaguje na ilość, ręczne pozostaje; przywrócenie standardu, usunięcie presprayu i dopisanie proszku.
- Dwie kategorie w jednej rezerwacji, jawna zerowa prognoza drugiej pozycji, kwota 680 zł i prognoza chemii 7,90 zł zapisane w PostgreSQL.
- Kontrolowany HTTP 400 zachowuje uzgodnioną cenę, ilości chemii i dane klienta; po ponowieniu zapis jest poprawny.
- Zmiana ceny opakowania po rezerwacji nie zmienia jej prognozy; zlecenia, standardy i plan zostają po restarcie.
- Widoki 360, 390, 768 i 1440 px: kalendarz, rozwinięte kategorie/standardy i prognoza, formularz tygodniowy oraz kartoteka. Brak poziomego przewijania i błędów JavaScript; suma/zapis i pierwsze zlecenie widoczne. Test nie zastępuje sprawdzenia fizycznej klawiatury iOS/Android.

Instrukcja obsługi: [PLAN-CENNIK-CHEMIA.md](PLAN-CENNIK-CHEMIA.md). Stary HTML demonstracyjny nie jest aktualną aplikacją ani źródłem wyników tych testów.
