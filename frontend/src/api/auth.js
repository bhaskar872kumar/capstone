import api from './client.js';

/** Is there a logged-in session? Also gives the browser its CSRF cookie. */
export async function session() {
  const { data } = await api.get('/auth/session');
  return data;
}

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password });
  return data;
}

export async function logout() {
  await api.post('/auth/logout');
}

export async function me() {
  const { data } = await api.get('/me');
  return data;
}
