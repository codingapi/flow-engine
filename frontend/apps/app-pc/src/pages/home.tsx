import React from "react";
import { useNavigate } from "react-router";
import { Button, Space } from "antd";

const HomePage: React.FC = () => {

    const navigate = useNavigate();

    return (
        <Space>
            <Button onClick={() => {
                navigate("/login")
            }}>login</Button>
            <Button>desion</Button>
            <Button>todo</Button>
        </Space>
    )
}

export default HomePage;