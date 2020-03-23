import axios from 'axios';
export const baseUrl = 'http://localhost:8000/issues';

export const fetchIssuesByDate = async date => {
  return await axios.get(`${baseUrl}/${date}`);
};

export const createIssue = async issue => {
  return await axios.post(baseUrl, issue);
};

export const closeIssue = async (date, id) => {
  await axios.put(`${baseUrl}/${date}/${id}`);
};

export const deleteIssue = async (date, id) => {
  await axios.delete(`${baseUrl}/${date}/${id}`);
};
