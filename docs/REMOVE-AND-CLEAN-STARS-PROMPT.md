# Prompt: usuwanie pomyłek i spokojne animacje ncLEAN

Pracuj w istniejącym Spring Boot/Thymeleaf CRM na obecnej gałęzi. Nie twórz
nowego prototypu ani Pull Requesta. Zachowaj OWNER, CSRF, PostgreSQL i dane.

Dodaj spójne, widoczne „Usuń” dla zleceń, kategorii/usług/chemii w cenniku,
wydatków i tankowań oraz archiwizację sprzętu. Jedno krótkie potwierdzenie
z konkretną nazwą; anulowanie nic nie zmienia. Błąd lub konflikt wersji zachowuje
formularz. Zapobiegaj podwójnemu żądaniu. Usunięcie ma być odwracalne i nie
może kasować historycznych cen, zdjęć, wpłat, zużycia ani powiązań. Dla kosztów
wykorzystaj istniejące anulowanie, zamiast liczyć równolegle drugi rodzaj kosztu.
Nie usuwaj zlecenia z nieskorygowaną wpłatą ani aktywnym pomiarem czasu.

Usunięte zlecenia wyłącz z kalendarza, wyszukiwania, statystyk, sugestii zużycia,
list kontaktu i przypomnień; zapewnij widok usuniętych i przywrócenie. Usunięta
kategoria znika wraz z ofertą swoich usług z nowej rezerwacji; stare rezerwacje
zachowują zakres i cenę. Przywrócenie kategorii nie zmienia wcześniejszej
aktywności usług. Środki nadal potrzebne do standardów lub rozliczenia dawnych
wizyt zachowaj; jasno blokuj usunięcie aktywnie używanego środka.

Dodaj nową migrację, nie zmieniaj V1–V14. Nie unieważniaj starych zamknięć dnia
samym dodaniem technicznej kolumny. Zmiana faktycznej zawartości dnia powinna
ponownie wymagać jego sprawdzenia. Zapisuj audyt usunięcia/przywrócenia.

Po potwierdzonym zakończeniu zlecenia pokaż krótką animację kilku niebieskich,
błękitnych i białych gwiazdek czystości. Bez blokowania dotyku, dźwięku,
przesuwania układu i zasypywania ekranu. Nie pokazuj jej po błędzie, usuwaniu
ani anulowaniu. Nie nakładaj komunikatu osiągnięcia, scenki i przypomnienia
ncLEANKA na siebie. Ograniczenie animacji i ukryta karta wyłączają efekty.
Ruch ncLEANKA pozostaje wyłącznie lewo/prawo przy dolnej krawędzi.

Sprawdź bazę i HTTP: usunięcie/przywrócenie, relacje i ceny historyczne,
wykluczenie ze statystyk, brak duplikatów, konflikt wersji, OWNER/CSRF i restart.
W przeglądarce przy 360/390/768/1440 px sprawdź potwierdzenie/anulowanie,
rzeczywiste zniknięcie/przywrócenie, błędy i animacje. Odśwież interaktywny HTML
z poprawionej aplikacji. Wyraźnie odróżnij symulację HTML od prawdziwej bazy.
