import api from './api.js';

// Every backend endpoint as a small function. Pages never write URLs themselves,
// so if an endpoint changes there is exactly one place to edit.
const data = (promise) => promise.then((res) => res.data);

export const authApi = {
  register: (body) => data(api.post('/auth/register', body)),
  login: (body) => data(api.post('/auth/login', body)),
  me: () => data(api.get('/auth/me')),
};

export const topicApi = {
  list: () => data(api.get('/topics')),
};

export const questionApi = {
  list: (params) => data(api.get('/questions', { params })),
  get: (id) => data(api.get(`/questions/${id}`)),
  practice: (id, optionId) => data(api.post(`/questions/${id}/practice`, { optionId })),
};

export const bookmarkApi = {
  list: () => data(api.get('/bookmarks')),
  add: (questionId) => api.post(`/bookmarks/${questionId}`),
  remove: (questionId) => api.delete(`/bookmarks/${questionId}`),
};

export const solvedApi = {
  list: () => data(api.get('/solved')),
  mark: (questionId) => api.post(`/solved/${questionId}`),
  unmark: (questionId) => api.delete(`/solved/${questionId}`),
};

export const revisionApi = {
  list: () => data(api.get('/revision-lists')),
  create: (name) => data(api.post('/revision-lists', { name })),
  get: (id) => data(api.get(`/revision-lists/${id}`)),
  remove: (id) => api.delete(`/revision-lists/${id}`),
  addQuestion: (id, questionId) => data(api.post(`/revision-lists/${id}/questions/${questionId}`)),
  removeQuestion: (id, questionId) => data(api.delete(`/revision-lists/${id}/questions/${questionId}`)),
};

export const testApi = {
  list: () => data(api.get('/tests')),
  start: (testId) => data(api.post(`/tests/${testId}/start`)),
  submit: (attemptId, answers) => data(api.post(`/attempts/${attemptId}/submit`, { answers })),
  history: () => data(api.get('/attempts')),
  attempt: (attemptId) => data(api.get(`/attempts/${attemptId}`)),
};

export const submissionApi = {
  record: (body) => data(api.post('/submissions', body)),
  list: (questionId) => data(api.get('/submissions', { params: { questionId } })),
};

export const statsApi = {
  me: () => data(api.get('/stats/me')),
};

export const recommendationApi = {
  list: (limit = 6) => data(api.get('/recommendations', { params: { limit } })),
  weakTopics: () => data(api.get('/recommendations/weak-topics')),
};

export const companyApi = {
  list: () => data(api.get('/companies')),
  get: (id) => data(api.get(`/companies/${id}`)),
};

export const adminApi = {
  dashboard: () => data(api.get('/admin/dashboard')),
  users: () => data(api.get('/admin/users')),
  // topics
  createTopic: (body) => data(api.post('/admin/topics', body)),
  updateTopic: (id, body) => data(api.put(`/admin/topics/${id}`, body)),
  deleteTopic: (id) => api.delete(`/admin/topics/${id}`),
  // questions
  listQuestions: (params) => data(api.get('/admin/questions', { params })),
  getQuestion: (id) => data(api.get(`/admin/questions/${id}`)),
  createQuestion: (body) => data(api.post('/admin/questions', body)),
  updateQuestion: (id, body) => data(api.put(`/admin/questions/${id}`, body)),
  deleteQuestion: (id) => api.delete(`/admin/questions/${id}`),
  // tests
  listTests: () => data(api.get('/admin/tests')),
  createTest: (body) => data(api.post('/admin/tests', body)),
  updateTest: (id, body) => data(api.put(`/admin/tests/${id}`, body)),
  deleteTest: (id) => api.delete(`/admin/tests/${id}`),
  // companies and roadmap steps
  createCompany: (body) => data(api.post('/admin/companies', body)),
  deleteCompany: (id) => api.delete(`/admin/companies/${id}`),
  addStep: (companyId, body) => data(api.post(`/admin/companies/${companyId}/steps`, body)),
  deleteStep: (stepId) => api.delete(`/admin/roadmap-steps/${stepId}`),
};
