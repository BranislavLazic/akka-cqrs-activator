import React, { useState, useEffect } from 'react';
import { Card, Button } from 'antd';
import { EditOutlined, CloseOutlined, DeleteOutlined } from '@ant-design/icons';
import styles from './IssueTicket.module.css';

const IssueTicket = ({ issue, handleEdit, handleClose, handleDelete }) => {
  const { id, summary, description, status } = issue;
  const [isCloseButtonLoading, setCloseButtonLoading] = useState(false);
  const [isDeleteButtonLoading, setDeleteButtonLoading] = useState(false);

  useEffect(() => {
    setCloseButtonLoading(false);
    console.log(issue);
  }, [issue]);

  const handleOnClose = () => {
    setCloseButtonLoading(true);
    handleClose(id);
  };

  const handleOnDelete = () => {
    setDeleteButtonLoading(true);
    handleDelete(id);
  };

  return (
    <Card title={summary}>
      <p>{description}</p>

      {status === 'OPENED' && (
        <>
          {status !== 'CLOSED' && (
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => handleEdit(issue)}
            >
              Edit
            </Button>
          )}
          <Button
            className={styles.ticketButton}
            icon={<CloseOutlined />}
            loading={isCloseButtonLoading}
            onClick={handleOnClose}
          >
            Close
          </Button>
        </>
      )}
      <Button
        type="danger"
        className={styles.ticketButton}
        icon={<DeleteOutlined />}
        loading={isDeleteButtonLoading}
        onClick={handleOnDelete}
      >
        Delete
      </Button>
    </Card>
  );
};

export default IssueTicket;
