import React, { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Breadcrumb, Card, Col, Row } from "antd";
import { CalendarOutlined } from "@ant-design/icons";
import { fetchIssuesByDate } from "./issuesApi";

const IssuePage = () => {
  const { date } = useParams();
  const [issues, setIssues] = useState([]);
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
