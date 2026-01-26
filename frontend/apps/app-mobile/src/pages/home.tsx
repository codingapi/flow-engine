import React from "react";
import { useNavigate } from "react-router";


const HomePage:React.FC = ()=>{

    const navigate = useNavigate();

    return (
        <div>
            <button onClick={()=>{
                navigate("/login")
            }}>login</button>
            <button>desion</button>
            <button>todo</button>
        </div>
    )
}

export default HomePage;