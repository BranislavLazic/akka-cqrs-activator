import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Breadcrumb, Card, Col, Row, Button } from 'antd';
import { CalendarOutlined } from '@ant-design/icons';
import { fetchIssuesByDate, subscribeToIssuesStream } from './issuesApi';
import { IssueModal } from '../IssueModal';

const IssuePage = () => {
  const { date } = useParams();
  const [issues, setIssues] = useState([]);
  const [isModalVisible, setModalVisible] = useState(false);
  const [isSaveButtonLoading, setSaveButtonLoading] = useState(false);

  useEffect(() => {
    if (issues.length) {
      const issueCreatedEventStream = subscribeToIssuesStream(
        'issue-created',
        event => {
          setIssues([...issues, JSON.parse(event.data)]);
          setSaveButtonLoading(false);
          setModalVisible(false);
        },
      );
      return () => {
        issueCreatedEventStream.close();
      };
    }
  }, [issues]);

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

  return (
    <div>
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
        visible={isModalVisible}
        handleClose={toggleModal}
        isSaveButtonLoading={isSaveButtonLoading}
        setSaveButtonLoading={setSaveButtonLoading}
      />
      {issues.map(({ id, summary, description }) => (
        <Row key={id} gutter={[16, 16]}>
          <Col span={12}>
            <Card title={summary}>
              <p>{description}</p>
            </Card>
          </Col>
        </Row>
      ))}
    </div>
  );
};

export default IssuePage;
