# DP-Hologram

마인크래프트 서버용 **홀로그램 플러그인**입니다. 1.19.4+의 **Display 엔티티**(텍스트/아이템/블록 디스플레이)를 사용해 가볍고 부드러운 홀로그램을 띄우며, 플레이스홀더·권한·클릭 액션·플레이어별 표시 등 운영에 필요한 기능을 갖추고 있습니다.

> 버전: **1.0.0** · 제작: **DEAD_POOLIO_** (darksoldier1029@gmail.com)

---

## 목차

1. [요구 사항](#요구-사항)
2. [설치](#설치)
3. [빠른 시작](#빠른-시작)
4. [핵심 개념](#핵심-개념)
5. [config.yml 설정](#configyml-설정)
6. [명령어 전체 목록](#명령어-전체-목록)
7. [GUI 편집기](#gui-편집기)
8. [고급 기능](#고급-기능)
9. [권한](#권한)
10. [자주 묻는 질문 / 문제 해결](#자주-묻는-질문--문제-해결)

---

## 요구 사항

| 항목 | 내용 |
|------|------|
| 서버 | Spigot / Paper **1.20.1 이상** (`api-version: 1.20`) |
| Java | 8 이상 |
| **필수** 의존 플러그인 | **[DPP-Core](https://github.com/darksoldier1404)** (`depend`) — 없으면 플러그인이 로드되지 않습니다 |
| 선택 의존 플러그인 | **PlaceholderAPI** (`softdepend`) — 플레이스홀더, 표시 조건 기능에 필요 |

> ⚠️ Display 엔티티 기반이므로 1.20 미만 서버에서는 동작하지 않습니다.

---

## 설치

1. **DPP-Core** 플러그인을 먼저 `plugins/` 폴더에 넣습니다.
2. (선택) 플레이스홀더 기능을 쓰려면 **PlaceholderAPI**도 설치합니다.
3. `DP-Hologram-1.0.0-SNAPSHOT.jar` 를 `plugins/` 폴더에 넣습니다.
4. 서버를 재시작합니다.
5. `plugins/DP-Hologram/config.yml` 이 생성됩니다. 필요 시 수정 후 `/hologram reload` 하거나 재시작합니다.

홀로그램 데이터는 `plugins/DP-Hologram/data/holograms/` 안에 홀로그램별 YAML 파일로 저장됩니다.

---

## 빠른 시작

```text
# 1. 서 있는 위치에 홀로그램 생성
/hologram create spawn

# 2. 텍스트 줄 추가 (색상코드 & 사용 가능)
/hologram addline spawn &6&l서버에 오신 것을 환영합니다!
/hologram addline spawn &7우클릭하면 상점이 열립니다

# 3. 잘 보이도록 위로 살짝 올리기 (전체 줄을 Y로 0.5 이동)
/hologram setalloffset spawn 0 0.5 0

# 4. 위치를 옮기고 싶으면 원하는 곳에 서서
/hologram move spawn

# 5. 시각 편집기를 열어 세부 조정
/hologram edit spawn
```

> 명령어 별칭으로 `/holo` 도 사용할 수 있습니다.

---

## 핵심 개념

### 홀로그램 타입

| 타입 | 설명 |
|------|------|
| `complex` (기본) | 줄마다 **텍스트 / 아이템 / 블록** 을 자유롭게 섞을 수 있는 일반 홀로그램 |
| `textonly` | 모든 줄을 **하나의 멀티라인 텍스트 디스플레이**로 합쳐서 표시 (가장 가볍고 간격이 깔끔함, 아이템·블록 줄은 사용 불가) |

타입은 `/hologram create <이름> [complex|textonly]` 또는 `/hologram settype <이름> <타입>` 으로 지정합니다.

### 줄(Line)

홀로그램은 **0번부터 시작하는 줄들의 목록**입니다. 줄마다 다음 세 가지 타입 중 하나입니다.

- **TEXT** — 문자열 (색상코드 지원, 플레이스홀더 지원)
- **ITEM** — 손에 든 아이템 (`/hologram setitem`)
- **BLOCK** — 블록 (`/hologram setblock`)

각 줄은 위치 오프셋, 회전(yaw/pitch/roll), 크기(scale), 줄 간격, 빌보드(바라보는 방식), 밝기, 배경색, 클릭 액션을 개별적으로 가질 수 있습니다.

### 개별 설정 vs 전체(global) 설정

- `set...` 계열 명령은 **특정 줄 하나**를 조정합니다. (예: `setoffset`, `setrotation`)
- `setall...` 계열 명령은 **홀로그램 전체**에 적용됩니다.
  - 오프셋·회전·크기·간격은 각 줄 값에 **더해지는(delta) 보정값**입니다.
  - 빌보드·밝기·배경은 모든 줄을 **덮어쓰는(override) 값**이며, `none`/음수로 해제하면 다시 줄별 설정을 따릅니다.
- `/hologram resetglobal <이름>` 으로 전체 설정을 한 번에 초기화합니다.

---

## config.yml 설정

```yaml
Settings:
  prefix: "&f[ &eDP-Hologram &f] "   # 메시지 접두사 (색상코드 적용)
  Lang: ko_KR                        # 언어 코드

refresh-interval: 20            # 플레이스홀더 텍스트 갱신 주기 (틱, 20 = 1초)
default-line-spacing: 0.25      # 새 홀로그램의 기본 줄 간격
visibility-check-interval: 40   # 권한/조건 기반 표시 여부 재검사 주기 (틱)
default-click-cooldown: 0       # 클릭 액션 쿨다운 (틱, 0 = 쿨다운 없음)

# 플레이어별 홀로그램 근접 추적 설정
# (줄 텍스트에 %placeholder%가 포함된 홀로그램은 가까운 플레이어마다
#  개인 사본으로 렌더링되어 플레이스홀더가 각자의 값으로 표시됩니다)
per-player-view-distance: 64    # 블록; 이 거리 안에 들어와야 개인 사본 생성
per-player-check-interval: 20   # 생성/제거 재평가 주기 (틱, 20 = 1초)
```

> ⚠️ `*-interval`, `default-click-cooldown` 같은 주기/쿨다운 값은 `/hologram reload` 로는 반영되지 않습니다. **서버 전체 재시작**이 필요합니다. (reload 는 홀로그램 데이터와 config 값만 다시 읽습니다.)

---

## 명령어 전체 목록

기본 명령어: `/hologram` (별칭 `/holo`) · 모든 하위 명령은 권한 **`dphologram.admin`** 이 필요합니다.

### 홀로그램 관리

| 명령어 | 설명 |
|--------|------|
| `/hologram create <이름> [complex\|textonly]` | 현재 위치에 홀로그램 생성 |
| `/hologram delete <이름>` | 홀로그램 삭제 (디스크 파일 포함) |
| `/hologram list` | 정의된 홀로그램 목록 |
| `/hologram settype <이름> <complex\|textonly>` | 타입 변경 |
| `/hologram setperplayer <이름> <true\|false>` | 플레이어별 개인 사본 렌더링 토글 |
| `/hologram tp <이름>` | 홀로그램 위치로 순간이동 |
| `/hologram move <이름>` | 현재 서 있는 위치로 홀로그램 이동 |
| `/hologram reload` | config + 모든 홀로그램 데이터 다시 로드 |
| `/hologram edit <이름>` | **GUI 편집기 열기** |

### 줄(Line) 편집

| 명령어 | 설명 |
|--------|------|
| `/hologram addline <이름> <텍스트...>` | 텍스트 줄 추가 (색상코드 지원) |
| `/hologram setitem <이름> <줄번호>` | 해당 줄을 **손에 든 아이템**으로 설정 |
| `/hologram setblock <이름> <줄번호> <블록>` | 해당 줄을 블록으로 설정 |
| `/hologram removeline <이름> <줄번호>` | 줄 삭제 |

### 줄 단위 외형 조정

| 명령어 | 설명 |
|--------|------|
| `/hologram setoffset <이름> <줄> <x> <y> <z>` | 줄 위치 오프셋 설정 |
| `/hologram setrotation <이름> <줄> <yaw> <pitch> <roll>` | 줄 회전(도 단위) 설정 |
| `/hologram setspacing <이름> <줄> <값\|default>` | 줄 아래 간격 설정 (`default` = 전체 간격 상속) |
| `/hologram setbillboard <이름> <줄> <fixed\|vertical\|horizontal\|center>` | 줄이 카메라를 바라보는 방식 |
| `/hologram setbrightness <이름> <줄> <블록광 0-15> <하늘광 0-15>` | 줄 밝기 고정 (음수 입력 시 해제 → 월드 조명 따라감) |

### 전체(global) 조정

| 명령어 | 설명 |
|--------|------|
| `/hologram setalloffset <이름> <dx> <dy> <dz>` | 전체 오프셋 보정값 누적 |
| `/hologram setallrotation <이름> <dyaw> <dpitch> <droll>` | 전체 회전 보정값 누적 |
| `/hologram setallscale <이름> <delta>` | 전체 크기 보정값 누적 |
| `/hologram setallspacing <이름> <값>` | 전체 기본 줄 간격 설정 |
| `/hologram setallbillboard <이름> <fixed\|vertical\|horizontal\|center\|none>` | 전체 빌보드 덮어쓰기 (`none` = 해제) |
| `/hologram setallbrightness <이름> <블록광 0-15> <하늘광 0-15>` | 전체 밝기 덮어쓰기 (음수 = 해제) |
| `/hologram resetglobal <이름>` | 모든 전체 설정 초기화 |

### 표시 조건 / 상호작용

| 명령어 | 설명 |
|--------|------|
| `/hologram setperm <이름> <권한\|none>` | 이 권한을 가진 플레이어에게만 표시 |
| `/hologram setcondition <이름> <PAPI식\|none>` | 플레이스홀더 조건이 참일 때만 표시 |
| `/hologram setclick <이름> <줄> <left\|right\|any> <액션>` | 줄 클릭 시 실행할 **DPP-Core 액션** 연결 |
| `/hologram removeclick <이름> <줄> <인덱스>` | 줄의 클릭 액션 제거 |

> **빌보드(billboard)** 값 의미: `center`(항상 정면, 기본) · `vertical`(좌우만 회전) · `horizontal`(상하만 회전) · `fixed`(고정, 회전 없음).

---

## GUI 편집기

`/hologram edit <이름>` 으로 시각적 편집기를 엽니다.

**줄 목록 화면**
- 각 줄이 아이콘으로 표시되며, 아이콘 설명에 현재 오프셋·크기·간격·클릭 액션 개수가 보입니다.
- **좌클릭**: 해당 줄 상세 편집
- **우클릭**: 줄 삭제
- **Shift+좌클릭 / Shift+우클릭**: 줄 순서 위/아래로 이동
- **이름표 아이콘**: 줄 추가 (채팅으로 텍스트 입력, `cancel` 입력 시 취소)
- **모루 아이콘**: 전체(global) 설정 화면 열기
- 좌우 화살표로 페이지 이동 (한 페이지 45줄)

**줄 상세 화면 / 전체 설정 화면**
- 색유리·나침반 등 컨트롤 아이콘을 클릭해 오프셋·회전·크기·간격·빌보드·배경을 조정합니다.
- **좌클릭** 증가 · **우클릭** 감소 · **Shift** 큰 폭 이동 · (간격은 **휠클릭** 으로 전체값으로 리셋).
- 전체 설정 화면에서는 **플레이어별 표시(per-player)** 토글도 가능합니다.

---

## 고급 기능

### 플레이스홀더 (PlaceholderAPI)

PlaceholderAPI가 설치되어 있으면 텍스트 줄에 `%placeholder%` 를 넣을 수 있습니다. `refresh-interval` 주기마다 자동 갱신됩니다.

```text
/hologram addline stats &e당신의 잔액: &a%vault_eco_balance%
```

- 플레이스홀더가 **플레이어마다 다른 값**이어야 한다면 **플레이어별 렌더링**을 켜세요:
  ```text
  /hologram setperplayer stats true
  ```
  → `per-player-view-distance` 안에 들어온 플레이어마다 개인 사본이 생성되어 각자의 값이 보입니다. 끄면 모두에게 동일한 한 개의 홀로그램이 표시됩니다.

### 표시 조건 (setcondition)

플레이스홀더 식을 평가한 결과가 `true` / `yes` / `1` 중 하나면 해당 플레이어에게 표시됩니다.

```text
# VIP 등급인 플레이어에게만 보이게
/hologram setcondition vipboard %luckperms_has_permission_group.vip%
```

> 조건은 `visibility-check-interval` 주기로 재평가됩니다. PlaceholderAPI가 없으면 조건은 무시되고 항상 표시됩니다.

### 권한 기반 표시 (setperm)

```text
/hologram setperm staffboard myserver.staff
```
해당 권한이 없는 플레이어에게는 홀로그램이 숨겨집니다. `none` 으로 해제합니다.

### 클릭 액션

줄을 좌/우클릭했을 때 **DPP-Core의 ActionBuilder 액션**을 실행합니다. 먼저 DPP-Core에서 액션을 등록(`/dppca`)한 뒤, 그 액션 이름을 연결합니다.

```text
# 0번 줄을 우클릭하면 'open_shop' 액션 실행
/hologram setclick mainmenu 0 right open_shop
```

- 클릭 타입: `left`(좌클릭) · `right`(우클릭) · `any`(둘 다)
- 연속 클릭을 막으려면 config 의 `default-click-cooldown` 을 틱 단위로 설정하세요.
- 연결한 액션이 DPP-Core에 등록되어 있지 않으면 콘솔 경고 후 무시됩니다.

---

## 권한

| 권한 노드 | 기본값 | 설명 |
|-----------|--------|------|
| `dphologram.admin` | `op` | 모든 `/hologram` 명령 사용 권한 |

> 일반 유저에게 홀로그램 **표시**를 제어할 때는 위의 `setperm` 으로 지정하는 **커스텀 권한**(예: `myserver.vip`)을 사용합니다. `dphologram.admin` 은 관리 명령 전용입니다.

---

## 자주 묻는 질문 / 문제 해결

**Q. 플러그인이 로드되지 않아요.**
A. **DPP-Core** 가 설치되어 있는지, 서버 버전이 **1.20.1 이상**인지 확인하세요. 콘솔 로그에 의존성 오류가 출력됩니다.

**Q. `/hologram reload` 했는데 갱신 주기/쿨다운이 안 바뀌어요.**
A. 주기·쿨다운 관련 설정은 재시작이 필요합니다. reload 는 홀로그램 데이터와 config 값만 다시 읽습니다.

**Q. 플레이스홀더가 `%...%` 그대로 보여요.**
A. PlaceholderAPI 설치 여부와 해당 확장(expansion) 설치 여부를 확인하세요. 플레이어별 값이 필요하면 `setperplayer ... true` 도 켜야 합니다.

**Q. 멀리서 안 보이거나, 청크가 언로드되면 사라져요.**
A. 홀로그램은 청크가 로드된 상태에서만 렌더링됩니다. 플레이어가 다가와 청크가 로드되면 자동으로 다시 표시됩니다.

**Q. 홀로그램을 클릭해도 액션이 실행되지 않아요.**
A. ① 연결한 액션이 DPP-Core(`/dppca`)에 실제로 등록되어 있는지, ② 클릭 타입(left/right/any)이 맞는지, ③ 표시 권한/조건을 만족하는지(숨겨진 홀로그램은 클릭 불가) 확인하세요.

---

*DP-Hologram — DP-Plugins 시리즈. Powered by DPP-Core.*
