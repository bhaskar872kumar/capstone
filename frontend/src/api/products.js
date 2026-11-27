import api from './client.js';

export async function availableProducts() {
  const { data } = await api.get('/products/available');
  return data;
}
