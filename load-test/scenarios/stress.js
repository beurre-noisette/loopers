export const stress = {
  stress_test: {                      // 시나리오 이름 (사용자 정의)
    executor: 'constant-vus',         // 일정한 가상 사용자 수를 유지하는 실행 방식
    vus: 50,                        // Virtual Users 200명 (동시 접속자 200명)
    duration: '1m',                  // 테스트 지속 시간 2분 (시스템 한계 테스트)
  },
};