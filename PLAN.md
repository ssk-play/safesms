1. 프로젝트 개요
   앱 이름: SafeSms

목표: 자녀가 SMS 메시지 내 외부 링크를 무분별하게 접속하는 것을 방지하고, 부모가 허용한 웹사이트만 인앱 브라우저를 통해 안전하게 이용하도록 돕는 SMS 앱 개발

타겟 사용자: 자녀의 스마트폰 웹 접근 관리가 필요한 부모

2. 핵심 기능
   SMS 기본 기능:

문자 메시지 수신, 발신 및 전체 대화 목록 조회

앱 사용자 간 무료 메시징 (Firebase 기반):

SafeSms 앱을 설치한 사용자끼리는 데이터 통신을 이용해 무료로 메시지 송수신 (SMS 요금 미부과)

Firestore를 이용한 실시간 채팅 기능 구현

Firebase Storage를 이용한 사진 등 파일 첨부 기능

URL 링크 제어:

메시지 내 모든 URL 링크 클릭 시, 외부 브라우저 실행을 차단하고 내부 필터로 전달

URL 화이트리스트 필터:

부모가 암호로 보호된 설정 화면에서 접속을 허용할 웹사이트 도메인 목록 관리 (추가/삭제)

클릭된 URL이 허용 목록에 없으면 "허용되지 않은 링크" 알림 후 차단

인앱 브라우저 (WebView):

허용된 URL만 앱 내장 브라우저로 실행

주소창을 제공하며, 주소창에 입력된 URL 또한 화이트리스트 필터의 검사를 받도록 구현

3. 개발 단계 (로드맵)
   Phase 1: 기본 SMS 기능 구현

SMS 읽기/쓰기 권한 획득 및 메시지 목록, 대화창 UI 개발

Phase 2: Firebase 기반 무료 메시징 구현

Firebase 프로젝트 설정 및 사용자 인증 시스템 구축

Firestore를 이용한 실시간 메시징 기능 개발

상대방이 앱 사용자인지 판별하여 SMS/무료 메시지로 자동 전환하는 로직 구현

Phase 3: 링크 제어 및 필터 기능 개발

메시지 내 URL 클릭 이벤트 가로채기 기능 구현

부모용 설정 화면 및 URL 허용 목록 저장/관리 기능 개발 (SharedPreferences 또는 Room DB)

Phase 4: 인앱 브라우저 연동

WebView 기반의 인앱 브라우저 화면 개발

URL 필터 결과에 따라 허용된 링크만 WebView로 로드하는 로직 구현

Phase 5: 안정화 및 배포

UI/UX 개선, 기능 테스트 및 버그 수정

구글 플레이 스토어 배포 준비

4. 주요 기술 스택
   플랫폼: Android (Kotlin 또는 Java)

최소 SDK 버전: API 26 (Android 8.0 Oreo)

백엔드: Firebase (Firestore, Authentication, Cloud Storage)

UI: RecyclerView, WebView, TextView(URLSpan 커스텀)

데이터 저장: SharedPreferences (간단한 목록), Room DB (확장성 고려 시)

5. 기대 효과
   자녀는 SMS를 통한 필수적인 소통을 유지하면서 유해 웹사이트 노출 위험은 감소

부모는 자녀의 스마트폰 사용 환경을 더 효과적으로 관리하며 안심감 획득

앱 사용자 간 통신 비용 절감 효과