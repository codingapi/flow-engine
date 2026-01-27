import { httpClient } from ".";

export const list = (request: any) => {
    return httpClient.page('/api/query/workflow/list', request, {}, {}, []);
}

export const remove = (id:string) => {
    return httpClient.post('/api/cmd/workflow/remove',{id});
}

export const create = () => {
    return httpClient.post('/api/cmd/workflow/create',{});
}

export const save = (body:any) => {
    return httpClient.post('/api/cmd/workflow/save',body);
}

export const load = (id:string) => {
    return httpClient.get('/api/cmd/workflow/load',{id});
}