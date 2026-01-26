import { createHashRouter, type RouteObject } from "react-router";
import LoginPage from "@/pages/login";
import HomePage from "@/pages/home";
import DesignPage from "@/pages/desgin";
import TodoPage from "@/pages/todo";


const routers:RouteObject[] = [
    {
        path:'/login',
        element:<LoginPage/>
    },
    {
        path:'/design',
        element:<DesignPage/>
    },
    {
        path:'/todo',
        element:<TodoPage/>
    },
    {
        path:'/',
        element:<HomePage/>
    }
]


export const hashRouters = createHashRouter(routers);