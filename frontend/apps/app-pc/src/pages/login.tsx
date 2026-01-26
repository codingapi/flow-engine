import { login } from "@/api/login";
import { Button, Flex, Form, Input, message } from "antd";
import React from "react";
import { useNavigate } from "react-router";

const LoginPage: React.FC = () => {

    const navigate = useNavigate();

    const [form] = Form.useForm();

    return (
        <Flex justify="center">
            <div style={{
                width: 400
            }}>
                <Flex justify="center"><h1>Login Page</h1></Flex>
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={(body) => {
                        login(body).then(res => {
                            if (res.success) {
                                const token = res.data.token;
                                localStorage.setItem('token', token);
                                message.success('login success');
                                navigate('/');
                            }else{
                                message.error(res.errMessage);
                            }
                        })   
                    }}>
                    <Form.Item name={"username"} label="username:">
                        <Input />
                    </Form.Item>
                    <Form.Item name={"password"} label="password:">
                        <Input />
                    </Form.Item>
                </Form>
                <Flex justify="center" style={{ marginTop: 10 }}>
                    <Button block type="primary" onClick={() => { form.submit() }}>Login</Button>
                </Flex>
            </div>
        </Flex>
    )
}

export default LoginPage;