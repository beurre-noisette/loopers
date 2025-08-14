export const baseline = {
  baseline_test: {                    // 시나리오 이름 (사용자 정의)
    executor: 'constant-vus',         // 일정한 가상 사용자 수를 유지하는 실행 방식
    vus: 5,                          // Virtual Users 5명 (동시 접속자 5명)
    duration: '2m',                  // 테스트 지속 시간 2분
  },
};