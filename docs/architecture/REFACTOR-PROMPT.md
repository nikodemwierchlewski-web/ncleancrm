# Prompt: fundament rozbudowy ncLEAN CRM

Pracuj na osobnej gałęzi od pełnej obecnej implementacji, zachowując klientów,
rezerwacje, historyczne ceny, zdjęcia, role OWNER/CSRF i migracje V1–V15.
Nie twórz drugiej aplikacji, Reacta, mikroserwisów ani nowych modułów biznesowych.
Nie wdrażaj na produkcję i nie twórz Pull Requesta.

Najpierw zaudytuj kod i zależności. Przyjmij modularny monolit: pakiety domenowe,
kontrolery jako adaptery HTTP, transakcje w usługach aplikacyjnych, jawne kontrakty
między obszarami, współdzielony kod techniczny bez zależności od domen.

Zastosuj pierwszy kompletny etap refaktoryzacji:
1. Usuń SQL i granice transakcji z kontrolerów. Przenieś zapis profilu klienta
   wraz ze źródłem pozyskania do jednej transakcji i jawnego polecenia. Oddziel
   zapytania eksportu od HTTP. Napraw eksport usuniętych rezerwacji i dodaj regresję.
2. Przenieś wspólne daty/liczby do małych, niezależnych klas. Moduły kosztów,
   paliwa i notatek nie mogą zależeć od dashboardu tylko po walidację lub datę.
3. Rozbij centralny serwis usuwania: domeny rejestrują własne implementacje
   wspólnego kontraktu. Koordynator nie zna tabel ani innych usług domenowych.
   Wspólny mechanizm ma zachować blokady, jedną transakcję, audyt, wersje,
   idempotencję i reguły biznesowe. Nowy rodzaj usuwania nie wymaga edycji
   koordynatora. Użyj jawnych typów zamiast map dla nowego kontraktu.
4. Dodaj wykonywane w Maven/CI testy architektury: brak SQL/JDBC/repozytoriów
   i transakcji w kontrolerach, brak zależności shared od domen, brak zależności
   usług od kontrolerów oraz brak cykli między pakietami logiki biznesowej.
   Oddziel orkiestrację ekranów od domen, zamiast ukrywać cykle wyjątkami.
5. Sprawdź walidację, rollback profilu klienta, usuwanie/przywracanie i audyt,
   konflikt wersji, JSON i CSV, role/CSRF oraz start/restart na PostgreSQL.
   Zachowaj istniejące adresy, formularze i wygląd. Ponów pełne verify i odpowiednie
   scenariusze przeglądarkowe; podaj rzeczywiste wyniki.
6. Zapisz audyt przed/po, mapę odpowiedzialności i zależności oraz krótką zasadę
   dodawania nowej funkcji. Zaplanuj kolejne etapy rozdzielenia CrmRecords,
   typowanych modeli i testów integracyjnych. Nie ogłaszaj całego długu usuniętym.
7. Uwzględnij znane blokery nclean.pl/crm: wspólna obsługa ścieżek i izolacja PWA
   pozostają oddzielnym etapem. Refaktoryzacja nie oznacza gotowego wdrożenia.
