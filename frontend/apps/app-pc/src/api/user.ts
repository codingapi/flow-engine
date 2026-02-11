import { httpClient } from "."

export const list = (request: any) => {
    return httpClient.page('/api/user/list', request, {}, {}, []);
}

export const remove = (id:string) => {
    return httpClient.post('/api/user/remove',{id});
}

export const save = (body:any) => {
    return httpClient.post('/api/user/save',body);
}