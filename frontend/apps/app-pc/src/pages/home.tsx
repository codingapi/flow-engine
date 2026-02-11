import React from "react";
import { useNavigate } from "react-router";
import { Button, Flex, Space } from "antd";
import {routers} from "@/config/routers.tsx";

const HomePage: React.FC = () => {

    const navigate = useNavigate();

    const routerButtons = routers.filter(item=>item.path!=='/');

    return (
        <div>
            <Flex justify="center"><h1>Flow-Engine Home Page</h1></Flex>
            <Space>
                {routerButtons.map((item)=>{
                    return (
                        <Button
                            onClick={() => {
                                navigate(item.path)
                            }}>
                            {item.name}
                        </Button>
                    )
                })}
            </Space>
        </div>
    )
}

export default HomePage;