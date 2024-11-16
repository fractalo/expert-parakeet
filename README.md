# Streaming Settlement System
영상의 실시간 조회수 등 메타데이터를 관리하고, 시청 기록을 수집하여 정산 및 통계 데이터를 제공하는 시스템

## 사용 기술
Java, Spring Boot, Spring Batch, JPA, MySQL, Redis, Docker

## 주요 기능
1. 로그인 및 회원가입
   - Google OAuth2 지원
2. 영상 및 광고영상 메타데이터 관리
   - 영상 정보 및 광고영상 정보 조회
   - 영상 및 광고영상 조회수 기록
   - 조회수 어뷰징 방지
3. 영상 시청 기록 수집
   - 영상 재생 시간 및 마지막 재생 지점
4. 영상 정산 및 통계 데이터 생성 일일 배치 작업
   - 영상별 조회수 및 광고영상 조회수에 대한 정산 금액
   - 일별, 주별, 월별 영상 조회수 증가량
   - 일별, 주별, 월별 영상 시청 시간

## 아키텍처
![streaming_settlement drawio](https://github.com/user-attachments/assets/e2be219d-9d61-4466-b162-6dd08da67d54)

## 성능 개선
- 배치 작업 속도 개선: 영상 500만 개 기준으로, 일일 정산 및 통계 배치 작업 속도 개선
  - 싱글스레드: x분
  - 파티셔닝 적용 (멀티스레드): y분
  - 커서 기반 페이지네이션 사용 및 쿼리 최적화: z분
- 조회수 증가 요청 TPS 개선
  - Redis 사용

## 트러블 슈팅
- 일별 조회수 일관성 문제: 일별 조회수 증가량의 시간 범위가 일관적이지 않을 수 있는 문제
  - 일별 최종 조회수를 별도 테이블에 스냅샷으로 저장하여 일관성 및 배치 멱등성 확보
- 비관적 락으로 인한 타임아웃 문제: 대규모 트래픽 상황에서 발생하는 조회수 증가 요청 처리 문제
  - Redis를 사용해 조회수 증가 및 어뷰징 방지 처리를 하고, 1분 간격으로 RDB에 동기화
- [@Transactional noRollbackFor 트랜잭션 전파](https://violet-level-671.notion.site/Transactional-noRollbackFor-134244fa33bd80cf9fb4e3c1d1e2f158?pvs=4)
- [MySQL Table Data Import Wizard 속도가 매우 느린 문제](https://violet-level-671.notion.site/MySQL-Table-Data-Import-Wizard-13b244fa33bd80cf9613fd2a58c04986?pvs=4)

## API 문서
[Postman](https://www.postman.com/observer-poa/streaming-settlement/documentation/850ervr/streaming-settlement-system-api)

## ERD
![ERD](https://github.com/user-attachments/assets/6547e51c-0e94-4fe6-959c-3ced7e311ad5)


## 빌드 및 실행
### 백엔드 Docker 이미지 빌드
1. Gradle `jibDockerBuild` task 실행
### Docker Compose 실행
1. 환경 변수 파일 생성
    1. `.env` (docker-compose.yml 참조 용도)
    ```
    MYSQL_ROOT_PASSWORD = (MySQL 곹통 루트 비밀번호)
    MYSQL_USER = (MySQL replication을 위한 유저 이름)
    MYSQL_PASSWORD = (MySQL replication을 위한 유저 비밀번호)
    SERVER_PORT = (백엔드 서버 포트)
    ```
    2. `.env.backend` (application.yml 참조 용도)
    ```
    PRIMARY_DB_URL=jdbc:mysql://source-database:3306/streaming_settlement
    PRIMARY_DB_USERNAME=root
    PRIMARY_DB_PASSWORD=(MySQL 루트 비밀번호)
    REPLICA_DB_URL=jdbc:mysql://replica-database:3306/streaming_settlement
    REPLICA_DB_USERNAME=root
    REPLICA_DB_PASSWORD=(MySQL 루트 비밀번호)
    SERVER_PORT=8080
    ```
2. 실행
```
docker compose up -d
```




