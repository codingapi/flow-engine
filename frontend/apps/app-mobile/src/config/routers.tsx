import { createHashRouter, type RouteObject } from "react-router";
import LoginPage from "@/pages/login";
import HomePage from "@/pages/home";


const routers:RouteObject[] = [
    {
        path:'/login',
        element:<LoginPage/>
    },
    {
        path:'/',
        element:<HomePage/>
    }
]


export const hashRouters = createHashRouter(routers);