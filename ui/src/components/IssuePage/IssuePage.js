import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Col, Row, Button } from 'antd';
import { fetchIssuesByDate, closeIssue, deleteIssue } from './issuesApi';
import { IssueModal } from '../IssueModal';
import { IssueTicket } from './IssueTicket';
import styles from './IssuePage.module.css';
import { PlusOutlined } from '@ant-design/icons';
import BreadcrumbNav from './BreadcrumbNav/BreadcrumbNav';

const IssuePage = ({ eventSource }) => {
  const { date } = useParams();
  const [issues, setIssues] = useState([]);
  const [isModalVisible, setModalVisible] = useState(false);
  const [isSaveButtonLoading, setSaveButtonLoading] = useState(false);

  const [selectedIssue, setSelectedIssue] = useState();

  useEffect(() => {
    if (!isModalVisible) {
      setSelectedIssue(undefined);
    }
  }, [isModalVisible]);

  useEffect(() => {
    eventSource.addEventListener(
      'issue-created',
      ({ data }) => {
        setIssues(currentIssues => [...currentIssues, JSON.parse(data)]);
        setSaveButtonLoading(false);
        setModalVisible(false);
      },
      false,
    );
    eventSource.addEventListener(
      'issue-updated',
      ({ data }) => {
        setIssues(currentIssues =>
          currentIssues.map(issue => {
            const { id, summary, description } = JSON.parse(data);
            return issue.id === id
              ? {
                  ...issue,
                  summary: summary,
                  description: description,
                }
              : issue;
          }),
        );
        setSaveButtonLoading(false);
        setModalVisible(false);
      },
      false,
    );
    eventSource.addEventListener(
      'issue-deleted',
      ({ data }) => {
        setIssues(currentIssues =>
          currentIssues.filter(issue => JSON.parse(data).id !== issue.id),
        );
      },
      false,
    );
    eventSource.addEventListener(
      'issue-closed',
      ({ data }) => {
        setIssues(currentIssues =>
          currentIssues.map(issue =>
            issue.id === JSON.parse(data).id
              ? { ...issue, status: 'CLOSED' }
              : issue,
          ),
        );
      },
      false,
    );
    return () => {
      eventSource.close();
    };
  }, [date, eventSource, setIssues]);

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

  const handleEdit = issue => {
    setSelectedIssue(issue);
    setModalVisible(true);
  };

  const handleClose = id => {
    closeIssue(date, id);
  };

  const handleDelete = id => {
    deleteIssue(date, id);
  };

  return (
    <div className={styles.container}>
      <BreadcrumbNav date={date} className={styles.breadcrumbNav} />
      <Button icon={<PlusOutlined />} type="primary" onClick={toggleModal}>
        Add an issue
      </Button>
      <IssueModal
        issue={selectedIssue}
        visible={isModalVisible}
        handleClose={toggleModal}
        isSaveButtonLoading={isSaveButtonLoading}
        setSaveButtonLoading={setSaveButtonLoading}
      />
      {issues.map((issue, idx) => (
        <Row
          className={styles.ticketRow}
          key={`${idx}-${issue.id}`}
          gutter={[16, 16]}
        >
          <Col span={12}>
            <IssueTicket
              issue={issue}
              handleEdit={handleEdit}
              handleClose={handleClose}
              handleDelete={handleDelete}
            />
          </Col>
        </Row>
      ))}
    </div>
  );
};

export default IssuePage;
