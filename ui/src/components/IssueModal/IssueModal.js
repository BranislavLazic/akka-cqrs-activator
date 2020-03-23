import React, { useCallback } from 'react';
import { Modal, Form } from 'antd';
import { IssueForm } from './IssueForm';
import { useParams } from 'react-router-dom';
import { createIssue } from '../IssuePage/issuesApi';

const mapValuesToIssue = (date, { summary, description }) => ({
  date: date,
  summary: summary,
  description: description,
});

const IssueModal = ({
  id,
  visible,
  handleClose,
  isSaveButtonLoading,
  setSaveButtonLoading,
}) => {
  const [form] = Form.useForm();
  const { date } = useParams();

  const handleSave = useCallback(() => {
    form
      .validateFields()
      .then(values => {
        setSaveButtonLoading(true);
        createIssue(mapValuesToIssue(date, values));
        form.resetFields();
      })
      .catch(() => {});
  }, [form, date, setSaveButtonLoading]);

  return (
    <>
      <Modal
        title={id ? 'Edit issue' : 'Add issue'}
        okText="Save"
        visible={visible}
        onOk={handleSave}
        onCancel={handleClose}
        confirmLoading={isSaveButtonLoading}
      >
        <IssueForm form={form} />
      </Modal>
    </>
  );
};

export default IssueModal;
