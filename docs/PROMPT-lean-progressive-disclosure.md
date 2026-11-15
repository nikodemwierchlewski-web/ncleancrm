# Zastosowany prompt: mniej opcji na wierzchu

Pracuj na ostatniej pełnej implementacji ncLEAN CRM, na osobnej gałęzi. Przejrzyj Dzisiaj, notatki/cele, szczegóły zlecenia, koszty, paliwo, kalendarz i cennik z perspektywy codziennej pracy właściciela. Zachowaj pikselowy wygląd i istniejące funkcje.

Zasada LEAN: wyniki i podstawowe działania są widoczne; konfiguracja, filtrowanie, historia i szczegółowe analizy pojawiają się dopiero na żądanie. Nie chowaj ostrzeżeń o niepełnych danych, przycisków zapisu, planu dnia ani głównych działań. Unikaj wielopiętrowych menu.

1. Na Dzisiaj domyślnie zwiń filtry Wyniku firmy. Pokaż aktywny zakres i przycisk/sekcję „Zmień zakres”. Po zastosowaniu filtrów panel ponownie jest zwinięty, wyniki i zakres pozostają widoczne. Własne daty muszą nadal działać. Szczegóły pojedynczych dni wykresu są opcjonalne.
2. W notatce/celu schowaj termin, godzinę i przypięcia pod jedną sekcją z krótkim podsumowaniem. Konfigurację przypomnień przy zleceniu i notatce zwiń; pokaż liczbę ustawionych przypomnień. Nie usuwaj wartości po zwinięciu, nie wyłączaj zapisujących się pól. Błąd walidacji odsłania potrzebne pole. Dawne cykle zachowaj.
3. W kosztach zbierz okres i wyszukiwanie w jeden zwijany formularz. Aktywne filtry są jawne, a ich zmiana i wyczyszczenie nie wymaga szukania. Nie chowaj wydatków ani dodawania nowego.
4. W paliwie zwiń analizę spalania i historię odczytów; formularz szybkiego tankowania zostaw widoczny. Nie twórz nowych modułów, ustawień globalnych ani migracji.
5. Użyj natywnych details/summary, obsłuż klawiaturę i minimum 44 px na dotyk. Kotwice, błędy i pola z błędami mają otwierać właściwą sekcję. Samo zwinięcie nie jest zapisem ani resetem. Bez zmian w bazie, OWNER, CSRF i regułach finansowych.

Sprawdź prawdziwą aplikację przy 360/390/768/1440 px, filtry niestandardowe i ich zachowanie po zwinięciu, przypomnienia przy wpisie/zleceniu, walidację ukrytego pola i ponowienie zapisu, filtrowanie kosztów, tankowanie i kotwice. Uruchom właściwe testy, podaj wyniki i przygotuj aktualny demonstracyjny HTML na podstawie poprawionej aplikacji, jeśli podgląd serwera jest niedostępny.

Dodatkowa decyzja po pomiarze: plan dnia przed analizami, na komputerze oddzielna kolumna celu i wyniku; na telefonie skrót do wyniku. Pozostawić główne działania bez dodatkowych kroków. Wykorzystać istniejący `interface.js` do odsłaniania błędów i kotwic, bez kopiowania jego obsługi.
