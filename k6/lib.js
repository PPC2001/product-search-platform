import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8085';

export const QUERIES = ['iphone', 'samsung', 'laptop', 'shoes', 'tshirt', 'headphones', 'watch', 'camera'];
export const CATEGORIES = ['Mobiles', 'Electronics', 'Fashion', 'Footwear', 'Accessories'];

export const writeCreated = new Counter('products_created');

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

export function searchProducts() {
  const params = [`q=${encodeURIComponent(pick(QUERIES))}`, 'page=0', 'size=10'];
  if (Math.random() < 0.5) params.push(`category=${encodeURIComponent(pick(CATEGORIES))}`);
  if (Math.random() < 0.3) {
    params.push('minPrice=100', 'maxPrice=100000', 'sortBy=price', 'sortDirection=asc');
  }
  const res = http.get(`${BASE_URL}/api/products/search?${params.join('&')}`, {
    tags: { name: 'GET /api/products/search' },
  });
  check(res, {
    'search status is 200': (r) => r.status === 200,
    'search returns products array': (r) => {
      try { return Array.isArray(r.json('products')); } catch (e) { return false; }
    },
  });
  return res;
}

export function fuzzySearch() {
  const res = http.get(`${BASE_URL}/api/products/fuzzy?q=${encodeURIComponent(pick(QUERIES))}`, {
    tags: { name: 'GET /api/products/fuzzy' },
  });
  check(res, { 'fuzzy status is 200': (r) => r.status === 200 });
  return res;
}

export function createProduct() {
  const unique = `${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    productName: `K6 Load Test Product ${unique}`,
    description: 'Created by k6 load test',
    category: pick(CATEGORIES),
    price: Math.round((100 + Math.random() * 99900) * 100) / 100,
    stockQuantity: Math.floor(Math.random() * 500),
    sku: `K6-${unique}`,
    active: 'Y',
  });
  const res = http.post(`${BASE_URL}/api/products`, payload, {
    ...JSON_HEADERS,
    tags: { name: 'POST /api/products' },
  });
  const ok = check(res, {
    'create status is 201': (r) => r.status === 201,
    'create returns productId': (r) => {
      try { return r.json('productId') > 0; } catch (e) { return false; }
    },
  });
  if (ok) writeCreated.add(1);
  return res;
}

export function updateProduct(productId) {
  const payload = JSON.stringify({
    productName: `K6 Updated Product ${productId}`,
    description: 'Updated by k6 load test',
    category: pick(CATEGORIES),
    price: Math.round((100 + Math.random() * 99900) * 100) / 100,
    stockQuantity: Math.floor(Math.random() * 500),
    sku: `K6-UPD-${productId}`,
    active: 'Y',
  });
  const res = http.put(`${BASE_URL}/api/products/${productId}`, payload, {
    ...JSON_HEADERS,
    tags: { name: 'PUT /api/products/{id}' },
  });
  check(res, { 'update status is 200': (r) => r.status === 200 });
  return res;
}

export function healthCheck() {
  const res = http.get(`${BASE_URL}/actuator/health`, { tags: { name: 'GET /actuator/health' } });
  check(res, { 'health is UP': (r) => r.status === 200 });
  return res;
}
