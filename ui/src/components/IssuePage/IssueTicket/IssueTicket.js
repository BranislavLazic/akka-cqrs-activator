import React from 'react';
import { Card, Button } from 'antd';

const IssueTicket = ({ issue, handleEdit, handleClose, handleDelete }) => {
  const { id, summary, description, status } = issue;
  return (
    <Card title={summary}>
      <p>{description}</p>
      {status === 'OPENED' && (
        <>
          <Button type="primary" onClick={() => handleEdit(issue)}>
            Edit
          </Button>
          <Button onClick={() => handleClose(id)}>Close</Button>
        </>
      )}
      {/* {status === 'CLOSED' && ( */}
      <Button onClick={() => handleDelete(id)}>Delete</Button>
      {/* )} */}
    </Card>
  );
};

export default IssueTicket;
