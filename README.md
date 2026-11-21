# Tablica wyników na stadion — MVP

Projekt szkoleniowy. Budujesz od zera system do obsługi ekranu wyników na stadionie i przy okazji przechodzisz przez cały proces wytwarzania oprogramowania: od pustego repozytorium po wersję gotową do uruchomienia na stadionie.

Pracujesz etapami. **Każdy etap to osobna gałąź i osobny Pull Request do review.** Nie zaczynaj kolejnego etapu, dopóki poprzedni nie jest zmergowany (wyjątek: możesz czytać materiały do następnego etapu, czekając na review).

---

## 1. Co budujemy

System składa się z trzech części:

| Część | Kto używa | Co robi |
|---|---|---|
| **Serwer** (backend) | — | Przechowuje mecze, wynik, stan zegara i sponsorów. Jest jedynym źródłem prawdy. Wysyła zmiany do ekranu w czasie rzeczywistym. |
| **Panel operatora** (klient web) | Operator przy stole sędziowskim / w reżyserce | Tworzy mecz, startuje i kończy połowy, dodaje i cofa gole, ustawia doliczony czas, zarządza sponsorami. |
| **Ekran** (klient web, tryb pełnoekranowy) | Kibice na stadionie | Pokazuje drużyny, wynik i czas gry. Przed meczem, w przerwie i po meczu wyświetla rotację reklam sponsorów. |

Ekran to strona internetowa otwarta w przeglądarce w trybie kiosk. Komputer jest podłączony do telebimu (np. przez HDMI). **Integrację ze sprzętem** (sterowniki LED, tablice dedykowane) **na razie pomijamy.** Projektujemy tylko miejsce, w którym kiedyś się ją podłączy.

### Zakres MVP

- Mecz: drużyna gospodarzy i gości, data i godzina, długość połowy (domyślnie 45 min, np. 40 min dla juniorów).
- Przebieg meczu: `ZAPLANOWANY → I_POŁOWA → PRZERWA → II_POŁOWA → ZAKOŃCZONY`.
- Zegar: start i koniec połowy, pauza i wznowienie, ręczna korekta czasu.
- Doliczony czas: operator ogłasza liczbę minut, a ekran pokazuje np. `+3`. Po przekroczeniu regulaminowego czasu zegar pokazuje `45+2'`.
- Wynik: gol dla gospodarzy lub gości, cofnięcie gola (np. po decyzji VAR lub pomyłce operatora).
- Ekran: tryb „wynik” w trakcie gry oraz tryb „reklamy + wynik” przed meczem, w przerwie i po meczu.
- Sponsorzy: nazwa, grafika, czas wyświetlania, kolejność, aktywny/nieaktywny.
- Logowanie operatora i administratora.

### Poza zakresem (świadomie)

Integracja ze sprzętem, wiele stadionów naraz (multi-tenancy), kartki, zmiany, składy, statystyki, dogrywka i karne, aplikacja mobilna, płatności dla sponsorów. Jeśli coś z tej listy wydaje Ci się potrzebne, zapisz to jako issue z etykietą `pomysł`. Nie implementuj tego.

---

## 2. Stos technologiczny

| Obszar | Technologia |
|---|---|
| Język backendu | Kotlin, JDK 25 |
| Build | Gradle Kotlin DSL, wersje zależności centralnie w `gradle/libs.versions.toml` |
| Framework | Spring Boot (Web, Validation, Data JPA, JDBC, Security, Actuator) |
| Baza danych | PostgreSQL + migracje Flyway |
| Testy | Kotest (specyfikacje i asercje), MockMvc, Testcontainers |
| API | REST + JSON, dokumentacja OpenAPI, czas rzeczywisty przez SSE |
| Klient | React + TypeScript + Vite; klient API generowany z OpenAPI |
| Uruchomienie | Docker Compose |
| CI | GitHub Actions |

### Docelowa struktura repozytorium

```
.
├── backend/
│   ├── common/      # wspólne: obsługa błędów, typy bazowe
│   ├── matches/     # mecze, zegar, wynik
│   ├── sponsors/    # sponsorzy i grafiki
│   ├── display/     # stan ekranu i strumień SSE
│   └── app/         # uruchomienie, konfiguracja, security, połączenie modułów
├── frontend/        # jedna aplikacja: /panel (operator) i /display (ekran)
├── docs/
│   ├── adr/         # decyzje architektoniczne
│   └── ops.md       # uruchamianie i utrzymanie
├── gradle/libs.versions.toml
└── docker-compose.yml
```

Każdy moduł backendu ma ten sam podział pakietów:

- `domain` — modele i reguły biznesowe; czysty Kotlin, bez Springa i JPA,
- `application` — przypadki użycia (serwisy) i porty (interfejsy),
- `adapter.inbound.web` — kontrolery HTTP,
- `adapter.outbound.persistence` — zapis i odczyt z bazy.

---

## 3. Zasady pracy

### Git i Pull Requesty

1. `main` jest chroniony: nie pushujesz do niego bezpośrednio.
2. Na każdy etap zakładasz gałąź `etap-NN-krotki-opis`, np. `etap-03-zegar-meczu`.
3. Commity są małe i opisują *co* i *dlaczego*, np. `Dodaj liczenie doliczonego czasu w drugiej połowie`.
4. Gdy etap jest gotowy, otwierasz PR. W opisie umieszczasz:
   - co zrobiłeś,
   - jak to sprawdzić (komendy, kroki),
   - odpowiedzi na **pytania na review** z danego etapu,
   - czego nie jesteś pewien. Śmiało pisz, to nie jest egzamin.
5. PR jest mergowany po akceptacji reviewera i zielonym CI (squash merge).
6. Uwagi z review poprawiasz kolejnymi commitami w tej samej gałęzi. Nie rób force-pusha w trakcie review.

### Zanim oznaczysz PR jako gotowy

- [ ] `./gradlew build` przechodzi lokalnie (a od etapu 12 także `npm run build` i testy frontendu).
- [ ] Nowa logika ma testy.
- [ ] Brak zakomentowanego kodu, `println` i plików IDE w repo.
- [ ] Nazwy klas, funkcji i zmiennych są czytelne bez komentarza.
- [ ] Przejrzałeś własny diff na GitHubie linijka po linijce.

### Organizacja

Załóż na GitHubie **issue dla każdego etapu** (skopiuj sekcję „Do zrobienia” i „Gotowe, gdy”) i podepnij je pod milestone `MVP`. PR zamyka issue słowem `Closes #N` w opisie.

---

## 4. Model domeny w skrócie

```
Match
 ├─ id: UUID
 ├─ homeTeam, awayTeam: String
 ├─ kickoffAt: Instant
 ├─ periodLength: Duration                  (domyślnie 45 min)
 ├─ status: SCHEDULED | FIRST_HALF | HALF_TIME | SECOND_HALF | FINISHED
 ├─ clock: MatchClock
 │    ├─ running: Boolean
 │    ├─ startedAt: Instant?                (kiedy zegar ostatnio ruszył)
 │    └─ elapsedBeforeStart: Duration       (czas zebrany przed ostatnim startem)
 ├─ addedTime: Map<Period, Int>             (ogłoszony doliczony czas w minutach)
 └─ version: Long                           (optymistyczne blokowanie)

MatchEvent                                  (wynik liczymy ze zdarzeń, nie trzymamy „na sztywno”)
 ├─ id: UUID                                (nadaje klient, co daje idempotencję)
 ├─ matchId
 ├─ type: GOAL | GOAL_REVERSAL
 ├─ team: HOME | AWAY
 ├─ reversedEventId: UUID?                  (dla cofnięcia gola)
 └─ occurredAt: Instant, matchMinute: String

Sponsor
 ├─ id, name, imagePath, displaySeconds, position, active
```

**Dlaczego wynik liczymy ze zdarzeń?** Gol nigdy nie jest „kasowany”. Cofnięcie to nowe zdarzenie, które unieważnia poprzednie. Dzięki temu zawsze wiadomo, co się stało i kiedy, a podwójne kliknięcie operatora nie doda dwóch goli.

---

## 5. Szkic API

| Metoda | Ścieżka | Rola | Opis |
|---|---|---|---|
| POST | `/api/auth/login` | — | logowanie, zwraca JWT |
| GET | `/api/matches?page=0&size=20` | OPERATOR | lista meczów |
| POST | `/api/matches` | OPERATOR | utworzenie meczu |
| GET | `/api/matches/{id}` | OPERATOR | szczegóły meczu z wynikiem |
| POST | `/api/matches/{id}/start-period` | OPERATOR | start połowy |
| POST | `/api/matches/{id}/end-period` | OPERATOR | koniec połowy |
| POST | `/api/matches/{id}/clock/pause` / `resume` | OPERATOR | pauza i wznowienie zegara |
| PUT | `/api/matches/{id}/clock` | OPERATOR | ręczna korekta czasu |
| PUT | `/api/matches/{id}/added-time` | OPERATOR | ogłoszenie doliczonego czasu |
| POST | `/api/matches/{id}/events` | OPERATOR | gol (z `id` nadanym przez klienta) |
| POST | `/api/matches/{id}/events/{eventId}/reversal` | OPERATOR | cofnięcie gola |
| PUT | `/api/display/active-match` | OPERATOR | który mecz pokazuje ekran |
| GET | `/api/display/current` | publiczne | aktualny stan ekranu |
| GET | `/api/display/stream` | publiczne | strumień SSE ze zmianami |
| GET/POST/PUT/DELETE | `/api/sponsors…` | ADMIN | zarządzanie sponsorami |
| GET | `/api/sponsors/{id}/image` | publiczne | grafika sponsora |

Szczegóły ustalisz sam w trakcie etapów. To jest kierunek, nie gotowa specyfikacja.

---

## 6. Etapy

Każdy etap ma tę samą strukturę:
**Czego się uczysz** → **Do zrobienia** → **Gotowe, gdy** → **Pytania na review**.

Szacowany czas podano orientacyjnie. Jeśli utkniesz na dłużej niż pół dnia, pytaj.

---

### Etap 0 — Repozytorium i sposób pracy
Gałąź: `etap-00-repo` · ok. 0,5 dnia

**Czego się uczysz:** Git (clone, branch, commit, push, pull, rebase vs merge), GitHub (issues, milestone, PR, review, ochrona gałęzi), Markdown.

**Do zrobienia:**
- Utwórz repozytorium `stadium-scoreboard` na GitHubie. Ten plik wklej jako `README.md` (pierwszy commit może trafić bezpośrednio do `main`).
- Włącz ochronę `main`: wymagany PR i co najmniej 1 akceptacja.
- Dodaj `.gitignore` (Gradle, IntelliJ, Node, `.env`), `.editorconfig` i szablon PR `.github/pull_request_template.md` z sekcjami z punktu 3.
- Załóż milestone `MVP` i issues dla etapów 1–17.
- Dodaj mentora jako współpracownika z prawem review.

**Gotowe, gdy:** pierwszy PR (np. z szablonem PR i `.gitignore`) przeszedł review i jest zmergowany.

**Pytania na review:** Czym różni się `git merge` od `git rebase`? Dlaczego nie commitujemy pliku `.env`?

---

### Etap 1 — Narzędzia i szkielet projektu
Gałąź: `etap-01-szkielet` · 1 dzień

**Czego się uczysz:** terminal, IntelliJ IDEA, JDK vs JRE (JDK do budowania, JRE do uruchamiania), Gradle Kotlin DSL, multi-module build, `libs.versions.toml`, debugger, czytanie błędów kompilacji.

**Do zrobienia:**
- Wygeneruj projekt Spring Boot (Kotlin, Gradle KTS, JDK 25) i ułóż moduły `backend/common` i `backend/app`. Pozostałe moduły dodasz później.
- Przenieś wszystkie wersje zależności i pluginów do `gradle/libs.versions.toml`.
- Dodaj Spring Actuator i sprawdź, że `GET /actuator/health` zwraca `UP`.
- Dodaj GitHub Actions: workflow uruchamiający `./gradlew build` na każdym PR.
- Uruchom aplikację z IntelliJ w trybie debug, ustaw breakpoint w dowolnym kontrolerze lub filtrze i prześledź jedno żądanie.

**Gotowe, gdy:** `./gradlew build` przechodzi lokalnie i w CI, aplikacja startuje, health zwraca `UP`.

**Pytania na review:** Do czego służy `libs.versions.toml`? Co się dzieje, gdy wpiszesz `./gradlew build`, czyli jakie taski się wykonują?

---

### Etap 2 — Podstawy Kotlina: domena meczu
Gałąź: `etap-02-domena` · 2–3 dni

**Czego się uczysz** (w tej kolejności): zmienne i funkcje → klasy i `data class` → nullowalność → `enum` i `when` → kolekcje → lambdy → interfejsy → wyjątki. Następnie `let`, `apply`, `copy`, `map`, `filter`, `sortedBy`, `count`. Do tego pierwsze testy w Kotest.

**Do zrobienia** (moduł `backend/matches`, pakiet `domain`, **bez Springa**):
- `Match`, `MatchStatus`, `TeamSide (HOME, AWAY)`, `MatchEvent`, `Score`.
- Przejścia statusu jako metody zwracające **nową** kopię meczu (`copy`), np. `startFirstHalf()`, `endPeriod()`. Niedozwolone przejście (np. start II połowy z `SCHEDULED`) rzuca własny wyjątek domenowy.
- Funkcja `score(events: List<MatchEvent>): Score`, która liczy wynik z listy zdarzeń z uwzględnieniem cofnięć.
- `Sponsor` w module `backend/sponsors` oraz funkcja zwracająca aktywnych sponsorów posortowanych po `position`, a przy remisie po nazwie.
- Testy Kotest dla wszystkich przejść statusu, liczenia wyniku (w tym: cofnięcie gola, cofnięcie nieistniejącego gola, podwójne cofnięcie) i filtrowania sponsorów.

**Gotowe, gdy:** domena ma testy pokrywające poprawne i niepoprawne scenariusze, a w pakiecie `domain` nie ma żadnego importu ze Springa.

**Pytania na review:** Dlaczego `Match` jest niemutowalny? Co zwraca `score()` dla pustej listy i dlaczego?

---

### Etap 3 — Czas: zegar meczu i doliczony czas
Gałąź: `etap-03-zegar-meczu` · 2 dni

**Czego się uczysz:** `Instant`, `Duration`, `LocalDate`, `LocalDateTime`, `ZoneId`, `java.time.Clock`. Czas UTC vs czas lokalny stadionu (`Europe/Warsaw`, zmiana czasu letni/zimowy). Sterowanie zegarem w testach. Dlaczego nie liczymy czasu na `Double`.

**Do zrobienia:**
- `MatchClock` z operacjami `start(now)`, `pause(now)`, `elapsed(now): Duration`, `adjust(newElapsed)`.
- Funkcja wyliczająca to, co ma pokazać ekran, np. `displayTime(match, now)`:
  - I połowa: `00:00`–`45:00`, po przekroczeniu `45+1'`, `45+2'`…
  - II połowa liczy od `45:00` do `90:00`, potem `90+1'`…
  - Długość połowy bierzemy z meczu (40 min daje `40+1'` i `80+1'`).
- Doliczony czas: `announceAddedTime(period, minutes)`. Dozwolone tylko w trakcie danej połowy, zakres 0–15.
- Testy z `Clock.fixed(...)` i z własnym, przesuwalnym zegarem testowym: start → 10 min → pauza → 2 min → wznowienie → 5 min daje `15:00`.

**Gotowe, gdy:** wszystkie przypadki brzegowe (dokładnie 45:00, 45:00 + 1 s, pauza w doliczonym czasie, korekta zegara) mają testy, a nigdzie w domenie nie ma `Instant.now()`.

**Pytania na review:** Dlaczego wstrzykujemy `Clock`, zamiast wołać `Instant.now()`? Co się stanie z godziną meczu zapisaną jako `LocalDateTime` w noc zmiany czasu?

---

### Etap 4 — HTTP, REST, JSON i Spring Boot
Gałąź: `etap-04-rest` · 2–3 dni

**Czego się uczysz:** metody GET/POST/PUT/DELETE, adresy zasobów, nagłówki, body, paginacja, statusy 200, 201, 204, 400, 401, 403, 404, 409. Spring: bean, kontener, wstrzykiwanie przez konstruktor, `@RestController`, `@Service`, `@Configuration`, `@Bean`, profile i zmienne środowiskowe.

**Do zrobienia:**
- `MatchController` i `MatchService` z magazynem **w pamięci** (`ConcurrentHashMap`), bez bazy na tym etapie.
- Endpointy: utworzenie meczu, lista z paginacją, szczegóły z wynikiem i czasem, start i koniec połowy, gol.
- Osobne klasy DTO dla żądań i odpowiedzi. Domena nie wychodzi bezpośrednio do JSON-a.
- Bean `Clock` zdefiniowany w `@Configuration` w module `app`.
- Plik `http/matches.http` (klient HTTP w IntelliJ) z przykładowymi żądaniami: poprawnymi i błędnymi.

**Gotowe, gdy:** cały mecz da się „rozegrać” z pliku `.http`, a każde błędne żądanie zwraca sensowny status (nie 500).

**Pytania na review:** Skąd `MatchService` dostaje `Clock`, skoro sam go nie tworzy? Dlaczego utworzenie meczu zwraca 201, a nie 200?

---

### Etap 5 — Architektura modułowa i heksagonalna
Gałąź: `etap-05-architektura` · 1–2 dni

**Czego się uczysz:** odpowiedzialności warstw (`domain`, `application`, `adapter.inbound.web`, `adapter.outbound.persistence`, `app`), porty i adaptery, kierunek zależności, ADR (Architecture Decision Record).

**Do zrobienia:**
- Ułóż kod zgodnie z podziałem pakietów z punktu 2.
- Wydziel port `MatchStore` (interfejs w `application`). Magazyn w pamięci staje się jego adapterem.
- Dodaj pusty port `ScoreboardHardware` z metodą `publish(state)` i adapter „no-op”. To jest miejsce na przyszłą integrację ze sprzętem.
- Napisz `docs/adr/001-modularny-monolit-heksagonalny.md`: kontekst, decyzja, konsekwencje.
- Narysuj (Mermaid w Markdown) drogę żądania „gol”: kontroler → serwis → port → adapter.

**Gotowe, gdy:** moduł `matches` nie zależy od `sponsors` i odwrotnie, domena nie importuje niczego spoza Kotlina i `java.*`, a ADR jest w repo.

**Pytania na review:** Co zyskujemy, trzymając `MatchStore` jako interfejs? Gdzie podłączysz kiedyś sterownik LED i czego nie trzeba będzie wtedy zmieniać?

---

### Etap 6 — SQL, PostgreSQL i Flyway
Gałąź: `etap-06-baza` · 2–3 dni

**Czego się uczysz:** tabele, klucze główne i obce, `NULL`, ograniczenia (`CHECK`, `UNIQUE`), indeksy, `JOIN`, `COUNT`, `SUM`, `GROUP BY`, `CASE`, widoki, transakcje. Flyway i zasada: **zastosowanej migracji się nie zmienia, dodaje się nową.**

**Do zrobienia:**
- `docker-compose.yml` z PostgreSQL do pracy lokalnej.
- Migracje: `V1__matches.sql` (`match`, `match_event`), `V2__sponsors.sql`, `V3__display.sql` (który mecz jest aktywny na ekranie).
- Ograniczenia w bazie: status tylko z dozwolonej listy, doliczony czas 0–15, `reversed_event_id` wskazuje na zdarzenie tego samego meczu.
- Zapytanie SQL liczące wynik meczu ze zdarzeń (gole minus cofnięte gole) dla obu drużyn. Zapisz je w `docs/sql/wynik.sql` z przykładowymi danymi i wynikiem.

**Gotowe, gdy:** aplikacja startuje na pustej bazie i Flyway zakłada schemat, a zapytanie o wynik działa na danych testowych z cofniętym golem.

**Pytania na review:** Co się stanie, gdy zmienisz `V1__matches.sql` po tym, jak został już zastosowany? Które ograniczenia trzymasz w bazie, a które w domenie, i dlaczego niektóre w obu miejscach?

---

### Etap 7 — Spring Data JPA oraz JDBC
Gałąź: `etap-07-persystencja` · 2–3 dni

**Czego się uczysz:** encje JPA, repozytoria, paginacja, mapowanie encja ↔ domena, `NamedParameterJdbcTemplate`, parametryzowany SQL (i dlaczego nigdy nie sklejamy SQL-a ze stringów).

**Do zrobienia:**
- `JpaMatchStore` i `JpaSponsorStore`: adaptery portów oparte na JPA. Encje żyją tylko w `adapter.outbound.persistence`.
- `JdbcScoreboardQuery`: odczyt stanu ekranu (mecz + wynik policzony w SQL) jednym zapytaniem przez `NamedParameterJdbcTemplate`.
- Usuń magazyn w pamięci z kodu produkcyjnego (może zostać jako fake w testach).
- Lista meczów: filtr po dacie i paginacja na poziomie bazy.

**Gotowe, gdy:** po restarcie aplikacji dane są zachowane, a domena nadal nie zna JPA.

**Pytania na review:** Dlaczego domena nie importuje encji JPA? Kiedy wybierasz JPA, a kiedy czysty SQL przez JDBC?

---

### Etap 8 — Testy integracyjne: Kotest, MockMvc, Testcontainers
Gałąź: `etap-08-testy-integracyjne` · 2 dni

**Czego się uczysz:** różnica między testem jednostkowym a integracyjnym, uruchamianie kontekstu Springa w teście, prawdziwy PostgreSQL w kontenerze, żądania przez MockMvc, sprawdzanie stanu w bazie po teście, sterowany `Clock` w teście integracyjnym.

**Do zrobienia:**
- Wspólna baza testów integracyjnych w `backend/app/src/test` (kontener PostgreSQL współdzielony między specyfikacjami).
- `MatchFlowIntegrationSpec`: pełny mecz przez HTTP (utworzenie → I połowa → gol → przerwa → II połowa → gol → cofnięcie → koniec), z przesuwaniem zegara testowego i sprawdzaniem wyniku oraz czasu po każdym kroku.
- Testy błędnych danych (400) i nieistniejącego meczu (404).
- Sprawdzenie zawartości tabeli `match_event` po teście.

**Gotowe, gdy:** testy integracyjne przechodzą lokalnie i w CI (Docker w GitHub Actions).

**Pytania na review:** Co sprawdza test integracyjny, czego nie sprawdzi test jednostkowy? Jak izolujesz dane między testami?

---

### Etap 9 — Walidacja, błędy i dokumentacja API
Gałąź: `etap-09-walidacja-openapi` · 1–2 dni

**Czego się uczysz:** Jakarta Validation (`@NotBlank`, `@Size`, `@Min`, `@Max`), `ProblemDetail` (RFC 9457), globalna obsługa wyjątków (`@RestControllerAdvice`), OpenAPI (springdoc). Rozróżnienie: **niepoprawny format żądania** (400) vs **naruszenie reguły biznesowej** (409/422).

**Do zrobienia:**
- Walidacja DTO: nazwy drużyn 1–40 znaków, długość połowy 10–60 min, doliczony czas 0–15.
- `ProblemHandler` w `backend/common`: mapowanie wyjątków domenowych i walidacji na `ProblemDetail` z **polskim** komunikatem dla operatora.
- OpenAPI dostępne pod `/v3/api-docs` i Swagger UI. Opisz endpointy i przykłady.
- Testy: każdy rodzaj błędu ma test sprawdzający status i treść odpowiedzi.

**Gotowe, gdy:** żaden znany błąd nie kończy się 500, a schemat OpenAPI opisuje wszystkie endpointy.

**Pytania na review:** Dlaczego „start II połowy, gdy trwa I połowa” to nie jest 400? Jakich informacji nie wolno zwracać w treści błędu?

---

### Etap 10 — Transakcje, współbieżność i idempotencja
Gałąź: `etap-10-idempotencja` · 2–3 dni · **etap kluczowy**

**Czego się uczysz:** `@Transactional`, rollback, optymistyczne blokowanie (`@Version`), równoczesne żądania, bezpieczne ponawianie operacji, klucz idempotencji.

**Sytuacje z życia:** operator klika „Gol” dwa razy, bo sieć się przycięła. Klient ponawia żądanie po timeoucie. Dwóch operatorów (stół sędziowski i reżyserka) zmienia ten sam mecz w tej samej sekundzie.

**Do zrobienia:**
- Gol i cofnięcie mają `id: UUID` nadawany przez klienta:
  - to samo `id` i te same dane → 200 z istniejącym zdarzeniem, wynik zmienia się **raz**,
  - to samo `id` i inne dane (np. inna drużyna) → **409 Conflict**.
- Optymistyczne blokowanie meczu: równoczesna zmiana statusu albo zegara daje 409 dla jednej ze stron, a klient może pobrać świeży stan.
- Zmiana statusu i zapis zdarzenia w jednej transakcji: błąd w połowie operacji niczego nie zapisuje.
- Testy: podwójne wysłanie gola, konflikt danych przy tym samym UUID, dwa równoległe żądania (np. dwa wątki i `CountDownLatch`).

**Gotowe, gdy:** żaden scenariusz powtórzenia lub wyścigu nie zmienia wyniku dwa razy ani nie psuje stanu meczu.

**Pytania na review:** Dlaczego UUID zdarzenia nadaje klient, a nie serwer? Czym różni się blokowanie optymistyczne od pesymistycznego i dlaczego tu wystarcza optymistyczne?

---

### Etap 11 — Bezpieczeństwo: Spring Security, JWT i role
Gałąź: `etap-11-security` · 2–3 dni

**Czego się uczysz:** uwierzytelnianie vs autoryzacja, token Bearer, podpis i czas ważności JWT, haszowanie haseł (BCrypt/Argon2), `@PreAuthorize`, CORS, sekrety w zmiennych środowiskowych.

**Do zrobienia:**
- Tabela użytkowników (migracja), role `OPERATOR` i `ADMIN`. Pierwszy admin powstaje z konfiguracji przy starcie, a hasło pochodzi ze zmiennej środowiskowej, nie z repo.
- `POST /api/auth/login` zwraca JWT (np. ważny 12 h, czyli cały dzień meczowy).
- Uprawnienia zgodnie z tabelą z punktu 5. **Endpointy ekranu są publiczne i tylko do odczytu.** Wynik i tak widzi cały stadion, a przeglądarkowy `EventSource` nie wysyła nagłówka `Authorization`. Zapisz tę decyzję w `docs/adr/002-publiczny-ekran.md`.
- Testy: brak tokenu (401), zła rola (403), wygasły token (401), operator próbuje zarządzać sponsorami (403).

**Gotowe, gdy:** żadna operacja zmieniająca dane nie działa bez tokenu z właściwą rolą, a w repo nie ma żadnego sekretu.

**Pytania na review:** Czym różni się 401 od 403? Co się stanie, gdy ktoś ukradnie token, i jak ograniczamy szkody?

---

### Etap 12 — Czas rzeczywisty: zdarzenia aplikacyjne i SSE
Gałąź: `etap-12-sse` · 2 dni

**Czego się uczysz:** zdarzenia aplikacyjne Springa (`ApplicationEventPublisher`, `@TransactionalEventListener`), Server-Sent Events (`SseEmitter`), heartbeat, ponowne łączenie klienta, różnica SSE vs WebSocket vs polling.

**Do zrobienia:**
- Moduł `backend/display`: `DisplayState`, czyli wszystko, czego potrzebuje ekran (tryb, drużyny, wynik, status, stan zegara, doliczony czas, lista aktywnych sponsorów, `serverTime`).
- Każda zmiana meczu, sponsorów lub aktywnego meczu publikuje zdarzenie, a po **zatwierdzeniu transakcji** do wszystkich podłączonych ekranów idzie nowy `DisplayState`.
- Heartbeat co 15 s, żeby połączenie nie wygasało na proxy.
- **Ważne:** serwer nie wysyła czasu co sekundę. Wysyła stan zegara (`running`, `startedAt`, `elapsedBeforeStart`, `serverTime`), a klient sam odlicza. Dzięki temu ekran działa płynnie przy słabej sieci.
- Tryb ekranu wyliczany ze statusu: `SCHEDULED`, `HALF_TIME`, `FINISHED` dają `SCORE_WITH_ADS`, a `FIRST_HALF` i `SECOND_HALF` dają `SCORE_ONLY`.
- Adapter `ScoreboardHardware` (no-op z etapu 5) też dostaje każdą zmianę.

**Gotowe, gdy:** `curl -N localhost:8080/api/display/stream` pokazuje nowy stan po każdym golu dodanym z pliku `.http`, a test integracyjny to potwierdza.

**Pytania na review:** Dlaczego wysyłamy zdarzenie po commicie, a nie w trakcie transakcji? Dlaczego klient liczy czas sam, zamiast dostawać go co sekundę?

---

### Etap 13 — Klient: ekran stadionu
Gałąź: `etap-13-ekran` · 3–4 dni

**Czego się uczysz:** podstawy TypeScriptu i Reacta (komponenty, propsy, stan, `useEffect`), Vite, generowanie klienta API z OpenAPI, `EventSource`, CSS do dużych ekranów (czytelność z 50 m, kontrast, `vw/vh`), testy komponentów (Vitest + Testing Library).

**Do zrobienia:**
- `frontend/` z Vite + React + TypeScript. Typy i klient API wygenerowane ze schematu OpenAPI skryptem `npm run generate-api`.
- Trasa `/display`: pełny ekran, drużyny, wynik, zegar `MM:SS` lub `45+2'`, znacznik doliczonego czasu `+3`.
- Lokalne odliczanie zegara z korektą różnicy między zegarem serwera a przeglądarki (`serverTime`).
- W trybie `SCORE_WITH_ADS`: wynik w pasku u góry, a w głównej części rotacja grafik sponsorów, każda przez `displaySeconds`.
- Utrata połączenia: ekran **dalej pokazuje ostatni znany stan** (nigdy pusty ekran ani komunikat błędu dla kibiców), łączy się ponownie i synchronizuje przez `GET /api/display/current`.
- CI: build i testy frontendu.

**Gotowe, gdy:** po wyłączeniu i włączeniu backendu ekran sam wraca do aktualnego stanu, a zegar nie „skacze” o więcej niż sekundę.

**Pytania na review:** Co widzi kibic, gdy serwer padnie w 30. minucie? Jak sprawdziłeś czytelność na dużym ekranie?

---

### Etap 14 — Klient: panel operatora
Gałąź: `etap-14-panel` · 3–4 dni

**Czego się uczysz:** formularze i walidacja po stronie klienta, routing, przechowywanie tokenu, obsługa błędów `ProblemDetail`, UX pod presją czasu (duże przyciski, potwierdzenia, zero zgadywania).

**Do zrobienia:**
- Trasa `/panel`: logowanie, lista meczów, tworzenie meczu, wybór meczu aktywnego na ekranie.
- Ekran prowadzenia meczu: duże przyciski „Gol gospodarze” / „Gol goście”, start i koniec połowy, pauza i wznowienie, doliczony czas (szybki wybór 1–10 min), korekta zegara, lista zdarzeń z przyciskiem „Cofnij”.
- Każdy gol dostaje UUID **po stronie klienta** (`crypto.randomUUID()`), a ponowienie po błędzie sieci używa tego samego UUID.
- Panel też subskrybuje SSE, więc dwóch operatorów widzi ten sam stan. 409 pokazuje komunikat „Stan meczu zmienił się, odświeżono dane”.
- Potwierdzenie przed operacjami nieodwracalnymi: koniec meczu i cofnięcie gola.
- Podgląd ekranu (miniatura `/display`) w panelu.

**Gotowe, gdy:** osoba nietechniczna (poproś kogoś z rodziny lub znajomych) prowadzi testowy mecz bez Twojej pomocy.

**Pytania na review:** Co się dzieje, gdy operator kliknie „Gol”, a sieć zniknie na 5 sekund? Czego się dowiedziałeś z testu z osobą nietechniczną?

---

### Etap 15 — Sponsorzy end-to-end
Gałąź: `etap-15-sponsorzy` · 2–3 dni

**Czego się uczysz:** upload plików (`multipart/form-data`), walidacja typu i rozmiaru pliku, przechowywanie plików poza bazą (katalog na wolumenie), nagłówki cache, miękkie usuwanie, zmiana kolejności. To pierwsza funkcja, którą robisz samodzielnie przez wszystkie warstwy: baza, domena, API, panel i ekran.

**Do zrobienia:**
- Backend: CRUD sponsorów (rola `ADMIN`), upload grafiki (tylko PNG, JPG, WebP, maks. 5 MB), serwowanie grafiki z `Cache-Control`.
- Miękkie usuwanie: sponsor znika z rotacji i listy, ale rekord zostaje (`deleted_at`).
- Zmiana kolejności rotacji.
- Panel: zakładka „Sponsorzy” (lista, dodaj, edytuj, włącz/wyłącz, zmień kolejność, podgląd).
- Ekran: zmiana listy sponsorów działa od razu przez SSE, bez przeładowania strony. Ekran wstępnie ładuje grafiki, żeby nie migały.
- Testy: zły typ pliku, za duży plik, rotacja pomija nieaktywnych.

**Gotowe, gdy:** admin dodaje sponsora w przerwie meczu, a jego grafika pojawia się w rotacji na ekranie bez żadnego restartu.

**Pytania na review:** Dlaczego nie trzymamy grafik w bazie? Dlaczego sponsora usuwamy „miękko”?

---

### Etap 16 — Uruchamianie i utrzymanie
Gałąź: `etap-16-ops` · 2–3 dni

**Czego się uczysz:** Docker i Docker Compose (obrazy, wolumeny, sieci), konfiguracja środowisk (profile, zmienne), Actuator (health, info), logi (poziomy, co logować, czego nie logować), TLS w sieci lokalnej, kopie zapasowe i odtwarzanie (`pg_dump`/`pg_restore`), tryb kiosk przeglądarki.

**Do zrobienia:**
- `Dockerfile` backendu (build wieloetapowy) i serwowanie frontendu (np. nginx albo statyczne pliki z backendu, zdecyduj i opisz w ADR).
- `docker-compose.yml` uruchamiający cały system jedną komendą, z wolumenami na bazę i grafiki.
- `docs/ops.md`:
  - instalacja od zera na komputerze w reżyserce,
  - uruchomienie ekranu w trybie kiosk (autostart po włączeniu komputera),
  - backup przed meczem i odtwarzanie,
  - co robić, gdy ekran nie pokazuje wyniku (lista kontrolna dla operatora).
- Logi w czytelnym formacie, bez haseł i tokenów.

**Gotowe, gdy:** na czystej maszynie (albo w nowym katalogu) system startuje od pustej bazy według `docs/ops.md`, a backup daje się odtworzyć w osobnej bazie i dane się zgadzają.

**Pytania na review:** Ile danych stracimy, jeśli dysk padnie w przerwie meczu? Co jest pojedynczym punktem awarii w tym systemie?

---

### Etap 17 — Wydanie v1.0 i retrospektywa
Gałąź: `etap-17-release` · 1 dzień

**Czego się uczysz:** wersjonowanie semantyczne, tagi i GitHub Releases, CHANGELOG, demo dla klienta, retrospektywa.

**Do zrobienia:**
- `CHANGELOG.md` i tag `v1.0.0` z Release Notes na GitHubie.
- Uzupełnij README o sekcję „Szybki start” (3–5 komend).
- Demo dla mentora: pełny mecz od utworzenia do końca, ze sponsorami w przerwie, utratą sieci na ekranie i konfliktem dwóch operatorów.
- `docs/retro.md`: co poszło dobrze, co było najtrudniejsze, co zrobiłbyś inaczej, 3 pomysły na wersję 1.1 (np. integracja ze sprzętem, kartki, wiele stadionów).

**Gotowe, gdy:** demo przeszło bez poprawek „na żywo”, a release jest opublikowany.

---

## 7. Mapa etapów

```
 0 repo ─ 1 szkielet ─ 2 domena ─ 3 zegar ─ 4 REST ─ 5 architektura
                                                        │
 9 walidacja ─ 8 testy integr. ─ 7 JPA/JDBC ─ 6 baza ───┘
     │
10 idempotencja ─ 11 security ─ 12 SSE ─ 13 ekran ─ 14 panel ─ 15 sponsorzy ─ 16 ops ─ 17 v1.0
```

Etapy 2–3 to najlepszy moment na solidne opanowanie Kotlina i testów, bo nie ma tam jeszcze Springa ani bazy. Nie spiesz się z nimi. Etap 10 to serce systemu: błąd w tym miejscu oznacza zły wynik na telebimie przed kilkoma tysiącami ludzi.

Powodzenia! ⚽
