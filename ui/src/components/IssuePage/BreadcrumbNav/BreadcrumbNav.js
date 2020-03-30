import React from 'react';
import { Breadcrumb } from 'antd';
import { Link } from 'react-router-dom';
import { CalendarOutlined } from '@ant-design/icons';

const BreadcrumbNav = ({ date, className }) => (
  <div className={className}>
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
  </div>
);

export default BreadcrumbNav;
