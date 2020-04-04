import React from 'react';
import { Form, Input } from 'antd';

const { TextArea } = Input;

const layout = {
  labelCol: {
    span: 8,
  },
  wrapperCol: {
    span: 16,
  },
};

const IssueForm = ({ form }) => {
  return (
    <Form {...layout} form={form} name="issueForm">
      <Form.Item
        label="Summary"
        name="summary"
        rules={[
          { required: true, message: 'Enter summary' },
          { min: 2, message: 'Enter at least two characters' },
        ]}
      >
        <Input />
      </Form.Item>
      <Form.Item label="Description" name="description">
        <TextArea />
      </Form.Item>
    </Form>
  );
};

export default IssueForm;
