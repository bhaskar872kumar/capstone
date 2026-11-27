import api from './client.js';

// The AI call can take a few seconds; the backend gives up after its own timeout and falls back.
export async function getRecommendations(request) {
  const { data } = await api.post('/recommendations', request, { timeout: 45000 });
  return data;
}
