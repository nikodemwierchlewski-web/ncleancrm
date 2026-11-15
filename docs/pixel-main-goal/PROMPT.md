# Zastosowany zakres: pixelowy interfejs i główny cel

Rozwijaj istniejący ncLEAN CRM na osobnej gałęzi. Zachowaj modele, dane,
Spring Boot, Thymeleaf, PostgreSQL, OWNER i CSRF. Nie twórz drugiego CRM.
Nie łącz z produkcją, nie twórz PR ani nie scalaj zmian.

1. Rozwiń spójny styl 8-bit z izometrycznymi ilustracjami firmy sprzątającej.
   Wykorzystaj istniejący koordynator animacji. Reakcje potwierdzające zapis
   uruchamiaj dopiero po odpowiedzi serwera. Efekty nie mogą zasłaniać pól,
   konkurować z dialogami ani wpływać na zapis. Uwzględnij ograniczenie ruchu,
   pauzę poza ekranem i ukrytą kartę. Zachowaj pozycję i rozmiar ncLEANKA.
2. W Notatkach i celach umożliw jawny wybór jednego głównego celu,
   niezależnie od przypiętych wpisów. Zapisuj wybór w PostgreSQL, również
   po restarcie. Zmiana wyboru ma być atomowa i chroniona przed konfliktami.
3. Pokaż na dashboardzie kartę celu z pixelowym wykresem rzeczywistego
   postępu, tytułem, terminem i kolejnym krokiem do odhaczenia. W kalendarzu
   pokaż niewielki, domyślnie zwinięty skrót. Nie powielaj celu w odznakach.
4. Postęp licz z istniejących kroków. Nie wymyślaj procentu dla celu bez kroków.
   Ukończenie wymaga świadomego potwierdzenia. Archiwizacja usuwa wybór
   głównego celu, zachowując wpis i historię kroków. Przywrócenie z archiwum
   nie wybiera celu ponownie. Nie zmieniaj wykonanych migracji.
5. Sprawdź walidację, CSRF/OWNER, dwa równoczesne zapisy, powtórzenie
   żądania, migrację ze starej bazy oraz restart. Przeklikaj prawdziwą aplikację
   w Chromium przy 360, 390, 768 i 1440 px, również błąd sieci i ponowienie.
6. Przygotuj klikalny HTML na podstawie prawdziwych widoków z danymi
   syntetycznymi, wyraźnie oddzielony od trwałego zapisu aplikacji.
   Podaj rzeczywiste wyniki i ograniczenia podglądu platformy.
