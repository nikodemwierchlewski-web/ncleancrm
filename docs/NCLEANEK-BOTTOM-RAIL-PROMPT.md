# Prompt: stabilny ncLEANEK i przypomnienie o zamknięciu dnia

Pracuj w istniejącej aplikacji ncLEAN CRM. Napraw ncLEANKA bez zmiany danych,
reguł zamknięcia dnia, logowania OWNER ani CSRF. Nie twórz Pull Requesta.

1. Domyślnie ustaw małego ludzika w prawym dolnym rogu, nad mobilną nawigacją
   i bezpiecznym marginesem telefonu. Rozmiar ma być stały: pole dotykowe 44×60 px.
2. Pozwól przeciągać go wyłącznie w poziomie. Zapisuj wybraną pozycję poziomą.
   Ruch palca w pionie nie przesuwa postaci. Nie zmieniaj punktu złapania,
   nie przyciągaj do innych miejsc i nie szukaj automatycznie wolnej przestrzeni.
3. Przewijanie, aktualizacje treści i otwieranie formularza nie mogą zmieniać
   rozmiaru ani wybranej strony. Dopasowanie do rzeczywistej zmiany rozmiaru
   ekranu zachowuje poziomą preferencję. Strzałki lewo/prawo i Home działają.
4. Zachowaj subtelną animację podnoszenia oraz kliknięcie otwierające pomoc
   bieżącego ekranu. Przeciągnięcie, anulowanie i drugi palec nie otwierają pomocy.
   Respektuj ograniczenie ruchu. Nie blokuj zapisu ani aktywnego pola formularza.
5. Wykorzystaj istniejący endpoint /help/context i reguły DayReviewService.
   Pokaż niewielkie przypomnienie z odnośnikiem do uzupełnienia/zamknięcia dnia,
   informacją o brakach i możliwością odłożenia. Zachowaj wyciszenie na dziś.
   Bez nowych SMS, push, pracy w tle, automatycznego zamykania lub dublowania
   wyliczeń. Nie przerywaj pisania ani nie otwieraj automatycznie okna.
6. Przypomnienie ma ustępować podczas edycji, przeciągania i otwartej pomocy.
   Sam ludzik pozostaje na dolnej krawędzi; w kolizji z zapisem nie przejmuje
   dotyku. Nie zmieniaj zapisanej pozycji, by uniknąć elementu interfejsu.
7. Sprawdź prawdziwą aplikację i DEMO: serie przeciągnięć myszy/dotyku,
   ruchy pionowe, przewijanie, zmiany tras, anulowanie, formularz i zapis,
   przypomnienia/brak przypomnienia/odłożenie/wyciszenie/ukończony dzień,
   szerokości 360/390/768/1440 px. Podaj rzeczywiste wyniki.
8. Odśwież klikalny HTML z poprawionych modułów aplikacji, bez danych klientów
   i połączenia z produkcją. Zapisz zmiany na obecnej gałęzi GitHub.
