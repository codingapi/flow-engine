import { httpClient } from "."

export const login = (body:any) =>{
    return httpClient.post('/user/login',body);
}