import { api } from '../auth/AuthContext.jsx';

// ---- projects ----
export async function createProject(payload) {
  const res = await api.post('/projects', payload);
  return res.data;
}
export async function listProjects(params = {}) {
  const res = await api.get('/projects', { params });
  return res.data.content || res.data;
}
export async function getProject(id) {
  const res = await api.get(`/projects/${id}`);
  return res.data;
}
export async function renameProject(id, name, description) {
  const res = await api.put(`/projects/${id}`, { name, description });
  return res.data;
}
export async function duplicateProject(id) {
  const res = await api.post(`/projects/${id}/duplicate`);
  return res.data;
}
export async function archiveProject(id, archived) {
  const res = await api.put(`/projects/${id}`, { archived });
  return res.data;
}
export async function deleteProject(id) {
  await api.delete(`/projects/${id}`);
}
export function fileTreeUrl(id) { return `/projects/${id}/file-tree`; }
export async function getFileTree(id) {
  const res = await api.get(`/projects/${id}/file-tree`);
  return res.data;
}

// ---- files ----
export async function readFile(projectId, path) {
  const res = await api.get(`/projects/${projectId}/files`, { params: { path } });
  return res.data;
}
export async function writeFile(projectId, path, content) {
  const res = await api.post(`/projects/${projectId}/files`, { path, content });
  return res.data;
}
export async function mkdir(projectId, path) {
  const res = await api.post(`/projects/${projectId}/files/mkdir`, { path });
  return res.data;
}
export async function searchCode(projectId, query) {
  const res = await api.get(`/projects/${projectId}/files/search`, { params: { query } });
  return res.data;
}

// ---- ai conversations ----
export async function listConversations(projectId) {
  const res = await api.get(`/projects/${projectId}/ai/conversations`);
  return res.data;
}
export async function createConversation(projectId) {
  const res = await api.post(`/projects/${projectId}/ai/conversations`, {});
  return res.data;
}
export async function getMessages(projectId, conversationId) {
  const res = await api.get(`/projects/${projectId}/ai/conversations/${conversationId}/messages`);
  return res.data;
}
export async function getActions(projectId) {
  const res = await api.get(`/projects/${projectId}/ai/actions`);
  return res.data;
}
export async function approveAction(projectId, conversationId, actionId) {
  const res = await api.post(`/projects/${projectId}/ai/conversations/${conversationId}/approve/${actionId}`);
  return res.data;
}
export async function cancelAction(projectId, conversationId, actionId) {
  const res = await api.post(`/projects/${projectId}/ai/conversations/${conversationId}/cancel/${actionId}`);
  return res.data;
}

// ---- terminal ----
export async function runTerminal(projectId, command) {
  const res = await api.post(`/projects/${projectId}/terminal/execute`, { command });
  return res.data;
}
export async function terminalHistory(projectId) {
  const res = await api.get(`/projects/${projectId}/terminal/history`);
  return res.data;
}

// ---- build/test ----
export async function runBuild(projectId, target = 'all', attempt = 1) {
  const res = await api.post(`/projects/${projectId}/build/run`, { target, attempt });
  return res.data;
}
export async function lastBuild(projectId) {
  const res = await api.get(`/projects/${projectId}/build/last`);
  return res.data;
}
export async function buildHistory(projectId) {
  const res = await api.get(`/projects/${projectId}/build/history`);
  return res.data;
}
export async function runTests(projectId, target = 'all') {
  const res = await api.post(`/projects/${projectId}/build/tests/run`, { target });
  return res.data;
}

// ---- preview ----
export async function startPreview(projectId, command) {
  const res = await api.post(`/projects/${projectId}/build/preview/start`, command ? { command } : {});
  return res.data;
}
export async function stopPreview(projectId) {
  const res = await api.post(`/projects/${projectId}/build/preview/stop`);
  return res.data;
}
export async function previewStatus(projectId) {
  const res = await api.get(`/projects/${projectId}/build/preview/status`);
  return res.data;
}

// ---- git ----
export async function gitStatus(projectId) {
  const res = await api.get(`/projects/${projectId}/git/status`);
  return res.data;
}
export async function gitDiff(projectId) {
  const res = await api.get(`/projects/${projectId}/git/diff`);
  return res.data;
}
export async function gitLog(projectId) {
  const res = await api.get(`/projects/${projectId}/git/log`);
  return res.data;
}
export async function gitCommit(projectId, message) {
  const res = await api.post(`/projects/${projectId}/git/commit`, { message });
  return res.data;
}

// ---- admin ----
export async function adminOverview() {
  const res = await api.get('/admin/overview');
  return res.data;
}
export async function adminUsers(page = 0) {
  const res = await api.get('/admin/users', { params: { page } });
  return res.data;
}
export async function adminSetRole(id, role) {
  const res = await api.put(`/admin/users/${id}/role`, { role });
  return res.data;
}
export async function adminDeleteUser(id) {
  await api.delete(`/admin/users/${id}`);
}
export async function adminProjects(page = 0) {
  const res = await api.get('/admin/projects', { params: { page } });
  return res.data;
}
export async function adminAiActions(size = 50) {
  const res = await api.get('/admin/ai/actions', { params: { size } });
  return res.data;
}

// ---- templates ----
export async function listTemplates() {
  const res = await api.get('/templates');
  return res.data;
}
