# Plan tygodniowy, cena uzgodniona i prognoza chemii

Dane zapisuje prawdziwa aplikacja Spring Boot w PostgreSQL. Po aktualizacji Flyway wykonuje nowe migracje V4 i V5; V1–V3 pozostają niezmienione. Nie usuwaj katalogu `.local` ani wolumenu PostgreSQL. Instalacja i uruchomienie: [WINDOWS.md](WINDOWS.md).

## Obszary według dni tygodnia

W **Ustawienia → Obszary pracy → Tygodniowy plan obszarów** wybierz obszary dla każdego dnia i datę obowiązywania. Przykład: poniedziałek — Krzyki, wtorek — Fabryczna, środa — dwa obszary. Brak zaznaczenia oznacza brak obszarów w tym dniu tygodnia.

Plan można zapisać od dziś lub daty przyszłej. Wersja obowiązuje od swojej daty do kolejnej zapisanej wersji. Odnośniki „Plan od…” wczytują zapisane wersje. Aby poprawić plan historyczny bez zmiany historii, wybierz datę obowiązywania dziś lub później.

W kalendarzu **Obszary dnia** pokazują źródło: **Z planu tygodniowego** lub **Ustawiono indywidualnie**. Zapis indywidualnego dnia ma pierwszeństwo przed planem, również jeśli odznaczysz wszystkie pola. **Przywróć plan tygodniowy** usuwa wyjątek i ponownie stosuje wersję planu właściwą dla tej daty. Wcześniejsze ręczne przypisania zostały zachowane jako wyjątki.

Dezaktywacja nie usuwa istniejących przypisań ani wersji planu. Nieaktywny obszar można zachować w istniejącym przypisaniu; nie można dopisać go jako nowego obszaru. Plan nie przenosi zleceń i nie rozpoznaje adresów.

## Kategorie i usługi

W **Ustawienia → Cennik** rozwiń kategorię. Możesz zmienić jej nazwę, dodać usługę bezpośrednio w kategorii i edytować istniejące usługi. Edycja umożliwia przeniesienie usługi do innej kategorii oraz dezaktywację bez usuwania historii.

Usługa ma jednostkę, sugerowaną cenę jednostkową, orientacyjny czas oraz osobno edytowany standard chemii. Dane kategorii i usług są trwałe, nie są ponownie importowane przy starcie.

## Opakowania i standard chemii

W sekcji **Środki chemiczne — opakowania i koszty** dodaj nazwę, ilość i jednostkę opakowania oraz cenę. Cena jest wymagana; 0 zł można podać świadomie. Przeliczenia:

- l → ml, współczynnik 1000;
- kg → g, współczynnik 1000;
- ml, g i szt. pozostają w swojej jednostce.

Koszt jednostki jest obliczany z ceny i zawartości opakowania. Nie można zmienić rodzaju jednostki (np. ml na g), jeśli środek ma przypisany standard usługi. Wtedy dodaj osobny środek. Zmiana l na ml lub kg na g jest dozwolona, po poprawieniu ilości opakowania.

W usłudze rozwiń **Standardowe zużycie chemii** i wpisz ilości na jedną jednostkę usługi. Formularz pokazuje na bieżąco koszt pozycji i sumę. Dodatnie ilości zapisują standard. Jeśli świadomie nie używasz chemii, zaznacz **Standard uzupełniony (również zużycie 0)**. Niezaznaczone pole i brak ilości oznaczają **Nie określono**, a nie darmową usługę.

## Umawianie zlecenia

Wybierz usługi i ilości, również z kilku kategorii. W podsumowaniu każda pozycja pokazuje sugerowaną cenę i edytowalne pole **Cena dla tego zlecenia**. Zmiana 180 → 220 zł dotyczy wyłącznie nowej rezerwacji; nie przepisuje cennika.

Rozwiń **Ile chemii przewidujesz zużyć?**. Standard jest mnożony przez ilość usługi. Możesz poprawić ilości, dodać środek, usunąć pozycję lub wpisać 0. Ręcznie poprawione i dodatkowe pozycje pozostają przy zmianie ilości usługi; automatyczne pozycje przeliczają się nadal. **Przywróć standardowe zużycie** usuwa ręczne poprawki danej usługi i ponownie mnoży standard przez bieżącą ilość.

Brak standardu nie blokuje zapisu. Dla kompletnej prognozy zaznacz **Prognoza uzupełniona (również zużycie 0)**. Odznaczona prognoza jest nieokreślona; jej ilości nie są wysyłane. Przy częściowej prognozie suma wskazuje znane koszty oraz nieokreślone pozycje. To przewidywany koszt chemii, bez pracy, dojazdu i pozostałych kosztów; nie jest nazywany zyskiem.

## Historia, precyzja i zabezpieczenia

Rezerwacja kopiuje nazwy/jednostki usług, cenę sugerowaną i uzgodnioną oraz prognozy dla każdej pozycji. Prognoza kopiuje nazwę środka, jednostkę, ilość i koszt jednostki. Późniejsze zmiany cennika i standardów nie przeliczają rezerwacji. Szczegóły kalendarza i historia klienta pokazują zapisane prognozy.

W dawnych zleceniach cena sugerowana jest zachowana na podstawie ówczesnej zapisanej ceny (przed V5 aplikacja zawsze używała ceny katalogowej). Prognozy nie są dopisywane wstecz: pozostają **Nie określono**.

Serwer używa BigDecimal. Ilości chemii mają do trzech miejsc po przecinku; automatyczne przemnożenie standardu jest zaokrąglane do 0,001 jednostki bazowej. Koszt jednostki ma 16 miejsc po przecinku, kwoty pozycji 2 miejsca, a sumy są sumami zaokrąglonych pozycji. JS pokazuje podpowiedzi; autorytatywne wyliczenie i walidację wykonuje serwer.

Zapis rezerwacji i prognoz jest jedną transakcją. Token zapobiega powieleniu tego samego zlecenia. Nadal wymagane są OWNER i CSRF. Nie dodano magazynu, rzeczywistego rozliczania chemii, finansów, SMS ani GPS; aplikacja nie korzysta z produkcji nclean.pl.
