import React from "react";
import { useNavigate } from "react-router";
import { Button, Flex, Space } from "antd";

const HomePage: React.FC = () => {

    const navigate = useNavigate();

    return (
        <div>
            <Flex justify="center"><h1>Home Page</h1></Flex>
            <Space>
                <Button
                    onClick={() => {
                        navigate("/login")
                    }}>login</Button>
                <Button
                    onClick={() => {
                        navigate("/design")
                    }}>design</Button>
                <Button
                    onClick={() => {
                        navigate('/todo');
                    }}
                >todo</Button>
            </Space>
        </div>
    )
}

export default HomePage;