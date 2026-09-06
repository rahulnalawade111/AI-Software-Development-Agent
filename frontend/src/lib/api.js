import { api } from '../auth/AuthContext.jsx';

export async function createProject(payload) {
  const res = await api.post('/projects', payload);
  return res.data;
}

export async function listProjects() {
  const res = await api.get('/projects');
  return res.data.content || res.data;
}
