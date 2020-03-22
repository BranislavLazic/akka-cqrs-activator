import React, { useState, useCallback } from 'react';
import { Button, Modal, Form } from 'antd';
import { IssueForm } from './IssueForm';

const IssueModal = ({ visible }) => {
  const [isModalVisible, setModalVisible] = useState(visible);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [form] = Form.useForm();

  const handleAddIssue = () => {
    setModalVisible(true);
  };

  const handleCancel = useCallback(() => {
    setModalVisible(false);
  }, [setModalVisible]);

  const handleSave = useCallback(() => {
    form
      .validateFields()
      .then(values => {
        console.log(values);
        setConfirmLoading(true);
        form.resetFields();
        setTimeout(() => {
          setModalVisible(false);
          setConfirmLoading(false);
        }, 2000);
      })
      .catch(() => {});
  }, [setConfirmLoading, form, setModalVisible]);

  return (
    <>
      <Button onClick={handleAddIssue}>Add issue</Button>
      <Modal
        title="Add issue"
        okText="Save"
        visible={isModalVisible}
        onOk={handleSave}
        onCancel={handleCancel}
        confirmLoading={confirmLoading}
      >
        <IssueForm form={form} />
      </Modal>
    </>
  );
};

export default IssueModal;
