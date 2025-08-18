import http from 'k6/http';
import { check, group } from 'k6';
import { baseline } from './scenarios/baseline.js';
import { rampup } from './scenarios/rampup.js';
import { spike } from './scenarios/spike.js';
import { stress } from './scenarios/stress.js';

const BASE = 'http://localhost:8080';

const SCENARIO = __ENV.SCENARIO || 'baseline';

const scenarios = {
  baseline: baseline,
  rampup: rampup,
  spike: spike,
  stress: stress,
};

export const options = {
  scenarios: scenarios[SCENARIO],
};

// 테스트할 상품 ID들 (1~100 범위에서 랜덤)
const PRODUCT_IDS = [1, 2, 3, 4, 5, 10, 20, 30, 50, 100];

export default function () {
  const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
  
  group('캐시 적용 상품 조회', () => {
    const cacheUrl = `${BASE}/api/v1/products/${productId}`;
    const response = http.get(cacheUrl);
    
    check(response, {
      'cache request is 200': (r) => r.status === 200,
      'cache response time < 100ms': (r) => r.timings.duration < 100,
    });
  });
  
  group('캐시 미적용 상품 조회', () => {
    const noCacheUrl = `${BASE}/api/v1/products/${productId}/no-cache`;
    const response = http.get(noCacheUrl);
    
    check(response, {
      'no-cache request is 200': (r) => r.status === 200,
      'no-cache response has data': (r) => {
        try {
          const json = JSON.parse(r.body);
          return json.data && json.data.id;
        } catch (e) {
          return false;
        }
      },
    });
  });
}