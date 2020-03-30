import React, { useCallback, useState } from 'react';
import { Calendar as AntdCalendar, Layout, Typography } from 'antd';
import { Redirect } from 'react-router-dom';
import styles from './Calendar.module.css';

const { Content } = Layout;
const { Title } = Typography;

const Calendar = () => {
  const [currentDate, setCurrentDate] = useState();
  const [isRedirectToIssuePage, setRedirectToIssuePage] = useState(false);

  const onDateSelect = useCallback(
    selectedDate => {
      setCurrentDate(selectedDate.format('YYYY-MM-DD'));
      setRedirectToIssuePage(true);
    },
    [setCurrentDate, setRedirectToIssuePage],
  );

  return isRedirectToIssuePage ? (
    <Redirect push to={`/issues/${currentDate}`} />
  ) : (
    <div className={styles.container}>
      <Content>
        <Title>Issue tracker</Title>
        <AntdCalendar onSelect={onDateSelect} />
      </Content>
    </div>
  );
};

export default Calendar;
