export const rampup = {
  ramp_up_test: {                     // 시나리오 이름 (사용자 정의)
    executor: 'ramping-vus',          // 가상 사용자 수를 단계적으로 증가/감소시키는 실행 방식
    startVUs: 1,                      // 시작할 때 가상 사용자 1명
    stages: [                         // 단계별 부하 증가 설정
      { duration: '2m', target: 10 }, // 2분 동안 10명까지 증가
      { duration: '2m', target: 20 }, // 2분 동안 20명까지 증가
      { duration: '2m', target: 50 }, // 2분 동안 50명까지 증가
      { duration: '2m', target: 100 },// 2분 동안 100명까지 증가
      { duration: '1m', target: 0 },  // 1분 동안 0명까지 감소 (종료)
    ],
  },
};