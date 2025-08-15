export const spike = {
  spike_test: {                       // 시나리오 이름 (사용자 정의)
    executor: 'ramping-arrival-rate', // 초당 요청 수를 단계적으로 증가/감소시키는 실행 방식
    startRate: 1,                     // 시작 요청률 (초당 1개 요청)
    timeUnit: '1s',                   // 요청률 단위 (1초)
    preAllocatedVUs: 50,              // 미리 할당할 가상 사용자 수 (테스트 시작 시 준비)
    maxVUs: 200,                      // 최대 생성 가능한 가상 사용자 수 (필요시 추가 생성)
    stages: [                         // 단계별 트래픽 급증 설정
      { duration: '1m', target: 10 }, // 1분 동안 초당 10개 요청까지 증가
      { duration: '30s', target: 100 }, // 30초 동안 초당 100개 요청으로 급증 (스파이크)
      { duration: '1m', target: 10 }, // 1분 동안 초당 10개 요청으로 감소
    ],
  },
};