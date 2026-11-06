# myalerts 프로젝트 규칙

## 저장소 공개 원칙 (PUBLIC)

- **이 저장소는 PUBLIC이다.** 모든 파일은 공개된다고 가정하고 작성한다.
- Dockerfile, `.dockerignore`, 설정 파일, SQL, 주석, 문서 등 **모든 파일**에 민감정보를 넣지 않는다.
- 금지 항목: 비밀번호, API 키, 토큰, 개인키, 실제 서버 계정명, IP, 호스트 경로, 내부 도메인, DB 접속정보.
- 서버별 실제 값은 환경변수/Coolify 설정으로 주입하고, 코드·문서에는 placeholder만 둔다.
- seed/예시 데이터도 실제 개인정보나 운영 값을 쓰지 않는다.
- 로그, 주석, 커밋 메시지에도 민감정보를 남기지 않는다.

## 기술 스택

- Spring Boot 4.1.2 / Java 21
- Thymeleaf (템플릿)
- MyBatis (SQL 매퍼) — **필수**
- Lombok
- SQLite (내장 파일 DB)

### DB / SQL

- 외부 DB 서버를 두지 않는다. **내장 SQLite** 파일 DB를 사용한다.
  - 토이 프로젝트라 가볍게 가는 게 목적이다.
- DB 파일은 로컬 기준 `data/myalerts.db` 에 둔다.
- DB 파일은 git에 올리지 않는다. (`.gitignore` 처리, `data/.gitkeep`만 커밋)
- 테이블 생성/초기화는 `schema.sql` 로 처리한다. (`CREATE TABLE IF NOT EXISTS`)
- 초기 설정 row는 `data.sql` 로 처리한다. (`INSERT OR IGNORE`, unique 제약 필요)
- 개발자가 로컬에서 데이터 확인할 때는 **HeidiSQL** 로 DB 파일을 직접 열어서 본다.
  - HeidiSQL은 SQLite 파일을 직접 열 수 있다.
  - 원격 서버 파일은 SQLite에 네트워크 접근이 없으므로 `scp`로 내려받아 열거나 컨테이너 안 `sqlite3`를 쓴다.

### 환경 설정 (로컬 vs 컨테이너)

- 로컬과 컨테이너는 datasource url만 다르다. 프로파일을 3개로 쪼개지 않는다.
- `application.properties` 기본값은 로컬 경로로 둔다.

```properties
spring.datasource.url=jdbc:sqlite:./data/myalerts.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
```

- Coolify(운영/개발 서버)에서는 환경변수로 덮어쓴다.
  - `SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/myalerts.db`
- **`spring.sql.init.mode=always` 필수.** SQLite는 Spring이 임베디드 DB로 인식하지 않아서 기본값(`embedded`)이면 `schema.sql`이 실행되지 않는다.
- 초기화 SQL은 **멱등**해야 한다. `always`라서 매 부팅마다 실행되기 때문이다.
  - 테이블: `CREATE TABLE IF NOT EXISTS`
  - seed row: `INSERT OR IGNORE` (또는 `WHERE NOT EXISTS`)

## 배포 / 데이터 저장소

- Coolify로 배포하며, SQLite 파일은 **호스트 디렉토리를 컨테이너에 bind mount** 해서 유지한다.
- 컨테이너 내부 경로는 항상 `/app/data` 이다.
  - datasource url: `jdbc:sqlite:/app/data/myalerts.db`
- 서버별 실제 호스트 경로는 이 저장소에 적지 않는다. Coolify 설정에서 관리한다.
  - 컨테이너 내부 경로만 고정: `/app/data`
- 애플리케이션은 JDK/JRE 포함 멀티스테이지 Docker 이미지로 빌드한다. 호스트에 JDK를 두지 않는다.
- SQLite 특성상 **replica는 1개만** 운영한다. (스케일 아웃 금지)
- mount 디렉토리 권한은 컨테이너 실행 유저가 쓰기 가능해야 한다. (`-wal`, `-journal` 생성 때문)
- 배포 전 호스트에서 마운트 디렉토리를 미리 `mkdir -p` + `chown` 해둔다. (Docker가 root 소유로 만들면 권한 문제 발생)
- 마운트에 이미 DB 파일이 있어도 초기화 SQL은 매 부팅마다 실행된다. 멱등 SQL이라 데이터는 유지된다.

### SQL / 데이터 접근

- **MyBatis**를 사용한다.
- JPA, JdbcTemplate, QueryDSL 등은 쓰지 않는다.
- SQL은 코드상에 직접 작성한다. XML 매퍼 대신 어노테이션(`@Select`, `@Insert`, `@Update`, `@Delete`)을 기본으로 한다.
- 바인드 변수는 반드시 `#{}`를 사용한다. (`${}` 문자열 치환 금지)
- PreparedStatement 기반으로 파라미터가 안전하게 바인딩되어야 한다.

## 아키텍처

### 1. 라우팅 컨트롤러 분리

- 모든 라우팅(URL 매핑)은 **`MainController`** 에서만 처리한다.
- `MainController`는 비즈니스 로직을 갖지 않는다.
- 데이터나 처리가 필요하면 `Service`를 호출해서 받아온다.

```
요청 → MainController → Service → DAO(MyBatis) → DB
```

### 2. 레이어 구성

- **Controller / Service / DAO** 3계층으로 구성한다.
- **DTO는 사용하지 않는다.**
- 데이터 구조는 DAO에서 만들어지고, 그대로 Service를 거쳐 Controller로 전달된다.
  - 별도의 DTO/Entity 변환 계층을 두지 않는다.

## Thymeleaf 구조

- 템플릿은 **도메인 단위 디렉토리**로 구성한다.
- 공통 요소는 `common` 디렉토리 안에서 분리한다.
  - 예: `common/layout/header.html`, `common/layout/header.js`
- **html, js, css는 같은 이름으로 같은 디렉토리에 둔다.**
  - 별도의 `css/`, `js/` 폴더로 분리하지 않는다.

```
templates/
├── common/
│   └── layout/
│       ├── header.html
│       ├── header.js
│       └── header.css
└── {도메인}/
    ├── list.html
    ├── list.js
    └── list.css
```

## 코드 스타일

- 코드는 최대한 단순하게 작성한다.
- 로직의 흐름 순서대로 `// 1. ...`, `// 2. ...` 형태의 주석을 반드시 단다.
- 클래스 소개 주석은 `/** */`를 사용하며, 내용은 간결하게 한두 줄로 끝낸다.
- 불필요한 추상화, 과한 유틸 클래스, 의미 없는 인터페이스 분리는 하지 않는다.
