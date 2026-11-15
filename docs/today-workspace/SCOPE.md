# Zakres zaakceptowanego prompta

Rozbudowa istniejącego ekranu Dzisiaj: główny cel, wykonany przychód i wynik po
znanych kosztach za miesiąc/tydzień, pixelowy wykres słupkowy, nawigacja do adresu
wizyty, przypięte notatki i cele oraz przypomnienia wewnątrz aplikacji.

Kalendarz pokazuje miesiąc w siedmiu kolumnach na wszystkich szerokościach;
telefon otwiera szczegóły dnia w dolnym panelu. Obszary pozostają w kalendarzu,
bez osobnej sekcji na Dzisiaj. Dane liczbowe używają czytelnej czcionki.

Tankowanie: data/godzina, aktualny przebieg, litry i cena za litr. Kwota jest
liczona na serwerze; szczegóły i rachunek opcjonalne. Stara historia, odczyty
samochodu/agregatu, załączniki i bezpieczne usuwanie pozostają dostępne.
Zakup paliwa nie jest ponownie doliczany do już oszacowanego zużycia.

Przypomnienia: jednorazowe, codziennie, wybrane dni tygodnia i co N dni,
Europe/Warsaw. Wykonanie wystąpienia, odłożenie i trwałe ukończenie wpisu mają
osobne znaczenie. Stan i potwierdzenie pierwszego pokazania są w PostgreSQL.

Warunki: osobna gałąź, bez PR/scalenia, bez produkcji i nowych modułów,
Thymeleaf/JS, OWNER/CSRF, zachowanie danych i wykonanych migracji. Rzeczywiste
testy i klikalne DEMO HTML z wyraźnym odróżnieniem od trwałego zapisu aplikacji.
