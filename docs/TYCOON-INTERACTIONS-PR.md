Eksperymentalny Cleaning Tycoon otrzymuje reakcje na działania w prawdziwym CRM: wciśnięcia przycisków, znacznik zakładki, przejścia dnia/miesiąca i formularzy, licznik usług, płynny postęp oraz krótkie sceny po potwierdzonym zapisie. Klasyczny wygląd pozostaje domyślny.

- Wspólny moduł CSS/Web Animations zastępuje poprzedni efekt tego samego elementu; ruch można ograniczyć, respektowana jest preferencja systemowa i zatrzymanie poza ekranem/tłem. Fokus i operacje biznesowe nie czekają na animację.
- API rezerwacji zwraca ID i rodzaj efektu dopiero po zapisie; formularz wymaga `saved:true`. Zwykłe formularze korzystają z jednorazowego flasha serwera. `?saved=1` samo nie uruchamia sceny. Krótkie przejścia między stronami przechowują tylko metadane efektu w pamięci sesji przeglądarki.
- Wykonanie pokazuje czyszczenie, zamknięcie dnia krótki błysk, istniejące odznaki mają pikselowe iskierki. Nie zmieniono naliczania odznak, modeli biznesowych, historycznych danych ani migracji V1–V14. OWNER i CSRF zachowane.

Weryfikacja: Maven Wrapper `clean verify`, 27 jednostkowych/MVC +78 integracyjnych PostgreSQL/Testcontainers =105; zero błędów, porażek i pominięć. Chromium na osobnym schemacie lokalnym: 360/390/768/1440 px, osiem wizyt, szybkie klikanie, dwie kategorie,503/ponowienie i HTTP200 z `saved:false`, poprawny ID, fokus, wykonanie, cele, zamknięcie, restart. Powtarzalny `scripts/verify-browser.py --pixel-tycoon` rozszerzono o te przypadki.

ZIP w `preview/` zawiera 60 rzeczywistych widoków z tymi samymi modułami UI i jawnymi adapterami demonstracyjnymi. Sprawdzono formularze, nawigację, reakcję po zapisie między nieprzezroczystymi ramkami, postęp/odznaki, ograniczony ruch, klasyczny wygląd i zagnieżdżoną ramkę. Dane i zapis HTML są syntetyczne, w pamięci karty. Dokumentacja: `docs/TYCOON-INTERACTIONS.md`, wyniki: `docs/audit/tycoon-interactions-evidence.json`, sumy paczki: `preview/build.json`.

Bazą PR jest istniejący eksperyment `experiment/cleaning-tycoon-progress`; ten przyrost nie wdraża niczego na produkcję i nie scala do main.
