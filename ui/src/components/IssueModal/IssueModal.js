import React, { useCallback, useEffect } from 'react';
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
  issue,
  visible,
  handleClose,
  isSaveButtonLoading,
  setSaveButtonLoading,
}) => {
  const [form] = Form.useForm();
  const { date } = useParams();

  useEffect(() => {
    if (issue) {
      form.setFieldsValue(issue);
    }
  }, [issue, form]);

  const handleResetFormClose = () => {
    form.resetFields();
    handleClose();
  };

  const handleSave = useCallback(() => {
    form
      .validateFields()
      .then(values => {
        setSaveButtonLoading(true);
        if (issue?.id) {
        } else {
          createIssue(mapValuesToIssue(date, values));
        }
        form.resetFields();
      })
      .catch(() => {});
  }, [form, date, setSaveButtonLoading, issue]);

  return (
    <>
      <Modal
        getContainer={false}
        title={issue?.id ? 'Edit issue' : 'Add issue'}
        okText="Save"
        visible={visible}
        onOk={handleSave}
        onCancel={handleResetFormClose}
        confirmLoading={isSaveButtonLoading}
      >
        <IssueForm form={form} />
      </Modal>
    </>
  );
};

export default IssueModal;
