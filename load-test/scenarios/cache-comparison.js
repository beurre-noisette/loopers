// 캐시 성능 비교를 위한 시나리오들

export const cacheBaseline = {
  cache_baseline: {
    executor: 'constant-vus',
    vus: 10,                    // 동시 사용자 10명
    duration: '2m',             // 2분간 테스트
  },
};

export const cacheStress = {
  cache_stress: {
    executor: 'constant-vus',
    vus: 50,                    // 동시 사용자 50명
    duration: '1m',             // 1분간 스트레스 테스트
  },
};

export const cacheRampUp = {
  cache_ramp_up: {
    executor: 'ramping-vus',
    startVUs: 1,                // 1명으로 시작
    stages: [
      { duration: '30s', target: 10 },   // 30초 동안 10명까지 증가
      { duration: '1m', target: 20 },    // 1분 동안 20명까지 증가
      { duration: '30s', target: 50 },   // 30초 동안 50명까지 증가
      { duration: '1m', target: 50 },    // 1분 동안 50명 유지
      { duration: '30s', target: 0 },    // 30초 동안 0명까지 감소
    ],
  },
};