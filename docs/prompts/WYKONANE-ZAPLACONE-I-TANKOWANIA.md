# Uproszczenie rozliczeń ncLEAN

Pracuj w istniejącej aplikacji Spring Boot/Thymeleaf, zachowując PostgreSQL, OWNER, CSRF, historyczne ceny i wszystkie zapisane dane.

1. Wykonane zlecenie oznacza zapłaconą pełną kwotę końcową w dniu usługi. Przy niepotwierdzonej cenie zachowaj dotychczasową wycenę oraz ostrzeżenie o niepełnych danych. Zaplanowane, anulowane i usunięte zlecenia nie dają rzeczywistego przychodu.
2. Usuń z codziennego interfejsu dopisywanie wpłat, korekty wpłat i należności. Dawne transakcje zachowaj jako archiwum tylko do odczytu. Nie generuj fikcyjnych transakcji i nie doliczaj ich drugi raz do wyniku. Wycofaj stare endpointy zapisu wpłat z jednoznacznym komunikatem; nadal wymagaj uwierzytelnienia, OWNER i CSRF.
3. Zmiana końcowej kwoty, statusu, terminu oraz usunięcie/przywrócenie zlecenia muszą spójnie aktualizować wynik, raporty i eksport. Dawne wpłaty nie blokują tych czynności i pozostają w historii. Zachowaj blokady wersji i audyt.
4. Samochód = diesel, agregat = benzyna. Ustawiam początkowy licznik przy pełnym zbiorniku. Następnie przy tankowaniu do pełna podaję datę, przebieg lub motogodziny, litry i kwotę. Cena/litr obliczana automatycznie; spalanie między pełnymi zbiornikami jest informacją pomocniczą.
5. Cała kwota każdego aktywnego tankowania obniża wynik operacyjny w dniu zakupu, również bez zleceń tego dnia. Nie dziel przebiegu na prywatny i firmowy. Nie odejmuj drugi raz kosztu wyliczonego ze spalania. Sam początkowy stan licznika nie jest wydatkiem; rzeczywisty rachunek za pierwsze napełnienie można zapisać jako tankowanie.
6. Zachowaj prosty formularz paliwa, historię i rachunki. Nie zmieniaj wykonanych migracji, nie łącz się z produkcją i nie dodawaj nowych modułów.
7. Sprawdź na rzeczywistym PostgreSQL: wykonanie i korektę ceny, zmianę statusu/terminu, archiwalne wpłaty bez podwojenia przychodu, kasowanie/przywrócenie, restart, tankowania obu urządzeń i wyniki różnych okresów. Sprawdź rzeczywisty interfejs w przeglądarce i podaj faktyczne wyniki.
