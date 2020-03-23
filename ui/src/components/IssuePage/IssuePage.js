import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Breadcrumb, Card, Col, Row, Button } from 'antd';
import { CalendarOutlined } from '@ant-design/icons';
import { fetchIssuesByDate, closeIssue, deleteIssue } from './issuesApi';
import { IssueModal } from '../IssueModal';
import { useSSE } from 'react-hooks-sse';

const IssuePage = () => {
  const { date } = useParams();
  const [issues, setIssues] = useState([]);
  const [isModalVisible, setModalVisible] = useState(false);
  const [isSaveButtonLoading, setSaveButtonLoading] = useState(false);
  const [isDeleteButtonLoading, setDeleteButtonLoading] = useState(false);
  const [issueId, setIssueId] = useState([]);
  const issueCreatedEventSource = useSSE('issue-created');
  const issueDeletedEventSource = useSSE('issue-deleted');

  useEffect(() => {
    if (!isModalVisible) {
      setIssueId(undefined);
    }
  }, [isModalVisible]);

  useEffect(() => {
    if (issueCreatedEventSource && issueCreatedEventSource.data.date === date) {
      setIssues(issue => [...issue, issueCreatedEventSource.data]);
      setSaveButtonLoading(false);
      setModalVisible(false);
    }
    if (issueDeletedEventSource && issueDeletedEventSource.data.date === date) {
      setIssues(issue =>
        issue.filter(i => i.id !== issueDeletedEventSource.data.id),
      );
      setDeleteButtonLoading(false);
    }
  }, [issueCreatedEventSource, issueDeletedEventSource, date]);

  useEffect(() => {
    if (date) {
      (async () => {
        try {
          const { data: issuesData } = await fetchIssuesByDate(date);
          setIssues(issuesData);
        } catch (error) {
          console.error(error);
        }
      })();
    }
  }, [date]);

  const toggleModal = () => {
    setModalVisible(!isModalVisible);
  };

  const handleEdit = id => {
    setIssueId(id);
    setModalVisible(true);
  };

  const handleClose = id => {
    closeIssue(date, id);
  };

  const handleDelete = id => {
    setDeleteButtonLoading(true);
    deleteIssue(date, id);
  };

  return (
    <>
      <Breadcrumb>
        <Breadcrumb.Item key="home">
          <Link to="/">
            <CalendarOutlined /> <span>Calendar</span>
          </Link>
        </Breadcrumb.Item>
        <Breadcrumb.Item>
          <span>{date}</span>
        </Breadcrumb.Item>
      </Breadcrumb>
      <Button onClick={toggleModal}>Add issue</Button>
      <IssueModal
        id={issueId}
        visible={isModalVisible}
        handleClose={toggleModal}
        isSaveButtonLoading={isSaveButtonLoading}
        setSaveButtonLoading={setSaveButtonLoading}
      />
      {issues.map(({ id, summary, description, status }) => (
        <Row key={id} gutter={[16, 16]}>
          <Col span={12}>
            <Card title={summary}>
              <p>{description}</p>
              {status === 'OPENED' && (
                <>
                  <Button type="primary" onClick={() => handleEdit(id)}>
                    Edit
                  </Button>
                  <Button onClick={() => handleClose(id)}>Close</Button>
                </>
              )}
              {status === 'CLOSED' && (
                <Button
                  onClick={() => handleDelete(id)}
                  loading={isDeleteButtonLoading}
                >
                  Delete
                </Button>
              )}
            </Card>
          </Col>
        </Row>
      ))}
    </>
  );
};

export default IssuePage;
