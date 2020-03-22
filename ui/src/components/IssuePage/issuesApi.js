import axios from 'axios';
const baseUrl = 'http://localhost:8000';

export const fetchIssuesByDate = async date => {
  return await axios.get(baseUrl + `/issues/${date}`);
};

export const createIssue = async issue => {
  return await axios.post(baseUrl + '/issues', issue);
};
