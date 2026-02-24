import { httpClient } from ".";

export const detail = (id:string) => {
    return httpClient.get('/api/cmd/record/detail',{id});
}

export const processNodes = (body:any) => {
    return httpClient.post('/api/cmd/record/processNodes',body);
}

export const create = (body:any) => {
    return httpClient.post('/api/cmd/record/create',body);
}

export const action = (body:any) => {
    return httpClient.post('/api/cmd/record/action',body);
}