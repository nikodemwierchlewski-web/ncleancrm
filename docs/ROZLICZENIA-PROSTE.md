# Proste rozliczenia: wykonane = zapłacone

W szczegółach wizyty wybierz wykonanie i potwierdź kwotę końcową. CRM od razu uznaje całą tę kwotę za zapłaconą w dniu usługi. Nie wpisujesz dodatkowej wpłaty, jej metody ani daty. Dotyczy to również starych zleceń wprowadzanych po czasie.

Przychód rzeczywisty obejmuje wyłącznie wykonane, nieusunięte zlecenia. Zmiana kwoty końcowej aktualizuje wynik; zmiana daty przenosi przychód do właściwego okresu. Anulowanie, przywrócenie do planu i usunięcie wyłączają kwotę z wykonania. Przywrócenie usuniętego wykonanego zlecenia ponownie ją uwzględnia. Wycen i wcześniejszych zapisów audytu nie nadpisujemy. Jeżeli stare wykonane zlecenie nie ma jeszcze ceny końcowej, pozostaje dotychczasowe zastępstwo wyceną, z informacją o niepotwierdzonej cenie.

Dawne wpłaty i korekty zostają w PostgreSQL oraz w rozwijanym archiwum szczegółów zlecenia. Nie wpływają drugi raz na wynik i nie blokują korekt zlecenia. Stare formularze zapisu wpłat otrzymują HTTP 410 z wyjaśnieniem; uwierzytelnienie, OWNER i CSRF nadal obowiązują. Nie tworzymy sztucznych transakcji wyrównujących. Eksport `orders.csv`, w polu `paid`, pokazuje teraz pełną kwotę wykonanego zlecenia albo zero dla pozostałych statusów.

## Paliwo — pełny zbiornik do pełnego

1. W „Samochód i agregat” ustaw początkowy przebieg auta i motogodziny agregatu, gdy zbiorniki są pełne.
2. Po jeździe/pracy zatankuj wybrany zbiornik do pełna. Wybierz samochód (diesel) lub agregat (benzyna).
3. Wpisz datę, aktualny licznik, liczbę litrów i kwotę z paragonu. Cenę litra CRM wyliczy sam.
4. Przy kolejnych pełnych tankowaniach zobaczysz spalanie l/100 km albo l/mth. Nie musisz prowadzić codziennych odczytów.

Cała kwota każdego aktywnego tankowania jest kosztem operacyjnym według daty zakupu. Nie ma podziału na jazdę prywatną i firmową. Spalanie jest statystyką i nie tworzy drugiego kosztu. Koszt może wystąpić również w dniu bez zleceń; wynik dnia może wtedy być ujemny.

Sam stan początkowy licznika nie oznacza wydatku. Jeżeli płacisz za pierwsze napełnienie, zapisz również tankowanie z kwotą i litrami — ten paragon także obniży wynik. Bez znanego wcześniejszego pełnego zbiornika pierwsze tankowanie stanowi punkt odniesienia; jego kwota nadal jest kosztem.

Przykład bez innych kosztów: usługa 1000 zł, diesel 200 zł i benzyna 50 zł w październiku dają przychód 1000 zł i wynik operacyjny 750 zł. Korekta diesla do 210 zł daje wynik 740 zł. Miesięczny zakres obejmuje wszystkie zakupy miesiąca, także dokonane po dniu usługi. Zakres „do dzisiaj” nie obejmuje przyszłych dat.

## Aktualizacja i dane

Zmiana nie wymaga nowej migracji ani usunięcia danych. Pozostają istniejące migracje V1–V20, wolumen PostgreSQL i lokalne sekrety. Przed aktualizacją wykonaj backup istniejącej bazy, a następnie zbuduj aktualną wersję aplikacji według [DOCKER-PC.md](DOCKER-PC.md). Samo pobranie kodu nie aktualizuje już działającego kontenera. Nie używaj `docker compose down -v`.

Prompt wykonawczy: [WYKONANE-ZAPLACONE-I-TANKOWANIA.md](prompts/WYKONANE-ZAPLACONE-I-TANKOWANIA.md).

W dotychczasowym klonie, przed aktualizacją kontenerów, sprawdź `git status --short`. Jeżeli są własne zmiany, najpierw je zachowaj — nie resetuj katalogu. Dla czystego katalogu pobierz właściwą gałąź:

```powershell
git fetch origin
git switch feat/completed-means-paid
git pull --ff-only origin feat/completed-means-paid
.\scripts\CRM-PC.ps1 -Action Update
```

Nie uruchamiaj ponownie inicjalizacji sekretów ani bazy podczas aktualizacji istniejącej instalacji.
