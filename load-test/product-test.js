import http from 'k6/http';
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

export default function () {
  // const sort = 'LIKES_DESC';
  // const url = `${BASE}/api/v1/products?sort=${sort}`;
   const url = `${BASE}/api/v1/test`;
  http.get(url);
}