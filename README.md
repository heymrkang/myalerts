# myalerts

지인들에게 이것저것 써보라고 열어주는 잡동사니 공간이다.
게임, 심리테스트, 각종 오락거리 같은 가벼운 기능을 하나씩 올려두고 사람들에게 권유하는 게 목적이다.

이름의 `alerts` 는 "내 사람들에게 알린다"는 의미에서 가져왔다.

## 기술 스택

- Spring Boot 4.1.2 / Java 21
- Thymeleaf
- MyBatis
- SQLite (내장 파일 DB)
- Lombok
- Gradle

## 실행

로컬에 JDK 21만 있으면 된다.

```bash
./gradlew dev
```

- `dev` 태스크는 `bootRun` 을 실행한다.
- 기본 주소: `http://localhost:8080`
- 코드를 수정하면 `spring-boot-devtools` + `spring.thymeleaf.cache=false` 로 반영된다. 브라우저는 직접 새로고침해야 한다.

테스트 / 빌드:

```bash
./gradlew test
./gradlew build
```

## 구조

```
src/main/java/com/mrkang/myalerts
├── MyalertsApplication.java
└── controller
    └── MainController.java
src/main/resources
├── application.properties
├── schema.sql
├── data.sql
└── templates
    ├── common
    │   └── layout
    └── {도메인}
```

- 라우팅은 `MainController` 에서만 처리한다.
- Controller / Service / DAO 3계층을 사용하고 DTO는 두지 않는다.
- 템플릿은 도메인 단위로 나누고, `html` / `js` / `css` 는 같은 이름으로 같은 디렉토리에 둔다.
- 자세한 규칙은 [AGENTS.md](./AGENTS.md) 참고.

## 데이터베이스

- 외부 DB 없이 내장 SQLite 파일 DB를 쓴다.
- 로컬 파일: `data/myalerts.db`
- 테이블은 `schema.sql` 의 `CREATE TABLE IF NOT EXISTS` 로, 초기 데이터는 `data.sql` 로 처리한다.
- SQLite 파일은 git에 올리지 않는다. (`.gitignore`)
- 로컬 데이터는 HeidiSQL로 파일을 직접 열어 확인할 수 있다.

## 배포

- Docker 멀티스테이지 이미지로 빌드하고 Coolify에서 배포한다.
- 컨테이너 내부 DB 경로는 항상 `/app/data/myalerts.db` 이다.
- 저장소 환경변수로 datasource 경로를 주입한다.

```properties
SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/myalerts.db
```

- SQLite 파일은 호스트 디렉토리를 컨테이너 `/app/data` 에 bind mount 해서 유지한다.
- 호스트 경로는 서버마다 다르므로 이 저장소에 적지 않는다. Coolify 설정에서 관리한다.
- SQLite 특성상 replica는 1개만 운영한다.

## 왜 이 스택인가

### Spring Boot + Thymeleaf

학원 다니면서 배운 자바를 안 까먹으려고 쓰는 거다. (물론 바이브코딩이지만)

Thymeleaf를 같이 쓰는 이유는 서버에서 HTML을 바로 렌더링할 수 있어서다.
프론트엔드를 따로 만들지 않아도 되고, 토이 프로젝트처럼 가볍게 굴리기 좋다.

### SQLite

DB를 따로 띄우는 리소스가 아까워서다.
파일 하나로 끝나서 백업도 복사만 하면 되고, 별도 DB 서버를 관리할 필요가 없다.

## 구조 히스토리

프로젝트를 만들면서 정한 결정들이다.

- **내장 SQLite 선택**: 별도 DB 서버 없이 가볍게 가는 토이 프로젝트라서 파일 DB로 확정했다.
- **MyBatis 선택**: JPA 대신 SQL을 코드에 직접 쓰고 `#{}` 바인드 변수로 처리하는 방식을 쓰기로 했다.
- **라우팅 분리**: URL 매핑은 `MainController` 한 곳에 모으고, 비즈니스 로직은 Service로 뺐다.
- **DTO 제거**: Controller / Service / DAO 3계층만 두고 별도 변환 계층을 만들지 않는다.
- **템플릿 규칙**: 도메인 단위 디렉토리 + `html`/`js`/`css` 동일 이름 배치로 통일했다.
- **개발 편의**: `bootRun { sourceResources sourceSets.main }` 설정으로 템플릿 수정이 즉시 반영되게 했다.
- **배포 방식**: Coolify + Docker 멀티스테이지, DB는 bind mount로 유지하는 구조로 갔다.
- **저장소 공개**: 저장소는 public이므로 모든 파일에 민감정보를 두지 않는 원칙을 적용했다.

## 주의

이 저장소는 public이다. 비밀번호, 토큰, 실제 서버 정보, 개인정보를 커밋하지 않는다.
