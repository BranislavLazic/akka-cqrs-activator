import React, { useState, useEffect } from 'react';
import { Card, Button } from 'antd';

const IssueTicket = ({ issue, handleEdit, handleClose, handleDelete }) => {
  const { id, summary, description, status } = issue;
  const [isCloseButtonLoading, setCloseButtonLoading] = useState(false);
  const [isDeleteButtonLoading, setDeleteButtonLoading] = useState(false);

  useEffect(() => {
    setCloseButtonLoading(false);
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
            <Button type="primary" onClick={() => handleEdit(issue)}>
              Edit
            </Button>
          )}
          <Button loading={isCloseButtonLoading} onClick={handleOnClose}>
            Close
          </Button>
        </>
      )}
      <Button loading={isDeleteButtonLoading} onClick={handleOnDelete}>
        Delete
      </Button>
    </Card>
  );
};

export default IssueTicket;
