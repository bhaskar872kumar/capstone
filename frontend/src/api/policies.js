import api from './client.js';

export async function myPolicies() {
  const { data } = await api.get('/policies/mine');
  return data;
}

export async function getQuote(productCode) {
  const { data } = await api.get('/registrations/quote', { params: { productCode } });
  return data;
}

export async function registerPolicy(productCode, recommendationItemId) {
  const body = { productCode };
  if (recommendationItemId) body.recommendationItemId = Number(recommendationItemId);
  const { data } = await api.post('/policies', body);
  return data;
}
