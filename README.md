# Create: Kinetism

**Mekanism의 기계들을 Create의 회전력 위에 다시 세운 애드온.**

Minecraft 1.21.1 · NeoForge 21.1 · [Create](https://github.com/Creators-of-Create/Create) 6.0+ 필요

---

## 한눈에 보기

- 겉모습과 조작감은 **Create** — 톱니바퀴, 회전 응력(SU), 벨트, 베이슨, 블레이즈 버너
- 내부 처리 로직은 **Mekanism** — 2배/3배/4배/5배 광물 증식, 가스·슬러리·화학 주입
- **FE(Forge Energy)는 한 줄도 안 씀.** 동력은 전부 Create의 회전력임
- Create 클래스를 **상속·오버라이드**해서 재사용함. Mekanism 기계 12종이 블록 클래스 2개로 끝남
- 블록 29종, 유체 26종, 중간 산물 아이템 19종
- Create에 이미 있는 기계(분쇄 휠, 맷돌, 기계톱, 송풍기)는 중복 제작하지 않고 레시피만 추가함
- 석유 계통은 [Petrochem](https://github.com/hadron13/Petrochem)(MIT) 포팅. 황산이 여기서 나옴

---

## 설계 원칙

Mekanism은 모드 마인크래프트에서 가장 잘 만들어진 광물 가공 진행도를 가지고 있음. Create는 가장
잘 만들어진 *기계의 감촉*을 가지고 있음. 이 모드는 전자를 후자 안에 집어넣는 것임.

모든 결정은 세 규칙을 따름.

1. **밖에서 보면 Create여야 함.** 톱니바퀴, 응력, RPM, 벨트, 깔때기, 베이슨, 블레이즈 버너,
   커스텀 GUI 없음. 기계식 혼합기를 연결할 줄 알면 정제조도 연결할 수 있음.
2. **안에서 돌아가는 건 Mekanism이어야 함.** 2배 → 3배 → 4배 → 5배 광물 체인, 산소, 염화 수소,
   황산, 슬러리, 야금 주입 라인. 이식 대상은 **레시피**지 블록 동작이 아님.
3. **Create에 이미 있는 기계는 다시 만들지 않음.** Mekanism의 Crusher는 분쇄 휠과 맷돌이,
   Precision Sawmill은 기계톱이, Energized Smelter는 용암을 문 송풍기가 이미 하는 일임. 그런 건
   블록 대신 `create:crushing` / `create:milling` 레시피를 추가해서 Create 기계가 처리하게 함.

그리고 강한 제약 하나. **FE 사용 금지.** 기계는 회전력으로 돌아감. 처리 속도는 RPM의 함수이고
(= Mekanism의 속도 업그레이드), 응력 부하가 Mekanism의 틱당 에너지 소모에 대응함.

---

## 기계 목록

Mekanism 계열 기계는 12종이지만 뼈대는 단 2종류임. 여기에 석유·배관 계열 블록 16종과 회전 축전기가
따로 붙어서 전부 29종임.

### Chamber — 아이템 in, 아이템 out

독립형 회전 기계임. 아래에서 축으로, 또는 옆에서 톱니바퀴로 구동함. 벨트·깔때기·슈트·호퍼로
넣거나 위에서 아이템을 떨어뜨려 넣고, 맨손 우클릭 또는 아무 면에서나 추출해서 비움.

Create의 `MillstoneBlockEntity`에서 레시피 타입만 갈아끼운 것임.

| 블록 | Mekanism 대응 | 입력 슬롯 | 응력 |
|---|---|---|---|
| 농축실 (Enrichment Chamber) | Enrichment Chamber | 1 | 4 SU |
| 결합실 (Combining Chamber) | Combiner | 2 | 8 SU |
| 야금 주입기 (Metallurgic Infuser) | Metallurgic Infuser | 2 | 8 SU |

### Vat — 화학 물질을 다루는 모든 기계

**Create 베이슨 위에 한 칸 띄우고** 설치함. 기계식 혼합기와 완전히 같은 배치임. 베이슨 자체가
기계의 인벤토리 역할을 함 — 입력 탱크 2개, 아이템 슬롯, 출력 버퍼, 필터, 증발조의 경우 열원까지.

Create의 `MechanicalMixerBlockEntity`와 `BasinOperatingBlockEntity`에서 레시피 타입만 갈아끼운
것임. 이 모드에 커스텀 GUI가 하나도 없고 유체 수송 코드가 한 줄도 없는 이유가 이것임. Create의
파이프·펌프·밸브·주입구·아이템 배수구·탱크가 이미 전부 해주고 있음.

| 블록 | Mekanism 대응 | 응력 |
|---|---|---|
| 정제조 (Purification Vat) | Purification Chamber | 8 SU |
| 주입조 (Injection Vat) | Chemical Injection Chamber | 8 SU |
| 용해조 (Dissolution Vat) | Chemical Dissolution Chamber | 16 SU |
| 세척조 (Washing Vat) | Chemical Washer | 8 SU |
| 결정화조 (Crystallizing Vat) | Chemical Crystallizer | 8 SU |
| 산화조 (Oxidation Vat) | Chemical Oxidizer | 8 SU |
| 화학 주입조 (Chemical Infusion Vat) | Chemical Infuser | 8 SU |
| 전기 분해기 (Electrolytic Separator) | Electrolytic Separator | 16 SU |
| 증발조 (Evaporation Vat) | Thermal Evaporation Plant | 8 SU |

증발조 레시피에는 **열 요구 조건**이 붙어 있어서 베이슨 아래에 블레이즈 버너가 필요함. 염수는
점화 상태, 리튬은 초가열 상태(블레이즈 케이크)여야 함. Mekanism의 5칸짜리 증발탑이 블록 하나 +
Create의 기존 열 메커니즘으로 대체됨.

---

## 화학 물질은 전부 유체임

Mekanism은 가스·주입물·색소·슬러리라는 4개의 독립된 수송 체계를 가지고 있고 각각 전용 파이프와
탱크와 GUI를 씀. 그걸 그대로 옮기면 Create의 유체 컨텐츠 절반을 다시 구현해야 하고, 결과물은
기계식 펌프로 옮길 수도 없음.

그래서 **이 모드의 모든 화학 물질은 평범한 마인크래프트 유체임.** Create의 유체 생태계 전체가
수정 없이 그대로 동작함.

단, Create의 `VirtualFluid` — 즉 **가상 유체**임. 양동이 아이템 없음, 유체 블록 없음, 월드에
설치 불가. 파이프로 옮기거나 아니면 못 옮김.

**가스:** 산소 · 수소 · 염소 · 염화 수소 · 이산화 황 · 삼산화 황 · 황산 · 염수 · 리튬 · 황화 수소

**석유 계열:** 원유 · 중유 · 경유 · 등유 · 나프타 · 휘발유 · 사워가스 · 천연가스 · 증기 · 공기

**슬러리:** 더러운/깨끗한 철 · 더러운/깨끗한 금 · 더러운/깨끗한 구리

주입물(레드스톤, 탄소 등)은 유체가 **아님**. Mekanism에서도 어차피 아이템으로 공급하므로 야금
주입기는 두 번째 아이템 슬롯으로 주입 재료를 받음.

---

## 광물 증식 체인

철·금·구리가 기본 포함됨. 전부 `c:` 태그 기반이라 데이터팩만으로 금속을 추가할 수 있음.

```
2배   원석 ──[농축실]──> 가루 2 ──제련──> 주괴 2

3배   원석 ──[정제조 + 산소]──> 덩이 3
              ──[분쇄 휠 / 맷돌]──> 더러운 가루 3
              ──[농축실]──> 가루 3

4배   원석 ──[주입조 + 염화수소]──> 조각 4
              ──[정제조 + 산소]──> 덩이 4 ──> … ──> 가루 4

5배   원석 ──[용해조 + 황산]──> 더러운 슬러리 1000mB
              ──[세척조 + 물]──> 깨끗한 슬러리 1000mB
              ──[결정화조]──> 결정 5
              ──[주입조 + 염화수소]──> 조각 5 ──> … ──> 가루 5
```

각 단계는 아래 단계의 완전한 상위 집합임. Mekanism과 똑같이, 이미 지은 기계는 그대로 두고 앞단에
한 단계를 더 붙이는 방식임.

### 화학 라인

```
물     ──[전기 분해기]──> 수소 + 산소
물     ──[증발조, 점화]──> 염수
염수   ──[증발조, 초가열]──> 리튬
염수   ──[전기 분해기]──> 염소 + 수소

수소 + 염소            ──[화학 주입조]──> 염화 수소
이산화 황 + 산소       ──[화학 주입조]──> 삼산화 황
삼산화 황 + 물         ──[화학 주입조]──> 황산
```

황은 하늘에서 떨어지지 않음. 이산화 황은 **석유에서 나옴** — 아래 Claus 공정 참고.

### 강철

Mekanism의 강철 라인이자 이 모드에 야금 주입기가 존재하는 이유임.

```
철 주괴 + 레드스톤 8      ──[야금 주입기]──> 농축된 철
농축된 철 + 석탄 8        ──[야금 주입기]──> 강철 가루 ──제련──> 강철 주괴
```

강철은 가장 비싼 기계 2종(용해조, 전기 분해기)의 재료임. 덕분에 5배 체인에 실제 선행 조건이
생기고 첫날부터 못 지음.

---

## 석유 (Petrochem 포팅)

[Petrochem](https://github.com/hadron13/Petrochem)(MIT, hadron13)에서 가져온 계통임. 라이선스
전문은 [LICENSE-THIRD-PARTY.md](LICENSE-THIRD-PARTY.md) 참고.

여기가 이 모드의 후반 진행도이자, **황산 라인의 유일한 시작점**임. Mekanism에서는 황이 광석으로
그냥 나오지만, 여기서는 원유를 뽑아서 증류하고 Claus 공정을 돌려야만 나옴. 5배 광물 증식을 하려면
정유 공장을 먼저 지어야 한다는 뜻임.

### 펌프잭

블록 3개가 한 세트임. Arm(워킹빔) 기준으로 아래 2칸, 바라보는 방향으로 2칸 떨어진 곳에 Crank,
반대쪽 2칸에 Well이 있어야 함.

```
              [Arm]
  [Well] ..    ..    .. [Crank]     (둘 다 Arm 기준 2칸 아래)
    |
   파이프
    |
   ...  <- 베드락까지 Create 유체 파이프가 끊기지 않아야 함
```

| 블록 | 역할 |
|---|---|
| 유정 헤드 (Pumpjack Well) | 실제로 원유가 나오는 곳. 바라보는 면으로만 배출함 |
| 펌프잭 크랭크 (Pumpjack Crank) | 축을 물림. **32 RPM 미만이면 아예 안 돎** |
| 펌프잭 암 (Pumpjack Arm) | 둘을 이어주는 워킹빔. 크랭크가 한 바퀴 돌 때마다 한 번 뽑음 |

두 가지 제약이 있음.

- **파이프.** 유정 바로 아래부터 베드락까지 Create 유체 파이프가 이어져 있어야 함. 아무 데나 놓고
  되는 블록이 아님.
- **간섭.** 8칸 안에 다른 유정이 있으면 개당 산출량이 25%씩 깎임. 유정을 몰아 짓는 것보다 퍼뜨리는
  게 이득임.

산출량은 바이옴이 정함 — 황무지 120 > 사막 100 > 바다 80 > 늪 60 > 평원 40 mB.
`createkinetism:pumpjack` 레시피 타입으로 바이옴(또는 `#바이옴태그`)을 추가할 수 있음.

### 증류탑

**강철 탱크(Steel Tank)** 스택 옆면에 **증류 컨트롤러**를 붙이면 그 스택 전체가 증류탑으로 바뀜.
창문이 막히고, 탱크 두 층마다 한 개의 **분류 단(fractionation stage)** 이 됨. 각 단에는
**증류 출구**를 붙여서 그 유분을 받아감.

```
[강철탱크][강철탱크]  <- 4단
[강철탱크][강철탱크]  <- 3단
[강철탱크][강철탱크]  <- 2단
[강철탱크][강철탱크]  <- 1단   [증류 컨트롤러] <- 원유 / 증기 투입
        └─ 아래에 블레이즈 버너
```

컨트롤러 앞면을 렌치로 스크롤해서 방식을 고름. 이게 이 계통의 진행도임.

| 방식 | 요구 조건 | 결과 |
|---|---|---|
| 플래시 | 증기 1000mB | 중유 600 + 경유 400 |
| 상압 | 3칸 폭 + 열 | 중유 300 + 경유 300 + 등유 200 + 나프타 200 |
| 감압 | 열 + 진공 유지 | 중유 200 + 경유 250 + 등유 200 + 나프타 200 + **사워가스 150** |

감압 모드는 컨트롤러가 매 틱 공기를 다시 채워 넣기 때문에, 계속 빼내지 않으면 진공이 유지되지
않음. 사워가스는 감압에서만 나오고, 사워가스가 없으면 황도 없음.

### Claus 공정 — 황이 나오는 곳

```
사워가스 + 수소       ──[화학 주입조]──> 황화수소 + 천연가스     (수소첨가탈황)
황화수소 + 산소       ──[산화조]──> 이산화 황 + 물              (Claus 1단계: 1/3 연소)
황화수소 + 이산화 황  ──[화학 주입조]──> 황 가루 ×3 + 물         (Claus 2단계)
```

여기서 나온 황이 이산화황 → 삼산화황 → **황산**으로 이어지고, 그 황산이 용해조를 돌려서 5배
광물 증식을 가능하게 함.

### 엔진

전부 회전력 **생산자**임. 아래로 연료를 넣고, 바라보는 면으로 축이 나옴.

다만 **디젤 엔진만 연결 방식이 다름**. Create의 증기 기관과 같은 구조라서, 자기가 회전원이 아니라
**2칸 앞의 축을 동력 축(Powered Shaft)으로 바꿔서** 그걸 돌림. 엔진을 놓고 **축을 든 채 엔진을
우클릭**하면 알맞은 위치와 축방향으로 알아서 놓임. 동력 축 하나는 엔진 하나만 받으므로, 여러 대를
쓰려면 각자 자기 축을 주고 그 축들을 이어야 함 — 용량은 네트워크에서 합산됨.

| 블록 | 연료 | 응력 용량 (1 RPM 기준) |
|---|---|---|
| 가솔린 엔진 | 휘발유, 나프타 | 128 |
| 디젤 엔진 | 경유, 중유 | 1,536 |
| 가스 터빈 | 천연가스, 사워가스, 등유 | 1,536 |

값은 **Petrochem 원본과 같음**. 가스 터빈은 Petrochem에 대응 블록이 없어서(원본은 FE 발전기임)
디젤 엔진과 같은 스펙으로 맞췄음.

윗면을 스크롤해서 **RPM을 직접 지정**함(기본 -256 ~ 256). 응력 용량은 RPM에 반비례해서, 실제
공급량은 항상 같음 — 64 RPM으로 돌리면 256 RPM일 때보다 용량이 4배임. 빠르고 약하게 쓸지 느리고
강하게 쓸지는 사용자가 정하는 것임.

연료 소모는 **네트워크 부하에 비례함**. 한가한 네트워크에서는 조금씩 먹고, 꽉 찬 네트워크에서는
많이 먹음. 다만 최저 30%는 항상 소모하므로 켜두는 것 자체가 공짜는 아님.

### 강철 배관

Create의 파이프 계열을 강철로 옮긴 것임. **강철 탱크**를 빼면 전부 Create 클래스를 그대로
상속하고 블록 엔티티 타입만 새로 등록한 얇은 껍데기임.

| 블록 | 비고 |
|---|---|
| 강철 유체 파이프 | 렌치로 직선 → 창문 순으로 전환됨 (Create 파이프와 동일) |
| 스마트 강철 유체 파이프 | 필터 지정 |
| 강철 유체 밸브 | 레드스톤 차단 |
| 강철 기계식 펌프 | 회전력 4 SU |
| 강철 탱크 | 증류탑 본체로도 쓰임 |

> **처리량은 구리 파이프와 같음.** Petrochem 원본과 동일하게 맞춘 것이고, 유일한 기능 차이는
> **케이싱을 못 씌운다**는 것뿐임(`tryEncase`가 FAIL). 정유 설비를 주변 구리 배관과 시각적으로
> 구분하는 용도임.
>
> 애초에 Create에서 **파이프에는 전송 속도라는 개념이 없음**. `FluidNetwork`가
> `transferSpeed = max(1, pressure / 2)`로 계산하고, 그 `pressure`는 펌프가
> `|getSpeed()|` 만큼 밀어 넣는 값임. 즉 배관을 빠르게 만들고 싶으면 파이프가 아니라
> **펌프**에 배율을 걸어야 함.

### 나머지 처리

- **플레어 스택** — 파이프로 넣은 유체를 태워 없앰. 정유하면 반드시 안 쓰는 유분이 남고, 태우지
  않으면 탑이 막힘. 태우는 양에 비례해서 불꽃이 커지므로 얼마나 버리고 있는지 눈으로 보임
- **중유 크래킹** — 증발조(초가열)로 중유 → 경유 + 나프타
- **나프타 개질** — 화학 주입조로 나프타 + 수소 → 휘발유
- **증기** — 증발조(점화)로 물 → 증기. 플래시 증류용

---

## 결합실 밸런스 관련 안내

조약돌 8 + 가루 1 → 원석 1. 이 원석을 5배 체인에 다시 돌리면 조약돌 8당 가루 4가 순증함.
**이건 Mekanism의 실제 동작이지 버그가 아님.** 의도된 조약돌→광물 루프이고, 대신 막대한 인프라가
선행 조건임. 모드팩에서 원치 않으면 데이터팩으로 `data/createkinetism/recipe/combining/` 을
지우면 됨.

---

## 설정 (config)

응력 수치는 코드에 박아두지 않았음. **전부 `config/createkinetism-server.toml` 에서 바꿀 수 있음.**

```
[kinetics.stressValues.impact]      부하 — 기계 17종 전부
[kinetics.stressValues.capacity]    용량 — 엔진 3종
[kinetics.stressValues.rpm]         엔진 스크롤의 RPM 상한
```

부하와 용량은 **게임 중 config 리로드로 즉시 반영됨**. Create의 응력 레지스트리에 값이 아니라
공급자를 등록해뒀기 때문임. RPM 상한만 블록 엔티티가 만들어질 때 읽으므로 청크를 다시 불러와야 함.

Create 본체 방식과 같으나, Create가 **자기 config에 외부 모드 블록을 넣는 것을 막아둬서** 우리
config를 따로 둔 것임. Petrochem도 같은 이유로 같은 구조를 씀.

---

## 레시피 추가하기

모든 기계가 Create의 `ProcessingRecipe` 형식을 씀. 따라서 JSON 구조가 `create:milling`,
`create:mixing`과 완전히 동일함. 아이템과 유체를 섞어 담는 `ingredients` 배열 하나, 마찬가지로
섞어 담는 `results` 배열 하나임.

```json
{
  "type": "createkinetism:purifying",
  "ingredients": [
    { "tag": "c:raw_materials/iron" },
    { "type": "neoforge:single", "amount": 200, "fluid": "createkinetism:oxygen" }
  ],
  "results": [
    { "id": "createkinetism:iron_clump", "count": 3 }
  ],
  "processing_time": 200
}
```

레시피 타입: `enriching` · `combining` · `infusing` · `purifying` ·
`injecting` · `dissolving` · `washing` · `crystallizing` · `oxidizing` · `chemical_infusing` ·
`separating` · `evaporating` · `pumpjack` · `distilling` · `gasoline_engine_fuel` ·
`diesel_engine_fuel` · `turbine_fuel`

알아둘 것 두 가지.

- **재료 개수.** Create의 처리 레시피는 개수 없는 `Ingredient`만 담음. 조약돌 8개를 요구하려면
  조약돌 재료를 8번 나열하면 됨. Chamber가 이 반복을 슬롯별 소모 개수로 접음. 그래서 입력 슬롯이
  2개인 Chamber는 "A 8개 + B 1개"를 받을 수 있음.
- **열.** Vat 레시피는 `"heat_requirement": "heated"` 또는 `"superheated"`를 붙일 수 있고
  Create의 블레이즈 버너가 처리함. 현재는 증발조 레시피 타입만 이 값을 받음.

이 모드의 데이터팩에는 `create:` 네임스페이스 레시피도 들어 있음. 덩이→더러운 가루, 주괴→가루가 그것으로,
전부 Create의 분쇄 휠과 맷돌이 처리함
(`data/createkinetism/recipe/crushing/`, `.../milling/`).

---

## Create 코드를 얼마나 재사용했는가

의도적으로 거의 전부임. 이 기계들이 *진짜로* Create 기계라는 게 이 모드의 핵심임.

| Kinetism 클래스 | 상속받은 Create 클래스 |
|---|---|
| `ChamberBlock` | `extends KineticBlock implements IBE, ICogWheel` (`MillstoneBlock` 구조) |
| `ChamberBlockEntity` | `extends KineticBlockEntity` — 타이머/`getProcessingSpeed()` 루프, `CombinedInvWrapper` 입출력 필터, `DirectBeltInputBehaviour` 전부 밀스톤 그대로 |
| `ChamberRenderer` | `extends KineticBlockEntityRenderer` + `AllPartialModels.MILLSTONE_COG` |
| `VatBlock` | `extends KineticBlock` (`MechanicalMixerBlock` 구조) — 베이슨 인접 검사, 충돌 상자, 최소 속도 등급 |
| `VatBlockEntity` | `extends BasinOperatingBlockEntity` — 40틱 헤드 사이클, RPM 비례 처리 시간, 레시피 트라이 조회, 베이슨 출력 수용 |
| `VatRenderer` | `extends KineticBlockEntityRenderer` + Create의 폴/헤드/톱니 파샬 모델 |
| `ChamberRecipe` | `extends StandardProcessingRecipe` (`AbstractCrushingRecipe` 구조) |
| `VatRecipe` | `extends BasinRecipe` — 유체 재료, 유체 결과, 열 조건, 잔여물 처리 전부 상속 |
| `CKRecipeTypes` | `AllRecipeTypes`와 동일한 열거형 패턴 |
| `SteelTankBlockEntity` | `extends FluidTankBlockEntity` — Create의 탱크 멀티블록을 그대로 상속하고 증류탑 모드만 얹음 |
| `FuelEngineBlockEntity` | `extends GeneratingKineticBlockEntity` — RPM 다이얼, 응력 용량 산출 |
| 응력·고글·툴팁 | `CreateRegistrate`, `KineticStats`, `BlockStressValues` |

블록 모델도 `create:block/millstone/block`과 `create:block/mechanical_mixer/block`을 `parent`로
상속하고 텍스처만 교체함. Mekanism 기계 12종을 통틀어 블록 클래스 2개, 블록 엔티티 클래스 2개뿐임. 정제조와
세척조의 차이는 전적으로 레시피에만 존재하기 때문임. 석유 계열 블록만 각자의 클래스를 가짐.

---

## 빌드

```bash
./gradlew build
```

결과물은 `build/libs/`에 생성됨. 의존성은 `maven.createmod.net`(Create, Ponder, Flywheel)과
`maven.tterrag.com`(Registrate)에서 받아옴. 고정된 버전은 `gradle.properties` 참고.

> **Gradle이 `java.io.IOException: Unable to establish loopback connection` 으로 실패한다면**
> 프로젝트 문제가 아니라 JVM/OS 문제임. `java.nio.channels.Selector.open()`이 내부 AF_UNIX 소켓
> 쌍을 만들지 못하는 것으로, `Selector.open()`만 호출하는 3줄짜리 프로그램으로 재현됨. 보통
> 보안 소프트웨어가 Windows의 `afunix` 드라이버를 막아서 발생함. JDK를 예외 처리하거나 WSL 또는
> 컨테이너에서 빌드할 것.

---

## 현재 상태

**검증된 것:** 전체 소스(자바 62개 파일)가 Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.11-300 /
Registrate MC1.21-1.3.0+67 / Ponder 1.0.85 / Flywheel 1.0.6 대상으로 **에러 0, 경고 0** 컴파일
통과함. 리소스 JSON 244개 전부 파싱 검증 통과함.

**검증 안 된 것:** 위에 적은 Gradle 문제 때문에 이 PC에서 인게임 실행을 못 해봤음. 첫 실행이 곧
진짜 스모크 테스트임.

### 임시로 때워둔 것들

- **텍스처.** 모든 모델과 아이템 아이콘이 기존 Create/바닐라 텍스처를 빌려 쓰고 있음. 기계는
  케이싱 텍스처로만 구분되고 아이템은 `crushed_raw_*` / `raw_*` 아이콘을 재사용하기 때문에 한
  금속의 5가지 형태가 지금은 전부 똑같이 보임. 실제 아트가 가장 큰 남은 작업임 (블록 텍스처 세트
  29개, 아이템 아이콘 19개).
- **JEI / EMI.** 레시피 뷰어 연동이 아직 없음. 그래서 현재 레시피는 데이터팩을 직접 읽어야만 알
  수 있음. 가치 대비 우선순위가 가장 높은 다음 작업임.
- **Ponder 씬.** 없음. Create 유저라면 당연히 기대할 부분임.
- **발전 과제(Advancements).** 없음.

---

## Mekanism 기계 전수 목록

Mekanism의 기계를 전부 훑고 각각이 지금 어느 단계인지 정리한 표임.

### 구현 완료 — 전용 블록

Enrichment Chamber · Purification Chamber · Chemical Injection Chamber ·
Chemical Dissolution Chamber · Chemical Washer · Chemical Crystallizer · Chemical Oxidizer ·
Chemical Infuser · Electrolytic Separator · Metallurgic Infuser · Combiner ·
Thermal Evaporation Plant (단일 블록 증발조로 축약)

### 구현 완료 — Create 기존 기계로 대체

블록을 새로 만들지 않고 레시피만 추가한 것들임. 같은 일을 하는 기계를 두 개 두는 건 애드온이 할
짓이 아님.

| Mekanism 기계 | 대신 쓰는 Create 기계 | 추가한 레시피 타입 |
|---|---|---|
| Crusher | 분쇄 휠 (Crushing Wheels), 맷돌 (Millstone) | `create:crushing`, `create:milling` |
| Precision Sawmill | 기계톱 (Mechanical Saw) | Create 기본 레시피로 충분 |
| Energized Smelter | 용암을 문 송풍기 (Encased Fan) | Create 기본 동작으로 충분 |

### 구현 예정

| 기계 | 메모 |
|---|---|
| Osmium Compressor | Vat. 오스뮴 계열 금속 추가가 선행 |
| Rotary Condensentrator | Vat. 가스↔유체 전환 모드 토글이 필요해서 렌치 상호작용 추가 필요 |
| Pressurized Reaction Chamber | Vat. 아이템+유체+가스 → 아이템+가스. 현재 뼈대 그대로 들어감 |
| Solar Neutron Activator | Vat보다는 하늘을 보는 독립 블록이 맞음 |
| Isotopic Centrifuge | Vat |
| Pigment Extractor / Pigment Mixer / Painting Machine | 색소를 또 하나의 유체 계열로 추가 |
| Nutritional Liquifier | Vat |
| Antiprotonic Nucleosynthesizer | 후반부. 상위 티어가 먼저 필요 |
| Formulaic Assemblicator | Create의 기계식 조합기와 역할이 크게 겹침 |
| Digital Miner · Seismic Vibrator | 대형 독립 기계. 공유 뼈대 없음 |
| Resistive Heater · Fuelwood Heater | Create의 블레이즈 버너가 이미 그 역할 |
| Thermoelectric Boiler · Dynamic Tank · SPS · Induction Matrix | 멀티블록. 별도의 큰 프로젝트 |

### 범위 밖

에너지 큐브, 케이블, 발전기, 텔레포터, 양자 엉킴 전송기, 레이저 계열은 전부 FE를 저장하거나
옮기려고 존재함. 이 모드에는 FE가 없으므로 의미가 없음. Create의 회전 네트워크, 기계식 펌프,
아이템 물류가 같은 영역을 이미 담당함.

공장(Factory: Basic/Advanced/Elite/Ultimate) 티어도 범위 밖임. Mekanism은 기계를 업그레이드해서
처리량을 늘리지만 Create는 더 빨리 돌리거나 하나 더 지음. **축을 더 빨리 돌리는 것이 이 모드의
공장 티어임.**

---

## 라이선스

[LICENSE.md](LICENSE.md) 참고.

석유 계통은 [Petrochem](https://github.com/hadron13/Petrochem)(MIT, hadron13)에서 포팅했음. 라이선스
전문은 [LICENSE-THIRD-PARTY.md](LICENSE-THIRD-PARTY.md)에 그대로 실어 뒀음.

Create는 [Creators of Create](https://github.com/Creators-of-Create/Create), Mekanism은
[Mekanism 팀](https://github.com/mekanism/Mekanism)의 저작물임. 이 프로젝트는 둘 중 어느 것도
번들하지 않으며 어느 쪽과도 제휴 관계가 없음. Create의 공개 클래스를 상속해서 기계 뼈대를 빌려
쓰고 Mekanism의 처리 설계를 재구현했을 뿐임.
